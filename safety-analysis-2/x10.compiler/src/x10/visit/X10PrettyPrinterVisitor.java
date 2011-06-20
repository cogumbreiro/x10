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

package x10.visit;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import polyglot.ast.Assign;
import polyglot.ast.Binary;
import polyglot.ast.Block_c;
import polyglot.ast.CanonicalTypeNode;
import polyglot.ast.CanonicalTypeNode_c;
import polyglot.ast.Catch;
import polyglot.ast.ClassMember;
import polyglot.ast.ConstructorCall;
import polyglot.ast.Expr;
import polyglot.ast.FieldAssign_c;
import polyglot.ast.FieldDecl_c;
import polyglot.ast.Field_c;
import polyglot.ast.Formal;
import polyglot.ast.Formal_c;
import polyglot.ast.Id;
import polyglot.ast.Id_c;
import polyglot.ast.Import_c;
import polyglot.ast.IntLit_c;
import polyglot.ast.Labeled_c;
import polyglot.ast.Lit;
import polyglot.ast.Local;
import polyglot.ast.LocalAssign_c;
import polyglot.ast.Loop_c;
import polyglot.ast.New;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Receiver;
import polyglot.ast.Special;
import polyglot.ast.Special_c;
import polyglot.ast.Stmt;
import polyglot.ast.Throw;
import polyglot.ast.Try_c;
import polyglot.ast.TypeNode;
import polyglot.ast.Unary;
import polyglot.ast.Unary_c;
import polyglot.types.ArrayType;
import polyglot.types.ClassDef;
import polyglot.types.Context;
import polyglot.types.FieldDef;
import polyglot.types.Flags;
import polyglot.types.LocalDef;
import polyglot.types.LocalInstance;
import polyglot.types.MethodInstance;
import polyglot.types.MethodInstance_c;
import polyglot.types.Name;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.types.VarInstance;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.visit.ContextVisitor;
import polyglot.visit.InnerClassRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.Translator;
import x10.Configuration;
import x10.ast.Async_c;
import x10.ast.AtEach_c;
import x10.ast.AtExpr_c;
import x10.ast.AtStmt_c;
import x10.ast.Atomic_c;
import x10.ast.Await_c;
import x10.ast.ClosureCall;
import x10.ast.ClosureCall_c;
import x10.ast.Closure_c;
import x10.ast.Contains_c;
import x10.ast.DepParameterExpr;
import x10.ast.Finish_c;
import x10.ast.ForEach_c;
import x10.ast.ForLoop_c;
import x10.ast.Future_c;
import x10.ast.Here_c;
import x10.ast.Next_c;
import x10.ast.Now_c;
import x10.ast.ParExpr;
import x10.ast.PropertyDecl;
import x10.ast.PropertyDecl_c;
import x10.ast.SettableAssign_c;
import x10.ast.StmtExpr_c;
import x10.ast.SubtypeTest_c;
import x10.ast.Tuple_c;
import x10.ast.TypeDecl_c;
import x10.ast.TypeParamNode;
import x10.ast.When_c;
import x10.ast.X10Binary_c;
import x10.ast.X10Call;
import x10.ast.X10Call_c;
import x10.ast.X10CanonicalTypeNode;
import x10.ast.X10Cast_c;
import x10.ast.X10ClassDecl_c;
import x10.ast.X10ConstructorCall_c;
import x10.ast.X10ConstructorDecl_c;
import x10.ast.X10Field_c;
import x10.ast.X10Formal;
import x10.ast.X10Instanceof_c;
import x10.ast.X10IntLit_c;
import x10.ast.X10LocalDecl_c;
import x10.ast.X10MethodDecl_c;
import x10.ast.X10New_c;
import x10.ast.X10Return_c;
import x10.ast.X10Special;
import x10.ast.X10Unary_c;
import x10.emitter.CastExpander;
import x10.emitter.Emitter;
import x10.emitter.Expander;
import x10.emitter.Inline;
import x10.emitter.Join;
import x10.emitter.Loop;
import x10.emitter.RuntimeTypeExpander;
import x10.emitter.Template;
import x10.emitter.TryCatchExpander;
import x10.emitter.TypeExpander;
import x10.types.FunctionType;
import x10.types.ParameterType;
import x10.types.X10ClassDef;
import x10.types.X10ClassType;
import x10.types.X10ConstructorDef;
import x10.types.X10ConstructorInstance;
import x10.types.X10Context;
import x10.types.X10FieldInstance;
import x10.types.X10Flags;
import x10.types.X10MethodInstance;
import x10.types.X10TypeMixin;
import x10.types.X10TypeSystem;
import x10c.ast.X10CBackingArrayAccessAssign_c;
import x10c.ast.X10CBackingArrayAccess_c;
import x10c.ast.X10CBackingArray_c;


/**
 * Visitor on the AST nodes that for some X10 nodes triggers the template
 * based dumping mechanism (and for all others just defaults to the normal
 * pretty printing).
 *
 * @author Christian Grothoff
 * @author Igor Peshansky (template classes)
 * @author Rajkishore Barik 26th Aug 2006 (added loop optimizations)
 * @author vj Refactored Emitter out.
 */
public class X10PrettyPrinterVisitor extends X10DelegatingVisitor {
	public static final String X10_RUNTIME_TYPE_CLASS = "x10.rtt.Type";
	public static final String X10_FUN_CLASS_PREFIX = "x10.core.fun.Fun";
	public static final String X10_RUNTIME_CLASS = "x10.runtime.impl.java.Runtime";

	public static final int PRINT_TYPE_PARAMS = 1;
	public static final int BOX_PRIMITIVES = 2;
	public static final int NO_VARIANCE = 4;
	public static final int NO_QUALIFIER = 8;
	public static final boolean serialize_runtime_constraints = false;
	public static final boolean reduce_generic_cast = true;

	public static final String RETURN_PARAMETER_TYPE_SUFFIX = "$G";

	final public CodeWriter w;
	final public Translator tr;
	final public Emitter er;

	private static final String X10_RTT_TYPES = "x10.rtt.Types";
	
	private static int nextId_;
	/* to provide a unique name for local variables introduce in the templates */
	public static Integer getUniqueId_() {
		return new Integer(nextId_++);
	}

	public static Name getId() {
		return Name.make("__var" + getUniqueId_() + "__");
	}

	public X10PrettyPrinterVisitor(CodeWriter w, Translator tr) {
		this.w = w;
		this.tr = tr;
		this.er = new Emitter(w,tr);
	}

	public void visit(Node n) {
	    
	    // invoke appropriate visit method for Java backend's specific nodes
	    if (n instanceof X10CBackingArray_c) {visit((X10CBackingArray_c) n);return;}
	    if (n instanceof X10CBackingArrayAccess_c) {visit((X10CBackingArrayAccess_c) n); return;} 
	    if (n instanceof X10CBackingArrayAccessAssign_c) {visit((X10CBackingArrayAccessAssign_c)n); return;}

	    // FIXME
//    	    tr.job().compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR,
// 	        "Unhandled node type: "+n.getClass(), n.position());

	    // Don't call through del; that would be recursive.
	    n.translate(w, tr);
    	}

	public void visit(Block_c n) {
	    String s = er.getJavaImplForStmt(n, (X10TypeSystem) tr.typeSystem());
	      if (s != null) {
	          w.write(s);
	      } else {
            super.visit(n);
	      }
	}

	public void visit(StmtExpr_c n) {
	    final Context c = tr.context();
	    final ArrayList<LocalInstance> capturedVars = new ArrayList<LocalInstance>();
	    // This visitor (a) finds all captured local variables,
	    // (b) adds a qualifier to every "this", and
	    // (c) rewrites fields and calls to use an explicit "this" target.
	    n = (StmtExpr_c) n.visit(new ContextVisitor(tr.job(), tr.typeSystem(), tr.nodeFactory()) {
	        public Node leaveCall(Node n) {
	            if (n instanceof Local) {
	                Local l = (Local) n;
	                assert (l.name() != null) : l.position().toString();
	                VarInstance<?> found = c.findVariableSilent(l.name().id());
	                if (found != null) {
	                    VarInstance<?> local = context().findVariableSilent(l.name().id());
	                    if (found.equals(local)) {
	                        assert (found.equals(l.localInstance())) : l.toString();
	                        capturedVars.add(l.localInstance());
	                    }
	                }
	            }
	            if (n instanceof Field_c) {
	                Field_c f = (Field_c) n;
	                return f.target((Receiver) f.target()).targetImplicit(false);
	            }
	            if (n instanceof X10Call_c) {
	                X10Call_c l = (X10Call_c) n;
	                return l.target((Receiver) l.target()).targetImplicit(false);
	            }
	            if (n instanceof Special_c) {
	                NodeFactory nf = nodeFactory();
	                Special_c s = (Special_c) n;
	                if (s.qualifier() == null) {
	                    return s.qualifier(nf.CanonicalTypeNode(n.position(), s.type()));
	                }
	            }
	            return n;
	        }
	    }.context(c.pushBlock()));
	    boolean valid = true;
	    for (LocalInstance li : capturedVars) {
	        if (!li.flags().isFinal()) {
	            valid = false;
	            break;
	        }
	    }
	    if (!valid) {
	        System.err.println("Bad statement expression: " +n+ " at " +n.position()); // DEBUG
	        n.dump(System.err);                                   // DEBUG
	        throw new InternalCompilerError("Statement expression uses non-final variables from the outer scope", n.position());
	    }
	    w.write("(new Object() { ");
	    er.printType(n.type(), PRINT_TYPE_PARAMS);
	    w.write(" eval() {");
	    w.newline(4); w.begin(0);
	    Translator tr = this.tr.context(c.pushBlock());
	    List<Stmt> statements = n.statements();
	    for (Stmt stmt : statements) {
	        tr.print(n, stmt, w);
	        w.newline();
	    }
	    w.write("return ");
	    tr.print(n, n.result(), w);
	    w.write(";");
	    w.end(); w.newline();
	    w.write("} }.eval())");
	}

	public void visit(LocalAssign_c n) {
		Local l = n.local();
		TypeSystem ts = tr.typeSystem();
		if (n.operator() == Assign.ASSIGN || l.type().isNumeric() || l.type().isBoolean() || l.type().isSubtype(ts.String(), tr.context())) {
			tr.print(n, l, w);
			w.write(" ");
			w.write(n.operator().toString());
			w.write(" ");
			er.coerce(n, n.right(), l.type());
		}
		else {
			Binary.Operator op = SettableAssign_c.binaryOp(n.operator());
			Name methodName = X10Binary_c.binaryMethodName(op);
			tr.print(n, l, w);
			w.write(" = ");
			tr.print(n, l, w);
			w.write(".");
			w.write(Emitter.mangleToJava(methodName));
			w.write("(");
			tr.print(n, n.right(), w);
			w.write(")");
		}
	}

