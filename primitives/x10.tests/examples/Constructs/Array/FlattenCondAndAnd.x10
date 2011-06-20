/*
 *
 * (C) Copyright IBM Corporation 2006
 *
 *  This file is part of X10 Test.
 *
 */
import harness.x10Test;

/**
 * Conditional and is evaluated, well, conditionally. So it must be
 * translated thus:
 *
   c = a && b
   =>
   <stmt-a>
   boolean result = a;
   if (a) {
     <stmt-b>;
     result = b;
     }
     c = result;
 *
 * @author vj
 */
 
public class FlattenCondAndAnd extends x10Test {

    val a: Array[Boolean](2);

    public def this(): FlattenCondAndAnd = {
        a = Array.make[Boolean](([1..10, 1..10] as Region)->here, ((i,j): Point) => false);
    }

    def m(x: boolean)= x;

    public def run(): boolean = {
        var x: boolean = (m(a(1, 1)) && a(0, 0))&& a(-1, -1); // the second expression will throw an exception if it is evaluated.
        return !x;
    }

    public static def main(var args: Rail[String]): void = {
        new FlattenCondAndAnd().execute();
    }
    
}