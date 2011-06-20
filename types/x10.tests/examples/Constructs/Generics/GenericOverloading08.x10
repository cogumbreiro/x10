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
 * Two or more methods of a class or interface may have the same name
 * if they have a different number of type parameters, or they have
 * value parameters of different types.
 *
 * @author bdlucas 8/2008
 */

public class GenericOverloading08 extends GenericTest {

    static def m[T]() = 0;
    static def m[T,U]() = 1;

    public def run(): boolean = {

        check("m[int]()", m[int](), 0);
        check("m[String]()", m[String](), 0);
        check("m[String,int]()", m[String,int](), 1);

        return result;
    }
    public static def main(var args: Array[String](1)): void = {
        new GenericOverloading08().execute();
    }
}