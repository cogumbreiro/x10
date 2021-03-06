/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2014.
 */

import harness.x10Test;

import x10.util.concurrent.Future;
/**
 * Future test.
 */
public class Future0 extends x10Test {
	public def run() {
	  val x = Future.make[int](()=> 47n);
	  return x() == 47n;
	}

	public static def main(Rail[String])  {
		new Future0().execute();
	}
}
