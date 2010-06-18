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

package ImportTestPackage2;

/**
 * Auxiliary class for ImportTest
 */
public class _T4 {
	public static def m4(val x: int): boolean = {
		return (future(here) { x == 49 }).force();
	}
}
