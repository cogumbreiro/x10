/*
 *  This file is part of the X10 project (http://x10-lang.org).
 * 
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 *  (C) Copyright IBM Corporation 2006-2013.
 */

package x10.simplearray;

import x10.compiler.Inline;

/**
 * Implementation of 1-dimensional Array.
 */
public final class Array_1[T] extends Array[T] {
    
    public property rank() = 1;

    /**
     * Construct a 1-dimensional array with indices 0..n-1 whose elements are zero-initialized.
     */
    public def this(n:Long) { T haszero } {
        super(n, true);
    }

    /**
     * Construct a 1-dimensional array with indices 0..n-1 whose elements are initialized to init.
     */
    public def this(n:Long, init:T) {
        super(n, false);
	for (i in raw.range()) {
            raw(i) = init;
        }
    }

    /**
     * Construct a 1-dimensional array with indices 0..n-1 whose elements are initialized to 
     * the value computed by the init closure when applied to the element index.
     */
    public @Inline def this(n:Long, init:(long)=>T) {
        super(n, false);
	for (i in raw.range()) {
            raw(i) = init(i);
        }
    }

    // Intentionally private: only for use of makeView factory method.
    private def this(r:Rail[T]{self!=null}) {
        super(r);
    }

    /**
     * Construct an Array_1 view over an existing Rail
     */
    public static def makeView[T](r:Rail[T]{self!=null}) {
        return new Array_1[T](r);
    }


    /**
     * Return the string representation of this array.
     * 
     * @return the string representation of this array.
     */
    public def toString():String = raw.toString();


    /**
     * @return an IterationSpace containing all valid Points for indexing this Array.
     */  
    public def indices():IterationSpace{self.rank==1,self.rect,self!=null} {
        return new DenseIterationSpace_1(0L, size-1L) as IterationSpace{self.rank==1,self.rect,self!=null}; // FIXME: Constraint system weakness. This cast should not be needed.
    }

    /**
     * Return the element of this array corresponding to the given index.
     * 
     * @param i the given index in the first dimension
     * @return the element of this array corresponding to the given index.
     * @see #set(T, Int)
     */
    public @Inline operator this(i:long):T {
        // Bounds checking by backing Rail is sufficient
        return raw(i);
    }

    /**
     * Return the element of this array corresponding to the given Point.
     * 
     * @param p the given Point
     * @return the element of this array corresponding to the given Point.
     * @see #set(T, Point)
     */
    public @Inline operator this(p:Point(this.rank())):T  = this(p(0));

    
    /**
     * Set the element of this array corresponding to the given index to the given value.
     * Return the new value of the element.
     * 
     * @param v the given value
     * @param i the given index in the first dimension
     * @return the new value of the element of this array corresponding to the given index.
     * @see #operator(Int)
     */
    public @Inline operator this(i:long)=(v:T):T{self==v} {
        // Bounds checking by backing Rail is sufficient
        raw(i) = v;
        return v;
    }

    /**
     * Set the element of this array corresponding to the given Point to the given value.
     * Return the new value of the element.
     * 
     * @param v the given value
     * @param p the given Point
     * @return the new value of the element of this array corresponding to the given Point.
     * @see #operator(Int)
     */
    public @Inline operator this(p:Point(this.rank()))=(v:T):T{self==v} = this(p(0)) = v;
}

// vim:tabstop=4:shiftwidth=4:expandtab