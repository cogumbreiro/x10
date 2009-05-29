/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file was derived from code developed by the
 *  Jikes RVM project (http://jikesrvm.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  See the COPYRIGHT.txt file distributed with this work for information
 *  regarding copyright ownership.
 */

package x10me.opt.ir;

import x10me.opt.ir.operand.*;

/**
 * Class file for RefLoad Instruction class.
 */
public final class RefLoad extends Load {

  /**
    * Constructor for RefLoad.
    *
    * @param result
    * @param address
    * @param offset
    * @param field
    * @param guard
    */
  public RefLoad (Operand result, Operand address, Operand offset, FieldOperand field, Operand guard) {
    super (result, address, offset, field, guard);
  }
  /**
    * Constructor for RefLoad without option parameter.
    *
    * @param result
    * @param address
    * @param offset
    * @param field
    */
  public RefLoad (Operand result, Operand address, Operand offset, FieldOperand field) {
    super (result, address, offset, field, null);
  }

  /**
    * Return the name of the instruction.
    */
  public String nameOf() {
    return "RefLoad";
  }

  @Override
  public char getOpcode() {
    return Operators.RefLoad;
  }

  @Override
  public boolean isExplicitLoad() {
    return true;
  }

  @Override
  public boolean isImplicitLoad() {
    return true;
  }

}
