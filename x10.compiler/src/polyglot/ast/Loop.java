/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 * This file was originally derived from the Polyglot extensible compiler framework.
 *
 *  (C) Copyright 2000-2007 Polyglot project group, Cornell University
 *  (C) Copyright IBM Corporation 2007-2014.
 */

package polyglot.ast;

/**
 * An immutable representation of a Java language <code>for</code>
 * statement.  Contains a statement to be executed and an expression
 * to be tested indicating whether to reexecute the statement.
 */
public interface Loop extends CompoundStmt 
{    
    /** Loop condition */
    Expr cond();

    /** Returns true of cond() evaluates to a constant. */
    boolean condIsConstant();

    /** Returns true if cond() is a constant that evaluates to true. */
    boolean condIsConstantTrue();

    /** Loop body. */
    Stmt body();

    /** Target of a continue statement in the loop body. */
    Term continueTarget();
}