	public void visit(FieldAssign_c n) {
		Type t = n.fieldInstance().type();

		TypeSystem ts = tr.typeSystem();
		if (n.operator() == Assign.ASSIGN || t.isNumeric() || t.isBoolean() || t.isSubtype(ts.String(), tr.context())) {
			tr.print(n, n.target(), w);
			w.write(".");
			w.write(Emitter.mangleToJava(n.name().id()));
			w.write(" ");
			w.write(n.operator().toString());
			w.write(" ");
			er.coerce(n, n.right(), n.fieldInstance().type());
		}
		else if (n.target() instanceof TypeNode || n.target() instanceof Local || n.target() instanceof Lit) {
			// target has no side effects--evaluate it more than once
			Binary.Operator op = SettableAssign_c.binaryOp(n.operator());
			Name methodName = X10Binary_c.binaryMethodName(op);
			tr.print(n, n.target(), w);
			w.write(".");
			w.write(Emitter.mangleToJava(n.name().id()));
			w.write(" ");
			w.write(" = ");
			tr.print(n, n.target(), w);
			w.write(".");
			w.write(Emitter.mangleToJava(n.name().id()));
			w.write(".");
			w.write(Emitter.mangleToJava(methodName));
			w.write("(");
			tr.print(n, n.right(), w);
			w.write(")");
		}
		else {
			// x.f += e
			// -->
			// new Object() { T eval(R target, T right) { return (target.f = target.f.add(right)); } }.eval(x, e)
			Binary.Operator op = SettableAssign_c.binaryOp(n.operator());
			Name methodName = X10Binary_c.binaryMethodName(op);
			w.write("new java.lang.Object() {");
			w.allowBreak(0, " ");
			w.write("final ");
			er.printType(n.type(), PRINT_TYPE_PARAMS);
			w.write(" eval(");
			er.printType(n.target().type(), PRINT_TYPE_PARAMS);
			w.write(" target, ");
			er.printType(n.right().type(), PRINT_TYPE_PARAMS);
			w.write(" right) {");
			w.allowBreak(0, " ");
			w.write("return (target.");
			w.write(Emitter.mangleToJava(n.name().id()));
			w.write(" = ");
			w.write("target.");
			w.write(Emitter.mangleToJava(n.name().id()));
			w.write(".");
			w.write(Emitter.mangleToJava(methodName));
			w.write("(right));");
			w.allowBreak(0, " ");
			w.write("} }.eval(");
			tr.print(n, n.target(), w);
			w.write(", ");
			tr.print(n, n.right(), w);
			w.write(")");
		}
	}


	public void visit(X10MethodDecl_c n) {
		X10TypeSystem ts = (X10TypeSystem) tr.typeSystem();

		Flags flags = n.flags().flags();

		if (n.name().id().toString().equals("main") &&
				flags.isPublic() &&
				flags.isStatic() &&
				n.returnType().type().isVoid() &&
				n.formals().size() == 1 &&
				n.formals().get(0).declType().isSubtype(ts.Rail(ts.String()), tr.context()))
		{
			Expander throwsClause = new Inline(er, "");
			if (n.throwTypes().size() > 0) {
				List<Expander> l = new ArrayList<Expander>();
				for (TypeNode tn : n.throwTypes()) {
					l.add(new TypeExpander(er, tn.type(), PRINT_TYPE_PARAMS));
				}
				throwsClause = new Join(er, "", "throws ", new Join(er, ", ", l));
			}

			new Template(er, "Main", n.formals().get(0), n.body(), tr.context().currentClass().name(), throwsClause).expand();
			return;
		}

		X10MethodInstance mi = (X10MethodInstance) n.methodDef().asInstance();

		boolean boxPrimitives = true;

		if (mi.name() == Name.make("hashCode") && mi.formalTypes().size() == 0)
			boxPrimitives = false;
		if (mi.name() == Name.make("equals") && mi.formalTypes().size() == 1)
			boxPrimitives = false;
		if (mi.name() == Name.make("hasNext") && mi.formalTypes().size() == 0)
			boxPrimitives = false;

		// Olivier: uncomment hack below to generate java code with unboxed primitives 
		//	    if (mi.name() != Name.make("apply") && mi.name() != Name.make("write") && mi.name() != Name.make("read") && mi.name() != Name.make("set"))
		//            boxPrimitives = false;
		er.generateMethodDecl(n, false);
	}



	public void visit(Id_c n) {
                w.write(Emitter.mangleToJava(n.id()));
	}


	public void visit(X10ConstructorDecl_c n) {
		w.begin(0);

		tr.print(n, n.flags(), w);
		tr.print(n, n.name(), w);
		w.write("(");

		w.begin(0);

		X10ConstructorDef ci = (X10ConstructorDef) n.constructorDef();
		X10ClassType ct = (X10ClassType) Types.get(ci.container());
		List<String> typeAssignments = new ArrayList<String>();

		for (Iterator<ParameterType> i = ct.x10Def().typeParameters().iterator(); i.hasNext(); ) {
			ParameterType p = (ParameterType) i.next();
			w.write("final ");
			w.write(X10_RUNTIME_TYPE_CLASS);
			w.write(" ");
			w.write(Emitter.mangleToJava(p.name()));

			typeAssignments.add("this." + p.name() + " = " + p.name() + ";");

			if (i.hasNext() || n.formals().size() > 0) {
				w.write(",");
				w.allowBreak(0, " ");
			}
		}

		for (Iterator<Formal> i = n.formals().iterator(); i.hasNext(); ) {
			Formal f = (Formal) i.next();
			n.print(f, w, tr);

			if (i.hasNext()) {
				w.write(",");
				w.allowBreak(0, " ");
			}
		}

		w.end();
		w.write(")");

		if (! n.throwTypes().isEmpty()) {
			w.allowBreak(6);
			w.write("throws ");

			for (Iterator<TypeNode> i = n.throwTypes().iterator(); i.hasNext(); ) {
				TypeNode tn = (TypeNode) i.next();
				er.printType(tn.type(), PRINT_TYPE_PARAMS);

				if (i.hasNext()) {
					w.write(",");
					w.allowBreak(4, " ");
				}
			}
		}

		w.end();

		if (n.body() != null) {
			if (typeAssignments.size() > 0) {
				w.write(" {");
				w.begin(4);
				if (n.body().statements().size() > 0) {
					if (n.body().statements().get(0) instanceof ConstructorCall) {
						ConstructorCall cc = (ConstructorCall) n.body().statements().get(0);
						n.printSubStmt(cc, w, tr);
						w.allowBreak(0, " ");
						if (cc.kind() == ConstructorCall.THIS)
							typeAssignments.clear();
					}
				}
				for (String s : typeAssignments) {
					w.write(s);
					w.allowBreak(0, " ");
				}
				if (n.body().statements().size() > 0) {
					if (n.body().statements().get(0) instanceof ConstructorCall)
						n.printSubStmt(n.body().statements(n.body().statements().subList(1, n.body().statements().size())), w, tr);
					// vj: the main body was not being written. Added next two lines.
					else 
						n.printSubStmt(n.body(), w, tr);
				}
				else
					n.printSubStmt(n.body(), w, tr);
				w.write("}");
				w.end();
			}
			else {
				n.printSubStmt(n.body(), w, tr);
			}
		}
		else {
			w.write(";");
		}
	}

	public void visit(Contains_c n) {
		assert false;
	}

	public void visit(TypeDecl_c n) {
		// Do not write anything.
		return;
	}



	public void visit(X10ClassDecl_c n) {
		X10TypeSystem xts = (X10TypeSystem) tr.typeSystem();
		X10Context context = (X10Context) tr.context();

		X10ClassDef def = (X10ClassDef) n.classDef();

		// Do not generate code if the class is represented natively.
		if (er.getJavaRep(def) != null) {
			w.write(";");
			w.newline();
			return;
		}

		//	    StringBuilder sb = new StringBuilder();
		//	    for (TypeParamNode t : n.typeParameters()) {
		//		if (sb.length() > 0) {
		//		    sb.append(", ");
		//		}
		//		sb.append("\"");
		//		sb.append(t.name().id());
		//		sb.append("\"");
		//	    }

		//	    if (sb.length() > 0) {
		//		w.write("@x10.generics.Parameters({");
		//		w.write(sb.toString());
		//		w.write("})");
		//		w.allowBreak(0);
		//	    }

		//	    @Parameters({"T"})
		//	    public class List implements Runnable {
		//	    	public class T {};
		//	    	public List() { }
		//	    	@Synthetic public List(Class T) { this(); }
		//	    	@Synthetic public static boolean instanceof$(Object o, String constraint) { assert(false); return true; }
		//	    	public static boolean instanceof$(Object o, String constraint, boolean b) { /*check constraint*/; return b; }
		//	    	public static Object cast$(Object o, String constraint) { /*check constraint*/; return (List)o; }
		//	    	T x;

		X10Flags flags = X10Flags.toX10Flags( n.flags().flags());

		w.begin(0);
		if (flags.isInterface()) {
			w.write(X10Flags.toX10Flags(flags.clearInterface().clearAbstract()).translate());
		}
		else {
			w.write(X10Flags.toX10Flags(flags).translate());
		}

		if (flags.isInterface()) {
			w.write("interface ");
		}
		else {
			w.write("class ");
		}

		tr.print(n, n.name(), w);

		if (n.typeParameters().size() > 0) {
			w.write("<");
			w.begin(0);
			String sep = "";
			for (TypeParamNode tp : n.typeParameters()) {
				w.write(sep);
				n.print(tp, w, tr);
				List<Type> sups = new LinkedList<Type>(tp.upperBounds());
								
				Type supClassType = null;
				for (Iterator it = sups.iterator(); it.hasNext();) {
                                    Type type = X10TypeMixin.baseType((Type) it.next());
                                    if (type instanceof ParameterType) {
                                        it.remove();
                                    }
                                    if (type instanceof X10ClassType) {
                                        if (!((X10ClassType) type).flags().isInterface()) {
                                            if (supClassType != null ) {
                                                if (type.isSubtype(supClassType, context)) {
                                                    supClassType = type;
                                                }
                                            } else {
                                                supClassType = type;
                                            }
                                            it.remove();
                                        }
                                    }
                                }
				if (supClassType != null) {
				    sups.add(0, supClassType);
				}
				
				if (sups.size() > 0) {
				    w.write(" extends ");
				    for (int i = 0; i < sups.size(); ++i) {
				        if (i != 0) w.write(" & ");
				        er.printType(sups.get(i), PRINT_TYPE_PARAMS | NO_VARIANCE);
				    }
				}
				sep = ", ";
			}
			w.end();
			w.write(">");
		}

		if (n.superClass() != null) {
			w.allowBreak(0);
			w.write("extends ");
			Type superType = n.superClass().type();
			// FIXME: HACK! If a class extends x10.lang.Object, swipe in x10.core.Ref
			if (!xts.typeEquals(superType, xts.Object(), context))
			    er.printType(superType, PRINT_TYPE_PARAMS | BOX_PRIMITIVES | NO_VARIANCE);
			else
			    w.write("x10.core.Ref");
		}

		// Filter out x10.lang.Any from the interfaces.
		List<TypeNode> interfaces = new ArrayList<TypeNode>();

		for (TypeNode tn: n.interfaces()) {
		    if (!xts.isAny(tn.type())) {
		        interfaces.add(tn);
		    }
		}
	/* Interfaces automatically extend Any
	  	if (n.flags().flags().isInterface() && interfaces.isEmpty()) {
	
		    X10TypeSystem ts = (X10TypeSystem) tr.typeSystem();
		    interfaces.add(tr.nodeFactory().CanonicalTypeNode(n.position(), ts.Any()));
		}
*/
		if (!interfaces.isEmpty()) {
			w.allowBreak(2);
			if (flags.isInterface()) {
				w.write("extends ");
			}
			else {
				w.write("implements ");
			}

			w.begin(0);
			for (Iterator<TypeNode> i = interfaces.iterator(); i.hasNext(); ) {
				TypeNode tn = (TypeNode) i.next();
				er.printType(tn.type(), PRINT_TYPE_PARAMS | BOX_PRIMITIVES | NO_VARIANCE);
				if (i.hasNext()) {
					w.write(",");
					w.allowBreak(0);
				}
			}
			w.end();
		}
		w.unifiedBreak(0);
		w.end();
		w.write("{");

		// Generate the run-time type.  We have to wrap in a class since n might be an interface
		// and interfaces can't have static methods.

//		er.generateRTTMethods(def);

//		boolean isValueType = xts.isValueType(def.asType(), (X10Context) tr.context());
//		if (def.isTopLevel()) {
//			er.generateRTType(def);
//		}
		
		// XTENLANG-1102
		er.generateRTTInstance(def);
		
		// Generate dispatcher methods.
		er.generateDispatchers(def);

		er.generateBridgeMethods(def);

		if (n.typeParameters().size() > 0) {
			w.newline(4);
			w.begin(0);
			if (! flags.isInterface()) {
				for (TypeParamNode tp : n.typeParameters()) {
					w.write("private final ");
					w.write(X10_RUNTIME_TYPE_CLASS);
					w.write(" ");
					n.print(tp.name(), w, tr);
					w.write(";");
					w.newline();
				}
			}
			w.end();
		}

		if (! flags.isInterface()) {
			if (n.properties().size() > 0) {
				w.newline(4);
				w.begin(0);
				for (PropertyDecl pd : n.properties()) {
					n.print(pd, w, tr);
					w.newline();
				}
				w.end();
			}
		}

		n.print(n.body(), w, tr);
		w.newline();
		
		// Generate structEquals for structs
        if (flags.isStruct()) {
            w.write("final public boolean structEquals(final java.lang.Object o) {");
            w.newline(4);
            w.begin(0);
            w.write("if (!(o instanceof " + def.fullName() +")) return false;");
            w.newline();
            for (PropertyDecl pd : n.properties()) {
                w.write("if (!x10.rtt.Equality.equalsequals(this." + pd.name() + ", ((" + def.fullName() + ") o)." + pd.name() + ")) return false;");
                w.newline();
            }
            for (ClassMember member : n.body().members()) {
                if (member instanceof FieldDecl_c) {
                    FieldDecl_c field = (FieldDecl_c) member;
                    if (field.flags().flags().isStatic()) continue;
                    w.write("if (!x10.rtt.Equality.equalsequals(this." + field.name() + ", ((" + def.fullName() + ") o)." + field.name() + ")) return false;");
                    w.newline();
                }
            }
            w.write("return true;");
            w.newline();
            w.end();
            w.write("}");
            w.newline();            

            w.newline();            
        }

		w.write("}");
		w.newline(0);

		if (def.isLocal()) {
			er.generateRTType(def);
		}
	}





