/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2010.
 */

#ifndef X10_UTIL_VEC_H
#define X10_UTIL_VEC_H

#include <x10aux/config.h>
#include <x10aux/ref.h>
#include <x10aux/RTT.h>
#include <x10aux/serialization.h>

// NOTE: this class has a matching declaration in x10aux/vec_decl.h

namespace x10 {
    namespace util {

        // pull out the rtt as it should be the same for the two NativeVec implementations
        // (for each T)
        template <class T> struct NativeVec_rtt {
                RTT_H_DECLS_STRUCT
        };

        template <class T, x10_int SZ> struct NativeVec{

                static const x10aux::RuntimeType* getRTT() { return &NativeVec_rtt<T>::rtt; }

                inline x10_int size() const { return SZ; }
                T arr[SZ];
                NativeVec(void) { };
                NativeVec(size_t sz) { (void) sz; };
                const T &get (int i) const { return arr[i]; }
                const T &set (const T &v, int i) { arr[i] = v; return v; }
                NativeVec(const NativeVec<T,SZ> &src)
                {
                        ::memcpy(arr, src.arr, SZ * sizeof(T));
                }
                NativeVec(const NativeVec<T,-1> &src)
                {
                        assert(src.size() == SZ);
                        ::memcpy(arr, src.arr, SZ * sizeof(T));
                }
        };

        template <class T> struct NativeVec<T,-1> {

                static const x10aux::RuntimeType* getRTT() { return &NativeVec_rtt<T>::rtt; }

                T *arr;
                size_t sz;
                inline x10_int size() const { return sz; }
                NativeVec(void) :arr(NULL), sz(0) { };
                NativeVec(size_t sz_) :arr(x10aux::alloc<T>(sz*sizeof(T))), sz(sz_) { };
                ~NativeVec(void) { }
                const T &get (int i) const { return arr[i]; }
                const T &set (const T &v, int i) { arr[i] = v; return v; }
                NativeVec<T,-1> &operator= (const NativeVec<T,-1> &src)
                {
                        sz = src.size();
                        arr = x10aux::realloc(arr, sz*sizeof(T));
                        ::memcpy(arr, src.arr, sz * sizeof(T));
                        return *this;
                }
                NativeVec (const NativeVec<T,-1> &src)
                    : arr(x10aux::alloc<T>(src.sz*sizeof(T))), sz(src.sz)
                {
                        ::memcpy(arr, src.arr, sz * sizeof(T));
                }
                template<int SZ> NativeVec (const NativeVec<T,SZ> &src)
                    : arr(x10aux::alloc<T>(SZ*sizeof(T))), sz(SZ)
                {
                        ::memcpy(arr, src.arr, sz * sizeof(T));
                }
        };
    }
}

template<class T> x10aux::RuntimeType x10::util::NativeVec_rtt<T>::rtt;

template<class T> void x10::util::NativeVec_rtt<T>::_initRTT() {
    const x10aux::RuntimeType *canonical = x10aux::getRTT<x10::util::NativeVec_rtt<void> >();
    if (rtt.initStageOne(canonical)) return;
    // copying IndexedMemoryChunk which has 2 parents of Any for some reason
    const x10aux::RuntimeType* parents[2] = { x10aux::getRTT<x10::lang::Any>(), x10aux::getRTT<x10::lang::Any>()};
    const x10aux::RuntimeType* params[1] = { x10aux::getRTT<T>()};
    x10aux::RuntimeType::Variance variances[1] = { x10aux::RuntimeType::invariant};
    const char *baseName = "x10.util.Vec";
    rtt.initStageTwo(baseName, x10aux::RuntimeType::struct_kind, 2, parents, 1, params, variances);
}


#endif
// vim: ts=8:sw=8:et
