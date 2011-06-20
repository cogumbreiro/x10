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

 


/**
 * As with methods, a closure may declare a where clause to constraint
 * the actual parameters with which it may be invoked.
 *
 * @author bdlucas 8/2008
 */

public class ClosureConstraint3_MustFailCompile extends ClosureTest {

    public def run(): boolean = {
        
        val f = (x:int){x==1}=>x;

        val b:int = 1;
        f(b); // fails compilation

        return result;
    }


    public static def main(var args: Rail[String]): void = {
        new ClosureConstraint3_MustFailCompile().run ();
    }
}