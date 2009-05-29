#ifndef X10_LANG_VOIDFUN_0_5_H
#define X10_LANG_VOIDFUN_0_5_H

#include <x10aux/config.h>
#include <x10aux/RTT.h>
#include <x10/lang/Object.h>

namespace x10 {
    namespace lang {
        template<class P1, class P2, class P3, class P4, class P5> class VoidFun_0_5 : public virtual Object {
            public:
            static const x10aux::RuntimeType* rtt;
            static const x10aux::RuntimeType* getRTT() { return NULL == rtt ? _initRTT() : rtt; }
            static const x10aux::RuntimeType* _initRTT() {
                const char *name =
                    x10aux::alloc_printf("x10.lang.VoidFun_0_5[%s,%s,%s,%s,%s]",
                                         x10aux::getRTT<P1>()->name(),
                                         x10aux::getRTT<P2>()->name(),
                                         x10aux::getRTT<P3>()->name(),
                                         x10aux::getRTT<P4>()->name(),
                                         x10aux::getRTT<P5>()->name());
                const x10aux::RuntimeType *parent = x10::lang::Object::getRTT();
                const x10aux::RuntimeType *cand = new (x10aux::alloc<x10aux::RuntimeType >()) x10aux::RuntimeType(name, 1, parent);
                return x10aux::RuntimeType::installRTT(&rtt, cand);
            }
            virtual const x10aux::RuntimeType *_type() const { return getRTT(); }

            virtual ~VoidFun_0_5() { }
            virtual void apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) = 0;
        };
        template<class P1, class P2, class P3, class P4, class P5>
            const x10aux::RuntimeType* VoidFun_0_5<P1,P2,P3,P4,P5>::rtt = NULL;
    }
}
#endif
// vim:tabstop=4:shiftwidth=4:expandtab
