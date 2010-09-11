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
 * 
 * @author kemal 11/2005
 * @author vj 12 2006 --- converted to fail at compile time.
 */
public class AtomicContainingWhen_MustFailCompile extends x10Test {

	val fooArray: Rail[foo] = Rail.make([ new f0() as foo, new f1() as foo, 
	                                      new f2() as foo, new f3()as foo ]);

	public def run(): boolean = {
		val x: X = new X();

		val l: lockStruct = new lockStruct();

		atomic Y.test(fooArray(x.one()), l);
		atomic Y.test(fooArray(x.zero()), l);
		atomic Y.test(fooArray(x.two()), l);
		atomic Y.test(fooArray(3), l);

		return true;
	}

	// just to confuse a compiler
	public def modifyFoo(var x: X): void = { fooArray(x.two()+1) = new f2(); }

	public static def main(var args: Array[String](1)): void = {
		new AtomicContainingWhen_MustFailCompile().execute();
	}

	/**
	 * A class to invoke a 'function pointer' inside of async
	 */
	static class Y {
		static def test(val f: foo, val l: lockStruct): void = {
			// Compiler analysis may not be possible here
			f.apply(l); // it is hard to determine if f executes an async or when
		}
	}

	/**
	 * class containing miscellaneous synchronization constructs.
	 */
	static class lockStruct {
		var lock0: boolean;
		var lock1: boolean;
		var futureInt: Future[int];
		var c: Clock;
		def this(): lockStruct = {
			futureInt = future 1;
			lock0 = false;
			lock1 = false;
			c = Clock.make();
		}
	}

	/**
	 * An interface to use like a simple 'function pointer'
	 */
	static interface foo {
		public def apply(var l: lockStruct): void;
	}

	static class f0 implements foo {
		// it is hard to determine if this is invoked inside
		// an atomic, at compile time.
		public def apply(val l: lockStruct): void = {
			x10.io.Console.OUT.println("in f0:#1");
			when (!l.lock0) l.lock0 = true;
			x10.io.Console.OUT.println("in f0:#2");
		}
	}

	static class f1 implements foo {
		// it is hard to determine if this is invoked inside
		// an atomic, at compile time.
		public def apply(val l: lockStruct): void = {
			x10.io.Console.OUT.println("in f1:#1");
			when (!l.lock1) l.lock1 = true;
			x10.io.Console.OUT.println("in f1:#2");
		}
	}

	static class f2 implements foo {
		// it is hard to determine if this is invoked inside
		// an atomic, at compile time.
		public def apply(val l: lockStruct): void = {
			x10.io.Console.OUT.println("in f2:#1");
			l.c.resume();
			x10.io.Console.OUT.println("in f2:#2");
			finish async { };
			x10.io.Console.OUT.println("in f2:#3");
			var x: int = (future { l.futureInt.force() }).force();
			x10.io.Console.OUT.println("in f2:#4");
			next;
			x10.io.Console.OUT.println("in f2:#5");
		}
	}

	static class f3 implements foo {
		public def apply(val l: lockStruct): void = {
			x10.io.Console.OUT.println("in f3");
		}
	}

	/**
	 * Dummy class to make static memory disambiguation difficult
	 * for a typical compiler
	 */
	static class X {
		public var z: Array[int](1) = new Array[Int](0..1, (i:Point(1))=> 1-i(0));
		def zero(): int = { return z(z(z(1))); }
		def one(): int = { return z(z(z(0))); }
		def two(): int = { return (zero()-1) * (zero()-1) + one(); }
		def modify(): void = { z(0) += 1; }
	}
}
