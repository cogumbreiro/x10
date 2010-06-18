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
 * Check that in the return type of a closure call actuals are substituted for formals.
 * @author vj
 */

public class ClosureCall3 extends ClosureTest {

    public def run(): boolean = {
        val y = (x:Int)=> x;
        val z :Int(1) = y(1);
        return result;
    }

    public static def main(var args: Rail[String]): void = {
        new ClosureCall3().execute();
    }
}
