/*
 * thread.c
 * Thread support.
 *
 * Copyright (c) 1996, 1997
 *	Transvirtual Technologies, Inc.  All rights reserved.
 *
 * See the file "license.terms" for information on usage and redistribution 
 * of this file. 
 */

#include "config.h"
#include "debug.h"
#include "config-std.h"
#include "config-mem.h"
#include "config-io.h"
#include "config-signal.h"
#include "jtypes.h"
#include "access.h"
#include "object.h"
#include "constants.h"
#include "errors.h"
#include "classMethod.h"
#include "baseClasses.h"
#include "stringSupport.h"
#include "lookup.h"
#include "thread.h"
#include "jthread.h"
#include "locks.h"
#include "exception.h"
#include "support.h"
#include "external.h"
#include "gc.h"
#include "jni.h"
#include "md.h"

/* If not otherwise specified, assume at least 1MB for main thread */
#ifndef MAINSTACKSIZE
#define MAINSTACKSIZE (1024*1024)
#endif

/* 
 * store the native thread for the main thread here 
 * between init and createFirst 
 */
static jthread_t	mainthread;
static int threadStackSize;	/* native stack size */

/* referenced by native/Runtime.c */
jbool runFinalizerOnExit;	/* should we run finalizers? */

jbool deadlockDetection = 1;	/* abort if we detect deadlock */

Hjava_lang_Class* ThreadClass;
Hjava_lang_Class* ThreadGroupClass;
Hjava_lang_ThreadGroup* standardGroup;

static void firstStartThread(void*);
static void createInitialThread(const char*);
static void runfinalizer(void);
static iLock* thread_start_lock;

/*
 * How do I get memory?
 */
static
void *
thread_malloc(size_t s)
{
	return gc_malloc(s, GC_ALLOC_THREADCTX);
}

static
void
thread_free(void *p)
{
	gc_free(p);
}

/*
 * Initialise threads.
 */
void
initThreads(void)
{
	errorInfo info;

	/* Get a handle on the thread and thread group classes */
	ThreadClass = lookupClass(THREADCLASS, &info);
	assert(ThreadClass != 0);
	ThreadGroupClass = lookupClass(THREADGROUPCLASS, &info);
	assert(ThreadGroupClass != 0);

	/* Create base group */
	standardGroup = (Hjava_lang_ThreadGroup*)newObject(ThreadGroupClass);

	assert(standardGroup != 0);
	unhand(standardGroup)->parent = 0;
	unhand(standardGroup)->name = stringC2Java("main");
	assert(unhand(standardGroup)->name);
	unhand(standardGroup)->maxPriority = java_lang_Thread_MAX_PRIORITY;
	unhand(standardGroup)->destroyed = 0;
	unhand(standardGroup)->daemon = 0;
	unhand(standardGroup)->nthreads = 0;
	unhand(standardGroup)->threads = (HArrayOfObject*)newArray(ThreadClass, 0);
	unhand(standardGroup)->ngroups = 0;
	unhand(standardGroup)->groups = (HArrayOfObject*)newArray(ThreadGroupClass, 0);


	/* Allocate a thread to be the main thread */
	createInitialThread("main");
}

static int
createThread(Hjava_lang_Thread* tid, void* func, size_t stacksize,
	     struct _errorInfo *einfo)
{
	struct Hkaffe_util_Ptr* nativethread;

	nativethread = (struct Hkaffe_util_Ptr*)jthread_create(
		unhand(tid)->priority,
		func,
		unhand(tid)->daemon,
		tid,
		stacksize);

	if (!nativethread) {
		postOutOfMemory(einfo);
		return 0;
	}
		
	unhand(tid)->PrivateInfo = nativethread;
	/* preallocate a stack overflow error for this thread in case it 
	 * runs out 
	 */
	unhand(tid)->stackOverflowError = 
		(Hjava_lang_Throwable*)StackOverflowError;
	unhand(tid)->needOnStack = STACK_HIGH;
	return 1;
}

static
void
initThreadLock(Hjava_lang_Thread* tid)
{
#if defined(SETUP_POSIX_LOCKS)
	sem2posixLock* lk;

	unhand(tid)->sem = thread_malloc(sizeof(sem2posixLock));
	lk = (sem2posixLock*)unhand(tid)->sem;
	SETUP_POSIX_LOCKS(lk);
#endif
}

