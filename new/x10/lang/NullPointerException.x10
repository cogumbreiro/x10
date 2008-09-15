/*
 *
 * (C) Copyright IBM Corporation 2006-2008.
 *
 *  This file is part of X10 Language.
 *
 */

package x10.lang;

import x10.compiler.Native;
import x10.compiler.NativeRep;

@NativeRep("java", "java.lang.NullPointerException")
public class NullPointerException extends RuntimeException {
    public native def this(): NullPointerException;
    public native def this(message: String): NullPointerException;
}
