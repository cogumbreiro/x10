// (C) Copyright IBM Corporation 2006-2008.
// This file is part of X10 Language.

package x10.array;


/**
 * This class provides special-case efficient operations for
 * rectangular regions, such as bounds checking and scanning.
 *
 * @author bdlucas
 */

value class RectRegion extends PolyRegion{rect} {

    val size: int;

    val min0:int;
    val min1:int;
    val min2:int;
    val min3:int;

    val max0:int;
    val max1:int;
    val max2:int;
    val max3:int;


    /**
     * computation of size and min/max is deferred until needed to
     * allow unbounded regions
     */

    def this(val pm: PolyMat): RectRegion{self.rank==pm.rank && self.rect} {

        super(pm, true);

        size = pm.isBounded()? computeSize() : -1;

        min0 = rank>=1 && pm.isBounded()? min(0) : 0;
        min1 = rank>=2 && pm.isBounded()? min(1) : 0;
        min2 = rank>=3 && pm.isBounded()? min(2) : 0;
        min3 = rank>=4 && pm.isBounded()? min(3) : 0;

        max0 = rank>=1 && pm.isBounded()? max(0) : 0;
        max1 = rank>=2 && pm.isBounded()? max(1) : 0;
        max2 = rank>=3 && pm.isBounded()? max(2) : 0;
        max3 = rank>=4 && pm.isBounded()? max(3) : 0;
    }

    public static def make1(val min: Rail[int], val max: Rail[int]): Region{self.rank==min.length&&self.rect} { // XTENLANG-4

        if (max.length!=min.length)
            throw U.illegal("min and max must have same length");

        val pmb = new PolyMatBuilder(min.length);
        for (var i: int = 0; i<min.length; i++) {
            pmb.add(pmb.X(i), pmb.GE, min(i));
            pmb.add(pmb.X(i), pmb.LE, max(i));
        }

        val pm = pmb.toSortedPolyMat(true);
        return new RectRegion(pm);
    }


    // XTENLANG-109
    public static def make1(min: int, max: int): Region{self.rect && self.rank==1 /*&& self.zeroBased==(min==0)*/} {
        return make1([min], [max]) as Region{self.rect && self.rank==1 /*&& self.zeroBased==(min==0)*/};
    }

    private def computeSize(): int {
        val min = mat.rectMin();
        val max = mat.rectMax();
        var size:int = 1;
        for (var i: int = 0; i<rank; i++)
            size *= max(i) - min(i) + 1;
        return size;
    }

    public def size(): int {
        if (size<0)
            throw new UnboundedRegionException("unbounded");
        return size;
    }



    /**
     * scanner
     */

    final private static class Scanner implements Region.Scanner {

        private val min: ValRail[int];
        private val max: ValRail[int];

        def this(r: PolyRegion): Scanner {
            min = r.mat.rectMin();
            max = r.mat.rectMax();
        }

        final public def set(axis: int, position: int): void {
            // no-op
        }
        
        final public def min(axis: int): int {
            return min(axis);
        }
        
        final public def max(axis: int): int {
            return max(axis);
        }
    }

    public def scanner(): Region.Scanner {
        return new RectRegion.Scanner(this);
    }


    /**
     * specialized from PolyRegion.Iterator
     * keep them in sync
     *
     * XXX this is actually SLOWER than the generic PolyRegion.Iterator!!!???
     */

    final private static class It implements Iterator[Rail[int]] {
        
        // parameters
        private val rank: int;
        private val min: ValRail[int];
        private val max: ValRail[int];

        // state
        private val x: Rail[int];
        private var k: int;

        def this(val r: RectRegion): It {
            rank = r.rank;
            min = r.mat.rectMin();
            max = r.mat.rectMax();
            x = Rail.makeVar[int](rank, (i:nat)=>min(i));
            x(rank-1)--;
        }

        final public def hasNext(): boolean {
            k = rank-1;
            while (x(k)>=max(k))
                if (--k<0)
                    return false;
            return true;
        }

        final public def next(): Rail[int] {
            x(k)++;
            for (k=k+1; k<rank; k++)
                x(k) = min(k);
            return x;
        }

        incomplete public def remove(): void;
    }

    /* slower!!!
    public Region.Iterator iterator() {
        return new RectRegion.Iterator(this);
    }
    */


    //
    // specialized bounds checking for performance
    // 

    //const doChecks = Runtime.ARRAY_BOUNDS_RUNTIME_CHECK;
    const doChecks = true;

    def check(err:(Point)=>RuntimeException, i0: int) {rank==1} {
        if (doChecks && (
            i0<min0 || i0>max0
        ))
            throw err([i0] as Point);
    }

    def check(err:(Point)=>RuntimeException, i0: int, i1: int) {rank==2} {
        if (doChecks && (
            i0<min0 || i0>max0 ||
            i1<min1 || i1>max1
        ))
            throw err([i0,i1] as Point);
    }

    def check(err:(Point)=>RuntimeException, i0: int, i1: int, i2: int) {rank==3} {
        if (doChecks && (
            i0<min0 || i0>max0 ||
            i1<min1 || i1>max1 ||
            i2<min2 || i2>max2
        ))
            throw err([i0,i1,i2] as Point);
    }

    def check(err:(Point)=>RuntimeException, i0: int, i1: int, i2: int, i3: int) {rank==4} {
        if (doChecks && (
            i0<min0 || i0>max0 ||
            i1<min1 || i1>max1 ||
            i2<min2 || i2>max2 ||
            i3<min3 || i3>max3
        ))
            throw err([i0,i1,i2,i3] as Point);

    }


    //
    // region operations
    //

    protected def computeBoundingBox(): Region(rank) {
        return this;
    }

    public def min(): ValRail[int] {
        return mat.rectMin();
    }

    public def max(): ValRail[int] {
        return mat.rectMax();
    }


    // XTENLANG-28

    public def equals(that: Region): boolean {

        // we only handle rect==rect
        if (!(that instanceof RectRegion))
            return super.equals(that);

        // ranks must match
        if (this.rank!=that.rank)
            return false;

        // fetch bounds
        val thisMin = this.min();
        val thisMax = this.max();
        val thatMin = (that as RectRegion).min();
        val thatMax = (that as RectRegion).max();

        // compare 'em
        for (var i: int = 0; i<rank; i++) {
            if (thisMin(i)!=thatMin(i) || thisMax(i)!=thatMax(i))
                return false;
        }
        return true;
    }


    //
    //
    //

    public def toString(): String {
        val min = this.min();
        val max = this.max();
        var s: String = "[";
        for (var i: int = 0; i<rank; i++) {
            if (i>0) s += ",";
            s += min(i) + ".." + max(i);
        }
        s += "]";
        return s;
    }

}