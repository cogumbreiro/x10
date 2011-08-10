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
 * @author bdlucas 12/2008
 */

class XTENLANG_258 extends x10Test {

    public def run():boolean {
        val v: Rail[int] = new Rail[int](1);
        v(0) += 1;
        return true;
    }

    public static def main(Array[String](1)) {
        new XTENLANG_258().execute();
    }
}