        public void visit(X10Cast_c c) {
            TypeNode tn = c.castType();
            assert tn instanceof CanonicalTypeNode;
            
            Expr expr = c.expr();
            Type type = expr.type();


            switch (c.conversionType()) {
            case CHECKED:
            case PRIMITIVE:
            case SUBTYPE:
                    if (tn instanceof X10CanonicalTypeNode) {
                            X10CanonicalTypeNode xtn = (X10CanonicalTypeNode) tn;

                            Type t = X10TypeMixin.baseType(xtn.type());
                            Expander ex = new TypeExpander(er, t, reduce_generic_cast ? 0 : PRINT_TYPE_PARAMS);

                            Expander rt = new RuntimeTypeExpander(er, t);

                            String template= t.isBoolean() || t.isNumeric() || t.isChar() ? "primitive_cast_deptype" : "cast_deptype";

                            DepParameterExpr dep = xtn.constraintExpr();
                            
                            // for constraintedType
                            if (dep != null) {
                                    List<Expr> conds = dep.condition();
                                    X10Context xct = (X10Context) tr.context();
                                    boolean inAnonObjectScope = xct.inAnonObjectScope();
                                    xct.setAnonObjectScope();
                                    new Template(er, template, ex, expr, rt, new Join(er, " && ", conds)).expand();
                                    xct.restoreAnonObjectScope(inAnonObjectScope);
                            }
                            // e.g. any as Int (any:Any), t as Int (t:T)
                            else if (
                                    (X10TypeMixin.baseType(type) instanceof ParameterType || ((X10TypeSystem) type.typeSystem()).isAny(X10TypeMixin.baseType(type)))
                                    && (t.isBoolean() || t.isNumeric() || t.isChar())
                            ) { 
                                w.write(X10_RTT_TYPES + ".as");
                                new TypeExpander(er, t, 0).expand(tr);
                                w.write("(");
                                c.printSubExpr(expr, w, tr);
                                w.write(")");
                            }
                            else if (t.isBoolean() || t.isNumeric() || t.isChar() /*|| type.isSubtype(t, tr.context())*/) {
                                    w.begin(0);
                                    w.write("("); // put "(Type) expr" in parentheses.
                                    w.write("(");
                                    ex.expand(tr);
                                    w.write(")");
                                    // e.g. d as Int (d:Double) -> (int)(double)(Double) d 
                                    if (type.isBoolean() || type.isNumeric() || type.isChar()) {
                                            w.write(" ");
                                            w.write("(");
                                            new TypeExpander(er, type, 0).expand(tr);
                                            w.write(")");
                                            w.write(" ");
                                            if (!(expr instanceof Unary || expr instanceof Lit) && (expr instanceof X10Call)) {
                                                w.write("(");
                                                new TypeExpander(er, type, BOX_PRIMITIVES).expand(tr);
                                                w.write(")");
                                            }
                                    }
                                    w.allowBreak(2, " ");
                                    // HACK: (java.lang.Integer) -1
                                    //       doesn't parse correctly, but
                                    //       (java.lang.Integer) (-1)
                                    //       does
                                    if (expr instanceof Unary || expr instanceof Lit)
                                            w.write("(");
                                    c.printSubExpr(expr, w, tr);
                                    if (expr instanceof Unary || expr instanceof Lit)
                                            w.write(")");
                                    w.write(")");
                                    w.end();
                            }
                            else if (type.isSubtype(t, tr.context())) {
                                w.begin(0);
                                w.write("("); // put "(Type) expr" in parentheses.
                                w.write("(");
                                ex.expand(tr);
                                w.write(")");
                                w.allowBreak(2, " ");
                                X10TypeSystem xts = ((X10TypeSystem)tr.typeSystem());
                                if (expr instanceof Unary || expr instanceof Lit || (expr instanceof X10Call && !(X10TypeMixin.baseType(expr.type()) instanceof ParameterType) && xts.isRail(((X10Call) expr).target().type())))
                                    w.write("(");
                                c.printSubExpr(expr, w, tr);
                                if (expr instanceof Unary || expr instanceof Lit || (expr instanceof X10Call && !(X10TypeMixin.baseType(expr.type()) instanceof ParameterType) && xts.isRail(((X10Call) expr).target().type())))
                                    w.write(")");
                                w.write(")");
                                w.end();
                            }
                            // e.g. i as T (T is parameterType)
                            else if (t instanceof ParameterType) { 
                                    new Template(er, "cast_deptype_primitive_param", ex, expr, rt, "true").expand();
                            }
                            else {
                                    new Template(er, template, ex, expr, rt, "true").expand();
                            }
                    }
                    else {
                            throw new InternalCompilerError("Ambiguous TypeNode survived type-checking.", tn.position());
                    }
                    break;
            case UNBOXING:
                    throw new InternalCompilerError("Unboxing conversions not yet supported.", tn.position());
            case UNKNOWN_IMPLICIT_CONVERSION:
                    throw new InternalCompilerError("Unknown implicit conversion type after type-checking.", c.position());
            case UNKNOWN_CONVERSION:
                    throw new InternalCompilerError("Unknown conversion type after type-checking.", c.position());
            case BOXING:
                    throw new InternalCompilerError("Boxing conversion should have been rewritten.", c.position());
            }
    }

	public void visit(X10Instanceof_c c) {
		TypeNode tn = c.compareType();

		if (tn instanceof X10CanonicalTypeNode) {
			X10CanonicalTypeNode xtn = (X10CanonicalTypeNode) tn;
			Type t = X10TypeMixin.baseType(xtn.type());
			DepParameterExpr dep = xtn.constraintExpr();
			if (dep != null) {
				if (t.isBoolean() || t.isNumeric() || t.isChar()) {
					new Template(er, "instanceof_primitive_deptype", xtn.typeRef(Types.ref(t)), c.expr(), new Join(er, " && ", dep.condition())).expand();
				}
				else {
					new Template(er, "instanceof_deptype", xtn.typeRef(Types.ref(t)), c.expr(), new Join(er, " && ", dep.condition())).expand();
				}
				return;
			}
		}

		Type t = tn.type();
		
		// Fix for XTENLANG-1099
                X10TypeSystem xts = (X10TypeSystem) tr.typeSystem();
                if (xts.typeEquals(xts.Object(), t, tr.context())) {
		    /*
		     * Because @NativeRep of x10.lang.Object is java.lang.Object,
		     * we cannot compile "instanceof x10.lang.Object" as "instanceof @NativeRep".
		     */

                    w.write("(!");
                    w.write("(");

                    w.write("(null == ");
                    w.write("(");
                    tr.print(c, c.expr(), w);
                    w.write(")");
                    w.write(")");
                    
                    w.write(" || ");
                    new RuntimeTypeExpander(er, xts.Struct()).expand(tr);
                    w.write(".");
                    w.write("instanceof$(");
                    tr.print(c, c.expr(), w);
                    w.write(")");

                    w.write(" || ");
                    new RuntimeTypeExpander(er, xts.Byte()).expand(tr);
                    w.write(".");
                    w.write("instanceof$(");
                    tr.print(c, c.expr(), w);
                    w.write(")");

                    w.write(" || ");
                    new RuntimeTypeExpander(er, xts.Short()).expand(tr);
                    w.write(".");
                    w.write("instanceof$(");
                    tr.print(c, c.expr(), w);
                    w.write(")");
                    
                    w.write(" || ");
                    new RuntimeTypeExpander(er, xts.Int()).expand(tr);
                    w.write(".");
                    w.write("instanceof$(");
                    tr.print(c, c.expr(), w);
                    w.write(")");

                    w.write(" || ");
                    new RuntimeTypeExpander(er, xts.Long()).expand(tr);
                    w.write(".");
                    w.write("instanceof$(");
                    tr.print(c, c.expr(), w);
                    w.write(")");

                    w.write(" || ");
                    new RuntimeTypeExpander(er, xts.Float()).expand(tr);
                    w.write(".");
                    w.write("instanceof$(");
                    tr.print(c, c.expr(), w);
                    w.write(")");

                    w.write(" || ");
                    new RuntimeTypeExpander(er, xts.Double()).expand(tr);
                    w.write(".");
                    w.write("instanceof$(");
                    tr.print(c, c.expr(), w);
                    w.write(")");

                    w.write(" || ");
                    new RuntimeTypeExpander(er, xts.Char()).expand(tr);
                    w.write(".");
                    w.write("instanceof$(");
                    tr.print(c, c.expr(), w);
                    w.write(")");

                    w.write(" || ");
                    new RuntimeTypeExpander(er, xts.Boolean()).expand(tr);
                    w.write(".");
                    w.write("instanceof$(");
                    tr.print(c, c.expr(), w);
                    w.write(")");

                    // Java representation of unsigned types are same as signed ones.
                    /*
                    w.write(" || ");
                    new RuntimeTypeExpander(er, xts.UByte()).expand(tr);
                    w.write(".");
                    w.write("instanceof$(");
                    tr.print(c, c.expr(), w);
                    w.write(")");

                    w.write(" || ");
                    new RuntimeTypeExpander(er, xts.UShort()).expand(tr);
                    w.write(".");
                    w.write("instanceof$(");
                    tr.print(c, c.expr(), w);
                    w.write(")");
                    
                    w.write(" || ");
                    new RuntimeTypeExpander(er, xts.UInt()).expand(tr);
                    w.write(".");
                    w.write("instanceof$(");
                    tr.print(c, c.expr(), w);
                    w.write(")");

                    w.write(" || ");
                    new RuntimeTypeExpander(er, xts.ULong()).expand(tr);
                    w.write(".");
                    w.write("instanceof$(");
                    tr.print(c, c.expr(), w);
                    w.write(")");
                    */
                    
                    w.write(")");
                    w.write(")");
		    return;
		}
                
                // XTENLANG-1102
                if (t instanceof X10ClassType) {
                    X10ClassType ct = (X10ClassType) t;
                    X10ClassDef cd = ct.x10Def();
                    String pat = er.getJavaRTTRep(cd);

                    if (t instanceof FunctionType) {
                        FunctionType ft = (FunctionType) t;
                        List<Type> args = ft.argumentTypes();
                        Type ret = ft.returnType();
                        if (ret.isVoid()) {
                            w.write("x10.core.fun.VoidFun");
                        } else {
                            w.write("x10.core.fun.Fun");
                        }
                        w.write("_" + ft.typeParameters().size());
                        w.write("_" + args.size());
                        w.write("._RTT");
                    }
                    else if (pat == null && er.getJavaRep(cd) == null && ct.isGloballyAccessible() && ct.typeArguments().size() != 0) {
                        w.write(cd.fullName().toString() + "." + "_RTT");
                    }
                    else {
                        new RuntimeTypeExpander(er, t).expand(tr);
                    }
                } else {
                    new RuntimeTypeExpander(er, t).expand(tr);
                }

                w.write(".");
		w.write("instanceof$(");
		tr.print(c, c.expr(), w);
		
		if (t instanceof X10ClassType) {
		    X10ClassType ct = (X10ClassType) t;
		    X10ClassDef cd = ct.x10Def();
		    String pat = er.getJavaRTTRep(cd);

		    if (pat == null && er.getJavaRep(cd) == null) {
		        for (int i = 0; i < ct.typeArguments().size(); i++) {
		            w.write(", ");
		            new RuntimeTypeExpander(er, ct.typeArguments().get(i)).expand(tr);
		        }
		    }
		}
		w.write(")");
	}


