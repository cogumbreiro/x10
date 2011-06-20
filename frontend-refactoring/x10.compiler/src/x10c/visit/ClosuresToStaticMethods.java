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
package x10c.visit;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import polyglot.frontend.Job;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import x10.ast.Block;
import x10.ast.ClassBody;
import x10.ast.ClassDecl;
import x10.ast.ClassMember;
import x10.ast.Closure;
import x10.ast.ClosureCall;
import x10.ast.Expr;
import x10.ast.Formal;
import x10.ast.Local;
import x10.ast.Node;
import x10.ast.NodeFactory;
import x10.ast.ParExpr;
import x10.ast.TypeNode;
import x10.ast.TypeParamNode;
import x10.ast.X10Call;
import x10.ast.X10MethodDecl;
import x10.ast.NodeFactory;
import x10.ast.X10Special;
import x10.types.ClassType;
import x10.types.CodeDef;
import x10.types.Flags;
import x10.types.Name;
import x10.types.ParameterType;
import x10.types.Ref;
import x10.types.SemanticException;
import x10.types.Type;
import x10.types.TypeSystem;
import x10.types.Types;
import x10.types.X10MethodDef;
import x10.types.X10ParsedClassType;
import x10.types.TypeSystem;

public class ClosuresToStaticMethods extends ContextVisitor {
    
    private static final String STATIC_METHOD_BASE_NAME = "__$closure$apply$__";

    private final TypeSystem xts;
    private final NodeFactory xnf;
    private final Map<CodeDef,List<ParameterType>> closureDefToTypePrams = new HashMap<CodeDef,List<ParameterType>>();
    
    public ClosuresToStaticMethods(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
        xts = (TypeSystem) ts;
        xnf = (NodeFactory) nf;
    }
    
    @Override
    protected Node leaveCall(Node parent, Node old, Node n, NodeVisitor v) throws SemanticException {
        if (n instanceof Block && parent instanceof Closure) {
            
            class ClosureCapChecker extends NodeVisitor {
                public boolean isCap = false;
                @Override
                public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
                    if (n instanceof Local) {
                        if (!context.isLocal(((Local) n).name().id())) {
                            isCap = true;
                            finish();
                        }
                    }
                    if (n instanceof X10Special && !((X10Special) n).isSelf()) {
                        isCap = true;
                        finish();
                    }
                    return n;
                }
            }
            
            ClosureCapChecker cc = new ClosureCapChecker();
            n.visit(cc);
            
            List<ParameterType> mtps = null;
            CodeDef ci = context.pop().currentCode();
            if (ci instanceof X10MethodDef) {
                mtps = ((X10MethodDef) ci).typeParameters();
            }
            
            if (!cc.isCap && !context.inStaticContext()) {
                closureDefToTypePrams.put(((Closure) parent).codeDef(), mtps);
            }
        }
        if (n instanceof ClassDecl) {
            ClassDecl cd = (ClassDecl) n;
            final ClassType ct = cd.classDef().asType();
            final List<ClassMember> nmembers = new ArrayList<ClassMember>();
            cd = (ClassDecl) cd.visit(
                new NodeVisitor() {
                    @Override
                    public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
                        if (n instanceof ClosureCall) {
                            ClosureCall cc = (ClosureCall) n;
                            Expr target = cc.target();
                            
                            // TODO data flow analysis
                            if (target instanceof ParExpr) {
                                target = ((ParExpr) target).expr();
                            }
                            if (target instanceof Closure) {
                                Closure closure = (Closure) target;
                                
                                if (!closureDefToTypePrams.containsKey(closure.codeDef())) {
                                    return n;
                                }
                                
                                Block body = closure.body();
                                
                                Position cg = Position.COMPILER_GENERATED;
                                Flags flags = Flags.STATIC.Final().Private();
                                
                                Name name = Name.makeFresh(STATIC_METHOD_BASE_NAME);
                                X10MethodDecl mdcl = xnf.MethodDecl(cg, xnf.FlagsNode(cg, flags), closure.returnType(), xnf.Id(cg, name), closure.formals(),  body);
                                
                                List<Ref<? extends Type>> argTypes = new ArrayList<Ref<? extends Type>>(closure.formals().size());
                                for (Formal f : closure.formals()) {
                                    Type t = f.type().type();
                                    argTypes.add(f.type().typeRef());
                                }
                                
                                List<ParameterType> rts = new ArrayList<ParameterType>();
                                List<TypeNode> tns = new ArrayList<TypeNode>();
                                List<TypeParamNode> tps = new ArrayList<TypeParamNode>();
                                if (ct instanceof X10ParsedClassType) {
                                    X10ParsedClassType pct = (X10ParsedClassType) ct;
                                    if (pct.typeArguments().size() > 0) {
                                        for (Type t3 : pct.typeArguments()) {
                                            if (t3 instanceof ParameterType) {
                                                ParameterType pt = (ParameterType) t3;
                                                tps.add(xnf.TypeParamNode(cg, xnf.Id(cg, pt.name())).type(pt));
                                                tns.add(xnf.X10CanonicalTypeNode(cg, pt));
                                                rts.add(pt);
                                            }
                                        }
                                    }
                                }
                                
                                List<ParameterType> mtps = closureDefToTypePrams.get(closure.codeDef());
                                if (mtps != null) {
                                    for (ParameterType t : mtps) {
                                        ParameterType pt = (ParameterType) t;
                                        tps.add(xnf.TypeParamNode(cg, xnf.Id(cg, pt.name())).type(pt));
                                        tns.add(xnf.X10CanonicalTypeNode(cg, pt));
                                        rts.add(pt);
                                    }
                                }
                                
                                
                                X10MethodDef md = (X10MethodDef) xts.methodDef(cg, Types.ref(ct), flags, closure.returnType().typeRef(), name, argTypes);
                                mdcl = mdcl.typeParameters(tps);
                                nmembers.add(mdcl.methodDef(md));
                                md.setTypeParameters(rts);
                                
                                X10Call x10call = (X10Call) xnf.Call(n.position(), xnf.CanonicalTypeNode(n.position(), ct), xnf.Id(n.position(), name), cc.arguments()).methodInstance(md.asInstance()).type(ct);
                                x10call = x10call.typeArguments(tns);
                                return x10call;
                            }
                        }
                        return n;
                    }
                }
            );
            ClassBody body = cd.body();
            nmembers.addAll(0, body.members());
            return ((ClassDecl) n).body(body.members(nmembers));
        }
        return n;
    }
}