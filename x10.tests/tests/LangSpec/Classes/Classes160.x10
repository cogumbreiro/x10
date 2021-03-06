/* Current test harness gets confused by packages, but it would be in package Classes_In_Poly101;
*/
// Warning: This file is auto-generated from the TeX source of the language spec.
// If you need it changed, work with the specification writers.


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



public class Classes160 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(args:Rail[String]):void {
        new Classes160().execute();
    }


// file Classes line 1458
 // Integral-coefficient polynomials of one variable.
 static  class Poly {
   public val coeff : Rail[Long];
   public def this(coeff: Rail[Long]) { this.coeff = coeff;}
   public def degree() = (coeff.size-1);
   public def a(i:Long) = (i<0 || i>this.degree()) ? 0L : coeff(i);

   public static operator (c : Long) as Poly = new Poly([c as Long]);

   public operator this(x:Long) {
     val d = this.degree();
     var s : Long = this.a(d);
     for( i in 1L .. this.degree() ) {
        s = x * s + a(d-i);
     }
     return s;
   }

   public operator this + (p:Poly) =  new Poly(
      new Rail[Long](
         Math.max(this.coeff.size, p.coeff.size),
         (i:Long) => this.a(i) + p.a(i)
      ));
   public operator this - (p:Poly) = this + (-1)*p;

   public operator this * (p:Poly) = new Poly(
      new Rail[Long](
        this.degree() + p.degree() + 1,
        (k:Long) => sumDeg(k, this, p)
        )
      );


   public operator (n : Long) + this = (n as Poly) + this;
   public operator this + (n : Long) = (n as Poly) + this;

   public operator (n : Long) - this = (n as Poly) + (-1) * this;
   public operator this - (n : Long) = ((-n) as Poly) + this;

   public operator (n : Long) * this = new Poly(
      new Rail[Long](
        this.degree()+1,
        (k:Long) => n * this.a(k)
      ));
   private static def sumDeg(k:Long, a:Poly, b:Poly) {
      var s : Long = 0;
      for( i in 0L .. k ) s += a.a(i) * b.a(k-i);
        // x10.io.Console.OUT.println("sumdeg(" + k + "," + a + "," + b + ")=" + s);
      return s;
      };
   public final def toString() {
      var allZeroSoFar : Boolean = true;
      var s : String ="";
      for( i in 0L..this.degree() ) {
        val ai = this.a(i);
        if (ai == 0L) continue;
        if (allZeroSoFar) {
           allZeroSoFar = false;
           s = term(ai, i);
        }
        else
           s +=
              (ai > 0 ? " + " : " - ")
             +term(ai, i);
      }
      if (allZeroSoFar) s = "0";
      return s;
   }
   private final def term(ai: Long, n:Long) {
      val xpow = (n==0L) ? "" : (n==1L) ? "x" : "x^" + n ;
      return (ai == 1L) ? xpow : "" + Math.abs(ai) + xpow;
   }

   public static def Main(ss:Rail[String]):void {main(ss);};



  public static def main(Rail[String]):void {
     val X = new Poly([0L,1L]);
     val t <: Poly = 7 * X + 6 * X * X * X;
     val u <: Poly = 3 + 5*X - 7*X*X;
     val v <: Poly = t * u - 1;
     for( i in -3 .. 3) {
       x10.io.Console.OUT.println(
         "" + i + "	X:" + X(i) + "	t:" + t(i)
         + "	u:" + u(i) + "	v:" + v(i)
         );
     }
  }

}

 static class Hook {
   def run():Boolean = true;
}

}
