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

//LIMITATION: cannot extend more than one instantiation of the same interface

import harness.x10Test;

/**
 * @author bdlucas 8/2008
 */

public class GenericCast02 extends GenericTest {

    interface I[T] {
        global def m(T):int;
    }

    class A implements I[int], I[String] {
        public global def m(int) = 0;
        public global def m(String) = 1;
    }

    public def run() = {

        var a:Object = new A();
        var i1:I[int] = a as I[int];
        var i2:I[String] = a as I[String];
        check("i1.m(0)", i1.m(0), 0);
        check("i2.m(\"1\")", i2.m("1"), 1);

        return result;
    }

    public static def main(var args: Rail[String]): void = {
        new GenericCast02().execute();
    }
}
