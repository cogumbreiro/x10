// (C) Copyright IBM Corporation 2008
// This file is part of X10 Test. *

import harness.x10Test;

/**
 * @author bdlucas 10/2008
 */

class XTENLANG_8 extends x10Test {

    class It implements Iterator[int] {
        incomplete public def hasNext(): boolean;
        incomplete public def next(): int;
        incomplete public def remove(): void;
    }

    public def run(): boolean {
        return true;
    }

    public static def main(Rail[String]) {
        new XTENLANG_8().execute();
    }
}