	public void visit(Special_c n) {
		X10Context c = (X10Context) tr.context();
		/*
		 * The InnerClassRemover will have replaced the
		 */
		if (c.inAnonObjectScope() && n.kind() == Special.THIS
				&& c.inStaticContext()) {
			w.write(InnerClassRemover.OUTER_FIELD_NAME.toString());
			return;
		}
		if ((((X10Translator) tr).inInnerClass() || c.inAnonObjectScope() )
				&& n.qualifier() == null && n.kind() != X10Special.SELF && n.kind() != Special.SUPER) {
			er.printType(n.type(), 0);
			w.write(".");
		}
		else if (n.qualifier() != null) {
			er.printType(n.qualifier().type(), 0);
			w.write(".");
		}

		w.write(n.kind().toString());
	}



	public void visit(X10ConstructorCall_c c) {
		X10ConstructorCall_c n = c;

		if (n.qualifier() != null) {
			tr.print(c, n.qualifier(), w);
			w.write(".");
		}

		w.write(c.kind() == ConstructorCall.THIS ? "this" : "super");

		w.write("(");
		w.begin(0);

		X10ConstructorInstance mi = (X10ConstructorInstance) n.constructorInstance();
		X10ClassType ct = (X10ClassType) mi.container();

		for (Iterator<Type> i = ct.typeArguments().iterator(); i.hasNext(); ) {
			final Type at = i.next();
			new RuntimeTypeExpander(er, at).expand(tr);
			if (i.hasNext() || c.arguments().size() > 0) {
				w.write(",");
				w.allowBreak(0, " ");
			}
		}

		List<Expr> l = c.arguments();
		for (Iterator<Expr> i = l.iterator(); i.hasNext(); ) {
			Expr e = (Expr) i.next();
			c.print(e, w, tr);
			if (i.hasNext()) {
				w.write(",");
				w.allowBreak(0, " ");
			}
		}

		w.end();
		w.write(");");	
	}
	public void visit(X10New_c c) {
		X10New_c n = c;
		if (n.qualifier() != null) {

			tr.print(c, n.qualifier(), w);
			w.write(".");
		}

		w.write("new ");

		if (n.qualifier() == null) {
		        er.printType(n.objectType().type(), PRINT_TYPE_PARAMS | NO_VARIANCE);
		}
		else {
			er.printType(n.objectType().type(), PRINT_TYPE_PARAMS | NO_VARIANCE | NO_QUALIFIER);
		}

		w.write("(");
		w.begin(0);

		X10ConstructorInstance mi = (X10ConstructorInstance) n.constructorInstance();
		X10ClassType ct = (X10ClassType) mi.container();
		
		List<Type> ta = ct.typeArguments();
                if (ta.size() > 0 && !isJavaNative(n)) {
                    for (Iterator<Type> i = ta.iterator(); i.hasNext(); ) {
                        final Type at = i.next();
                        new RuntimeTypeExpander(er, at).expand(tr);
                        if (i.hasNext() || c.arguments().size() > 0) {
                                w.write(",");
                                w.allowBreak(0, " ");
                        }
                    }        
                }     

		List<Expr> l = c.arguments();
		for (Iterator<Expr> i = l.iterator(); i.hasNext(); ) {
			Expr e = (Expr) i.next();
			c.print(e, w, tr);
			if (i.hasNext()) {
				w.write(",");
				w.allowBreak(0, " ");
			}
		}
		w.end();
		w.write(")");

		if (c.body() != null) {
			w.write("{");
			tr.print(c, c.body(), w);
			w.write("}");
		}
	}

        private boolean isJavaNative(X10New_c n) {
                Type type = n.objectType().type();
                if (type instanceof X10ClassType) {
                    X10ClassDef cd = ((X10ClassType) type).x10Def();
                    String pat = er.getJavaRepParam(cd, 1);
                    if (pat != null && pat.startsWith("java.")) {
                        return true;
                    }
                }
                return false;
        }

	public void visit(Import_c c) {
		// don't generate any code at all--we should fully qualify all type names
	}

	public void visit(ClosureCall_c c) {
		Expr target = c.target();
		Type t = target.type();
		boolean base = false;

		X10TypeSystem xts = (X10TypeSystem) t.typeSystem();

		X10MethodInstance mi = c.closureInstance();

		c.printSubExpr(target, w, tr);
		w.write(".");
		w.write("apply");
		Expr expr = target;
		if (target instanceof ParExpr) {
		    expr = ((ParExpr) target).expr();
		}
		if (
		    (!(expr instanceof Closure_c) && !mi.returnType().isVoid())
		    || (expr instanceof Closure_c && X10TypeMixin.baseType(mi.returnType()) instanceof ParameterType)) {
		    w.write(RETURN_PARAMETER_TYPE_SUFFIX);
		}
		w.write("(");
		w.begin(0);
		
		for (Iterator<Type> i = mi.typeParameters().iterator(); i.hasNext(); ) {
			final Type at = i.next();
			new RuntimeTypeExpander(er, at).expand(tr);
			if (i.hasNext() || c.arguments().size() > 0) {
				w.write(",");
				w.allowBreak(0, " ");
			}
		}

		List<Expr> l = c.arguments();
		for (Iterator<Expr> i = l.iterator(); i.hasNext(); ) {
			Expr e = (Expr) i.next();
			c.print(e, w, tr);
			if (i.hasNext()) {
				w.write(",");
				w.allowBreak(0, " ");
			}
		}
		w.end();
		w.write(")");
	}
	
	public void visit(Try_c c) {
		
        TryCatchExpander expander = new TryCatchExpander(w, er, c.tryBlock(), c.finallyBlock());
        final List<Catch> catchBlocks = c.catchBlocks();
        
		if (!catchBlocks.isEmpty()) {
		    final String temp = "__$generated_wrappedex$__";
		    expander.addCatchBlock("x10.runtime.impl.java.WrappedRuntimeException", temp, new Expander(er) {
		        public void expand(Translator tr) {
                    w.newline();

                    for (int i = 0; i < catchBlocks.size(); ++i) {
		                Catch cb = catchBlocks.get(i);
		                w.write("if (" + temp + ".getCause() instanceof ");
		                new TypeExpander(er, cb.catchType(), false, false, false).expand(tr);
		                w.write(") {");
		                w.newline();
	                        
		                cb.formal().prettyPrint(w, tr);
		                w.write(" = (");
		                new TypeExpander(er, cb.catchType(), false, false, false).expand(tr);
		                w.write(") " + temp + ".getCause();");
		                w.newline();
		                
		                cb.body().prettyPrint(w, tr);
		                w.newline();
		                
		                w.write("}");
                        w.newline();
		            }
		            w.write("throw " + temp + ";");
                    w.newline();
		        }
		    });
		    
		    for (int i = 0; i < catchBlocks.size(); ++i) {
		        expander.addCatchBlock(catchBlocks.get(i));
		    }
		}
		
		expander.expand(tr);
	}
	
	public void visit(Tuple_c c) {
		Type t = X10TypeMixin.getParameterType(c.type(), 0);
		new Template(er, "tuple", 
				new TypeExpander(er, t, true, true, false),
				new RuntimeTypeExpander(er, t), 
				new TypeExpander(er, t, false, false, false),
				new Join(er, ",", c.arguments())).expand();
	}



