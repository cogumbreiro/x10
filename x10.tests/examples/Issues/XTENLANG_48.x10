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

import harness.x10Test;

/**
 * @author bdlucas 10/2008
 */

class XTENLANG_48 extends x10Test {

    static class R(rank:int) {
        def this() { property(0); }
    }
    
    static class C[T] {
        incomplete static def make[T](): C[T];
    }
    
    class Bug(foo:int) {
        def this() { property(0); }

        //var a: Rail[R{rank==foo}] = Rail.make[R{rank==foo}](10);
        var a: C[R{rank==foo}] = C.make[R{rank==foo}]();
    }
    
    public def run(): boolean {
        return true;
    }

    public static def main(Array[String](1)) {
        new XTENLANG_48().execute();
    }
}