/*
 * Start a new thread running.
 */
void
startThread(Hjava_lang_Thread* tid)
{
	int success;
	struct _errorInfo info;
	int iLockRoot;

	initThreadLock(tid);
	
#if 0
	if (aliveThread(tid) == true) {
		throwException(IllegalThreadStateException);
	}
#endif

	/* Hold the start lock while the thread is created.
	 * This lock prevents the new thread from running until we're
	 * finished in create.
	 * See also firstStartThread.
	 */
	lockStaticMutex(&thread_start_lock);

	success = createThread(tid, &firstStartThread,
			       threadStackSize, &info);

	unlockStaticMutex(&thread_start_lock);
	if (!success) {
		throwError(&info);
	}
}

/*
 * Interrupt a thread
 */
void
interruptThread(Hjava_lang_Thread* tid)
{
	if ((jthread_t)unhand(tid)->PrivateInfo) {
		jthread_interrupt((jthread_t)unhand(tid)->PrivateInfo);
	}
}

/*
 * Stop a thread from running and terminate it.
 */
void
stopThread(Hjava_lang_Thread* tid, Hjava_lang_Object* obj)
{
	if (getCurrentThread() == tid) {
		throwException((Hjava_lang_Throwable*)obj);
	}
	else {
		/*
		 * Note that we deviate from the spec here in that the target 
		 * thread won't throw the exception `obj', but it will 
		 * construct a new ThreadDeath exception when it dies.
		 */
		if ((jthread_t)unhand(tid)->PrivateInfo)
			jthread_stop((jthread_t)unhand(tid)->PrivateInfo);
	}
}

void
dontStopThread(void)
{
	/* We get a main jthread before java.lang.Thread is even
	   loaded, so we must check both */
	if (jthread_current() && getCurrentThread()) {
		Hjava_lang_Thread *tid = getCurrentThread();

		if (!unhand(tid)->noStopCount) {
			jthread_disable_stop();
		}
		unhand(tid)->noStopCount++;
	}
}

void
canStopThread(void)
{
	/* We get a main jthread before java.lang.Thread is even
	   loaded, so we must check both */
	if (jthread_current() && getCurrentThread()) {
		Hjava_lang_Thread *tid = getCurrentThread();
	
		assert(unhand(tid)->noStopCount > 0);
		unhand(tid)->noStopCount--;
		if (!unhand(tid)->noStopCount) {
			jthread_enable_stop();
		}
	}
}

/*
 * Create the initial thread with a given name.
 *  We should only ever call this once.
 */
static
void
createInitialThread(const char* nm)
{
	Hjava_lang_Thread* tid;

	/* Allocate a thread to be the main thread */
	tid = (Hjava_lang_Thread*)newObject(ThreadClass);
	assert(tid != 0);

	unhand(tid)->name = stringC2CharArray(nm);
	assert(unhand(tid)->name);
	unhand(tid)->priority = java_lang_Thread_NORM_PRIORITY;
	unhand(tid)->threadQ = 0;
	unhand(tid)->daemon = 0;
	unhand(tid)->interrupting = 0;
	unhand(tid)->target = 0;
	unhand(tid)->group = standardGroup;

	initThreadLock(tid);

	jthread_atexit(runfinalizer);
	/* set Java thread associated with main thread */
	mainthread = jthread_createfirst(MAINSTACKSIZE, java_lang_Thread_NORM_PRIORITY, tid);
	unhand(tid)->PrivateInfo = (struct Hkaffe_util_Ptr*)mainthread;
	unhand(tid)->stackOverflowError = 
		(Hjava_lang_Throwable*)StackOverflowError;
	unhand(tid)->needOnStack = STACK_HIGH;

	/* Attach thread to threadGroup */
	do_execute_java_method(unhand(tid)->group, "add", "(Ljava/lang/Thread;)V", 0, 0, tid);
}

/*
 * helper function to start gc thread
 */
static
void
startSpecialThread(void* arg)
{
	Hjava_lang_Thread* tid;
	void (*func)(void *);
	void *argument;

	tid  = getCurrentThread();
	func = (void*)tid->target;
	argument = (void*)tid->group;
	tid->target = 0;
	tid->group = 0;
	func(argument);
}

