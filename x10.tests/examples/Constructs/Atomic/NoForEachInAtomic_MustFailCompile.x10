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
 * An await statement cannot occur in an atomic.
 * @vj
 */
public class NoForEachInAtomic_MustFailCompile extends x10Test {

	var b: boolean;
	
	public def run(): boolean = {
		atomic {
		  foreach (val p: Point in [1..10]) {
		    x10.io.Console.OUT.println("Cannot reach this point.");
		  }
		  }
		  return true;
	}

	public static def main(var args: Array[String](1)): void = {
		new NoForEachInAtomic_MustFailCompile().execute();
	}
}
