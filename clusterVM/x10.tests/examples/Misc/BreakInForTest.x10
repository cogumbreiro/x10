/*
 *
 * (C) Copyright IBM Corporation 2006
 *
 *  This file is part of X10 Test.
 *
 */
import harness.x10Test;

/**
 * Verifying for (point p[i,j]:R) { S; } loops can accept
 * continue and break statements.
 *
 * Test resulted in branch target not found message
 * as of 5/26/2005
 *
 * @author kemal, 5/2005
 */
public class BreakInForTest extends x10Test {

	public const N: int = 100;
	var R: Region = [0..N];
	var D: Dist = Dist.make(R);
	var n1: int = 91;
	var n2: int = 27;

	public def run(): boolean = {
		for (var i: int = 0; i < N; i++) {
			if ((i+1) % n1 == 0) continue;
			if ((i+1) % n2 == 0) break;
		}
		for (val (i): Point in D) {
			if ((i+1) % n1 == 0) continue;
			if ((i+1) % n2 == 0) break;
		}
		return true;
	}

	public static def main(var args: Rail[String]): void = {
		new BreakInForTest().execute();
	}
}