/*
 * Start a daemon thread, such as a gc or finalizer thread.
 * We give these threads a java incarnation for consistency.
 * It might not be strictly necessary for the gc thread.
 *
 * This guy can fail because of allocation failure, or because
 * createThread failed.
 * XXX
 */
Hjava_lang_Thread*
createDaemon(void* func, const char* nm, void *arg, int prio,
	     size_t stacksize, struct _errorInfo *einfo)
{
	Hjava_lang_Thread* tid;

DBG(VMTHREAD,	dprintf("createDaemon %s\n", nm);	)

	/* Keep daemon threads as root objects */
	tid = (Hjava_lang_Thread*)newObject(ThreadClass);
	assert(tid != 0);

	unhand(tid)->name = stringC2CharArray(nm);
	if (!unhand(tid)->name) {
		postOutOfMemory(einfo);
		return 0;
	}
	unhand(tid)->priority = prio;
	unhand(tid)->threadQ = 0;
	unhand(tid)->daemon = 1;
	unhand(tid)->interrupting = 0;
	/* we abuse these two variables as carriers */
	unhand(tid)->target = (void*)func;
	unhand(tid)->group = (void*)arg;

	initThreadLock(tid);

	if (!createThread(tid, startSpecialThread, stacksize, einfo)) {
		return 0;
	}
	return (tid);
}

/*
 * All threads start here.
 */
static
void
firstStartThread(void* arg)
{
	extern JNIEnv Kaffe_JNIEnv;
	JNIEnv *env = &Kaffe_JNIEnv;
	Hjava_lang_Thread* tid;
	jmethodID runmethod;
	jthrowable eobj;
	int iLockRoot;

	/* 
	 * Make sure the thread who created us returned from
	 * startThread.  This ensures that privateInfo
	 * is set when we run.
	 */
	lockStaticMutex(&thread_start_lock);
	unlockStaticMutex(&thread_start_lock);

	tid  = getCurrentThread();

DBG(VMTHREAD,	
	dprintf("firstStartThread %x\n", tid);		
    )

	/*
	 * We use JNI here to make sure the stack is unrolled when we get
	 * into the uncaughtException handler.  Otherwise, we wouldn't be
	 * able to handle StackOverflowError.
	 */

	/* Find the run()V method and call it */
	runmethod = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, tid),
					"run", "()V");
	if (runmethod != 0) {
		(*env)->CallVoidMethod(env, tid, runmethod);

		/* note that since exception.c does not allow JNI to catch
		 * ThreadDeath (for now!), we won't see ThreadDeath here.
		 * That is, we must invoke the uncaughtException paper
		 * if we see an exception here.
		 */
		eobj = (*env)->ExceptionOccurred(env);
		(*env)->ExceptionClear(env);
	} else {
		/* eobj will usually be NoSuchMethodError */
		eobj = (*env)->ExceptionOccurred(env);
	}

	/* If all else fails we call the the uncaught exception method
	 * on this thread's group.  Note we must set a flag so we 
	 * don't do this again while in the handler.
	 */
	if (eobj != 0 && unhand(tid)->dying == false) {
		jobject group = unhand(tid)->group;
		jclass groupclass = (*env)->GetObjectClass(env, group);
		jmethodID uncaughtmeth = 
			(*env)->GetMethodID(env, groupclass,
				"uncaughtException",
				"(Ljava/lang/Thread;Ljava/lang/Throwable;)V");

		unhand(tid)->dying = true;
		(*env)->CallVoidMethod(env, group, uncaughtmeth, tid, eobj);
		/* exceptions thrown in `uncaughtException' are 
		 * silently ignored says the JLS.
		 */
		(*env)->ExceptionClear(env);
	}
	exitThread();
}

/*
 * Yield process to another thread of equal priority.
 */
void
yieldThread(void)
{
	jthread_yield();
}

/*
 * Change thread priority.
 */
void
setPriorityThread(Hjava_lang_Thread* tid, int prio)
{
	unhand(tid)->priority = prio;

	/* no native thread yet */
	if (unhand(tid)->PrivateInfo == 0)
		return;

	jthread_setpriority((jthread_t)unhand(tid)->PrivateInfo, prio);
}

