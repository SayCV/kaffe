/*
 * java.lang.reflect.Constructor.c
 *
 * Copyright (c) 1996, 1997, 2004
 *	Transvirtual Technologies, Inc.  All rights reserved.
 *
 * See the file "license.terms" for information on usage and redistribution 
 * of this file. 
 */

#include "config.h"
#include "config-std.h"
#include "config-mem.h"
#include "gtypes.h"
#include "access.h"
#include "constants.h"
#include "object.h"
#include "classMethod.h"
#include "itypes.h"
#include "support.h"
#include "baseClasses.h"
#include "java_lang_reflect_Constructor.h"
#include "java_lang_reflect_Method.h"
#include <native.h>
#include <jni.h>
#include "defs.h"
#include "stringSupport.h"

jint
java_lang_reflect_Constructor_getModifiersInternal(struct Hjava_lang_reflect_Constructor* this)
{
	Hjava_lang_Class* clazz;
	jint slot;

	clazz = unhand(this)->clazz;
	slot = unhand(this)->slot;

	assert(slot < CLASS_NMETHODS(clazz));

	return Kaffe_get_class_methods(clazz)[slot].accflags;
}

Hjava_lang_String *
java_lang_reflect_Constructor_getSignature(struct Hjava_lang_reflect_Constructor* this)
{
	Hjava_lang_Class* clazz;
	jint slot;

	clazz = unhand(this)->clazz;
	slot = unhand(this)->slot;

	assert(slot < CLASS_NMETHODS(clazz));

	return utf8Const2Java(Kaffe_get_class_methods(clazz)[slot].extSignature);
}