	public void visit(X10Call_c c) {
	    Type type = X10TypeMixin.baseType(c.type());
	    X10TypeSystem xts = (X10TypeSystem) type.typeSystem();
	    if (!(type instanceof ParameterType) && (xts.isRail(c.target().type()) || xts.isValRail(c.target().type()))) {
	        String methodName = c.methodInstance().name().toString();
	        // e.g. rail.set(a,i) -> ((Object[]) rail.value)[i] = a or ((int[]/* primitive array */)rail.value)[i] = a
	        if (methodName.equals("set")) {
	            w.write("(");
	            w.write("(");
	            if (c.type().isBoolean() || c.type().isNumeric() || c.type().isChar()) {
	                new TypeExpander(er, c.type(), 0).expand();
	            }
	            else {
	                w.write("Object");
	            }
	            w.write("[]");
	            w.write(")");
	            c.print(c.target(), w, tr);
	            w.write(".value");
	            w.write(")");

	            w.write("[");
	            c.print(c.arguments().get(1), w, tr);
	            w.write("]");

	            w.write(" = ");
	            c.print(c.arguments().get(0), w, tr);
	            return;
	        }
	        // e.g. rail.apply(i) -> ((String)((Object[])rail.value)[i]) or ((int[])rail.value)[i]
	        if (methodName.equals("apply")) {
	            if (!(c.type().isBoolean() || c.type().isNumeric() || c.type().isChar())) {
	                    w.write("(");
	                    w.write("(");
	                    new TypeExpander(er, c.type(), true, false, false).expand();
	                    w.write(")");
	            }
	            
	            w.write("(");
	            w.write("(");
	            if (c.type().isBoolean() || c.type().isNumeric() || c.type().isChar()) {
	                new TypeExpander(er, c.type(), 0).expand();
	            }
	            else {
	                w.write("Object");
	            }
	            w.write("[]");
	            w.write(")");
	            c.print(c.target(), w, tr);
	            w.write(".value");
	            w.write(")");

	            w.write("[");
	            c.print(c.arguments().get(0), w, tr);
	            w.write("]");

	            if (!(c.type().isBoolean() || c.type().isNumeric() || c.type().isChar())) {
	                w.write(")");
	            }
	            return;
	        }
	    }
	    
	    X10Context context = (X10Context) tr.context();
	    
	    if (xts.isRail(c.target().type()) || xts.isValRail(c.target().type())) {
	        String methodName = c.methodInstance().name().toString();
	        if (methodName.equals("make")) {
	            Type rt = X10TypeMixin.baseType(c.type());
	            if (rt instanceof X10ClassType) {
	                Type pt = ((X10ClassType) rt).typeArguments().get(0);
	                if (!(X10TypeMixin.baseType(pt) instanceof ParameterType)) {
	                    // for makeVaxRail(type,length,init);
	                    if (c.arguments().size() == 2 && c.arguments().get(0).type().isNumeric()) {
	                        Expr expr = c.arguments().get(1);
	                        if (expr instanceof Closure_c) {
	                            Closure_c closure = (Closure_c) expr;
	                            final List<Stmt> statements = closure.body().statements();
	                            if (!throwException(statements)) {
	                                
	                                Translator tr2 = ((X10Translator) tr).inInnerClass(true);
	                                tr2 = tr2.context(expr.enterScope(tr2.context()));
	                                
	                                final Expander ex;
	                                if (pt.isBoolean() || pt.isNumeric() || pt.isChar()) {
	                                    ex = new TypeExpander(er, pt, false, false, false);
	                                } else {
	                                    ex = new Expander(er) {
	                                        public void expand(Translator tr2) {w.write("Object");}
	                                    };
	                                }
	                                final Node n = c;
	                                final Id id = closure.formals().get(0).name();
	                                Expander ex1 = new Expander(er) {
                                            @Override
                                            public void expand(Translator tr2) {
                                                for (Stmt stmt : statements) {
                                                    if (stmt instanceof X10Return_c) {
                                                        w.write("array");
                                                        w.write("[");
                                                        w.write(id.toString());
                                                        w.write("] = ");
                                                        er.prettyPrint(((X10Return_c) stmt).expr(), tr2);
                                                        w.write(";");
                                                    }
                                                    else {
                                                        er.prettyPrint(stmt, tr2);
                                                    }
                                                }
                                            }
                                        };

                                        Expander ex2 = new Expander(er) {
                                            @Override
                                            public void expand(Translator tr2) {
                                                ex.expand();
                                                w.write("[] ");
                                                w.write("array = new ");
                                                ex.expand();
                                                w.write("[length];");
                                            }
                                        };
                                        
	                                Object[] components = {
                                            new TypeExpander(er, c.target().type(), false, true, false),
                                            new TypeExpander(er, pt, true, true, false),
                                            new RuntimeTypeExpander(er, pt),
                                            c.arguments().get(0),
                                            ex1,
                                            id,
                                            ex2
	                                };
	                                er.dumpRegex("rail-make", components, tr2, 
	                                        "(new java.lang.Object() {" +
	                                    	    "final #0<#1> apply(int length) {" +
	                                    	        "#6" + 
	                                    	        "for (int #5$ = 0; #5$ < length; #5$++) {" +
	                                    		    "final int #5 = #5$;" +
	                                    		    "#4" +
	                                    		"}" +
	                                    		"return new #0<#1>(#2, #3, array);" +
	                                             "}" +
	                                         "}.apply(#3))");
	                                
	                                return;
	                            }
	                        }
	                    }
	                }
	            }
	        }
	    }

		Receiver target = c.target();
		Type t = target.type();
		boolean base = false;

		X10MethodInstance mi = (X10MethodInstance) c.methodInstance();

		String pat = er.getJavaImplForDef(mi.x10Def());
		if (pat != null) {
			boolean needsHereCheck = er.needsHereCheck(target, context);
			
			CastExpander targetArg;
			boolean cast = xts.isParameterType(t);
			if (needsHereCheck && ! (target instanceof TypeNode || target instanceof New)) {
				Template tmplate = new Template(er, "place-check", new TypeExpander(er, target.type(), true, false, false), target);
				targetArg = new CastExpander(w, er, tmplate);
				if (cast) {
					targetArg = targetArg.castTo(mi.container(), BOX_PRIMITIVES);
				}
			} else {
				targetArg = new CastExpander(w, er, target);
				if (cast) {
					targetArg = targetArg.castTo(mi.container(), BOX_PRIMITIVES);
				}
			}
			List<Type> typeArguments  = Collections.<Type>emptyList();
			if (mi.container().isClass() && !mi.flags().isStatic()) {
			    X10ClassType ct = (X10ClassType) mi.container().toClass();
			    typeArguments = ct.typeArguments();
			}
			
			List<CastExpander> args = new ArrayList<CastExpander>();
			List<Expr> arguments = c.arguments();
			for (int i = 0; i < arguments.size(); ++ i) {
			    Type ft = c.methodInstance().def().formalTypes().get(i).get();
			    if ((ft.isBoolean() || ft.isNumeric() || ft.isChar()) && X10TypeMixin.baseType(ft) instanceof ParameterType) {
			        args.add(new CastExpander(w, er, arguments.get(i)).castTo(arguments.get(i).type(), BOX_PRIMITIVES));
			    }
			    else if ((ft.isBoolean() || ft.isNumeric() || ft.isChar())) {
			        args.add(new CastExpander(w, er, arguments.get(i)).castTo(arguments.get(i).type(), 0));
			    }
			    else {
			        args.add(new CastExpander(w, er, arguments.get(i)));                                    
			    }
			}
			er.emitNativeAnnotation(pat, targetArg, mi.typeParameters(), args, typeArguments);
			return;
		}


		// Check for properties accessed using method syntax.  They may have @Native annotations too.
		if (X10Flags.toX10Flags(mi.flags()).isProperty() && mi.formalTypes().size() == 0 && mi.typeParameters().size() == 0) {
			X10FieldInstance fi = (X10FieldInstance) mi.container().fieldNamed(mi.name());
			if (fi != null) {
				String pat2 = er.getJavaImplForDef(fi.x10Def());
				if (pat2 != null) {
					Object[] components = new Object[] { target };
					er.dumpRegex("Native", components, tr, pat2);
					return;
				}
			}
		}
		
		boolean runAsync = false;
		if (mi.container().isClass() && ((X10ClassType) mi.container().toClass()).fullName().toString().equals("x10.lang.Runtime")) {
			if (mi.signature().startsWith("runAsync")) {
				runAsync = true;
			}
		}
		
		// When the target class is a generics , print a cast operation explicitly.
		if (target instanceof TypeNode) {
			er.printType(t, 0);
		}
		else {
			CastExpander miContainer;
			// add a check that verifies if the target of the call is in place 'here'
			// This is not needed for:

			boolean needsHereCheck = er.needsHereCheck((Expr) target, context);

			if (! (target instanceof Special || target instanceof New)) {

				if (needsHereCheck) {
					// don't annotate calls with implicit target, or this and super
					// the template file only emits the target
					miContainer = new CastExpander(w, er, new Template(er, "place-check", new TypeExpander(er, t, true, false, false), target));
				}
				else {
					miContainer = new CastExpander(w, er, target);
				}

				if (xts.isParameterType(t)) {
					miContainer = new CastExpander(w, er, new TypeExpander(er,  mi.container(), false, false, false), miContainer);
				}
				miContainer = new CastExpander(w, er, miContainer);
			}
			else {
				miContainer = new CastExpander(w, er, target);
			}
			miContainer.expand();
		}
		
		w.write(".");

		if (mi.typeParameters().size() > 0) {
			w.write("<");
			for (Iterator<Type> i = mi.typeParameters().iterator(); i.hasNext(); ) {
				final Type at = i.next();
				new TypeExpander(er, at, PRINT_TYPE_PARAMS | BOX_PRIMITIVES).expand(tr);
				if (i.hasNext()) {
					w.write(",");
					w.allowBreak(0, " ");
				}
			}
			w.write(">");
		}

		w.write(Emitter.mangleToJava(c.name().id()));

		boolean isInstantiateReturn = false;
		List<MethodInstance> list = mi.implemented(tr.context());
		for (MethodInstance mj : list) {
		    if (
		        mj.container().typeEquals(mi.container(), context) &&
		        X10TypeMixin.baseType(mj.def().returnType().get()) instanceof ParameterType) {
		        isInstantiateReturn = true;
		        break;
		    }
		}
		
		Type returnType = X10TypeMixin.baseType(mi.def().returnType().get());
		if (returnType instanceof ParameterType || (isInstantiateReturn)) {
		    w.write(X10PrettyPrinterVisitor.RETURN_PARAMETER_TYPE_SUFFIX);
		}

		w.write("(");
		w.begin(0);

		for (Iterator<Type> i = mi.typeParameters().iterator(); i.hasNext(); ) {
			final Type at = i.next();
			new RuntimeTypeExpander(er, at).expand(tr);
			if (i.hasNext() || c.arguments().size() > 0) {
				w.write(",");
				w.allowBreak(0, " ");
			}
		}

		List<Expr> exprs = c.arguments();
		for (int i = 0; i < exprs.size(); ++i) {
			Expr e = exprs.get(i);
			if (runAsync && e instanceof Closure_c) {
				c.print(((Closure_c)e).methodContainer(mi), w, tr);
			}
//			else if (!er.isNoArgumentType(e)) {
//				new CastExpander(w, er, e).castTo(e.type(), BOX_PRIMITIVES).expand();
//			}
			else {
			        if (e.type().isBoolean() || e.type().isNumeric() || e.type().isChar()) {
			            if (X10TypeMixin.baseType(c.methodInstance().formalTypes().get(i)) instanceof ParameterType) {
			                w.write("(");
			                er.printType(e.type(), BOX_PRIMITIVES);
			                w.write(")");
			            } else {
			                w.write("(");
			                er.printType(e.type(), 0);
			                w.write(")");
			                if (e instanceof X10Call) {
                                            Type targetType = ((X10Call) e).target().type();
                                            if (
                                                !(
                                                    ((X10TypeSystem) tr.typeSystem()).isRail(targetType)
                                                    || ((X10TypeSystem) tr.typeSystem()).isValRail(targetType)
                                                    && !(X10TypeMixin.baseType(e.type()) instanceof ParameterType)
                                                )
                                                && X10TypeMixin.baseType(((X10Call) e).methodInstance().def().returnType().get()) instanceof ParameterType
                                            ) {
			                        w.write("(");
			                        er.printType(e.type(), BOX_PRIMITIVES);
			                        w.write(")");
			                    }
			                }
			                else if (e instanceof ClosureCall) {
			                    ClosureCall cl = (ClosureCall) e;
			                    Expr expr = cl.target();
			                    if (expr instanceof ParExpr) {
			                        expr = (ParExpr) expr;
			                    }
			                    if (!(expr instanceof Closure_c) && X10TypeMixin.baseType(cl.closureInstance().def().returnType().get()) instanceof ParameterType) {
                                                w.write("(");
                                                er.printType(e.type(), BOX_PRIMITIVES);
                                                w.write(")");
			                    }
			                }
			            }
			            w.write("(");
			        } 
			        c.print(e, w, tr);
			        if (e.type().isBoolean() || e.type().isNumeric() || e.type().isChar()) {
			            w.write(")");
			        }
			}
			
			if (i != exprs.size() - 1) {
				w.write(",");
				w.allowBreak(0, " ");
			}
		}
		w.end();
		w.write(")");
	}

