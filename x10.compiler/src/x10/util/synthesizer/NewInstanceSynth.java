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
package x10.util.synthesizer;

import java.util.ArrayList;
import java.util.List;

import polyglot.ast.Expr;
import polyglot.ast.New;
import polyglot.ast.Stmt;
import polyglot.types.ConstructorDef;
import polyglot.types.ConstructorInstance;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.Types;
import polyglot.util.Position;
import x10.ast.AnnotationNode;
import x10.ast.X10NodeFactory;
import x10.extension.X10Del;
import x10.types.X10Context;
import x10.types.X10TypeSystem;

/**
 * An simple synthesizer to create a new instance: new classType
 *
 */
public class NewInstanceSynth extends AbstractStateSynth implements IStmtSynth, IExprSynth {

    Type classType;     //The new instance's type
    
    //Default value for other needed information;
    
    List<AnnotationNode> annotations;  // annotations of the new instance
    List<Type> argTypes; //arguments' type --> If we could reason the args' type from args, the list could be eliminated
    List<Expr> args;     //arguments
    
    public NewInstanceSynth(X10NodeFactory xnf, X10Context xct, Position pos, Type classType){
        super(xnf, xct, pos);
        this.classType = classType;

        annotations = new ArrayList<AnnotationNode>();
        argTypes = new ArrayList<Type>();
        args = new ArrayList<Expr>();
    }
    
    public void addAnnotation(AnnotationNode annotation){
        annotations.add(annotation);
    }
    
    public void addArgument(Type argType, Expr arg){
        argTypes.add(argType);
        args.add(arg.type(argType));
    }
    
    public void addArguments(List<Type> argTypes, List<Expr> args){
        assert(argTypes.size() == args.size());
        for(int i = 0; i < argTypes.size(); i++){
            addArgument(argTypes.get(i), args.get(i));
        }
    }
    
    
    public Expr genExpr() throws SemanticException {

        X10TypeSystem xts = (X10TypeSystem) xct.typeSystem();

        ConstructorDef constructorDef = xts.findConstructor(classType, // receiver's
                                                                       // type
                                                            xts.ConstructorMatcher(classType, argTypes, xct))
                                                            .def();
        ConstructorInstance constructorIns = constructorDef.asInstance();

        New aNew = xnf.New(pos, xnf.CanonicalTypeNode(pos, Types.ref(classType)), args);
        // Add annotations to New
        if (annotations.size() > 0) {
            aNew = (New) ((X10Del) aNew.del()).annotations(annotations);
        }
        Expr construct = aNew.constructorInstance(constructorIns).type(classType);
        return construct;
    }
    
    public Stmt genStmt() throws SemanticException {
        return xnf.Eval(pos, genExpr());
    }
   
}
