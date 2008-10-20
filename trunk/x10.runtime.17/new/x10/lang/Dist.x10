// (C) Copyright IBM Corporation 2006-2008.
// This file is part of X10 Language.

package x10.lang;

import x10.array.BaseDist;

public abstract value class Dist(
    region: Region,
    unique: boolean,
    constant: boolean,
    onePlace: Place
) implements
    (Point/*(region.rank)*/)=>Place
    // (Place)=>Region XTENLANG-60
{

    property rank: int = region.rank;
    property rect: boolean = region.rect;
    property zeroBased: boolean = region.zeroBased;
    property rail: boolean = region.rail;

    // XTENLANG-50: workaround requires explicit return type decls here
    // XTENLANG-4: workaround requires BaseDist methods to be named differently from these methods

    //
    // factories - place is all applicable places
    //

    /**
     * Returns a distribution over a rank-1 region that maps every
     * point in the region to a distinct place, and which maps some
     * point in the region to every place.
     */

    public static def makeUnique(): Dist(1) = BaseDist.makeUnique1();

    /**
     * Returns a distribution over the specified region that maps
     * every point in the region to here.
     */

    public static def makeConstant(r: Region): Dist(r.rank) = BaseDist.makeConstant1(r);

    /**
     * Returns a distribution over the specified region that maps
     * every point in the region to here.
     */

    public static def make(r: Region): Dist(r.rank) = makeConstant(r);

    /**
     * Returns a distribution over the specified region that varies in
     * place only along the specified axis, and maps the ith
     * coordinate along that axis to place i%Place.NUM_PLACES.
     */

    public static def makeCyclic(r: Region, axis: int): Dist(r.rank)
        = BaseDist.makeBlockCyclic1(r, axis, 1);

    /**
     * Returns a distribution over the specified region that varies in
     * place only along the specified axis. It divides the coordinates
     * along that axis into Place.MAX_PLACES blocks, and assigns
     * successive blocks to successive places.
     */

    public static def makeBlock(r: Region, axis: int): Dist(r.rank) {
        val n = r.max()(axis) - r.min()(axis) + 1;
        val bs = (n + Place.MAX_PLACES - 1) / Place.MAX_PLACES;
        return BaseDist.makeBlockCyclic1(r, axis, bs);
    }

    /**
     * Returns a distribution over the specified region that varies in
     * place only along the specified axis. It divides the coordinates
     * along that axis into blocks of the specified size, and assigns
     * block i to place i%Place.MAX_PLACES.
     */

    public static def makeBlockCyclic(r: Region, axis: int, blockSize: int): Dist(r.rank)
        = BaseDist.makeBlockCyclic1(r, axis, blockSize);


    //
    // factories - place is a parameter
    //

    /**
     * Returns a distribution over a rank-1 region that maps every
     * point in the region to a place in ps, and which maps some
     * point in the region to every place in ps.
     */

    public static def makeUnique(ps:Rail[Place]): Dist(1)
        = BaseDist.makeUnique1(ps);

    /**
     * Returns a distribution over a rank-1 region that maps every
     * point in the region to a place in ps, and which maps some
     * point in the region to every place in ps.
     */

    public static def makeUnique(ps: Set[Place]): Dist(1)
        = BaseDist.makeUnique1(ps);

    /**
     * Returns a distribution over the specified region that maps
     * every point in the region to the specified place.
     */

    public static def makeConstant(r: Region, p: Place): Dist(r.rank)
        = BaseDist.makeConstant1(r, p);

    /**
     * Returns a distribution over the specified region that varies in
     * place only along the specified axis, and maps the ith
     * coordinate along that axis to place ps(i%ps.length).
     */

    public static def makeCyclic(r: Region, axis: int, ps: Set[Place]): Dist(r.rank)
        = BaseDist.makeCyclic1(r, axis, ps);

    /**
     * Returns a distribution over the specified region that varies in
     * place only along the specified axis. It divides the coordinates
     * along that axis into ps.length blocks, and assigns successive
     * blocks to successive places in ps.
     */

    public static def makeBlock(r: Region, axis: int, ps: Set[Place]): Dist(r.rank)
        = BaseDist.makeBlock1(r, axis, ps);

    /**
     * Returns a distribution over the specified region that varies in
     * place only along the specified axis. It divides the coordinates
     * along that axis into blocks of the specified size, and assigns
     * block i to place ps(i%ps.length).
     */

    public static def makeBlockCyclic(r: Region, axis: int, blockSize: int, ps: Set[Place])
        = BaseDist.makeBlockCyclic1(r, axis, blockSize, ps);


    //
    // mapping places to regions
    //

    /**
     * Returns the set of places that this distribution maps some
     * point to.
     */

    abstract public def places(): Rail[Place];

    /**
     * Returns the set of regions that this distribution maps some place to.
     */

    abstract public def regions(): Rail[Region]; // essentially regionMap().values()

    
    /**
     * Returns the region consisting of points which this distribution
     * maps to the specified place.
     */

    abstract public def get(p: Place): Region(rank);



    //
    // mapping points to places
    //

    /**
     * Returns the place which this distribution maps the specified
     * point to.
     */

    abstract public def apply(pt: Point/*(rank)*/): Place;


    abstract public def apply(i0: int): Place;
    abstract public def apply(i0: int, i1: int): Place;
    abstract public def apply(i0: int, i1: int, i2: int): Place;
    abstract public def apply(i0: int, i1: int, i2: int, i3: int): Place;


    //
    //
    //

    /**
     * Returns the iterator for the underlying region of this
     * distribution. Supports the syntax
     *
     *   (for p:Point in d)
     *       ... p ...
     */

    public def iterator(): Iterator[Point(rank)] = region.iterator();


    //
    // geometric ops with region
    //

    /**
     * Returns the distribution defined over this.region&&that.region,
     * and which maps every point in its region to the same place as
     * this distribution.
     */

    abstract public def intersection(r: Region(rank)): Dist(rank);

    /**
     * Returns the distribution defined over this.region-that.region,
     * and which maps every point in its region to the same place as
     * this distribution.
     */

    abstract public def difference(r: Region(rank)): Dist(rank);

    /**
     * Returns the distribution defined over r, which must be
     * contained in this.region, and which maps every point in its
     * region to the same place as this distribution.
     */

    abstract public def restriction(r: Region(rank)): Dist(rank);


    //
    // geometric ops with distribution
    //

    /**
     * Returns true iff that.region is contained in this.region, and
     * that distribution maps every point to the same place as this
     * distribution.
     */

    abstract public def isSubdistribution(that: Dist(rank)): boolean;

    /**
     * Returns a distribution containing only points that are
     * contained in both this distribution and that distribution,
     * and which the two distributions map to the same place.
     */

    abstract public def intersection(that: Dist(rank)): Dist(rank);

    /**
     * Returns the distribtution that contains every point in this
     * distribution, except for those points which are also contained
     * in that distribution and which that distribution maps to the
     * same place as this distribution.
     */

    abstract public def difference(that: Dist(rank)): Dist(rank);

    /**
     * If this distribution and that distribution are disjoint,
     * returns the distribution that contains all points p that are in
     * either distribution, and which map p to the same place as the
     * distribution that contains p. Otherwise throws an exception.
     */

    abstract public def union(that: Dist(rank)): Dist(rank);

    /**
     * Returns a distribution whose region is the union of the regions
     * of the two distributions, and which maps each point p to
     * this(p) if p is contained in this, otherwise maps p to that(p).
     */

    abstract public def overlay(that: Dist(rank)): Dist(rank);

    /**
     * Returns true iff both distributions are defined over the same
     * regions, and map every point in that region to the same place.
     */

    abstract public def equals(that: Dist/*(rank)*/): boolean;


    //
    // other geometric ops
    //

    /**
     * Returns the distribution restricted to those points which it
     * maps to the specified place.
     */

    abstract public def restriction(p: Place): Dist(rank);

    /**
     * Returns true iff this.region contains p.
     */

    abstract public def contains(p: Point): boolean;


    //
    // ops
    //

    public def $bar(r: Region(this.rank)): Dist(this.rank) = restriction(r);
    public def $bar(p: Place): Dist(rank) = restriction(p);
    public def $and(d: Dist(rank)): Dist(rank) = intersection(d);
    public def $or(d: Dist(rank)): Dist(rank) = union(d);
    public def $minus(d: Dist(rank)): Dist(rank) = difference(d);


    //
    //
    //

    protected def this(region: Region, unique: boolean, constant: boolean, onePlace: Place) = {
        property(region, unique, constant, onePlace);
    }
}
