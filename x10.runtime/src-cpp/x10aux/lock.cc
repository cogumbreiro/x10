/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2015.
 */

#include <x10aux/lock.h>

#include <errno.h>

void x10aux::reentrant_lock::initialize() {
    // create lock attributes object
    // ??check the return code for ENOMEM and throw OutOfMemoryError??
    (void)pthread_mutexattr_init(&__lock_attr);

    // set lock type to reentrant
    int type = PTHREAD_MUTEX_RECURSIVE;
    pthread_mutexattr_settype(&__lock_attr, type);

    // whether this lock is shared by threads across places
    // if so, set this to PTHREAD_PROCESS_SHARED
    int pshared = PTHREAD_PROCESS_PRIVATE;
    pthread_mutexattr_setpshared(&__lock_attr, pshared);

#ifdef PTHREAD_PRIO_NONE
    // we are not currently implementing any fairness policy
    int protocol = PTHREAD_PRIO_NONE;
    pthread_mutexattr_setprotocol(&__lock_attr, protocol);
#endif

    // create lock object
    // ??check the return code for ENOMEM and throw OutOfMemoryError??
    (void)pthread_mutex_init(&__lock, &__lock_attr);
}

// destructor
void x10aux::reentrant_lock::teardown() {
    // free lock object
    pthread_mutex_destroy(&__lock);

    // free lock attributes
    pthread_mutexattr_destroy(&__lock_attr);
}

// acquire lock (blocking)
void x10aux::reentrant_lock::lock() {
    // blocks until the lock becomes available
    pthread_mutex_lock(&__lock);
}

// release lock
bool x10aux::reentrant_lock::unlock() {
    // calling thread doesn't own the lock
    if (pthread_mutex_unlock(&__lock) == EPERM) {
        return false;
    }
    return true;
}

// acquire lock (non-blocking)
bool x10aux::reentrant_lock::tryLock() {
    // acquire the lock only if it is not held by another thread
    if (pthread_mutex_trylock(&__lock) == 0) {
        return true;
    }
    return false;
}

// get lock count
int x10aux::reentrant_lock::getHoldCount() {
    return -1;
}

// vim:tabstop=4:shiftwidth=4:expandtab
