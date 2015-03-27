/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2011.
 */

import harness.x10Test;

/**
 * Test Array[UByte]
 *
 * @author Salikh Zakirov 5/2011
 */
public class UByteArray0 extends x10Test {
    public def run(): boolean {
	if (!test_simple()) return false;
	return true;
    }

    public def test_simple():boolean {
	val aaa = new Rail[UByte](10);
	for (i in 0..9) aaa(i) = i as UByte;

	var s : UByte = 0uy;
	for (i in aaa.range) s += aaa(i);

	if (s != 45uy) return false;

	return true;
    }

    public static def main(Rail[String]) {
        new UByteArray0().execute();
    }
}
