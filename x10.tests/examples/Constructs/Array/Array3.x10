/*
 *
 * (C) Copyright IBM Corporation 2006
 *
 *  This file is part of X10 Test.
 *
 */
import harness.x10Test;;

/**
 * Tests declaration of arrays, storing in local variables, accessing and
 * updating 2D arrays.
 */
public class Array3 extends x10Test {

	public def run(): boolean = {
	
	    d:Dist = Dist.makeConstant([1..10, 1..10], here);
		var ia: Array[int] = Array.make[int](d, (x:point)=>0);
		ia(1, 1) = 42;
		return 42 == ia(1, 1);
	}

	public static def main(var args: Rail[String]): void = {
		new Array3().execute();
	}
}
