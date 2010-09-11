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
 * A method with a proto argument must return a proto type or be a void method.
 * @author vj
 */
public class MethodMustReturnProto_MustFailCompile extends x10Test {

    class A {}
    
    
    /**
     * This should be declared proto: a method with a proto argument must return
     * a proto type or be a void method.
     */
    def m(a: proto A): A = new A();
    
    
    public def run()=true;

    public static def main(Array[String](1))  {
	new MethodMustReturnProto_MustFailCompile().execute();
    }
}
