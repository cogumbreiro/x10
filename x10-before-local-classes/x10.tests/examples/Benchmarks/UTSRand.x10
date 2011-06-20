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

final class UTSRand {

    //
    // For now use util.Random instead of SHA. To substitute SHA
    // redefine descriptor, next(), and number().
    //
    // Instead of actually using util.Random, we replicate its
    // function here to avoid allocating a Random object.
    //

    final static type descriptor = long;

    final static def next(r:descriptor, i:int) {
        var seed: long = r+i;
        seed = (seed ^ 0x5DEECE66DL) & ((1L << 48) - 1);
        for (var k:int=0; k<11; k++)
            seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);

        // Extra cast to long to workaround XTENLANG-1112: Remove when it is fixed.
        val l0 = ((seed as ULong) >> (48 - 32)) as long as int;

        seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);

        // Extra cast to long to workaround XTENLANG-1112: Remove when it is fixed
        val l1 = ((seed as ULong) >> (48 - 32)) as long as int;

        return ((l0 as long) << 32) + l1;
    }

    const scale = (long.MAX_VALUE as double) - (long.MIN_VALUE as double);

    final static def number(r:descriptor) = (r / scale) - (long.MIN_VALUE / scale);

}