/*
 * jqueue.h - queue pool manager 
 * See the file "license.terms" for information on usage and redistribution
 * of this file.
 *
 * Written by Guilhem Lavaux <guilhem@kaffe.org> (C) 2003
 */
#ifndef _jqueue_h
#define _jqueue_h

#if defined(KVER)
#include "debug.h"
#include "config.h"
#include "config-std.h"
#include "config-mem.h"
#else
#include "config-jthreads.h"
#endif

typedef void *(*KaffeAllocator)(size_t s);
typedef void (*KaffeDeallocator)(void *ptr);

typedef struct _KaffeNodeQueue {
  void *element;
  struct _KaffeNodeQueue *next;
} KaffeNodeQueue;

typedef struct {
  KaffeNodeQueue *pool;
  KaffeNodeQueue **free_nodes;
  int num_free_nodes;
  int num_nodes_in_pool;

  KaffeAllocator allocator;
  KaffeDeallocator deallocator;
} KaffePool;

void KaffeSetDefaultAllocator(KaffeAllocator allocator,
			      KaffeDeallocator deallocator);
KaffePool *KaffeCreatePool(void);
void KaffeDestroyPool(KaffePool *pool);
KaffeNodeQueue *KaffePoolNewNode(KaffePool *pool);
void KaffePoolReleaseNode(KaffePool *pool, KaffeNodeQueue *node);
void KaffePoolReleaseList(KaffePool *pool, KaffeNodeQueue *node);

#endif