/*
 * Terminate a thread.
 */
void
exitThread(void)
{
        do_execute_java_method(getCurrentThread(), "finish", "()V", 0, 0);
	jthread_exit();
}

#if 0
/*
 * Put a thread to sleep.
 */
void
sleepThread(jlong time)
{
	if (time > 0) {
		jthread_sleep(time);
	}
}

/*
 * Is this thread alive?
 */
bool
aliveThread(Hjava_lang_Thread* tid)
{
	bool status;

DBG(VMTHREAD,	dprintf("aliveThread: tid 0x%x\n", tid);		)

	status = jthread_alive((jthread_t)unhand(tid)->PrivateInfo);

	return (status);
}

/*
 * How many stack frames have I invoked?
 */
jint
framesThread(Hjava_lang_Thread* tid)
{
	return (jthread_frames((jthread_t)unhand(tid)->PrivateInfo));
}
#endif

/*
 * Get the current Java thread.
 */
Hjava_lang_Thread*
getCurrentThread(void)
{
	return (jthread_getcookie(jthread_current()));
}

/*
 * Finalize a thread.
 *  This is to free the native thread context.
 */
void
finalizeThread(Hjava_lang_Thread* tid)
{
	if (unhand(tid)->PrivateInfo != 0) {
		jthread_t jtid = (jthread_t)unhand(tid)->PrivateInfo;
		unhand(tid)->PrivateInfo = 0;
		jthread_destroy(jtid);
	}
}

/* 
 * extract name of a thread as a C string.
 * Returns static buffer.
 */
char *
nameThread(Hjava_lang_Thread *tid)
{
	static char buf[80];
	int i = 0;
	HArrayOfChar* name = unhand(tid)->name;
	while (i < sizeof buf - 1 && i < ARRAY_SIZE(name)) {
		buf[i] = ((short*)ARRAY_DATA(name))[i];
		i++;  
	}
	buf[i] = 0;
	return buf;
}

static void 
broadcastDeath(void *jlThread)
{
        Hjava_lang_Thread *tid = jlThread;
	int iLockRoot;

        /* Notify on the object just in case anyone is waiting */
        lockMutex(&tid->base);
        broadcastCond(&tid->base);
        unlockMutex(&tid->base);
}

static void 
throwDeath(void)
{
	throwException(ThreadDeath);
}

static
void
runfinalizer(void)
{
	if (runFinalizerOnExit) {
		invokeFinalizer();
	}
}

/*
 * print info about a java thread.
 */
static void
dumpJavaThread(void *jlThread)
{
        Hjava_lang_Thread *tid = jlThread;
	fprintf(stderr, "`%s' ", nameThread(tid));
	jthread_dumpthreadinfo((jthread_t)unhand(tid)->PrivateInfo);
	fprintf(stderr, "\n");
}

static void
dumpThreads(void)
{
	fprintf(stderr, "Dumping live threads:\n");
	jthread_walkLiveThreads(dumpJavaThread);
}

/*
 * Return the name of a java thread, given its native thread pointer.
 */
char*
nameNativeThread(void* native)
{
	return nameThread((Hjava_lang_Thread*)
		jthread_getcookie((jthread_t)native));
}

/*
 * Invoked when threading system detects a deadlock.
 */
static void
onDeadlock(void)
{
	if (!deadlockDetection) {
		return;
	}
#if defined(JTHREAD_RESTORE_FD)
        jthreadRestoreFD(2);
#endif
	dumpLocks();
	dumpThreads();
	fprintf(stderr, "Deadlock: all threads blocked on internal events\n");
	fflush(stderr);
	ABORT();
}

void
initNativeThreads(int nativestacksize)
{
	/* Even though the underlying operating or threading system could
	 * probably extend the main thread's stack, we must impose this 
	 * artificial boundary, because otherwise we wouldn't be able to 
	 * catch stack overflow exceptions thrown by the main thread.
	 */
	threadStackSize = nativestacksize;
	jthread_init(
		DBGEXPR(JTHREADNOPREEMPT, false, true),
		java_lang_Thread_MAX_PRIORITY+1,
		java_lang_Thread_MIN_PRIORITY,
		thread_malloc,
		thread_free,
		broadcastDeath,
		throwDeath,
		onDeadlock);
}
