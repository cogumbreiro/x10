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
 * Tests conversion of arrays to regions/dists.
 *
 * @author kemal 3/2005
 */

public class ArrayToDist extends x10Test {

    const N = 4;

    public def run(): boolean = {

        val R = [0..N-1, 0..N-1];
        val D  = Dist.makeBlock(R, 0);
        val A1 = DistArray.make[int](D, ((i,j): Point ) => f(i, j));
        val A2 = DistArray.make[foo](D, ((i,j): Point) => new foo(f(i, j)));

        for (val p(i,j): Point(2) in A1.region) 
            chk(f(i, j) == (at(A1.dist(i, j)) A1(i, j)), "1");

        finish ateach (val p(i,j): Point(2) in A1.dist) 
            chk(f(i, j) == A1(i, j), "3");

        for (val p(i,j): Point(2) in A2.region) 
            chk(f(i, j) == (at(A2.dist(i, j)) { (A2(i, j) as foo!).val }), "4");

        finish foreach (val p(i,j): Point(2) in A2.region) 
           chk(f(i, j) == (at(A2.dist(i, j)) { (A2(i, j) as foo!).val }), "5");

        finish ateach (val p(i,j): Point(2)  in A2.dist) 
            chk(f(i, j) == (A2(i, j)as foo!).val, "6");

        return true;
    }

    static def f(i: int, j: int) = N * i + j;
    

    public static def main(Rail[String]) {
        new ArrayToDist().execute();
    }

    static class foo {
        public var val: int;
        public def this(var x: int): foo = { this.val = x; }
    }
}