	public void visit(final Closure_c n) {
		Translator tr2 = ((X10Translator) tr).inInnerClass(true);
		tr2 = tr2.context(n.enterScope(tr2.context()));

		List<Expander> formals = new ArrayList<Expander>();
		List<Expander> typeArgs = new ArrayList<Expander>();
		for (final Formal f : n.formals()) {
			TypeExpander ft = new TypeExpander(er, f.type().type(), PRINT_TYPE_PARAMS | BOX_PRIMITIVES);
			typeArgs.add(ft); // must box formals
			formals.add(new Expander(er) {
				public void expand(Translator tr) {
				    er.printFormal(tr, n, f, false);
				}
			});
		}

		boolean runAsync = false;
		MethodInstance_c mi = (MethodInstance_c) n.methodContainer();
		if (mi != null 
				&& mi.container().isClass() 
				&& ((X10ClassType) mi.container().toClass()).fullName().toString().equals("x10.lang.Runtime") 
				&& mi.signature().startsWith("runAsync")) {
			runAsync = true;
		}
		
		TypeExpander ret = new TypeExpander(er, n.returnType().type(), true, true, false);
		if (!n.returnType().type().isVoid()) {
			typeArgs.add(ret);
			w.write("new x10.core.fun.Fun_0_" + n.formals().size());
		}
		else {		
			w.write("new x10.core.fun.VoidFun_0_" + n.formals().size());
		}

		if (typeArgs.size() > 0) {
			w.write("<");
			new Join(er, ", ", typeArgs).expand(tr2);
			w.write(">");
		}

		w.write("() {");
		
		if (containsPrimitive(n) || !n.returnType().type().isVoid() && !(X10TypeMixin.baseType(n.returnType().type()) instanceof ParameterType)) {
		    w.write("public final ");
		    ret.expand(tr2);
		    w.write(" apply");
		    if (!n.returnType().type().isVoid()) {
		        w.write(RETURN_PARAMETER_TYPE_SUFFIX);
		    }
		    w.write("(");
		    List<Formal> formals2 = n.formals();
		    for (int i = 0; i < formals2.size(); ++i) {
		        if (i != 0) w.write(",");
		        er.printFormal(tr2, n, formals2.get(i), true);
		    }
		    w.write(") { ");
		    if (!n.returnType().type().isVoid()) {
		        w.write("return ");
		    }
		    w.write("apply");
		    if (X10TypeMixin.baseType(n.returnType().type()) instanceof ParameterType) {
		        w.write(RETURN_PARAMETER_TYPE_SUFFIX);
		    }
		    w.write("(");
		    String delim = "";
		    for (Formal f: formals2) {
		        w.write(delim);
		        delim = ",";
		        if (f.type().type().isBoolean() || f.type().type().isNumeric() || f.type().type().isChar()) {
		            w.write("(");
		            er.printType(f.type().type(), 0);
		            w.write(")");
		        }
		        w.write(f.name().toString());
		    }
		    w.write(");");
		    w.write("}");
		    w.newline();
		}
		
		w.write("public final ");
		new TypeExpander(er, n.returnType().type(), true, false, false).expand(tr2);
		w.write(" apply");
		if (X10TypeMixin.baseType(n.returnType().type()) instanceof ParameterType) {
		    w.write(RETURN_PARAMETER_TYPE_SUFFIX);
		}
		w.write("(");
		new Join(er, ", ", formals).expand(tr2);
		w.write(") { ");
		
                List<Stmt> statements = n.body().statements();
                boolean throwException = throwException(statements);
                
                TryCatchExpander tryCatchExpander = new TryCatchExpander(w, er, n.body(), null);
                if (runAsync) {
                    tryCatchExpander.addCatchBlock("x10.runtime.impl.java.WrappedRuntimeException", "ex", new Expander(er) {
                        public void expand(Translator tr) {
                            w.write("x10.lang.Runtime.pushException(ex.getCause());");
                        }
                    });
                }
                if (throwException) {
                    tryCatchExpander.addCatchBlock("java.lang.RuntimeException", "ex", new Expander(er) {
                        public void expand(Translator tr) {
                            w.write("throw ex;");
                        }
                    });
                    
                    if (runAsync) {
                        tryCatchExpander.addCatchBlock("java.lang.Exception", "ex", new Expander(er) {
                            public void expand(Translator tr) {
                                w.write("x10.lang.Runtime.pushException(ex);");
                            }
                        });
                    } else {
                        tryCatchExpander.addCatchBlock("java.lang.Exception", "ex", new Expander(er) {
                            public void expand(Translator tr) {
                                w.write("throw new x10.runtime.impl.java.WrappedRuntimeException(ex);");
                            }
                        });
                    }
                    tryCatchExpander.expand(tr2);
                } else if (runAsync) {
                    tryCatchExpander.expand(tr2);
                } else {
                    er.prettyPrint(n.body(), tr2);
                }
                
                w.write("}");
                w.newline();

                Type type = n.type();
                type = X10TypeMixin.baseType(type);
                if (type instanceof X10ClassType) {
                        X10ClassType ct = (X10ClassType) type;
                        X10ClassDef def = ct.x10Def();
                        
                        // XTENLANG-1102
                        Set<ClassDef> visited = new HashSet<ClassDef>();
                        
                        visited = new HashSet<ClassDef>();
                        visited.add(def);
                        if (!def.flags().isInterface()) {
                                List<Type> types = new ArrayList<Type>();
                                LinkedList<Type> worklist = new LinkedList<Type>();
                                for (Type t : def.asType().interfaces()) {
                                        Type it = X10TypeMixin.baseType(t);
                                        worklist.add(it);
                                }
                                while (!worklist.isEmpty()) {
                                        Type it = worklist.removeFirst();
                                        if (it instanceof X10ClassType) {
                                                X10ClassType ct2 = (X10ClassType) it;
                                                X10ClassDef idef = ct2.x10Def();

                                                if (visited.contains(idef))
                                                        continue;
                                                visited.add(idef);

                                                for (Type t : ct2.interfaces()) {
                                                        Type it2 = X10TypeMixin.baseType(t);
                                                        worklist.add(it2);
                                                }
                                                
                                                types.addAll(ct2.typeArguments());
                                        }
                                }
                                if (types.size() > 0) {
                                    w.write("public x10.rtt.RuntimeType<?> getRTT() { return _RTT;}");
                                    w.write("public x10.rtt.Type<?> getParam(int i) {");
                                    for (int i = 0; i < types.size(); i++) {
                                        w.write("if (i ==" + i + ")");
                                        Type t = types.get(i);
                                        w.write(" return ");
                                        new RuntimeTypeExpander(er, t).expand();
                                        w.write(";");
                                    }
                                    w.write("return null;");
                                    w.newline();
                                    w.write("}");
                                    w.newline();
                                } else {
                                    
                                }
                        }
                }

                w.write("}");
        }

        private boolean throwException(List<Stmt> statements) {
            for (Stmt stmt : statements) {
                final List<Type> exceptions = new ArrayList<Type>();
                stmt.visit(
                    new NodeVisitor() {
                        @Override
                        public Node leave(Node old, Node n, NodeVisitor v) {
                            if (n instanceof X10Call_c) {
                                List<Type> throwTypes = ((X10Call_c) n).methodInstance().throwTypes();
                                if (throwTypes != null) exceptions.addAll(throwTypes);
                            }
                            if (n instanceof Throw) {
                                exceptions.add(((Throw) n).expr().type());
                            }
                            if (n instanceof X10New_c) {
                                List<Type> throwTypes = ((X10New_c) n).procedureInstance().throwTypes();
                                if (throwTypes != null) exceptions.addAll(throwTypes);
                            }
                            return n;
                        }
                });
                if (exceptions == null) {
                    continue;
                }
                for (Type type : exceptions) {
                    if (type != null) {
                        if (type.isSubtype(tr.typeSystem().Exception(), tr.context()) && !type.isSubtype(tr.typeSystem().RuntimeException(), tr.context())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

	private boolean containsPrimitive(Closure_c n) {
	    Type t = n.returnType().type();
	    if (t.isBoolean() || t.isNumeric() || t.isChar()) {
	        return true;
            }
	    for (Formal f:n.formals()) {
	        Type type = f.type().type();
	        if (type.isBoolean() || type.isNumeric() || type.isChar()) {
	            return true;
	        }
	    }
	    return false;
	}
	    
	public void visit(FieldDecl_c n) {
		if (er.hasAnnotation(n, QName.make("x10.lang.shared"))) {
			w.write ("volatile ");
		}
		
		// Hack to ensure that X10Flags are not printed out .. javac will
		// not know what to do with them.
		Flags flags = X10Flags.toX10Flags(n.flags().flags());

		FieldDecl_c javaNode = (FieldDecl_c) n.flags(n.flags().flags(flags));

		//same with FiledDecl_c#prettyPrint(CodeWriter w, PrettyPrinter tr)
		FieldDef fieldDef = javaNode.fieldDef();
        boolean isInterface = fieldDef != null && fieldDef.container() != null &&
        fieldDef.container().get().toClass().flags().isInterface();

        Flags f = javaNode.flags().flags();

        if (isInterface) {
            f = f.clearPublic();
            f = f.clearStatic();
            f = f.clearFinal();
        }

        w.write(f.translate());
        tr.print(javaNode, javaNode.type(), w);
        w.allowBreak(2, 2, " ", 1);
        tr.print(javaNode, javaNode.name(), w);

        if (javaNode.init() != null) {
            w.write(" =");
            w.allowBreak(2, " ");
            
            //X10 unique
            er.coerce(javaNode, javaNode.init(), javaNode.type().type());
        }

        w.write(";");
	}

	public void visit(PropertyDecl_c dec) {
		//		polyglot.types.Context c = tr.context();
		// Don't generate property declarations for fields.
		//		if (c.currentClass().flags().isInterface()) {
		//			return;
		//		}
		super.visit(dec);
	}



	public void visit(Async_c a) {
		assert false;
		//		Translator tr2 = ((X10Translator) tr).inInnerClass(true);
		//		new Template("Async",
		//				a.place(),
		//				processClocks(a),
		//				a.body(),
		//				a.position().nameAndLineString().replace("\\", "\\\\")).expand(tr2);
	}

	public void visit(AtStmt_c a) {
		assert false;
		//		Translator tr2 = ((X10Translator) tr).inInnerClass(true);
		//		new Template("At",
		//					 a.place(),
		//					 a.body(),
		//                     getUniqueId_(),
		//                     a.position().nameAndLineString().replace("\\", "\\\\")).expand(tr2);
	}

	public void visit(Atomic_c a) {
		assert false;
		//		new Template("Atomic", a.body(), getUniqueId_()).expand();
	}

	public void visit(Here_c a) {
		assert false;
		//		new Template("here").expand();
	}

	public void visit(Await_c c) {
		assert false;
		//		new Template("await", c.expr(), getUniqueId_()).expand();
	}
	
	public void visit(X10LocalDecl_c n) {
		if (!X10PrettyPrinterVisitor.reduce_generic_cast) {
			n.prettyPrint(w, tr);
			return;
		}
		
		//same with FieldDecl_c#prettyPrint(CodeWriter w, PrettyPrinter tr)
        boolean printSemi = tr.appendSemicolon(true);
        boolean printType = tr.printType(true);

        tr.print(n, n.flags(), w);
        if (printType) {
        	tr.print(n, n.type(), w);
            w.write(" ");
        }
        tr.print(n, n.name(), w);

        if (n.init() != null) {
            w.write(" =");
            w.allowBreak(2, " ");
            
            //X10 unique
            er.coerce(n, n.init(), n.type().type());
        }

        if (printSemi) {
            w.write(";");
        }

        tr.printType(printType);
        tr.appendSemicolon(printSemi);
	}

	public void visit(Next_c d) {
		assert false;
		//		new Template("next").expand();
	}

	public void visit(Future_c f) {
		assert false;
		//		Translator tr2 = ((X10Translator) tr).inInnerClass(true);
		//		new Template("Future", f.place(), new TypeExpander(f.returnType().type(), true, true, false, false), f.body(), new RuntimeTypeExpander(f.returnType().type()), f.position().nameAndLineString().replace("\\", "\\\\")).expand(tr2);
	}

	public void visit(AtExpr_c f) {
		assert false;
		//		Translator tr2 = ((X10Translator) tr).inInnerClass(true);
		//		new Template("AtExpr", f.place(), new TypeExpander(f.returnType().type(), true, true, false, false), f.body(), new RuntimeTypeExpander(f.returnType().type()), f.position().nameAndLineString().replace("\\", "\\\\")).expand(tr2);
	}

	public void visit(Formal_c f) {
		if (f.name().id().toString().equals(""))
			f = (Formal_c) f.name(f.name().id(Name.makeFresh("a")));
		visit((Node) f);
	}

	public void visit(ForLoop_c f) {
		X10TypeSystem ts = (X10TypeSystem) tr.typeSystem();

		X10Formal form = (X10Formal) f.formal();

		X10Context context = (X10Context) tr.context();

		/* TODO: case: for (point p:D) -- discuss with vj */
		/* handled cases: exploded syntax like: for (point p[i,j]:D) and for (point [i,j]:D) */
		if (Configuration.LOOP_OPTIMIZATIONS && form.hasExplodedVars() && (ts.isSubtype(f.domain().type(), ts.Region(), context) || ts.isSubtype(f.domain().type(), ts.Dist(), context)) && X10TypeMixin.isRect(f.domain().type(), context)) {
			String regVar = getId().toString();
			List<Name> idxs = new ArrayList<Name>();
			List<Name> lims = new ArrayList<Name>();
			List<Name> idx_vars = new ArrayList<Name>();
			List<Object> vals = new ArrayList<Object>();
			LocalDef[] lis = form.localInstances();
			int rank = lis.length;

			for (int i = 0; i < rank; i++) {
				idxs.add(getId());
				lims.add(getId());
				idx_vars.add(lis[i].name());
				vals.add(new Integer(i));
			}

			Object body = f.body();

			if (!form.isUnnamed())
				body = new Join(er, "\n",
						new Template(er, "point-create",
								form.flags(),
								form.type(),
								form.name(),
								new Join(er, ",", idxs)
						),
						body);

			body = new Join(er, "\n",
					new Loop(er, "final-var-assign", new CircularList<String>("int"), idx_vars, idxs),
					body);

			new Template(er, "forloop-mult",
					f.domain(),
					regVar,
					new Loop(er, "forloop-mult-each", idxs, new CircularList<String>(regVar), vals, lims),
					body,
					//							 new Template("forloop",
					//									 form.flags(),
					//									 form.type(),
					//									 form.name(),
					//									 regVar,
					//									 new Join("\n", new Join("\n", f.locals()), f.body()),
					//     			   			   	     new TypeExpander(form.type().type(), PRINT_TYPE_PARAMS | BOX_PRIMITIVES)
					//								 )
					new Inline(er, "assert false;")
			).expand();
		} else
			new Template(er, "forloop",
					form.flags(),
					form.type(),
					form.name(),
					f.domain(),
					new Join(er, "\n", new Join(er, "\n", f.locals()), f.body()),
					new TypeExpander(er, form.type().type(), PRINT_TYPE_PARAMS | BOX_PRIMITIVES)
			).expand();
	}



	public void visit(ForEach_c f) {
		assert (false);
		// System.out.println("X10PrettyPrinter.visit(ForEach c): |" + f.formal().flags().translate() + "|");
		er.processClockedLoop("foreach", f);
	}

	public void visit(AtEach_c f) {
		assert (false);
		er.processClockedLoop("ateach", f);
	}

	public void visit(Now_c n) {
		Translator tr2 = ((X10Translator) tr).inInnerClass(true);
		new Template(er, "Now", n.clock(), n.body()).expand(tr2);
	}

	public void visit(Labeled_c n) {
	    Stmt statement = n.statement();
	    if (statement instanceof Block_c) {
	        w.write("{");
	        Block_c block = (Block_c) statement;
	        for (Stmt s : block.statements()) {
	            if (s instanceof Loop_c) {
	                w.write(n.labelNode() + ": ");
	            }
	            tr.print(n, s, w);
	        }
            w.write("}");
	    } else {
	        w.write(n.labelNode() + ": ");
	        tr.print(n, statement, w);
	    }
	}


	/*
	 * Field access -- this includes FieldAssign (because the left node of
	 * FieldAssign is a Field node!
	 */
	public void visit(Field_c n) {
		Receiver target = n.target();
		Type t = target.type();

		X10TypeSystem xts = (X10TypeSystem) t.typeSystem();
		X10Context context = (X10Context) tr.context();
		X10FieldInstance fi = (X10FieldInstance) n.fieldInstance();

		if (target instanceof TypeNode) {
			TypeNode tn = (TypeNode) target;
			if (t instanceof ParameterType) {
				// Rewrite to the class declaring the field.
				FieldDef fd = fi.def();
				t = Types.get(fd.container());
				target = tn.typeRef(fd.container());
				n = (Field_c) n.target(target);
			}
		}
		
		String pat = er.getJavaImplForDef(fi.x10Def());
		if (pat != null) {
			Object[] components = new Object[] { target };
			er.dumpRegex("Native", components, tr, pat);
			return;
		}

		if (target instanceof TypeNode) {
			er.printType(t, 0);
			w.write(".");
			w.write(Emitter.mangleToJava(n.name().id()));
		}
		else {

			boolean is_location_access = xts.isReferenceOrInterfaceType(fi.container(), context) && fi.name().equals(xts.homeName());
			boolean needsHereCheck = er.needsHereCheck((Expr) target, context) && ! is_location_access;

			if (needsHereCheck) {
				// no check required for implicit targets, this and super
				// the template file only emits the target
				new Template(er, "place-check", new TypeExpander(er, t, true, false, false), target).expand();
				// then emit '.' and name of the field.
				w.write(".");
				w.write(Emitter.mangleToJava(n.name().id()));
			} else
				// WARNING: it's important to delegate to the appropriate visit() here!
				visit((Node)n);

		}

		// Fix XTENLANG-945 (Java backend only fix)
	        // Change field access to method access
	        if (X10Field_c.isInterfaceProperty(target.type(), fi)) {
	            w.write("()");
	        }
	}

	public void visit(IntLit_c n) {
		String val;
		if (n.kind() == X10IntLit_c.ULONG) {
			val = Long.toString(n.value()) + "L";
		}
		else if (n.kind() == IntLit_c.LONG) {
			val = Long.toString(n.value()) + "L";
		}
		else if (n.kind() == X10IntLit_c.UINT) {
			if (n.value() >= 0x80000000L)
				val = "0x" + Long.toHexString(n.value() & 0xffffffffL);
			else
				val = Long.toString(n.value() & 0xffffffffL);
		}
		else if (n.kind() == IntLit_c.INT) {
			if (n.value() >= 0x80000000L)
				val = "0x" + Long.toHexString(n.value());
			else
				val = Long.toString((int) n.value());
		} else
			throw new InternalCompilerError("Unrecognized IntLit kind " + n.kind());
		w.write(val);
	}

	//	private Stmt optionalBreak(Stmt s) {
	//		NodeFactory nf = tr.nodeFactory();
	//		if (s.reachable())
	//			return nf.Break(s.position());
	//		// [IP] Heh, Empty cannot be unreachable either.  Who knew?
	////		return nf.Empty(s.position());
	//		return null;
	//	}

	public void visit(When_c w) {
		assert false;
		//		Integer id = getUniqueId_();
		//		List breaks = new ArrayList(w.stmts().size());
		//		for (Iterator i = w.stmts().iterator(); i.hasNext(); ) {
		//			Stmt s = (Stmt) i.next();
		//			breaks.add(optionalBreak(s));
		//		}
		//		new Template("when",
		//						 w.expr(),
		//						 w.stmt(),
		//						 optionalBreak(w.stmt()),
		//						 new Loop("when-branch", w.exprs(), w.stmts(), breaks),
		//						 id
		//					 ).expand();
	}

	public void visit(Finish_c a) {
		assert false;
		//		new Template("finish", a.body(), getUniqueId_()).expand();
	}

	public void visit(SettableAssign_c n) {
		SettableAssign_c a = n;
		Expr array = a.array();
		List<Expr> index = a.index();

		boolean effects = er.hasEffects(array);
		for (Expr e : index) {
			if (effects)
				break;
			if (er.hasEffects(e))
				effects = true;
		}

		TypeSystem ts = tr.typeSystem();
		X10Context context = (X10Context) tr.context();
		Type t = n.leftType();

		boolean needsHereCheck = er.needsHereCheck(array, context);
		Template tmp = null; 
		if (needsHereCheck) {
			tmp = new Template(er, "place-check", new TypeExpander(er, array.type(), true, false, false), array);
		}

		boolean nativeop = false;
		if (t.isNumeric() || t.isBoolean() || t.isChar() || t.isSubtype(ts.String(), context)) {
			nativeop = true;
		}

		X10MethodInstance mi = (X10MethodInstance) n.methodInstance();
		boolean superUsesClassParameter = ! mi.flags().isStatic() ; // && overridesMethodThatUsesClassParameter(mi);

		if (n.operator() == Assign.ASSIGN) {
			// Look for the appropriate set method on the array and emit native code if there is an
			// @Native annotation on it.
			List<Expr> args = new ArrayList<Expr>(index.size()+1);
			//args.add(array);
			args.add(n.right());
			for (Expr e : index) args.add(e);
			String pat = er.getJavaImplForDef(mi.x10Def());

			if (pat != null) {
				er.emitNativeAnnotation(pat, null == tmp ? array : tmp, mi.typeParameters(), args, Collections.<Type>emptyList());
				return;
			} else {
				// otherwise emit the hardwired code.
				er.arrayPrint(n, array, w, tmp);
				w.write(".set");
				w.write("(");
				tr.print(n, n.right(), w);
				if (index.size() > 0)
					w.write(", ");
				new Join(er, ", ", index).expand(tr);
				w.write(")");
			}
		}
		else if (! effects) {
			Binary.Operator op = SettableAssign_c.binaryOp(n.operator());
			Name methodName = X10Binary_c.binaryMethodName(op);
			X10TypeSystem xts = (X10TypeSystem) ts;
			if ((t.isBoolean() || t.isNumeric()) && (xts.isRail(array.type()) || xts.isValRail(array.type()))) {
			    w.write("(");
			    w.write("(");
			    new TypeExpander(er, t, 0).expand();
			    w.write("[])");
			    er.arrayPrint(n, array, w, tmp);
			    w.write(".value");
			    w.write(")");
			    w.write("[");
			    new Join(er, ", ", index).expand(tr);
			    w.write("]");
			    w.write(" ");
			    w.write(op.toString());
			    w.write("=");
			    w.write(" ");
			    tr.print(n, n.right(), w);
			    return;
			}
			
			er.arrayPrint(n, array, w, tmp);
			w.write(".set");
			w.write("((");
			tr.print(n, array, w);
			w.write(").apply(");
			new Join(er, ", ", index).expand(tr);
			w.write(")");
			if (nativeop) {
				w.write(" ");
				w.write(op.toString());
				tr.print(n, n.right(), w);
			}
			else {
				w.write(".");
				w.write(Emitter.mangleToJava(methodName));
				w.write("(");
				tr.print(n, n.right(), w);
				w.write(")");
			}
			if (index.size() > 0)
				w.write(", ");
			new Join(er, ", ", index).expand(tr);
			w.write(")");
		}
		else {
			// new Object() { T eval(R target, T right) { return (target.f = target.f.add(right)); } }.eval(x, e)
			Binary.Operator op = SettableAssign_c.binaryOp(n.operator());
			Name methodName = X10Binary_c.binaryMethodName(op);
			X10TypeSystem xts = (X10TypeSystem) ts;
			if ((t.isBoolean() || t.isNumeric()) && (xts.isRail(array.type()) || xts.isValRail(array.type()))) {
			    w.write("(");
			    w.write("(");
			    new TypeExpander(er, t, 0).expand();
			    w.write("[])");
			    er.arrayPrint(n, array, w, tmp);
			    w.write(".value");
			    w.write(")");
			    w.write("[");
			    new Join(er, ", ", index).expand(tr);
			    w.write("]");
			    w.write(" ");
			    w.write(op.toString());
			    w.write("=");
			    w.write(" ");
			    tr.print(n, n.right(), w);
			    return;
			}
			
			w.write("new java.lang.Object() {");
			w.allowBreak(0, " ");
			w.write("final ");
			er.printType(n.type(), PRINT_TYPE_PARAMS);
			w.write(" eval(");
			er.printType(array.type(), PRINT_TYPE_PARAMS);
			w.write(" array");
			{
				int i = 0;
				for (Expr e : index) {
					w.write(", ");
					er.printType(e.type(), PRINT_TYPE_PARAMS);
					w.write(" ");
					w.write("i" + i);
					i++;
				}
			}
			w.write(", ");
			er.printType(n.right().type(), PRINT_TYPE_PARAMS);
			w.write(" right) {");
			w.allowBreak(0, " ");
			if (! n.type().isVoid()) {
				w.write("return ");
			}
			w.write("array.set");
			w.write("(");

			w.write(" array.apply(");
			{
				int i = 0;
				for (Expr e : index) {
					if (i != 0)
						w.write(", ");
					w.write("i" + i);
					i++;
				}
			}
			w.write(")");
			if (nativeop) {
				w.write(" ");
				w.write(op.toString());
				w.write(" right");
			}
			else {
				w.write(".");
				w.write(Emitter.mangleToJava(methodName));
				w.write("(right)");
			}
			if (index.size() > 0)
				w.write(", ");
			{
				int i = 0;
				for (Expr e : index) {
					if (i != 0)
						w.write(", ");
					w.write("i" + i);
					i++;
				}
			}
			w.write(");");
			w.allowBreak(0, " ");
			w.write("} }.eval(");
			er.arrayPrint(n, array, w, tmp);
			if (index.size() > 0)
				w.write(", ");
			new Join(er, ", ", index).expand();
			w.write(", ");
			tr.print(n, n.right(), w);
			w.write(")");
		}
	}

	public void visit(CanonicalTypeNode_c n) {
		Type t = n.type();
		if (t != null)
			er.printType(t, PRINT_TYPE_PARAMS);
		else
			// WARNING: it's important to delegate to the appropriate visit() here!
			visit((Node)n);
	}

	public void visit(X10Unary_c n) {
		Expr left = n.expr();
		Type l =  left.type();
		X10TypeSystem xts = (X10TypeSystem) tr.typeSystem();
		NodeFactory nf = tr.nodeFactory();
		Unary.Operator op = n.operator();

		if (op == Unary.POST_DEC || op == Unary.POST_INC || op == Unary.PRE_DEC || op == Unary.PRE_INC) {
			Expr expr = left;
			Type t = left.type();

			Expr target = null;
			List<Expr> args = null;
			List<TypeNode> typeArgs = null;
			X10MethodInstance mi = null;

			// Handle a(i)++ and a.apply(i)++
			if (expr instanceof ClosureCall) {
				ClosureCall e = (ClosureCall) expr;
				target = e.target();
				args = e.arguments();
				typeArgs = Collections.EMPTY_LIST; // e.typeArgs();
				mi = e.closureInstance();
			}
			else if (expr instanceof X10Call) {
				X10Call e = (X10Call) expr;
				if (e.target() instanceof Expr && e.name().id() == Name.make("apply")) {
					target = (Expr) e.target();
					args = e.arguments();
					typeArgs = e.typeArguments();
					mi = (X10MethodInstance) e.methodInstance();
				}
			}

			X10TypeSystem ts = (X10TypeSystem) tr.typeSystem();
			if (mi != null) {
				MethodInstance setter = null;

				List<Type> setArgTypes = new ArrayList<Type>();
				List<Type> setTypeArgs = new ArrayList<Type>();
				for (Expr e : args) {
					setArgTypes.add(e.type());
				}
				setArgTypes.add(expr.type());
				for (TypeNode tn : typeArgs) {
					setTypeArgs.add(tn.type());
				}
				try {
					setter = ts.findMethod(target.type(), ts.MethodMatcher(t, Name.make("set"), setTypeArgs, setArgTypes, tr.context()));
				}
				catch (SemanticException e) {
				}

				// TODO: handle type args
				// TODO: handle setter method

				w.write("new java.lang.Object() {");
				w.allowBreak(0, " ");
				w.write("final ");
				er.printType(t, PRINT_TYPE_PARAMS);
				w.write(" eval(");
				er.printType(target.type(), PRINT_TYPE_PARAMS);
				w.write(" target");
				{int i = 0;
				for (Expr e : args) {
					w.write(", ");
					er.printType(e.type(), PRINT_TYPE_PARAMS);
					w.write(" a" + (i+1));
					i++;
				}}
				w.write(") {");
				w.allowBreak(0, " ");
				er.printType(left.type(), PRINT_TYPE_PARAMS);
				w.write(" old = ");
				String pat = er.getJavaImplForDef(mi.x10Def());
				if (pat != null) {
					Object[] components = new Object[args.size()+1];
					int j = 0;
					components[j++] = "target";
					{int i = 0;
					for (Expr e : args) {
						components[j++] = "a" + (i+1);
						i++;
					}}
					er.dumpRegex("Native", components, tr, pat);
				}
				else {
					w.write("target.apply(");
					{int i = 0;
					for (Expr e : args) {
						if (i > 0) w.write(", ");
						w.write("a" + (i+1));
						i++;
					}}
					w.write(")");
				}
				w.write(";");
				w.allowBreak(0, " ");
				er.printType(left.type(), PRINT_TYPE_PARAMS);
				w.write(" neu = (");
				er.printType(left.type(), PRINT_TYPE_PARAMS);
				w.write(") old");
				w.write((op == Unary.POST_INC || op == Unary.PRE_INC ? "+" : "-") + "1");
				w.write(";");
				w.allowBreak(0, " ");
				w.write("target.set(neu");
				{int i = 0;
				for (Expr e : args) {
					w.write(", ");
					w.write("a" + (i+1));
					i++;
				}}
				w.write(");");
				w.allowBreak(0, " ");
				w.write("return ");
				w.write((op == Unary.PRE_DEC || op == Unary.PRE_INC ? "neu" : "old"));
				w.write(";");
				w.allowBreak(0, " ");
				w.write("}");
				w.allowBreak(0, " ");
				w.write("}.eval(");
				tr.print(n, target, w);
				w.write(", ");
				new Join(er, ", ", args).expand(tr);
				w.write(")");

				return;
			}
		}

		if (l.isNumeric() || l.isBoolean()) {
			visit((Unary_c)n);
			return;
		}

		Name methodName = X10Unary_c.unaryMethodName(op);
		if (methodName != null)
			er.generateStaticOrInstanceCall(n.position(), left, methodName);
		else
			throw new InternalCompilerError("No method to implement " + n, n.position());
		return;
	}

	public void visit(SubtypeTest_c n) {
		// TODO: generate a real run-time test: if sub and sup are parameters, then this should test the actual parameters.
		TypeNode sub = n.subtype();
		TypeNode sup = n.supertype();

		w.write("((");
		new RuntimeTypeExpander(er, sub.type()).expand(tr);
		w.write(")");
		if (n.equals()) {
			w.write(".equals(");
		}
		else {
			w.write(".isSubtype(");
		}
		new RuntimeTypeExpander(er, sup.type()).expand(tr);
		w.write("))");

		//	    if (sub.type().isSubtype(sup.type())) {
		//	        w.write("true");
		//	    }
		//	    else {
		//	        w.write("false");
		//	    }
	}

	// This is an enhanced version of Binary_c#prettyPrint(CodeWriter, PrettyPrinter)
    private void prettyPrint(X10Binary_c n) {
        Expr left = n.left();
        Type l = left.type();
        Expr right = n.right();
        Type r = right.type();
        Binary.Operator op = n.operator();

        boolean asPrimitive = false;
        if (op == Binary.EQ || op == Binary.NE) {
            if (l.isNumeric() && r.isNumeric() || l.isBoolean() && r.isBoolean() || l.isChar() && r.isChar()) {
                asPrimitive = true;
            }
        }

        if (asPrimitive) {
            w.write("((");
            er.printType(l, 0);
            w.write(") ");
        }
        n.printSubExpr(left, true, w, tr);
        if (asPrimitive) w.write(")");
        w.write(" ");
        w.write(op.toString());
        w.allowBreak(n.type() == null || n.type().isPrimitive() ? 2 : 0, " ");
        if (asPrimitive) {
            w.write("((");
            er.printType(r, 0);
            w.write(") ");
        }
        n.printSubExpr(right, false, w, tr);
        if (asPrimitive) w.write(")");
    }

    public void visit(X10Binary_c n) {
		Expr left = n.left();
		Type l = left.type();
		Expr right = n.right();
		Type r =  right.type();
		X10TypeSystem xts = (X10TypeSystem) tr.typeSystem();
		Binary.Operator op = n.operator();


        if (l.isNumeric() && r.isNumeric() || l.isBoolean() && r.isBoolean() || l.isChar() && r.isChar()) {
            prettyPrint(n);
            return;
        }

		if (op == Binary.EQ) {
			new Template(er, "equalsequals", left, right).expand();
			return;
		}

		if (op == Binary.NE) {
			new Template(er, "notequalsequals", left, right).expand();
			return;
		}

		if (op == Binary.ADD && (l.isSubtype(xts.String(), tr.context()) || r.isSubtype(xts.String(), tr.context()))) {
            prettyPrint(n);
			return;
		}
		if (n.invert()) {
			Name methodName = X10Binary_c.invBinaryMethodName(op);
			if (methodName != null) {
				er.generateStaticOrInstanceCall(n.position(), right, methodName, left);
				return;
			}
		}
		else {
			Name methodName = X10Binary_c.binaryMethodName(op);
			if (methodName != null) {
				er.generateStaticOrInstanceCall(n.position(), left, methodName, right);
				return;
			}
		}
		throw new InternalCompilerError("No method to implement " + n, n.position());
	}

        public void visit(X10CBackingArray_c n) {
            w.write("(");
            w.write("(");
            ArrayType arrayType = (ArrayType) n.type();
            if (arrayType.base().isBoolean() || arrayType.base().isNumeric() || arrayType.base().isChar()) {
                er.printType(arrayType.base(), 0);
            } else {
                w.write("java.lang.Object");
            }
            w.write("[]");
            w.write(")");
            er.prettyPrint(n.container(), tr);
            w.write(".");
            w.write("value");
            w.write(")");
        }
        
        public void visit(X10CBackingArrayAccess_c n) {
            w.write("(");
            w.write("(");
            er.printType(n.type(), PRINT_TYPE_PARAMS);
            w.write(")");
            
            er.prettyPrint(n.array(), tr);
            w.write("[");
            er.prettyPrint(n.index(), tr);
            w.write("]");
            w.write(")");
        }

        public void visit(X10CBackingArrayAccessAssign_c n) {
            er.prettyPrint(n.array(), tr);
            w.write("[");
            er.prettyPrint(n.index(), tr);
            w.write("]");
            
            w.write(n.operator().toString());
            
            er.prettyPrint(n.right(), tr);
        }

	/**
	 * A list of one object that has an infinite circular iterator.
	 */
	public static class CircularList<T> extends AbstractList<T> {
		private T o;
		public CircularList(T o) { this.o = o; }
		public Iterator<T> iterator() {
			return new Iterator<T>() {
				public boolean hasNext() { return true; }
				public T next() { return o; }
				public void remove() { return; }
			};
		}
		public T get(int i) { return o; }
		public int size() { return -1; }
	}

} // end of X10PrettyPrinterVisitor
