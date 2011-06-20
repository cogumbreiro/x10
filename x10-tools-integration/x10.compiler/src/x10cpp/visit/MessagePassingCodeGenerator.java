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

package x10cpp.visit;

import static x10cpp.visit.ASTQuery.assertNumberOfInitializers;
import static x10cpp.visit.ASTQuery.getConstructorId;
import static x10cpp.visit.ASTQuery.getCppRep;
import static x10cpp.visit.ASTQuery.getStringPropertyInit;
import static x10cpp.visit.Emitter.mangled_field_name;
import static x10cpp.visit.Emitter.mangled_method_name;
import static x10cpp.visit.Emitter.mangled_non_method_name;
import static x10cpp.visit.Emitter.translateFQN;
import static x10cpp.visit.Emitter.translate_mangled_FQN;
import static x10cpp.visit.Emitter.voidTemplateInstantiation;
import static x10cpp.visit.SharedVarsMethods.CONSTRUCTOR;
import static x10cpp.visit.SharedVarsMethods.DESERIALIZATION_BUFFER;
import static x10cpp.visit.SharedVarsMethods.DESERIALIZE_METHOD;
import static x10cpp.visit.SharedVarsMethods.MAKE;
import static x10cpp.visit.SharedVarsMethods.CPP_NATIVE_STRING;
import static x10cpp.visit.SharedVarsMethods.SAVED_THIS;
import static x10cpp.visit.SharedVarsMethods.SERIALIZATION_BUFFER;
import static x10cpp.visit.SharedVarsMethods.SERIALIZATION_ID_FIELD;
import static x10cpp.visit.SharedVarsMethods.SERIALIZE_BODY_METHOD;
import static x10cpp.visit.SharedVarsMethods.SERIALIZE_ID_METHOD;
import static x10cpp.visit.SharedVarsMethods.STRUCT_EQUALS;
import static x10cpp.visit.SharedVarsMethods.STRUCT_EQUALS_METHOD;
import static x10cpp.visit.SharedVarsMethods.STRUCT_THIS;
import static x10cpp.visit.SharedVarsMethods.THIS;
import static x10cpp.visit.SharedVarsMethods.VOID;
import static x10cpp.visit.SharedVarsMethods.VOID_PTR;
import static x10cpp.visit.SharedVarsMethods.REFERENCE_TYPE;
import static x10cpp.visit.SharedVarsMethods.CLOSURE_TYPE;
import static x10cpp.visit.SharedVarsMethods.chevrons;
import static x10cpp.visit.SharedVarsMethods.getId;
import static x10cpp.visit.SharedVarsMethods.getUniqueId_;
import static x10cpp.visit.SharedVarsMethods.make_ref;
import static x10cpp.visit.SharedVarsMethods.make_captured_lval;
import static x10cpp.visit.SharedVarsMethods.refsAsPointers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map;


import polyglot.ast.Allocation_c;
import polyglot.ast.AmbReceiver;
import polyglot.ast.ArrayAccess_c;
import polyglot.ast.ArrayInit_c;
import polyglot.ast.Assert_c;
import polyglot.ast.Assign_c;
import polyglot.ast.Binary;
import polyglot.ast.Binary_c;
import polyglot.ast.Block;
import polyglot.ast.Block_c;
import polyglot.ast.BooleanLit_c;
import polyglot.ast.Branch_c;
import polyglot.ast.CanonicalTypeNode;
import polyglot.ast.Case_c;
import polyglot.ast.Catch;
import polyglot.ast.Catch_c;
import polyglot.ast.CharLit_c;
import polyglot.ast.ClassBody_c;
import polyglot.ast.ClassMember;
import polyglot.ast.Conditional_c;
import polyglot.ast.ConstructorCall;
import polyglot.ast.ConstructorCall_c;
import polyglot.ast.ConstructorDecl_c;
import polyglot.ast.Do_c;
import polyglot.ast.Empty_c;
import polyglot.ast.Eval_c;
import polyglot.ast.Expr;
import polyglot.ast.FieldAssign;
import polyglot.ast.FieldDecl_c;
import polyglot.ast.Field_c;
import polyglot.ast.FloatLit_c;
import polyglot.ast.ForInit;
import polyglot.ast.ForUpdate;
import polyglot.ast.For_c;
import polyglot.ast.Formal;
import polyglot.ast.Formal_c;
import polyglot.ast.Id_c;
import polyglot.ast.If_c;
import polyglot.ast.Import_c;
import polyglot.ast.Initializer_c;
import polyglot.ast.IntLit;
import polyglot.ast.IntLit_c;
import polyglot.ast.Labeled_c;
import polyglot.ast.LocalClassDecl_c;
import polyglot.ast.LocalDecl_c;
import polyglot.ast.Local_c;
import polyglot.ast.MethodDecl;
import polyglot.ast.MethodDecl_c;
import polyglot.ast.New_c;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Node_c;
import polyglot.ast.NullLit_c;
import polyglot.ast.PackageNode_c;
import polyglot.ast.Receiver;
import polyglot.ast.Return_c;
import polyglot.ast.Stmt;
import polyglot.ast.StringLit_c;
import polyglot.ast.SwitchBlock_c;
import polyglot.ast.SwitchElement;
import polyglot.ast.Switch_c;
import polyglot.ast.Throw_c;
import polyglot.ast.Try;
import polyglot.ast.Try_c;
import polyglot.ast.TypeNode;
import polyglot.ast.Unary;
import polyglot.ast.Unary_c;
import polyglot.ast.While_c;
import polyglot.main.Reporter;
import polyglot.types.ClassType;
import polyglot.types.CodeInstance;
import polyglot.types.ConstructorInstance;
import polyglot.types.Context;
import polyglot.types.FieldInstance;
import polyglot.types.Flags;
import polyglot.types.FunctionDef;
import polyglot.types.InitializerInstance;
import polyglot.types.LocalDef;
import polyglot.types.LocalInstance;
import polyglot.types.MethodDef;
import polyglot.types.Name;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.types.VarDef;
import polyglot.types.VarInstance;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil; import x10.util.CollectionFactory;
import polyglot.util.ErrorInfo;
import polyglot.util.InternalCompilerError;
import polyglot.util.Pair;
import polyglot.util.Position;
import polyglot.util.StringUtil;
import polyglot.util.TypedList;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.Translator;
import x10.Configuration;
import x10.ast.AssignPropertyCall_c;
import x10.ast.Async_c;
import x10.ast.AtEach_c;
import x10.ast.AtExpr_c;
import x10.ast.AtStmt_c;
import x10.ast.Atomic_c;
import x10.ast.Closure;
import x10.ast.ClosureCall;
import x10.ast.ClosureCall_c;
import x10.ast.Closure_c;
import x10.ast.Finish_c;
import x10.ast.ForLoop_c;
import x10.ast.HasZeroTest_c;
import x10.ast.Here_c;
import x10.ast.LocalTypeDef_c;
import x10.ast.Next_c;
import x10.ast.ParExpr_c;
import x10.ast.PropertyDecl;
import x10.ast.PropertyDecl_c;
import x10.ast.SettableAssign;
import x10.ast.SettableAssign_c;
import x10.ast.StmtExpr_c;
import x10.ast.StmtSeq_c;
import x10.ast.SubtypeTest_c;
import x10.ast.Tuple_c;
import x10.ast.TypeDecl_c;
import x10.ast.When_c;
import x10.ast.X10Binary_c;
import x10.ast.X10Call_c;
import x10.ast.X10CanonicalTypeNode;
import x10.ast.X10CanonicalTypeNode_c;
import x10.ast.X10Cast_c;
import x10.ast.X10ClassDecl_c;
import x10.ast.X10Field_c;
import x10.ast.X10Formal;
import x10.ast.X10Instanceof_c;
import x10.ast.X10Local_c;
import x10.ast.X10MethodDecl;
import x10.ast.X10MethodDecl_c;
import x10.ast.X10Special_c;
import x10.ast.X10Unary_c;
import x10.errors.Warnings;
import x10.extension.X10Ext;
import x10.extension.X10Ext_c;
import x10.types.ClosureDef;
import x10.types.ClosureInstance;
import x10.types.ParameterType;

import x10.types.FunctionType;
import x10.types.X10ClassDef;
import x10.types.X10ClassType;
import x10.types.X10ConstructorInstance;
import x10.types.X10Def;
import x10.types.X10FieldDef;
import x10.types.X10FieldInstance;
import x10.types.X10LocalDef;
import x10.types.X10MethodDef;
import x10.types.MethodInstance;
import x10.types.X10ParsedClassType_c;

import polyglot.types.TypeSystem_c.BaseTypeEquals;
import x10.types.checker.Converter;

import x10.visit.StaticNestedClassRemover;
import x10.visit.X10DelegatingVisitor;
import x10.util.ClassifiedStream;
import x10.util.ClosureSynthesizer;
import x10.util.StreamWrapper;
import x10cpp.X10CPPCompilerOptions;
import x10cpp.X10CPPJobExt;
import x10cpp.debug.LineNumberMap;
import x10cpp.types.X10CPPContext_c;

/**
 * Primary visitor for the C++ codegenerator.
 */
public class MessagePassingCodeGenerator extends X10DelegatingVisitor {

	protected final StreamWrapper sw;
	protected final Translator tr;

	protected Emitter emitter;
	protected ASTQuery query;
	protected Reporter reporter;
	
	public MessagePassingCodeGenerator(StreamWrapper sw, Translator tr) {
		this.sw = sw;
		this.tr = tr;
		this.emitter = new Emitter(tr);
		this.query = new ASTQuery(tr);
		this.reporter = tr.job().extensionInfo().getOptions().reporter;
	}

	public void visit(Node n) {
		X10CPPContext_c context = (X10CPPContext_c) tr.context();

		tr.job().compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR,
				"Unhandled node type: "+n.getClass(), n.position());
		//n.translate(w, tr);
	}

    public void visit(Allocation_c n) {
        Type type = n.type();
        sw.allowBreak(0, " ");
        String typeName = Emitter.translateType(type);
        X10CPPContext_c context = (X10CPPContext_c) tr.context();
        if (null != context.getStackAllocName()) return;
        if (Types.isX10Struct(type)) {
            sw.write(typeName+ "::" +SharedVarsMethods.ALLOC+ "()");
        } else {
            // XTENLANG-1407: Remove this memset call once we finish the default value specification/implementation.
            //                Expect it to be more efficient to explicitly initialize all of the object fields instead
            //                of first calling memset, then storing into most of the fields a second time.
            sw.write("x10aux::ref"+chevrons(typeName)+"((new (memset(x10aux::alloc"+chevrons(typeName)+"(), 0, sizeof("+typeName+"))) "+typeName+"()))");
            sw.newline();
        }
    }

    public void visit(ConstructorCall_c s) {
        Expr target = s.target();
        Type type   = target.type();
        ConstructorCall call = (ConstructorCall)s;
        boolean noArgsYet = true;
        sw.write("(");
        s.print(target, sw, tr);
        String container = Emitter.translateType(call.constructorInstance().container());
        sw.write(")->::" +container+ "::" +SharedVarsMethods.CONSTRUCTOR+ "(");
        List<Expr> args = call.arguments();
        for (int i=0; i<args.size(); i++) {
            if (noArgsYet) {
                sw.allowBreak(2, 2, "", 0); // miser mode
                sw.begin(0);
                noArgsYet = false;
            } else {
                sw.write(",");
                sw.allowBreak(0, " ");
            }
            Expr e = args.get(i);
            s.print(e, sw, tr);
        }
        if (!noArgsYet)
            sw.end();
        sw.write(");");
        sw.newline();
    }
    
    public void visit(TypeDecl_c n) {
        // do nothing
        sw.write(" /* " + n + " *" + "/ ");
        sw.newline();
    }

    public void visit(LocalTypeDef_c n) {
        // do nothing
        sw.write(" /* " + n + " *" + "/ ");
        sw.newline();
    }

	public void visit(X10ClassDecl_c n) {
		processClass(n);
	}

	/**
	 * Returns an all-void instantiation of a given class type ct.
	 */
	public static X10ClassType getStaticMemberContainer(X10ClassDef cd) {
	    if (cd.typeParameters().size() == 0)
	        return cd.asType();
	    TypeSystem xts = cd.typeSystem();
	    List<Type> args = new TypedList<Type>(new ArrayList<Type>(), Type.class, false);
	    for (int i = 0; i < cd.typeParameters().size(); i++) {
	        args.add(xts.Void());
	    }
	    return cd.asType().typeArguments(args);
	}

	/**
	 * Is the given expression constant and not a native field.
	 */
	private boolean isConstant(Expr e) {
	    // [IP] HACK for XTENLANG-486.  The only way a native expression would be marked a constant
	    // is if it's a field access.
	    String pat = e instanceof Field_c ? getCppImplForDef((X10FieldDef) ((Field_c) e).fieldInstance().def()) : null;
	    return query.isConstantExpression(e) && pat == null;
	}

	private boolean isGlobalInit(FieldDecl_c fd) {
	    // [DC] want these to occur in the static initialiser
	    // [IP] except for the ones that use a literal init - otherwise switch is broken
	    return (fd.init() != null &&
	            fd.flags().flags().isStatic() && fd.flags().flags().isFinal() &&
	            isConstant(fd.init()) &&
	            (fd.init().type().isNumeric() || fd.init().type().isBoolean() ||
	             fd.init().type().isChar() || fd.init().type().isNull()))
	             || isPerProcess((X10FieldDef) fd.fieldDef());
	}

    private void extractGenericStaticDecls(X10ClassDef cd, ClassifiedStream h) {
		if (cd.typeParameters().size() == 0) return;

		X10CPPContext_c context = (X10CPPContext_c) tr.context();
		h.write("template <> class ");
		h.write(mangled_non_method_name(cd.name().toString()));
		h.write(voidTemplateInstantiation(cd.typeParameters().size()));
		if (!cd.isStruct() && cd.superType() != null) {
		    h.write(" : public ");
		    X10ClassDef sdef = ((X10ClassType) Types.baseType(cd.superType().get())).x10Def();
		    h.write(Emitter.translateType(getStaticMemberContainer(sdef), false));
		}
		h.allowBreak(0, " ");
		h.write("{");
		h.newline(4); h.begin(0);
		h.write("public:"); h.newline();
		h.write("static x10aux::RuntimeType rtt;"); h.newline();
		h.write("static const x10aux::RuntimeType* getRTT() { return & rtt; }"); h.newline();
		
		// First process all classes
		for (ClassMember dec : context.pendingStaticDecls()) {
		    if (dec instanceof X10ClassDecl_c) {
		        assert false : "nested class alert! "+cd.position();
		    X10ClassDecl_c cdecl = (X10ClassDecl_c) dec;
		    ((X10CPPTranslator)tr).setContext(cdecl.enterScope(context)); // FIXME
		    X10ClassDef def = cdecl.classDef();
		    if (getCppRep(def) != null) {
		        // emit no c++ code as this is a native rep class
		        continue;
		    }
		    emitter.printTemplateSignature(def.typeParameters(), h);
		    h.write("class ");
		    h.write(Emitter.mangled_non_method_name(cdecl.name().id().toString()));
		    h.write(";");
		    ((X10CPPTranslator)tr).setContext(context); // FIXME
		    h.newline();
		    }
		}
		// Then process all fields and methods
		for (ClassMember dec : context.pendingStaticDecls()) {
		    if (dec instanceof FieldDecl_c) {
		        FieldDecl_c fd = (FieldDecl_c) dec;
		        ((X10CPPTranslator)tr).setContext(fd.enterScope(context)); // FIXME
		        sw.pushCurrentStream(h);
		        emitter.printHeader(fd, h, tr, false);
		        sw.popCurrentStream();
		        h.write(";");
		        ((X10CPPTranslator)tr).setContext(context); // FIXME
		    } else if (dec instanceof X10MethodDecl_c) {
		        X10MethodDecl_c md = (X10MethodDecl_c) dec;
		        ((X10CPPTranslator)tr).setContext(md.enterScope(context)); // FIXME
		        sw.pushCurrentStream(h);
		        emitter.printHeader(md, sw, tr, false, false);
		        sw.popCurrentStream();
		        h.write(";");
		        ((X10CPPTranslator)tr).setContext(context); // FIXME
		    }
		    h.newline(); h.forceNewline();
		}

        extractGenericStaticInits(cd);
		h.end(); h.newline();
		h.write("};");
		h.newline();
	}

	private void extractGenericStaticInits(X10ClassDef cd) {
	    X10CPPContext_c context = (X10CPPContext_c) tr.context();

	    // Always write non-template static decls into the implementation file
	    ClassifiedStream save_w = sw.currentStream();
	    ClassifiedStream w = context.staticDefinitions;
	    sw.pushCurrentStream(w);

	    X10ClassType declClass = (X10ClassType)cd.asType().toClass();
	    String declClassName = translate_mangled_FQN(cd.fullName().toString());
	    String container = declClassName+voidTemplateInstantiation(cd.typeParameters().size());

	    sw.write("x10aux::RuntimeType "+container+"::rtt;");
	    sw.newline();

	    for (ClassMember dec : context.pendingStaticDecls()) {
	        if (dec instanceof FieldDecl_c) {
	            FieldDecl_c fd = (FieldDecl_c) dec;
	            ((X10CPPTranslator)tr).setContext(fd.enterScope(context)); // FIXME
	            emitter.printType(fd.type().type(), sw);
	            sw.allowBreak(2, " ");
	            sw.write(container+"::");
	            sw.write(mangled_field_name(fd.name().id().toString()));
	            // [DC] want these to occur in the static initialiser instead
	            // [IP] except for the ones that use a literal init - otherwise switch is broken
	            boolean globalInit = isGlobalInit(fd);
	            if (globalInit) {
	                sw.write(" =");
	                sw.allowBreak(2, " ");
	                fd.print(fd.init(), sw, tr);
	            }
	            sw.write(";");
	            sw.newline();
	            if (!globalInit && fd.init() != null) {
	                generateStaticFieldInitializer(fd, container, sw);
	            }
	            generateStaticFieldSupportCode(fd, container, globalInit, sw);
	            ((X10CPPTranslator)tr).setContext(context); // FIXME
	        } else if (dec instanceof X10MethodDecl_c) {
	            X10MethodDecl_c md = (X10MethodDecl_c) dec;
	            X10MethodDef def = md.methodDef();
	            boolean templateMethod = def.typeParameters().size() != 0;
	            if (templateMethod)
	                sw.pushCurrentStream(save_w);
	            ((X10CPPTranslator)tr).setContext(md.enterScope(context)); // FIXME
	            emitter.printTemplateSignature(def.typeParameters(), sw);
	            emitter.printType(md.returnType().type(), sw);
	            sw.allowBreak(2, " ");
	            sw.write(container+"::");
	            sw.write(mangled_method_name(md.name().id().toString()) + "(");
	            sw.begin(0);
	            boolean first = true;
	            for (Formal f : md.formals()) {
	                if (first) {
	                    first = false;
	                } else {
	                    sw.write(",");
	                    sw.allowBreak(0, " ");
	                }
	                md.print(f, sw, tr);
	            }
	            sw.end();
	            sw.write(")");
	            if (md.body() != null) {
	                sw.allowBreak(0, " ");
	                md.printBlock(md.body(), sw, tr);
	            }
	            sw.newline();
	            ((X10CPPTranslator)tr).setContext(context); // FIXME
	            if (templateMethod)
	                sw.popCurrentStream();
	        } else if (dec instanceof Initializer_c) {
	            assert (false) : ("Static initializer alert!");
	        } else if (dec instanceof X10ClassDecl_c) {
	            assert (false) : ("Nested class alert!");
	        }
	    }

	    sw.forceNewline();
	    sw.popCurrentStream();
	}

	private void extractAllClassTypes(Type t, List<ClassType> types, Set<ClassType> dupes) {
        TypeSystem xts = (TypeSystem) t.typeSystem();
        t = xts.expandMacros(t);
		if (!t.isClass())
			return;
		X10ClassType ct = (X10ClassType) t.toClass();
		if (!dupes.contains(ct)) {
		    dupes.add(ct);
		    types.add(ct);
		}
		if (ct.typeArguments() != null) {
		    for (Type pt : ct.typeArguments())
		        extractAllClassTypes(pt, types, dupes);
		}
		if (ct.isMember() || ct.isLocal())
			extractAllClassTypes(ct.container(), types, dupes);
	}

    private void declareClass(X10ClassDef cd, ClassifiedStream h) {
        assert (cd != null);
        QName pkg = null;
        if (cd.package_() != null)
            pkg = cd.package_().get().fullName();
        if (pkg != null) {
            Emitter.openNamespaces(h, pkg);
            h.newline(0);
        }
        emitter.printTemplateSignature(cd.typeParameters(), h);
        String name;
        if (cd.isFunction()) {
            name = Emitter.baseName((FunctionType)cd.asType(), false);
        } else {
            name = StaticNestedClassRemover.mangleName(cd).toString();
        }
        h.write("class "+Emitter.mangled_non_method_name(name)+";");
        h.newline();
        if (pkg != null) {
            h.newline(0);
            Emitter.closeNamespaces(h, pkg);
            h.newline(0);
        }
    }
    
    public static String getHeader(ClassType ct) {
        String pkg = null;
        if (ct.package_() != null)
            pkg = ct.package_().fullName().toString();
        if (ct instanceof FunctionType) {
            String name = Emitter.baseName((FunctionType)ct, false);
            return X10CPPTranslator.outputFileName(pkg, name, StreamWrapper.Header);
        } else {
            Name name = StaticNestedClassRemover.mangleName(ct.def());
            String header = X10CPPTranslator.outputFileName(pkg, name.toString(), StreamWrapper.Header);
            return header;            
        }
    }

    private static String getHeaderGuard(String header) {
        return header.replace('/','_').replace('.','_').replace('$','_').toUpperCase();
    }
    
    void processClass(X10ClassDecl_c n) {
		X10CPPContext_c context = (X10CPPContext_c) tr.context();
		X10ClassDef def = (X10ClassDef) n.classDef();
        TypeSystem xts =  tr.typeSystem();
        boolean isStruct = xts.isStructType(def.asType());
        X10Ext ext = (X10Ext) n.ext();

		if (getCppRep(def) != null) {
			// emit no c++ code as this is a native rep class
			return;
		}

        assert (!def.isNested()) : ("Nested class alert!");

        boolean inTemplate = def.typeParameters().size() != 0;

        ClassifiedStream save_body = sw.body();
        ClassifiedStream save_header = sw.header();
        ClassifiedStream save_generic = context.genericFunctions;
        ClassifiedStream save_generic_cls = context.genericFunctionClosures;
        ClassifiedStream save_closures = context.closures;
        ClassifiedStream save_static_defs = context.staticDefinitions;
        // Header stream
        ClassifiedStream h = sw.getNewStream(StreamWrapper.Header, false);
        // Stream for closures that are within generic functions (always in the header, may be empty)
        ClassifiedStream g_cls = sw.getNewStream(StreamWrapper.Header, false);
        context.genericFunctionClosures = g_cls;
        // Stream for generic functions (always in the header, may be empty)
        ClassifiedStream g = sw.getNewStream(StreamWrapper.Header, false);
        context.genericFunctions = g;

        // Implementation stream.
        //    In generic context, in .h, but after the logical header stream.
        //    In non-generic context, in .cc
        ClassifiedStream w_header = sw.getNewStream(inTemplate ? StreamWrapper.Header : StreamWrapper.CC, false);
        ClassifiedStream w_closures = sw.getNewStream(inTemplate ? StreamWrapper.Header : StreamWrapper.CC, false);
        context.closures = w_closures;
        ClassifiedStream w_body = sw.getNewStream(inTemplate ? StreamWrapper.Header : StreamWrapper.CC, false);
        ClassifiedStream stat_defs = sw.getNewStream(StreamWrapper.CC, false);
        context.staticDefinitions = stat_defs;
        ClassifiedStream w_footer = sw.getNewStream(inTemplate ? StreamWrapper.Header : StreamWrapper.CC, false);
        // Dependences guard closing stream (comes at the end of the header)
        ClassifiedStream z = sw.getNewStream(StreamWrapper.Header, false);
        sw.set(h, w_body);

        context.setInsideClosure(false);

        // Write the header for the class
        String cheader = getHeader(def.asType());
        String cguard = getHeaderGuard(cheader);
        h.write("#ifndef __"+cguard); h.newline();
        h.write("#define __"+cguard); h.newline();
        h.forceNewline(0);
        h.write("#include <x10rt.h>"); h.newline();
        h.forceNewline(0);
        // process annotations relating to additional h/c++ files
        try {
            X10CPPCompilerOptions opts = (X10CPPCompilerOptions) tr.job().extensionInfo().getOptions();
            List<X10ClassType> as = ext.annotationMatching(xts.systemResolver().findOne(QName.make("x10.compiler.NativeCPPInclude")));
            for (Type at : as) {
                ASTQuery.assertNumberOfInitializers(at, 1);
                String include = getStringPropertyInit(at, 0);
                h.write("#include \""+include+"\""); h.newline();
            }
        } catch (SemanticException e) {
            assert false : e;
        }
        h.forceNewline(0);

        g.write("#ifndef "+cguard+"_GENERICS"); g.newline();
        g.write("#define "+cguard+"_GENERICS"); g.newline();

        if (inTemplate) {
            w_header.write("#ifndef "+cguard+"_IMPLEMENTATION"); w_header.newline();
            w_header.write("#define "+cguard+"_IMPLEMENTATION"); w_header.newline();
        }

        w_header.write("#include <"+cheader+">"); w_header.newline();
        w_header.forceNewline(0);
        
        if (inTemplate) {
            stat_defs.writeln("#include <"+cheader+">");
            stat_defs.forceNewline(0);
        }

        String packageName = context.package_() == null ? null : context.package_().fullName().toString();

		Set<String> allIncludes = CollectionFactory.newHashSet();
		if (n.superClass() != null) {
		    ClassType ct = n.superClass().type().toClass();
		    X10ClassDef scd = (X10ClassDef) ct.def();
		    if (scd != def) {
		        String header = getHeader(ct);
		        String guard = getHeaderGuard(header);
		        h.write("#define "+guard+"_NODEPS"); h.newline();
		        h.write("#include <" + header + ">");
		        h.newline();
		        h.write("#undef "+guard+"_NODEPS"); h.newline();
		        allIncludes.add(getHeader(ct));
		    }

		    ArrayList<ClassType> types = new ArrayList<ClassType>();
		    Set<ClassType> dupes = CollectionFactory.newHashSet();
		    dupes.add(ct);
		    extractAllClassTypes(ct, types, dupes);
		    for (ClassType t : types) {
		        X10ClassDef cd = ((X10ClassType)t).x10Def();
		        if (cd != def && getCppRep(cd) == null) {
		            String header = getHeader(t);
		            if (!allIncludes.contains(header)) {
		                declareClass(cd, h);
		                allIncludes.add(header);
		            }
		        }
		    }
		}
		for (TypeNode i : n.interfaces()) {
		    ClassType ct = i.type().toClass();
		    X10ClassDef icd = (X10ClassDef) ct.def();
		    if (icd != def) {
		        String header = getHeader(ct);
		        String guard = getHeaderGuard(header);
		        h.writeln("#define "+guard+"_NODEPS");
		        h.writeln("#include <" + header + ">");
		        h.writeln("#undef "+guard+"_NODEPS");
		        allIncludes.add(getHeader(ct));
		    }
		    ArrayList<ClassType> types = new ArrayList<ClassType>();
		    Set<ClassType> dupes = CollectionFactory.newHashSet();
		    dupes.add(ct);
		    extractAllClassTypes(ct, types, dupes);
		    for (ClassType t : types) {
		        X10ClassDef cd = ((X10ClassType)t).x10Def();
		        if (cd != def && getCppRep(cd) == null) {
		            String header = getHeader(t);
		            if (!allIncludes.contains(header)) {
		                declareClass(cd, h);
		                allIncludes.add(header);
		            }
		        }
		    }
		}

		// If any instance fields are struct types, then include the .h
		List<FieldInstance> fields = def.asType().fields();
		if (!fields.isEmpty()) {
		    Set<Type> dupes = CollectionFactory.newHashSet();
            Set<ClassType> dupes2 = CollectionFactory.newHashSet();
		    for (FieldInstance fi : fields) {
		        if (!fi.flags().isStatic()) {
		            Type fct = fi.type();
		            if (!dupes.contains(fct)) {
		                dupes.add(fct);
		                if (!((X10FieldInstance) fi).annotationsMatching(xts.Embed()).isEmpty()) {
		                    ArrayList<ClassType> types = new ArrayList<ClassType>();
                            extractAllClassTypes(fct, types, dupes2);
                            for (ClassType t : types) {
                                X10ClassDef cd = ((X10ClassType)t).x10Def();
                                if (cd != def && getCppRep(cd) == null) {
                                    String header = getHeader(t);
                                    String guard = getHeaderGuard(header);
                                    h.writeln("#define "+guard+"_NODEPS");
                                    h.writeln("#include <" + header + ">");
                                    h.writeln("#undef "+guard+"_NODEPS");
                                    allIncludes.add(getHeader(t));
                                }
                            }
		                }
		                if (xts.isStructType(fct)) {
		                    String header = getHeader(fct.toClass());
		                    String guard = getHeaderGuard(header);
		                    h.writeln("#define "+guard+"_NODEPS");
		                    h.writeln("#include <" + header + ">");
		                    h.writeln("#undef "+guard+"_NODEPS");
		                }
		            }
		        }
		    }
		}

		ArrayList<ClassType> types = referencedTypes(n, def);
        for (ClassType ct : types) {
            X10ClassDef cd = (X10ClassDef) ct.def();
            if (cd == def)
                continue;
            if (cd.isAnonymous())
                continue;
            String header = getHeader(ct);
            if (!allIncludes.contains(header)) {
                declareClass(cd, h);
                allIncludes.add(header);
            }
        }

		if (def.package_() != null) {
		    QName pkgName = def.package_().get().fullName();
		    Emitter.openNamespaces(h, pkgName);
		    h.newline(0);
		    h.forceNewline(0);
		    if (def.fullName().toString().equals("x10.lang.Place")) {
		        h.write("class Any; class String; // Forward reference are required in Place to prevent circularity");
		        h.newline();
		    }
		}

        /*
         * Ideally, classProperties would be added to the child context
         * that will be created for processing the classBody, but there
         * is no way to do that.  So instead we add and remove the properties
         * in the global context, for each class.
         */
		context.resetStateForClass(n.properties());

		if (inTemplate && !def.isStruct()) {
			// Pre-declare the void specialization for statics
			emitter.printTemplateSignature(def.typeParameters(), h);
			h.write("class ");
			h.write(mangled_non_method_name(def.name().toString()));
			h.write(";");
			h.newline();
			h.write("template <> class ");
			h.write(mangled_non_method_name(def.name().toString()));
			h.write(voidTemplateInstantiation(def.typeParameters().size()));
			h.write(";");
			h.newline();
		}

		// Open class/namespace bodies
		emitter.printHeader(n, h, tr);
		h.allowBreak(0, " ");
		h.write(" {"); h.newline(4); h.begin(0);

		n.print(n.body(), sw, tr);

		// Close class bodies
        h.end();
        h.newline(0);
        h.writeln("};");
        if (isStruct) {
            emitter.printRTTDefn((X10ClassType) Types.baseType(def.asType()), sw);
        } 
        
        ((X10CPPTranslator)tr).setContext(n.enterChildScope(n.body(), context)); // FIXME

        extractGenericStaticDecls(def, h);

        /*
         * See comment about resetStateForClass() above.
         */
        context.clearStateForClass();

        ((X10CPPTranslator)tr).setContext(context); // FIXME

        // Write the footer for the class
		if (def.package_() != null) {
		    QName pkgName = def.package_().get().fullName();
		    h.newline(0);
		    h.forceNewline(0);
		    Emitter.closeNamespaces(h, pkgName);
		    h.newline(0);
		}
		h.write("#endif // " + cguard); h.newline(0);
        h.forceNewline(0);

        g.write("#endif // "+cguard+"_GENERICS"); g.newline();

        if (inTemplate) {
            w_footer.write("#endif // "+cguard+"_IMPLEMENTATION"); w_footer.newline();
        }

        // The declarations below are intentionally outside of the guard
        if (context.package_() != null) {
            QName qn = context.package_().fullName();
            Emitter.openNamespaces(h, qn);
            h.newline(0);
        }
        emitter.printTemplateSignature(def.typeParameters(), h);
        h.write("class "+Emitter.mangled_non_method_name(n.name().toString())+";");
        h.newline(0);
        if (context.package_() != null) {
            h.newline(0);
            QName qn = context.package_().fullName();
            Emitter.closeNamespaces(h, qn);
            h.newline(0);
        }
        h.forceNewline(0);

        // [IP] Ok to include here, since the class is already defined
        h.write("#ifndef "+cguard+"_NODEPS"); h.newline();
        h.write("#define "+cguard+"_NODEPS"); h.newline();
        
        boolean implInHeader = n.typeParameters().size() > 0;
        if (!implInHeader) {
            Type headerAnnotation = null;
            try {
                headerAnnotation = xts.systemResolver().findOne(HEADER_ANNOTATION);
            } catch (SemanticException e) {
                // ignore.
            }
            for (ClassMember m : n.body().members()) {
                if (headerAnnotation != null && !((X10Ext) m.ext()).annotationMatching(headerAnnotation).isEmpty()) {
                    implInHeader = true; // conservative; something has been forced to the header stream.
                    break;
                }
                if (m instanceof FieldDecl_c) {
                    FieldDecl_c fd = (FieldDecl_c) m;
                    if (fd.flags().flags().isStatic() && !fd.type().type().isNumeric()) {
                        implInHeader = true; // conservative; we may have generated an inline get method into the generic functions stream
                        break;
                    }
                } else if (m instanceof X10MethodDecl_c) {
                    X10MethodDecl_c md = (X10MethodDecl_c) m;
                    if (md.typeParameters().size() > 0) {
                        implInHeader = true; // generic function went in header stream
                        break;
                    }
                }
            }
        }
        
        ClassifiedStream incS = implInHeader ? h : w_header;
        for (String header : allIncludes) {
            incS.writeln("#include <" + header + ">");
        }

        z.write("#endif // __"+cguard+"_NODEPS"); h.newline();
        z.forceNewline(0);

        context.genericFunctions = save_generic;
        context.genericFunctionClosures = save_generic_cls;
        context.closures = save_closures;
        context.staticDefinitions = save_static_defs;
        sw.set(save_header, save_body);
	}

    private ArrayList<ClassType> referencedTypes(X10ClassDecl_c n, X10ClassDef def) {
        // FIXME: [IP] There is a problem with include ordering.
		// We cannot just blindly include a header for every type used
		// because of recursive dependences.  So we need to do partial
		// ordering.  For that, we need to include only those headers
		// that define classes for which the code needs a full definition.
		// Otherwise, just declare a class and include the header in the
		// implementation file instead.
		// As I can see, the only uses that need a full definition are
		// field reads (in static final field initializers) and
		// inheritance.  So find all class declarations and field
		// declarations, and do #include for those headers.
		//
		// [DC] static final field initialisers should be in the cc file,
		// with everything else that needs a full definition (lookups,
		// construction, etc)
		//
		// [DC] generic classes might cause a problem though, as their
		// function bodies are in the header.  We can still get cycles
		// through this approach.  We may need two layers of headers or
		// something for generic classes, in a manner that reflects the
		// (h,cc) pairing for non-generic classes.

		// TODO: sort by namespace and combine things in the same namespace
		X10SearchVisitor<Node> xTypes = new X10SearchVisitor<Node>(X10CanonicalTypeNode_c.class, Closure_c.class, Tuple_c.class, Allocation_c.class, X10Call_c.class);
		n.visit(xTypes);
		ArrayList<ClassType> types = new ArrayList<ClassType>();
		Set<ClassType> dupes = CollectionFactory.newHashSet();
		dupes.add(def.asType());
		if (xTypes.found()) {
		    ArrayList<Node> typeNodesAndClosures = xTypes.getMatches();
		    for (Node tn : typeNodesAndClosures) {
		        if (tn instanceof X10CanonicalTypeNode_c) {
		            X10CanonicalTypeNode_c t = (X10CanonicalTypeNode_c) tn;
		            Type tt = t.type();
                    if (tt.isClass() && tt.toClass().isAnonymous()) {
                        ClassType c = tt.toClass();
                        assert (c.interfaces().size() == 1);
                        tt = c.interfaces().get(0);
                    }
		            extractAllClassTypes(tt, types, dupes);
		        } else if (tn instanceof Closure_c) {
                    Closure_c t = (Closure_c) tn;
		            ClassType c = t.type().toClass();
		            assert (c.interfaces().size() == 1);
                    extractAllClassTypes(c.interfaces().get(0), types, dupes);
		        } else if (tn instanceof Tuple_c) {
		            extractAllClassTypes(((Tuple_c) tn).type(), types, dupes);
		        } else if (tn instanceof Allocation_c) {
                    extractAllClassTypes(((Allocation_c) tn).type(), types, dupes);
		        } else if (tn instanceof X10Call_c) {
                    extractAllClassTypes(((X10Call_c) tn).methodInstance().container(), types, dupes);
		        }
		    }
        }

        return types;
    }



	public void visit(LocalClassDecl_c n) {
		assert (false) : ("Local classes should have been removed by a separate pass");
	}

    public void visit(ClassBody_c n) {
        X10CPPContext_c context = (X10CPPContext_c) tr.context();
        X10ClassType currentClass = (X10ClassType) context.currentClass();
        X10ClassType superClass = (X10ClassType) Types.baseType(currentClass.superClass());
        TypeSystem xts = (TypeSystem) tr.typeSystem();

        if (currentClass.flags().isInterface()) {
            visitInterfaceBody(n, context, currentClass, superClass, xts);
        } else if (currentClass.isX10Struct()) {
            visitStructBody(n, context, currentClass, superClass, xts);
        } else {
            visitClassBody(n, context, currentClass, superClass, xts);
        }
    }

    private void visitInterfaceBody(ClassBody_c n, X10CPPContext_c context,
                                X10ClassType currentClass, X10ClassType superClass,
                                TypeSystem xts) {
        ClassifiedStream h = sw.header();
        List<ClassMember> members = n.members();
        ITable itable = context.getITable(currentClass);

        h.write("public:"); h.newline();
        h.write("RTT_H_DECLS_INTERFACE");
        h.newline(); h.forceNewline();
        sw.begin(0);
        for (PropertyDecl p : context.classProperties()) {
            n.print(p, sw, tr);
        }

        h.write("template <class I> struct itable {"); h.newline(4); h.begin(0);
        h.write("itable(");
        boolean firstMethod = true;
        for (MethodInstance meth : itable.getMethods()) {
            if (!firstMethod) {
                h.write(", ");
            }
            firstMethod = false;
            itable.emitFunctionPointerDecl(h, emitter, meth, "I", true);
        }
        h.write(") ");
        firstMethod = true;
        for (MethodInstance meth : itable.getMethods()) {
            if (firstMethod) {
                h.write(": ");
                firstMethod = false;
            } else {
                h.write(", ");
            }
            String name = itable.mangledName(meth);
            h.write(name+"("+name+")");
        }
        h.write(" {}");
        h.newline();

        firstMethod = true;
        for (MethodInstance meth : itable.getMethods()) {
            if (!firstMethod) h.newline();
            firstMethod = false;
            itable.emitFunctionPointerDecl(h, emitter, meth, "I", true);
            h.write(";");
        }
        h.end(); h.newline();
        h.write("};"); h.newline();
        h.forceNewline();
        
        // Static methods that encapsulate itable lookup and invocation
        for (MethodInstance meth : itable.getMethods()) {
            String mname = itable.mangledName(meth);
            
            // Method for x10::lang::Reference (objects, closures, boxed structs)
            h.write("template <class R> static "+Emitter.translateType(meth.returnType(), true));
            h.write(" "+mname+"(x10aux::ref<R> _recv");
            int argNum=0;
            for (Type f : meth.formalTypes()) {
                h.write(", ");
                h.write(Emitter.translateType(f, true)+" arg"+(argNum++));
            }
            h.write(") {"); h.newline(4); h.begin(0);
            h.writeln("x10aux::ref<x10::lang::Reference> _refRecv(_recv);");
            if (!meth.returnType().isVoid()) h.write("return ");
            h.write("(_refRecv.operator->()->*(x10aux::findITable"+chevrons(Emitter.translateType(currentClass, false))+"(_refRecv->_getITables())->"+mname+"))(");
            boolean first = true;
            argNum = 0;
            for (Type f : meth.formalTypes()) {
                if (!first) h.write(", ");
                h.write("arg"+(argNum++));
                first = false;
            }
            h.write(");");
            h.end(); h.newline();
            h.writeln("}");
            
            
            // Method for unboxed structs that are not C++ built-in primitives
            h.write("template <class R> static "+Emitter.translateType(meth.returnType(), true));
            h.write(" "+mname+"(R _recv");
            argNum=0;
            for (Type f : meth.formalTypes()) {
                h.write(", ");
                h.write(Emitter.translateType(f, true)+" arg"+(argNum++));
            }
            h.write(") {"); h.newline(4); h.begin(0);
            if (!meth.returnType().isVoid()) h.write("return ");
            h.write("_recv->"+Emitter.mangled_method_name(meth.name().toString())+"(");
            argNum = 0;
            first = true;
            for (Type f : meth.formalTypes()) {
                if (!first) h.write(", ");
                h.write("arg"+(argNum++));
                first = false;
            }
            h.write(");");
            h.end(); h.newline();
            h.writeln("}");
        }

        if (!members.isEmpty()) {
            String className = Emitter.translateType(currentClass);

            for (ClassMember member : members) {
                if (! (member instanceof polyglot.ast.MethodDecl)){
                    n.printBlock(member, sw, tr);
                }
            }
        }

        sw.end();
        sw.newline();

        emitter.printRTTDefn(currentClass, sw);
    }

    private void visitClassBody(ClassBody_c n, X10CPPContext_c context,
                                X10ClassType currentClass, X10ClassType superClass,
                                TypeSystem xts) {
        ClassifiedStream h = sw.header();

        h.write("public:");
        h.newline();
        h.write("RTT_H_DECLS_CLASS");
        h.newline(); h.forceNewline();
        sw.begin(0);
        for (PropertyDecl p : context.classProperties()) {
            n.print(p, sw, tr);
        }

        List<ClassMember> members = n.members();

        generateITablesForClass(currentClass, context, xts, "virtual ", h);
        
        if (currentClass.isSubtype(xts.Mortal(), context)) {
            h.write("virtual x10_boolean _isMortal() { return true; }");
            h.newline(); h.forceNewline();
        }

        if (!members.isEmpty()) {
            String className = Emitter.translateType(currentClass);

            if (superClass != null) {
                generateUsingDeclsForInheritedMethods(context, currentClass,
                                                      superClass, xts, h, members);
            }
            
            ClassMember prev = null;
            for (ClassMember member : members) {
                if ((member instanceof polyglot.ast.CodeDecl)
                        || (prev instanceof polyglot.ast.CodeDecl)) {
                    h.newline(0);
                    sw.newline(0);
                }
                prev = member;
                n.printBlock(member, sw, tr);
            }

            emitter.generateClassSerializationMethods(currentClass, sw);
        }

        sw.end();
        sw.newline();

        emitter.printRTTDefn(currentClass, sw);
    }

    private void visitStructBody(ClassBody_c n, X10CPPContext_c context,
                                 X10ClassType currentClass, X10ClassType superClass,
                                 TypeSystem xts) {
        assert superClass==null : "Struct cannot have a superclass! superClass="+superClass;
        ClassifiedStream h = sw.header();
        String StructCType = Emitter.translateType(currentClass, false);
        X10ClassDef cd = currentClass.x10Def();

        h.writeln("public:");
        h.write("RTT_H_DECLS_STRUCT");
        h.newline(); h.forceNewline();
        h.write(StructCType+"* operator->() { return this; }");
        h.newline(); h.forceNewline();
        
        sw.begin(0);
        for (PropertyDecl p : context.classProperties()) {
            n.print(p, sw, tr);
        }

        List<ClassMember> members = n.members();

        generateITablesForStruct(currentClass, context, xts, h);

        // create static _alloc method
        h.newline();
        h.write("static " +StructCType+ " " +SharedVarsMethods.ALLOC+ "(){" +StructCType+ " t; return t; }");
        h.newline();
        h.forceNewline();
        
        if (!members.isEmpty()) {
            ClassMember prev = null;
            for (ClassMember member : members) {
                if ((member instanceof polyglot.ast.CodeDecl) || (prev instanceof polyglot.ast.CodeDecl)) {
                    h.newline(0);
                    sw.newline(0);
                }

                prev = member;
                n.printBlock(member, sw, tr);
            }

            emitter.generateStructSerializationMethods(currentClass, sw);
            sw.forceNewline();
        }
    }

	private void generateITablesForClass(X10ClassType currentClass, X10CPPContext_c context,
	                                     TypeSystem xts, String maybeVirtual, ClassifiedStream h) {
		X10ClassDef cd = currentClass.x10Def();
		List<X10ClassType> allInterfaces = xts.allImplementedInterfaces(currentClass);
		int numInterfaces = allInterfaces.size();
		if (numInterfaces > 0 && !currentClass.flags().isAbstract()) {
			/* ITables declarations */
			h.write("static x10aux::itable_entry _itables["+(numInterfaces+1)+"];"); h.newline(); h.forceNewline();
			h.write(maybeVirtual+"x10aux::itable_entry* _getITables() { return _itables; }"); h.newline(); h.forceNewline();
			int itableNum = 0;
			for (Type interfaceType : allInterfaces) {
				ITable itable = context.getITable((X10ClassType) Types.baseType(interfaceType));
				itable.emitITableDecl(currentClass, itableNum, emitter, h);
				itableNum += 1;
				h.forceNewline();
			}

			/* ITables initialization */
			itableNum = 0;
			for (Type interfaceType : allInterfaces) {
				ITable itable = context.getITable((X10ClassType) Types.baseType(interfaceType));
				itable.emitITableInitialization(currentClass, itableNum, this, h, sw);
				itableNum += 1;
			}

			emitter.printTemplateSignature(cd.typeParameters(), sw);
			sw.write("x10aux::itable_entry "+Emitter.translateType(currentClass)+"::_itables["+(numInterfaces+1)+"] = {");
			itableNum = 0;
			for (Type interfaceType : allInterfaces) {
				sw.write("x10aux::itable_entry(&x10aux::getRTT"+chevrons(Emitter.translateType(interfaceType, false))+", &_itable_"+itableNum+"), ");
				itableNum += 1;
			}
			sw.write("x10aux::itable_entry(NULL, (void*)x10aux::getRTT"+chevrons(Emitter.translateType(currentClass, false))+"())};"); sw.newline();
		}
	}

	private void generateITablesForStruct(X10ClassType currentClass, X10CPPContext_c context,
	                                      TypeSystem xts,
	                                      ClassifiedStream h) {
	    X10ClassDef cd = currentClass.x10Def();
	    List<X10ClassType> allInterfaces = xts.allImplementedInterfaces(currentClass);
        int numInterfaces = allInterfaces.size();
	    if (numInterfaces > 0 && !currentClass.flags().isAbstract()) {
            String thunkBaseName = Emitter.translateFQN(currentClass.fullName().toString());
            String thunkParams = "";
            if (cd.typeParameters().size() != 0) {
                String args = "";
                int s = cd.typeParameters().size();
                for (Type t: cd.typeParameters()) {
                    args += Emitter.translateType(t, true); // type arguments are always translated as refs
                    if (--s > 0)
                        args +=", ";
                }
                thunkParams = chevrons(args);
            }

	        /* ITables declarations */
	        h.writeln("static x10aux::itable_entry _itables["+(numInterfaces+1)+"];"); h.forceNewline();
	        h.writeln("x10aux::itable_entry* _getITables() { return _itables; }"); h.forceNewline();
            int itableNum = 0;
            for (Type interfaceType : allInterfaces) {
                ITable itable = context.getITable((X10ClassType) Types.baseType(interfaceType));
                itable.emitITableDecl(currentClass, itableNum, emitter, h);
                itableNum += 1;
                h.forceNewline();
            }
            h.writeln("static x10aux::itable_entry _iboxitables["+(numInterfaces+1)+"];"); h.forceNewline();
            h.writeln("x10aux::itable_entry* _getIBoxITables() { return _iboxitables; }"); h.forceNewline();
            
	        /* ITables initialization */
            itableNum = 0;
	        for (Type interfaceType : allInterfaces) {
	            ITable itable = context.getITable((X10ClassType) Types.baseType(interfaceType));
	            itable.emitITableInitialization(currentClass, itableNum, this, h, sw);
	            itableNum += 1;
	        }

	        String clsCType = Emitter.translateType(currentClass, false);

	        // Itables init for the struct itself
	        emitter.printTemplateSignature(cd.typeParameters(), sw);
	        sw.write("x10aux::itable_entry "+clsCType+"::_itables["+(numInterfaces+1)+"] = {");
	        itableNum = 0;
	        for (Type interfaceType : allInterfaces) {
	            sw.write("x10aux::itable_entry(&x10aux::getRTT"+chevrons(Emitter.translateType(interfaceType, false))+", &"+
	                     thunkBaseName+thunkParams+"::_itable_"+itableNum+"), ");
	            itableNum += 1;
	        }
	        sw.write("x10aux::itable_entry(NULL, (void*)x10aux::getRTT"+chevrons(Emitter.translateType(currentClass, false))+"())};"); sw.newline();
	        
	        // ITables init for the IBox thunk.
	        emitter.printTemplateSignature(cd.typeParameters(), sw);
	        sw.write("x10aux::itable_entry "+clsCType+"::_iboxitables["+(numInterfaces+1)+"] = {");
	        itableNum = 0;
	        for (Type interfaceType : allInterfaces) {
	            sw.write("x10aux::itable_entry(&x10aux::getRTT"+chevrons(Emitter.translateType(interfaceType, false))+", &"+
	                     thunkBaseName+"_ibox"+itableNum+thunkParams+"::itable), ");
	            itableNum += 1;
	        }
	        sw.write("x10aux::itable_entry(NULL, (void*)x10aux::getRTT"+chevrons(Emitter.translateType(currentClass, false))+"())};"); sw.newline();
	    }
	}

	// The goal of this method is for each methodName declared in the childClass,
	// determine if there is any superclass such that the superclass also declares
	// methods with that name such that some superclass methods are not overridden
	// in the subclass.  When that happens, we need to emit a using directive because
	// overriding/overloading is defined differently in C++ than in X10 (or Java).
	// By emitting the using directive, we pull the superclass methods into the subclass
	// namespace and thus get the X10/Java semantics.
	// The alternative to using "using" is to generate proxy methods that forward to the superclass
	// method.  This results in slightly less efficient generated code, but more importantly is 
	// significantly more complex to implement correctly.
    private void generateUsingDeclsForInheritedMethods(X10CPPContext_c context, X10ClassType childClass,
                                                       X10ClassType superClass, TypeSystem xts, ClassifiedStream h,
                                                       List<ClassMember> members) {
        boolean didSomething = false;
        Set<Name> possibleNames = CollectionFactory.newHashSet();
        for (MethodInstance mi : childClass.methods()) {
            possibleNames.add(mi.name());
        }
        while (superClass != null && !possibleNames.isEmpty()) {
            Iterator<Name> names = possibleNames.iterator();
            nameLoop: while (names.hasNext()) {
                Name methName = names.next();
                List<MethodInstance> childImpls = childClass.methodsNamed(methName);
                List<MethodInstance> parentImpls = superClass.methodsNamed(methName);
                if (!parentImpls.isEmpty()) {
                    boolean emitUsing = false;
                    if (childImpls.size() != parentImpls.size()) {
                        // Number of implementation differs, so we can't have an exact signature match.
                        emitUsing = true;
                    } else {
                        // Same number of impls, now check for identical signatures.
                        implLoop: for (MethodInstance childImpl : childImpls) {
                            for (MethodInstance parentImpl : parentImpls) {
                                if (childImpl.isSameMethod(parentImpl, context)) continue implLoop;
                            }
                            // If we get to here, then there is a childImpl that has a different signature than all parentImpls
                            emitUsing = true;
                            break implLoop;                 
                        }
                    }
                    
                    if (emitUsing) {
                        names.remove();
                        h.writeln("using "+Emitter.translateType(superClass,false)+"::"+mangled_method_name(methName.toString())+";");
                        didSomething = true;
                        continue nameLoop;
                    }  
                }
            }
            superClass = (X10ClassType)superClass.superClass();
        }
        if (didSomething) h.forceNewline();
    }

    public void visit(PackageNode_c n) {
        assert (false);
        sw.write(mangled_non_method_name(translateFQN(n.package_().get().fullName().toString())));
    }

    public void visit(Import_c n) {
        assert (false);
    }

    public static void processMain(X10ClassType container, CodeWriter sw, X10CPPCompilerOptions options) {
        TypeSystem xts = container.typeSystem();
        if (container.isClass())
            container = getStaticMemberContainer(container.x10Def());
        String typeString = Emitter.translateType(container);
        sw.write("#include <"+MessagePassingCodeGenerator.getHeader(container)+">"); sw.newline();
        Emitter.dumpString(createMainStub(typeString, options), sw);
        sw.newline(0);
    }

	public static String createMainStub(String container, X10CPPCompilerOptions options) {
		StringBuilder sb = new StringBuilder();
        sb.append("#include <x10/lang/Runtime.h>\n");
        sb.append("#include <x10aux/bootstrap.h>\n");
		String mainTypeArgs = "x10::lang::Runtime," + container;
        sb.append("extern \"C\" { int main(int ac, char **av) { return x10aux::template_main"+chevrons(mainTypeArgs)+"(ac,av); } }\n");
        if (options.x10_config.DEBUG)
		{
			sb.append("\n// Debugger stuff\n");
			sb.append("void* x10aux_place_local__fastData = &x10aux::place_local::_fastData;\n");
			sb.append("void* __x10MainRef = (void *) "+container+"::main;\n");
			sb.append("pthread_key_t* __x10ThreadMapper = (pthread_key_t *) &x10::lang::Thread::__thread_mapper;\n");
			sb.append("x10_boolean* __x10ThreadMapperInited = (x10_boolean *) &x10::lang::Thread::__thread_mapper_inited;\n");
		}
        return sb.toString();
	}

	public static final QName HEADER_ANNOTATION = QName.make("x10.compiler.Header");

	public void visit(X10MethodDecl_c dec) {
		X10CPPContext_c context = (X10CPPContext_c) tr.context();
		TypeSystem xts =  tr.typeSystem();
		Flags flags = dec.flags().flags();
		String nativePat = null;
        X10MethodDef def = dec.methodDef();
		
		if (flags.isNative()) {
		    if (!flags.isStatic()) {
		        // Must generate bodies for non-static @Native methods so
		        // that virtual dispatch actually works.
		        nativePat = getCppImplForDef(dec.methodDef());
		    }
		    if (nativePat == null) return; 
		}

		if (query.isMainMethod(def)) {
		    ((X10CPPJobExt) tr.job().ext()).addMainMethod(def);
		}
		MethodInstance mi = def.asInstance();
		X10ClassType container = (X10ClassType) mi.container();
		ClassifiedStream h = sw.header();
		if ((container.x10Def().typeParameters().size() != 0) && flags.isStatic()) {
			context.pendingStaticDecls().add(dec);
			return;
		}
		int mid = getUniqueId_().intValue();
		if (def.typeParameters().size() != 0) {
		    sw.pushCurrentStream(context.genericFunctions);
		    String guard = getHeaderGuard(getHeader(mi.container().toClass()));
		    sw.write("#ifndef "+guard+"_"+mi.name().toString()+"_"+mid); sw.newline();
		    sw.write("#define "+guard+"_"+mi.name().toString()+"_"+mid); sw.newline();
		}

		// we sometimes need to use a more general return type as c++ does not support covariant smartptr return types
		Type ret_type = emitter.findRootMethodReturnType(def, dec.position(), mi);
		String methodName = mi.name().toString();
		
        boolean inlineInClassDecl = false;
        boolean inlineDirective = false;
        try {
            Type annotation = xts.systemResolver().findOne(HEADER_ANNOTATION);
            if (!((X10Ext) dec.ext()).annotationMatching(annotation).isEmpty()) {
                if (container.x10Def().typeParameters().size() == 0) {
                    inlineInClassDecl = true;
                } else {
                    inlineDirective = true;
                }
            }
        } catch (SemanticException e) { 
            /* Ignore exception when looking for annotation */  
        }
        // Attempt to make it easy for the post compiler to inline trivial struct methods
        // by putting them in the h stream instead of the sw stream whenever possible. 
        // Don't bother doing this for generic structs the body stream is already in the header file.
        if (!flags.isNative() && !inlineInClassDecl && container.isX10Struct() && container.x10Def().typeParameters().size() == 0) {
            StructMethodAnalyzer analyze = new StructMethodAnalyzer(tr.job(), xts, tr.nodeFactory(), container);
            dec.visit(analyze.begin());
            inlineInClassDecl = analyze.canGoInHeaderStream();
        }		
		
        sw.pushCurrentStream(h);
        emitter.printHeader(dec, sw, tr, methodName, ret_type, false, false);
        if (!inlineInClassDecl) {
            sw.popCurrentStream();
            h.write(";");
            h.newline();
        }


        if (flags.isNative() || dec.body() != null) {
            if (!flags.isStatic()) {
                VarInstance<?> ti = xts.localDef(Position.COMPILER_GENERATED, Flags.FINAL,
                                                 Types.ref(container), Name.make(THIS)).asInstance();
                context.addVariable(ti);
            }
            if (!inlineInClassDecl) {
                emitter.printHeader(dec, sw, tr, methodName, ret_type, true, inlineDirective);
            }
            if (flags.isNative()) {
                assert nativePat != null;
                sw.write("{"); sw.newline(4); sw.begin(0);
                if (!dec.returnType().type().isVoid()) sw.write("return ");
                 
                String target = container.isX10Struct() ? STRUCT_THIS : THIS;
                List<String> params = new ArrayList<String>();
                List<String> args = new ArrayList<String>();
                int counter = 0;
                for (LocalInstance f : mi.formalNames()) {
                    Type fType = mi.formalTypes().get(counter);
                    params.add(f.name().toString());
                    args.add(f.name().toString());
                    counter++;
                }
                List<Type> classTypeArguments  = Collections.<Type>emptyList();
                List<ParameterType> classTypeParams  = Collections.<ParameterType>emptyList();
                emitNativeAnnotation(nativePat, mi.x10Def().typeParameters(), mi.typeParameters(), target, params, args, classTypeParams, classTypeArguments);
                sw.write(";");
                sw.end(); sw.newline();
                sw.writeln("}");
            } else {
                dec.printSubStmt(dec.body(), sw, tr);
            }
            sw.newline();
        } else {
            // Define property getter methods.
            if (flags.isProperty() && flags.isAbstract() && mi.formalTypes().size() == 0 && mi.typeParameters().size() == 0) {
                X10FieldInstance fi = (X10FieldInstance) container.fieldNamed(mi.name());
                if (fi != null) {
                    //assert (X10Flags.toX10Flags(fi.flags()).isProperty()); // FIXME: property fields don't seem to have the property flag set
                    // This is a property method in an interface.  Give it a body.
                    if (!inlineInClassDecl) {
                        emitter.printHeader(dec, sw, tr, methodName, ret_type, true, inlineDirective);
                    }
                    sw.write(" {");
                    sw.allowBreak(0, " ");
                    sw.write("return "+mangled_field_name(fi.name().toString())+";");
                    sw.allowBreak(0, " ");
                    sw.write("}");
                }
            }
        }

		
		if (inlineInClassDecl) {
		    sw.popCurrentStream();
		}
		
		if (def.typeParameters().size() != 0) {
		    sw.popCurrentStream();
		    String guard = getHeaderGuard(getHeader(mi.container().toClass()));
		    context.genericFunctions.writeln("#endif // "+guard+"_"+mi.name().toString()+"_"+mid);
		}
	}


	public void visit(ConstructorDecl_c dec) {

	    // [DC] here we generate a pair of functions to form a fake
	    // constructor.   Real constructors are no use to us because they
	    // cannot invoke virtual functions with the semantics we need.  The two
	    // functions are _make which behaves like a constructor by creating an
	    // uninitialised object and calling _constructor (the body of the X10
	    // constructor).  Calls to super() and this() are translated into calls
	    // to the appropriate OtherClass::_constructor (hence the separate
	    // function).

	    X10CPPContext_c context = (X10CPPContext_c) tr.context();
	    if (dec.flags().flags().isNative())
	        return;

	    ClassifiedStream h = sw.header();

	    X10ClassType container = (X10ClassType) dec.constructorDef().container().get();
	    String typeName = Emitter.translateType(container.def().asType());
	    TypeSystem xts = (TypeSystem)context.typeSystem();

	    boolean inlineInClassDecl = false;
	    try {
	        Type annotation = xts.systemResolver().findOne(HEADER_ANNOTATION);
	        if (!((X10Ext) dec.ext()).annotationMatching(annotation).isEmpty()) {
	            inlineInClassDecl = true;
	        }
	    } catch (SemanticException e) { 
	        /* Ignore exception when looking for annotation */  
	    }

	    // Attempt to make it easy for the post compiler to inline trivial struct constructors
	    // by putting them in the h stream instead of the sw stream whenever possible. 
	    if (!inlineInClassDecl && container.isX10Struct()) {
	        StructMethodAnalyzer analyze = new StructMethodAnalyzer(tr.job(), xts, tr.nodeFactory(), container);
	        dec.visit(analyze.begin());
	        inlineInClassDecl = analyze.canGoInHeaderStream();
	    }

	    sw.pushCurrentStream(h);
	    emitter.printHeader(dec, sw, tr, false, false, "void");
	    if (!inlineInClassDecl) {
	        h.write(";") ; h.newline();
	        h.forceNewline();
	        sw.popCurrentStream();

	        emitter.printHeader(dec, sw, tr, true, false, "void");
	    }
	    X10ConstructorInstance ci = (X10ConstructorInstance) dec.constructorDef().asInstance();
	    if (dec.body() == null) {
	        assert false : dec.position().toString();
	    sw.write("{ }"); sw.newline(); sw.forceNewline();
	    }

	    assert (!dec.flags().flags().isStatic());

	    TypeSystem ts = tr.typeSystem();

	    VarInstance<?> ti = ts.localDef(Position.COMPILER_GENERATED, Flags.FINAL,
	                                 Types.ref(container), Name.make(THIS)).asInstance();
	    context.addVariable(ti);

	    sw.allowBreak(0, " "); sw.write("{"); sw.newline(4); sw.begin(0);

	    Block_c body = (Block_c) dec.body();

	    // Synthetic fields must be initialized before everything else
	    for (Stmt s : body.statements()) {
	        if (query.isSyntheticOuterAccessor(s)) {
	            dec.print(s, sw, tr); sw.newline();
	        }
	    }

	    for (Stmt s : body.statements()) {
	        // FIXME: constructor calls won't get line number information
	        if ( s instanceof ConstructorCall && ( // ignore split New constructor calls here
	               ((ConstructorCall) s).kind() == ConstructorCall.SUPER ||
	               ((ConstructorCall) s).target() == null ||
	               ((ConstructorCall) s).target().type().typeEquals(container, context)
	           ) ) {
	            ConstructorCall call = (ConstructorCall)s;
	            if (call.kind() == ConstructorCall.SUPER) {
	                String superClass = Emitter.translateType(container.superClass());
	                sw.write("this->::"+superClass+"::"+CONSTRUCTOR+"(");
	            } else if (call.kind() == ConstructorCall.THIS) {
	                sw.write("this->"+CONSTRUCTOR+"(");
	            }
	            if (call.arguments().size() > 0) {
	                sw.allowBreak(2, 2, "", 0); // miser mode
	                sw.begin(0);
	                boolean first = true;
	                for (Expr e : (List<Expr>) call.arguments() ) {
	                    if (!first) {
	                        sw.write(",");
	                        sw.allowBreak(0, " ");
	                    }
	                    dec.print(e, sw, tr);
	                    first = false;
	                }
	                sw.end();
	            }
	            sw.write(");");
	            sw.newline();
	        } else if (query.isSyntheticOuterAccessor(s)) {
	            // we did synthetic field initialisation earlier
	        } else {
	            dec.printBlock(s, sw, tr);
	            sw.newline();
	        }
	    }
	    sw.end(); sw.newline();
	    sw.write("}");
	    sw.newline();
	    if (inlineInClassDecl) {
	        sw.popCurrentStream();
	    }

	    if (!container.flags().isAbstract()) {
	        int _makeStartLine = sw.currentStream().getStreamLineNumber();

	        // emit _make method
	        h.write("static ");
	        sw.pushCurrentStream(h);
	        emitter.printHeader(dec, sw, tr, false, true, container.isX10Struct() ? typeName : make_ref(typeName));
	        if (!inlineInClassDecl) {
	            h.write(";") ; h.newline();
	            h.forceNewline();
	            sw.popCurrentStream();

	            _makeStartLine = sw.currentStream().getStreamLineNumber();
	            emitter.printHeader(dec, sw, tr, true, true, container.isX10Struct() ? typeName : make_ref(typeName));
	        }

	        sw.allowBreak(0, " "); sw.write("{"); sw.newline(4); sw.begin(0);
	        if (container.isX10Struct()) {
	            sw.write(typeName+" this_; "); sw.newline();
	        } else {
	            // XTENLANG-1407: Remove this memset call once we finish the default value specification/implementation.
	            //                Expect it to be more efficient to explicitly initialize all of the object fields instead
	            //                of first calling memset, then storing into most of the fields a second time.
	            sw.write(make_ref(typeName)+" this_ = "+
	                    "new (memset(x10aux::alloc"+chevrons(typeName)+"(), 0, sizeof("+typeName+"))) "+typeName+"();"); sw.newline();
	        }
            sw.write("this_->"+CONSTRUCTOR+"(");
	        for (Iterator<Formal> i = dec.formals().iterator(); i.hasNext(); ) {
	            Formal f = i.next();
	            sw.write(mangled_non_method_name(f.name().id().toString()));
	            if (i.hasNext()) {
	                sw.write(",");
	                sw.allowBreak(0, " ");
	            }
	        }
	        sw.writeln(");");
	        sw.write("return this_;");
	        sw.end(); sw.newline();
	        sw.writeln("}");
	        sw.forceNewline();
	        sw.currentStream().omitLines(sw.currentStream().getStreamLineNumber() - _makeStartLine + 1);
	        if (inlineInClassDecl) {
	            sw.popCurrentStream();
	        }
	    }

	    sw.newline(); sw.forceNewline();
	}

	private String embeddedName(Name name) {
	    return "_Embed_"+mangled_non_method_name(name.toString());
	}
	
	public void visit(FieldDecl_c dec) {
	    // FIXME: HACK: skip synthetic serialization fields
	    if (query.isSyntheticField(dec.name().id().toString()))
	        return;

	    X10CPPContext_c context = (X10CPPContext_c) tr.context();
	    X10ClassType declaringClass = (X10ClassType)dec.fieldDef().asInstance().container();
	    boolean isStatic = dec.flags().flags().isStatic();
	    if ((((X10ClassDef)declaringClass.def()).typeParameters().size() != 0) && isStatic) {
	        context.pendingStaticDecls().add(dec);
	        return;
	    }

        ClassifiedStream h = sw.header();
        sw.pushCurrentStream(h);

        TypeSystem xts = context.typeSystem();
        
        boolean embed = false;
            Type annotation = xts.Embed();
            if (!((X10Ext) dec.ext()).annotationMatching(annotation).isEmpty()) {
                embed = true;
//                System.err.println("@StackAllocate " + dec);
            }
        
        if (embed) {
            String tmpName = embeddedName(dec.name().id());
            sw.writeln(Emitter.translateType(dec.type().type(), false)+" "+tmpName+";");
        }

        emitter.printHeader(dec, sw, tr, false);
        sw.popCurrentStream();

	    // Ignore the initializer -- this will have been done in extractInits/extractStaticInits
	    // FIXME: the above breaks switch constants!
	    h.write(";");
	    h.newline(); h.forceNewline();
	    if (isStatic) {
	        emitter.printHeader(dec, sw, tr, true);
	        // [DC] disabled because I want this done through the static initialisation framework
	        // [IP] re-enabled for a very limited set of cases, namely literal inits
	        boolean globalInit = isGlobalInit(dec);
	        if (globalInit) {
	            sw.write(" =");
	            sw.allowBreak(2, " ");
	            dec.print(dec.init(), sw, tr);
	        }
	        sw.write(";");
	        sw.newline();
	        String container = Emitter.translateType(dec.fieldDef().asInstance().container());
	        if (!globalInit) {
	            generateStaticFieldInitializer(dec, container, sw);
	        }
	        generateStaticFieldSupportCode(dec, container, globalInit, sw);
	        sw.forceNewline();
	    }
	}

	private static final String STATIC_FIELD_ACCESSOR_SUFFIX = "__get";
	private static final String STATIC_FIELD_STATUS_SUFFIX = "__status";
	private static final String STATIC_FIELD_INITIALIZER_SUFFIX = "__init";
	private static final String STATIC_FIELD_REAL_INIT_SUFFIX = "__do_init";
	private static final String STATIC_FIELD_BROADCASTID_SUFFIX = "__id";
	private static final String STATIC_FIELD_DESERIALIZER_SUFFIX = "__deserialize";
	private static final String STATIC_FIELD_UNINITIALIZED = "x10aux::UNINITIALIZED";
	private static final String STATIC_FIELD_INITIALIZING = "x10aux::INITIALIZING";
	private static final String STATIC_FIELD_INITIALIZED = "x10aux::INITIALIZED";
    private static final String STATIC_INIT_LOCK = "x10aux::StaticInitBroadcastDispatcher::lock";
    private static final String STATIC_INIT_AWAIT = "x10aux::StaticInitBroadcastDispatcher::await";
	private static final String STATIC_INIT_UNLOCK = "x10aux::StaticInitBroadcastDispatcher::unlock";
	private static final String STATIC_INIT_NOTIFY_ALL = "x10aux::StaticInitBroadcastDispatcher::notify";

	private static final String UNUSED = "X10_PRAGMA_UNUSED";

	/**
	 * Generate an initializer method for a given field declaration.
	 */
	private void generateStaticFieldInitializer(FieldDecl_c dec, String container, StreamWrapper sw) {
	    String name = dec.name().id().toString();
	    String fname = mangled_field_name(name);
	    String status = mangled_field_name(name+STATIC_FIELD_STATUS_SUFFIX);
	    String init_nb = mangled_field_name(name+STATIC_FIELD_REAL_INIT_SUFFIX);
	    String init = mangled_field_name(name+STATIC_FIELD_INITIALIZER_SUFFIX);
	    String id = mangled_field_name(name+STATIC_FIELD_BROADCASTID_SUFFIX);
	    ClassifiedStream h = sw.header();
	    sw.pushCurrentStream(h);
	    // declare the actual field initializer
	    h.write("static void ");
	    h.write(init_nb);
	    h.write("();");
	    h.newline();
	    // declare the on-demand field initializer
	    h.write("static void ");
	    h.write(init);
	    h.write("();");
	    h.newline();
	    sw.popCurrentStream();
	    // define the actual field initializer
	    sw.write("void ");
	    sw.write(container + "::" + init_nb);
	    sw.write("() {");
	    sw.newline(4); sw.begin(0);
	    // set the status (ok to do here because either we are in single-threaded
	    // mode, or we will have already set the status to INITIALIZING atomically)
	    sw.write(status + " = " + STATIC_FIELD_INITIALIZING + ";");
	    sw.newline();
	    // initialize the field
	    sw.write("_SI_(\"Doing static initialisation for field: "+container+"."+name+"\");"); sw.newline();
	    String val = getId();
	    emitter.printType(dec.type().type(), sw);
	    sw.allowBreak(2, 2, " ", 1);
	    sw.write(val + " =");
	    sw.allowBreak(2, 2, " ", 1);
	    dec.print(dec.init(), sw, tr);
	    sw.write(";");
	    sw.newline();
	    // copy into the field
	    sw.write(fname + " = " + val + ";");
	    sw.newline();
	    // update the status
	    sw.write(status + " = " + STATIC_FIELD_INITIALIZED + ";");
	    sw.end(); sw.newline();
	    sw.write("}");
	    sw.newline();
	    // define the on-demand field initializer
	    sw.write("void ");
	    sw.write(container + "::" + init);
	    sw.write("() {");
	    sw.newline(4); sw.begin(0);
	    sw.write("if (x10aux::here == 0) {");
	    sw.newline(4); sw.begin(0);
	    sw.newline();
	    // (atomically) check that the field is uninitialized
	    String tmp = getId();
	    sw.write("x10aux::status " + tmp + " =");
	    sw.allowBreak(2, 2, " ", 1);
	    sw.write("(x10aux::status)x10aux::atomic_ops::compareAndSet_32((volatile x10_int*)&" +
	            status + ", (x10_int)" + STATIC_FIELD_UNINITIALIZED +
	            ", (x10_int)" + STATIC_FIELD_INITIALIZING + ");");
	    sw.newline();
	    sw.write("if (" + tmp + " != " + STATIC_FIELD_UNINITIALIZED + ") goto WAIT;");
	    sw.newline();
	    // invoke the initializer
	    sw.write(init_nb + "();");
	    sw.newline();
	    // broadcast the new value
	    sw.write("x10aux::StaticInitBroadcastDispatcher::broadcastStaticField(");
	    sw.begin(0);
	    sw.write(fname);
	    sw.write(",");
	    sw.allowBreak(0, 2, " ", 1);
	    sw.write(id);
	    sw.end();
	    sw.write(");");
	    sw.newline();
	    sw.write("// Notify all waiting threads");
	    sw.newline();
        sw.write(STATIC_INIT_LOCK + "();");
        sw.newline();
	    sw.write(STATIC_INIT_NOTIFY_ALL + "();");
	    sw.end(); sw.newline();
	    sw.write("}"); sw.newline();
	    sw.write("WAIT:"); sw.newline();
	    sw.write("if ("+status+" != " + STATIC_FIELD_INITIALIZED + ") {"); sw.begin(4); sw.newline();
        sw.write(STATIC_INIT_LOCK + "();"); sw.newline();
	    sw.write("_SI_(\"WAITING for field: "+container+"."+name+" to be initialized\");"); sw.newline();
	    sw.write("while ("+status+" != " + STATIC_FIELD_INITIALIZED + ") " + STATIC_INIT_AWAIT + "();"); sw.newline();
	    sw.write("_SI_(\"CONTINUING because field: "+container+"."+name+" has been initialized\");");
	    sw.newline();
        sw.write(STATIC_INIT_UNLOCK + "();"); sw.end(); sw.newline();
	    sw.write("}");
	    sw.end(); sw.newline();
	    sw.write("}");
	    sw.newline();
	    sw.write("static " + VOID_PTR + " __init__"+getUniqueId_() + " " + UNUSED + " = x10aux::InitDispatcher::addInitializer(" + container + "::" + init + ")"+ ";");
	    sw.newline(); sw.forceNewline(0);
	}

	/**
	 * Generates the accessor method and the initialization flag for a given
	 * field declaration.
	 * TODO: some of this should really be done before codegen.
	 */
	private void generateStaticFieldSupportCode(FieldDecl_c dec, String container, boolean globalInit, StreamWrapper sw) {
	    String name = dec.name().id().toString();
	    String fname = mangled_field_name(name);
	    String status = mangled_field_name(name+STATIC_FIELD_STATUS_SUFFIX);
	    String accessor = mangled_field_name(name+STATIC_FIELD_ACCESSOR_SUFFIX);
	    String init = mangled_field_name(name+STATIC_FIELD_INITIALIZER_SUFFIX);
	    String deserializer = mangled_field_name(name+STATIC_FIELD_DESERIALIZER_SUFFIX);
	    String id = mangled_field_name(name+STATIC_FIELD_BROADCASTID_SUFFIX);
	    TypeSystem xts = tr.typeSystem();
	    ClassifiedStream h = sw.header();
	    sw.pushCurrentStream(h);
	    if (!globalInit) {
	        // declare the initialization flag
	        h.write("static volatile x10aux::status ");
	        h.write(status);
	        h.write(";");
	        h.newline();
	    }
	    // declare (and define) the accessor method
	    h.write("static ");
	    emitter.printType(dec.type().type(), h);
	    h.allowBreak(2, 2, " ", 1);
	    h.write(accessor);
	    h.writeln("();");
	    X10CPPContext_c context = (X10CPPContext_c) tr.context();
	    ClassifiedStream gh = context.genericFunctions;
	    gh.write("inline ");
	    emitter.printType(dec.type().type(), gh);
	    gh.allowBreak(2, 2, " ", 1);
	    gh.write(container+"::"+accessor);
	    gh.write("() {");
	    gh.newline(4); gh.begin(0);
	    if (!globalInit) {
	        gh.write("if ("+status+" != " + STATIC_FIELD_INITIALIZED + ") {");
	        gh.newline(4); gh.begin(0);
	        gh.write(init + "();");
	        gh.end(); gh.newline();
	        gh.write("}");
	        gh.newline();
	    }
	    gh.write("return ");
	    gh.write(container+"::");
	    gh.write(fname);
	    gh.write(";");
	    gh.end(); gh.newline();
	    gh.write("}");
	    gh.newline(); gh.forceNewline();
	    sw.popCurrentStream();
	    if (!globalInit) {
	        sw.pushCurrentStream(h);
	        // declare the deserializer method
	        h.write("static "+make_ref("x10::lang::Reference"));
	        h.allowBreak(2, 2, " ", 1);
	        h.write(deserializer + "(" + DESERIALIZATION_BUFFER + " &buf);");
	        h.newline();
	        // declare the broadcast id
	        h.write("static const x10aux::serialization_id_t "+id+";");
	        h.newline(); h.forceNewline();
	        sw.popCurrentStream();
	        // define the initialization flag
	        sw.write("volatile x10aux::status ");
	        sw.write(container+"::");
	        sw.write(status);
	        sw.write(";");
	        sw.newline();
	        // define the deserializer method
	        sw.write("// extract value from a buffer"); sw.newline();
	        sw.write(make_ref("x10::lang::Reference"));
	        sw.allowBreak(2, 2, " ", 1);
	        sw.write(container + "::" + deserializer + "(" + DESERIALIZATION_BUFFER + " &buf) {");
	        sw.newline(4); sw.begin(0);
	        sw.write(fname+" =");
	        sw.allowBreak(2, 2, " ", 1);
	        sw.write("buf.read"+chevrons(Emitter.translateType(dec.type().type(), true))+"();");
	        sw.newline();
	        sw.write(container+"::"+status+" = " + STATIC_FIELD_INITIALIZED + ";");
	        sw.newline();
	        sw.write("// Notify all waiting threads");
            sw.newline();
            sw.write(STATIC_INIT_LOCK + "();");
	        sw.newline();
	        sw.write(STATIC_INIT_NOTIFY_ALL + "();");
	        sw.newline();
	        sw.write("return X10_NULL;");
	        sw.end(); sw.newline();
	        sw.write("}");
	        sw.newline();
	        // define the broadcast id
	        sw.write("const x10aux::serialization_id_t " + container + "::"+id + " =");
	        sw.allowBreak(2, 2, " ", 1);
	        sw.write("x10aux::StaticInitBroadcastDispatcher::addRoutine(" + container + "::" + deserializer + ");");
	        sw.newline();
	    }
	}

	public void visit(PropertyDecl_c n) {
		super.visit(n);
	}

	public void visit(Initializer_c n) {
	    X10CPPContext_c context = (X10CPPContext_c) tr.context();
	    if (n.flags().flags().isStatic()) {
	        assert (false) : ("Static initializer alert!");
	    }
	    if (((X10ClassDef) context.currentClassDef()).typeParameters().size() != 0 &&
	            n.flags().flags().isStatic())
	    {
	        context.pendingStaticDecls().add(n);
	        return;
	    } else {
	        // Ignore -- this will have been processed earlier
	    }
	}


    public void visit(AssignPropertyCall_c n) {
        List<X10FieldInstance> definedProperties = n.properties();
        List<Expr> arguments = n.arguments();
        int aSize = arguments.size();
        assert (definedProperties.size() == aSize);

        for (int i = 0; i < aSize; i++) {
            Expr arg = arguments.get(i);
            FieldInstance fi = definedProperties.get(i);
            sw.write(mangled_field_name(fi.name().toString()));
            sw.write(" = ");
            n.print(arg, sw, tr);
            sw.write(";");
            sw.newline();
        }
    }


	public void visit(Assert_c n) {
		if (!tr.job().extensionInfo().getOptions().assertions)
			return;

		sw.write("#ifndef NO_ASSERTIONS");
		sw.newline();
		sw.write("if (x10aux::x10__assertions_enabled)");
		sw.newline(4); sw.begin(0);
		sw.write("x10aux::x10__assert(");
		n.print(n.cond(), sw, tr);
		if (n.errorMessage() != null) {
			sw.write(",");
			sw.allowBreak(4, " ");
			n.print(n.errorMessage(), sw, tr);
		}
		sw.write(");");
		sw.end(); sw.newline();
		sw.write("#endif//NO_ASSERTIONS");
		sw.newline();
	}


	public void visit(Switch_c n) {
	    sw.write("switch (");
	    if (n.expr().type().isChar()) {
	        sw.write("(");
	        n.print(n.expr(), sw, tr);
	        sw.write(").v");
	    } else {
	        n.print(n.expr(), sw, tr);
	    }
	    sw.write(")");
	    sw.allowBreak(0, " ");
	    sw.write("{");
	    sw.newline();
	    if (n.elements().size() > 0) {
	        sw.newline(4);
	        sw.begin(0);
	        for (SwitchElement s : n.elements()) {
	            n.print(s, sw, tr);
	        }
	        sw.end();
	        sw.newline(0);
	    }
	    else
	        sw.write(" ");

	    sw.write("}");
	}

	public void visit(SwitchBlock_c n) {
		sw.write("{");
		if (n.statements().size() > 0) {
			sw.newline(4);
			sw.begin(0);
			for (Iterator<Stmt> i = n.statements().iterator(); i.hasNext(); ) {
				Stmt s = i.next();
				n.print(s, sw, tr);
				if (i.hasNext())
					sw.newline();
			}
			sw.end();
			sw.newline(0);
		}
		else
			sw.write(" ");

		sw.write("}");
	}

	public void visit(Case_c n) {
		sw.newline();
		if (n.expr() == null) {
            sw.write("default: ;"); // Add gratituous ; to avoid post-compiler failure if default is last one in switch and is empty.
		} else {
			sw.write("case ");
			// FIXME: [IP] HACK HACK HACK! Substitute the actual constant if any
			// FIXME: [IP] Even worse hack: ignore @Native on const fields when used in switches.
			if (n.expr() instanceof Field_c && n.expr().isConstant()) {
				sw.write(""+n.expr().constantValue());
				sw.write("/"+"*");
				n.print(n.expr(), sw, tr);
				sw.write("*"+"/");
			} else if (n.expr() instanceof CharLit_c) {
			    CharLit_c lit = (CharLit_c)n.expr();
			    sw.write("'"+StringUtil.escape(lit.charValue())+"'");
			} else {
				n.print(n.expr(), sw, tr);
			}
			sw.write(": ;"); // Add gratituous ; to avoid post-compiler failure if case is last one in switch and is empty.
		}
		sw.newline();
	}

	public void visit(Branch_c br) {
		if (br.labelNode() != null) {
			if (br.kind() == Branch_c.CONTINUE)
				sw.write("goto " + br.labelNode().id() + "_next_");
			else
				sw.write("goto " + br.labelNode().id() + "_end_");
		} else
			sw.write(br.kind().toString());
		sw.write(";");
	}


	private void printLabel(String label, CodeWriter w) {
	    w.write("goto " + label + "; ");
	    w.write(label + ":");
	}

	public void visit(Labeled_c label) {
	    // For every labeled statement, generate one/three labels->
	    // L: S --> L :
	    //          S
	    // If S is a for / while / do-while loop then after the
	    // generated C++ for-loop, have a label L_end_ and before
	    // end-parenthesis of the loop, have one more label L_next_:
	    // L : for (...) { ... } -->
	    // L :
	    // for (...) {... L_next_: ; }
	    // L_end_: ;

	    X10CPPContext_c context = (X10CPPContext_c) tr.context();

	    printLabel(label.labelNode().id().toString(), sw);
	    sw.write(" ");
	    context.setLabel(label.labelNode().id().toString(), label.statement());
	    label.print(label.statement(), sw, tr);
	    sw.newline();
	}


	public void visit(Assign_c asgn) {
        X10CPPContext_c context = (X10CPPContext_c) tr.context();
	    boolean unsigned_op = false;
	    String opString = asgn.operator().toString();
	    TypeSystem xts = tr.typeSystem();

	    // TODO
//	    // Boolean short-circuiting operators are ok
//	    Assign_c n = asgn;
//	    assert (opString.equals("&&") || opString.equals("||"))
//	        : "visiting "+n.getClass()+" at "+n.position()+": "+n;

	    if (opString.equals(">>>=")) {
	        unsigned_op = true;
	        opString = opString.substring(1);
	    }

	    Expr lhs = asgn.left();
	    Expr rhs = asgn.right();
	    if (unsigned_op) {
	        sw.write("("+Emitter.translateType(asgn.type())+")(");
	        sw.write("(("+emitter.makeUnsignedType(lhs.type())+"&)");
	    }
	    asgn.printSubExpr(lhs, false, sw, tr);
	    if (unsigned_op)
	        sw.write(")");
	    sw.write(" ");
	    if (asgn.operator() != Assign_c.ASSIGN) {
	        assert (false);
	        sw.write("/"+"*"+" op= "+"*"+"/");
	        sw.write(" ");
	    }
	    // [IP] Are all the operators the same?
	    sw.write(opString);
	    sw.allowBreak(2, 2, " ", 1);

	    if (unsigned_op)
	        sw.write("(("+emitter.makeUnsignedType(rhs.type())+")");
	    Boolean embed = false;
        if (asgn instanceof FieldAssign) {
            FieldInstance fi = ((FieldAssign) asgn).fieldInstance();
            if (!((X10FieldInstance) fi).annotationsMatching(tr.typeSystem().Embed()).isEmpty()) {
                embed = true;
            }
        }
        if (embed && !xts.isStructType(rhs.type())) {
            FieldInstance fi = ((FieldAssign) asgn).fieldInstance();
            if (rhs instanceof StmtExpr_c && ((StmtExpr_c) rhs).statements().size()>=2 && ((StmtExpr_c) rhs).statements().get(1) instanceof ConstructorCall_c) {
                // rhs is split constructor
                sw.write("&"+embeddedName(fi.name())+";");
                sw.newline();
                asgn.print(((ConstructorCall_c) ((StmtExpr_c) rhs).statements().get(1)).target(lhs), sw, tr);
            } else {
                // rhs is new
                context.setEmbeddedFieldName(embeddedName(fi.name()));
                asgn.printSubExpr(rhs, true, sw, tr);
                context.setEmbeddedFieldName(null);
            }
        } else {
            asgn.printSubExpr(rhs, true, sw, tr);
        }
	    if (unsigned_op)
	        sw.write("))");
	}


	public void visit(Return_c ret) {
		Expr e = ret.expr();
		if (e == null) {
			sw.write("return;");
		} else {
			sw.write("return ");
			ret.print(e, sw, tr);
			sw.write(";"); sw.newline();
		}
	}


	public void visit(Formal_c n) {
		emitter.printHeader(n, sw, tr, true);
	}


	public void visit(LocalDecl_c dec) {
	    X10CPPContext_c context = (X10CPPContext_c) tr.context();
	    TypeSystem xts = (TypeSystem)context.typeSystem();

	    boolean stackAllocate = false;
	    Type annotation = xts.StackAllocate();
	    if (!((X10Ext) dec.ext()).annotationMatching(annotation).isEmpty()) {
	        stackAllocate = true;
	        //                System.err.println("@StackAllocate " + dec);
	    }

	    String tmpName = null;
	    if (stackAllocate) {
	        tmpName = "_StackAllocate_"+mangled_non_method_name(dec.name().id().toString());
	        sw.writeln(Emitter.translateType(dec.type().type(), false)+" "+tmpName+";");
	        assert context.getStackAllocName() == null;
	        context.setStackAllocName(tmpName);
	    } else {
            emitter.printHeader(dec, sw, tr, true);
        }

	    Expr initexpr = dec.init();
	    if (initexpr != null) {
	        if (!stackAllocate) sw.write(" =");
	        sw.allowBreak(2, " ");
	        Type aType = dec.type().type();
	        dec.print(initexpr, sw, tr);
	    }
	    
	    if (stackAllocate) {
	        context.setStackAllocName(null);
	        sw.writeln(";");
	        emitter.printHeader(dec, sw, tr, true);
	        sw.write("(&"+tmpName+")");
	    }

	    if (tr.appendSemicolon()) {
	        sw.write(";");
	        sw.newline(0);
	    }
	}

	public void visit(Block_c b) {
	    /* 
	     * If this block had a label, it might have Break's in it, so we need
	     * the L_end label at the end, as the Break's get written out as "goto L_end";
	     */
        String label = null;
        X10CPPContext_c context = (X10CPPContext_c) tr.context();
        if (context.getLabeledStatement() == b) {
            label = context.getLabel();
            context.setLabel(null, null);
        }

        String s = getCppImplForStmt(b);
        if (s != null) {
            sw.write(s);
            return;
        }
		sw.write("{");
		sw.newline();
		
        if (label != null) {
            sw.write("{");
            sw.newline(0);
            sw.begin(0);
        }

		if (b.statements().size() > 0) {
			sw.newline(4);
			sw.begin(0);
			for (Iterator<Stmt> i = b.statements().iterator(); i.hasNext(); ) {
				Stmt n = i.next();
				b.printBlock(n, sw, tr);
				if (i.hasNext())
					sw.newline();
			}
			sw.end();
			sw.newline(0);
		}
		else
			sw.write(" ");
        if (label != null) {
            sw.newline(0);
            sw.write("}");
            printLabel(label + "_end_", sw);
            sw.write(" ;");
            sw.newline();
        }
        sw.newline();
        sw.write("}");
	}

	public void visit(StmtSeq_c n) {
	    List<Stmt> stmts = n.statements();
	    if (stmts.size() == 0) {
	        sw.write(" ");
	        return;
	    }
	    for (Stmt s : stmts) {
	        n.printBlock(s, sw, tr);
	        sw.newline();
	    }
	}

	public void visit(StmtExpr_c n) {
	    X10CPPCompilerOptions opts = (X10CPPCompilerOptions) tr.job().extensionInfo().getOptions();
	    if (!opts.x10_config.ALLOW_STATEMENT_EXPRESSIONS) {
	        tr.job().compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR,
	                "Statement expression node encountered, but statement expressions are disabled: ", n.position());
	    }
        boolean oldSemiColon = tr.appendSemicolon(true);
	    sw.write("(__extension__ ({");
	    sw.newline(4); sw.begin(0);
	    List<Stmt> stmts = n.statements();
	    boolean oldPrintType = tr.printType(true);
	    for (Stmt stmt : stmts) {
	        n.printBlock(stmt, sw, tr);
	        sw.newline();
	    }
	    tr.printType(oldPrintType);
	    Expr e = n.result();
	    if (e != null) {
	        n.print(e, sw, tr);
	        sw.write(";");
	    }
	    sw.end(); sw.newline();
	    sw.write("}))"); sw.newline();
	    tr.appendSemicolon(oldSemiColon);
	}


	public void visit(For_c n) {
        // FIXME: Generate normal for-loop code, without
        // separating out the inits. [Krishna]

		String label = null;
		X10CPPContext_c context = (X10CPPContext_c) tr.context();
		if (context.getLabeledStatement() == n) {
		    label = context.getLabel();
		    context.setLabel(null, null);
		}

		sw.write("{");
		sw.newline(4); sw.begin(0);
        if (n.inits() != null) {
            for (ForInit s : n.inits()) {
                if (s instanceof LocalDecl_c) {
                    LocalDecl_c dec = (LocalDecl_c) s;
                    emitter.printHeader(dec, sw, tr, true);
                    sw.write(";");
                    sw.newline(0);
                }
            }
        }

		sw.newline(0);
		sw.write("for (");
		sw.begin(0);

        if (n.inits() != null) {
            for (Iterator<ForInit> i = n.inits().iterator(); i.hasNext();) {
                ForInit s = i.next();
                boolean oldSemiColon = tr.appendSemicolon(false);
                boolean oldPrintType = tr.printType(false);
                n.printBlock(s, sw, tr);
                tr.printType(oldPrintType);
                tr.appendSemicolon(oldSemiColon);

                if (i.hasNext()) {
                    sw.write(",");
                    sw.allowBreak(2, " ");
                }
            }
        }

		sw.write(";");
		sw.allowBreak(0);

		if (n.cond() != null) {
			n.printBlock(n.cond(), sw, tr);
		}

		sw.write(";");
		sw.allowBreak(0);

		if (n.iters() != null) {
			for (Iterator<ForUpdate> i = n.iters().iterator(); i.hasNext(); ) {
				ForUpdate s = i.next();
				boolean oldSemiColon = tr.appendSemicolon(false);
				n.printBlock(s, sw, tr);
				tr.appendSemicolon(oldSemiColon);

				if (i.hasNext()) {
					sw.write(",");
					sw.allowBreak(2, " ");
				}
			}
		}

		sw.end();
		sw.write(")");
		sw.allowBreak(0, " ");

		if (label != null) {
		    sw.write("{");
		    sw.newline(0); sw.begin(0);
		}

		Stmt body = n.body();
		if (!(body instanceof Block_c))
		    body = tr.nodeFactory().Block(body.position(), body);

		n.print(body, sw, tr);

		if (label != null) {
		    sw.newline(0);
		    printLabel(label + "_next_", sw);
		    sw.write(" ;");
		    sw.end(); sw.newline();
		    sw.write("}");
		}

		if (label != null) {
		    sw.newline(0);
		    printLabel(label + "_end_", sw);
		    sw.write(" ;");
		}

		sw.end(); sw.newline();
		sw.write("}");
		sw.newline(0);
	}

	public void visit(Do_c n) {
	    String label = null;
	    X10CPPContext_c context = (X10CPPContext_c) tr.context();
	    if (context.getLabeledStatement() == n) {
	        label = context.getLabel();
	        context.setLabel(null, null);
	    }

	    sw.write("do");
	    sw.allowBreak(0, " ");

	    if (label != null) {
	        sw.write("{");
	        sw.newline(0); sw.begin(0);
	    }

	    Stmt body = n.body();
	    if (!(body instanceof Block_c))
	        body = tr.nodeFactory().Block(body.position(), body);

	    n.print(body, sw, tr);

	    if (label != null) {
	        sw.newline(0);
	        printLabel(label + "_next_", sw);
	        sw.write(" ;");
	        sw.end(); sw.newline();
	        sw.write("}");
	    }

	    sw.allowBreak(0, " ");
	    sw.write("while (");
	    sw.begin(0);
	    n.printBlock(n.cond(), sw, tr);
	    sw.end();
	    sw.write(");");

	    if (label != null) {
	        sw.newline(0);
	        printLabel(label + "_end_", sw);
	        sw.write(" ;");
	    }
	    sw.newline(0);
	}

	public void visit(While_c n) {
	    String label = null;
	    X10CPPContext_c context = (X10CPPContext_c) tr.context();
	    if (context.getLabeledStatement() == n) {
	        label = context.getLabel();
	        context.setLabel(null, null);
	    }

	    sw.write("while (");
	    sw.begin(0);
	    n.printBlock(n.cond(), sw, tr);
	    sw.end();
	    sw.write(")");
	    sw.allowBreak(0, " ");

	    if (label != null) {
	        sw.write("{");
	        sw.newline(4); sw.begin(0);
	    }

	    Stmt body = n.body();
	    if (!(body instanceof Block_c))
	        body = tr.nodeFactory().Block(body.position(), body);

	    n.print(body, sw, tr);

	    if (label != null) {
	        sw.newline(0);
	        printLabel(label + "_next_", sw);
	        sw.write(" ;");
	        sw.end(); sw.newline();
	        sw.write("}");
	    }

	    if (label != null) {
	        sw.newline(0);
	        printLabel(label + "_end_", sw);
	        sw.write(" ;");
	    }
	    sw.newline(0);
	}


	public void visit(If_c n) {
		sw.write("if (");
		n.printBlock(n.cond(), sw, tr);
		sw.write(")");
		sw.allowBreak(0, " ");
		n.print(n.consequent(), sw, tr);
		if (n.alternative() != null) {
		    sw.allowBreak(0, " ");
		    sw.write("else");
		    sw.allowBreak(0, " ");
			// [IP] Semi-HACK: handle "else if" specially
			Stmt alternative = n.alternative();
			if (alternative instanceof Block_c) {
				Block_c block = (Block_c) alternative;
				if (block.statements().size() == 1 && block.statements().get(0) instanceof If_c)
					alternative = (Stmt) block.statements().get(0);
			}
			n.print(alternative, sw, tr);
		}
        sw.newline(0);
	}


	public void visit(Empty_c n) {
		sw.write(";");
	}


	public void visit(Eval_c n) {
		boolean semi = tr.appendSemicolon(true);
		n.print(n.expr(), sw, tr);
		if (semi)
			sw.write(";");
		tr.appendSemicolon(semi);
	}

	private static boolean needsNullCheck(Receiver e) {
	    if (e instanceof X10CanonicalTypeNode_c)
	        return false;
	    if (e instanceof X10Special_c)
	        return ((X10Special_c) e).qualifier() != null;
	    if (e instanceof X10Cast_c)
	        return needsNullCheck(((X10Cast_c) e).expr());
	    return !Types.isNonNull(e.type());
	}

	public void visit(X10Call_c n) {
		X10CPPContext_c context = (X10CPPContext_c) tr.context();

		TypeSystem xts =  tr.typeSystem();
		MethodInstance mi = (MethodInstance) n.methodInstance();
		Receiver target = n.target();
		Type t = target.type();
		
		X10MethodDef md = mi.x10Def();
		if (mi.flags().isStatic()) {
		    TypeNode tn =
		        target instanceof TypeNode ?
		                (TypeNode) target :
		                tr.nodeFactory().CanonicalTypeNode(target.position(), t);
		    if (xts.isParameterType(t)) {
		        // Rewrite to the class declaring the field.
		        target = tn.typeRef(md.container());
		        n = (X10Call_c) n.target(target);
		        t = target.type();
		    }
		    if (t.isClass()) {
		        X10ClassType ct = (X10ClassType)t.toClass();
		        target = tn.typeRef(Types.ref(getStaticMemberContainer(ct.x10Def())));
		    }
		}

		Flags flags = mi.flags();
		// Check for properties accessed using method syntax.  They may have @Native annotations too.
		if (flags.isProperty() && mi.formalTypes().size() == 0 && mi.typeParameters().size() == 0) {
		    X10FieldInstance fi = (X10FieldInstance) md.container().get().fieldNamed(mi.name());
		    if (fi != null) {
		        //assert (X10Flags.toX10Flags(fi.flags()).isProperty()); // FIXME: property fields don't seem to have the property flag set
		        // This is a property getter method.  Translate as a field access.
		        String pat = getCppImplForDef(fi.x10Def());
		        if (pat != null) {
					Map<String,Object> components = new HashMap<String,Object>();
					components.put("this", target);
					components.put("0", target);
		            emitter.nativeSubst("Native", components, tr, pat, sw);
		            return;
		        }
		    }
		}

		String lang[] = new String[1];
		String pat = getCppImplForDef(md, lang);
		if (pat != null) {
		    // If the method is static or if the method's container is a struct then we go ahead and inline
		    // the @Native annotation at the callsite.  This is safe because there is no virtual dispatch involved.
		    //
		    // If the method's container has a C++ @NativeRep annotation, then we inline the @Native annotation
		    // because we have nothing else we can reasonably do.  If the @NativeRep class was written "correctly" 
		    // this will not break the semantics because the @Native will forward to the proper virtual dispatch.
		    //
			// If the pattern is for the "cuda" backend then inline it also, as the CUDA backend does not yet support functions
			//
		    // Otherwise (virtual method of a non-NativeRep class), we ignore the @Native annotation at the
		    // callsite and generate a normal calling sequence.  This preserves virtual method semantics.
		    if (mi.flags().isStatic() || 
		            (mi.container().isClass() && ((X10ClassType)mi.container()).isX10Struct()) ||
		            (mi.container().isClass() && getCppRep(((X10ClassType)mi.container()).x10Def()) != null) ||
		            lang[0].equals("cuda")) {
		        List<String> params = new ArrayList<String>();
		        for (LocalDef fn : mi.def().formalNames()) {
		            params.add(fn.name().toString());
		        }
		        
		        
		        List<Type> classTypeArguments  = Collections.<Type>emptyList();
		        List<ParameterType> classTypeParams  = Collections.<ParameterType>emptyList();
		        if (mi.container().isClass() && !mi.flags().isStatic()) {
		            X10ClassType ct = (X10ClassType) mi.container().toClass();
		            classTypeArguments = ct.typeArguments();
		            classTypeParams = ct.x10Def().typeParameters();
		            if (classTypeArguments == null) classTypeArguments = Collections.<Type>emptyList();
		            if (classTypeParams == null) classTypeParams = Collections.<ParameterType>emptyList();
		        }
		        emitNativeAnnotation(pat, mi.x10Def().typeParameters(), mi.typeParameters(), target, params, n.arguments(), classTypeParams, classTypeArguments);
		        return;
		    }
		}

		// the cast is because our generated member function may use a more general
		// return type because c++ does not support covariant smartptr returns
		// TODO: See TODO in CastInjector.
        Type ret_type = emitter.findRootMethodReturnType(md, null, mi);
		boolean needsCast = !xts.typeDeepBaseEquals(mi.returnType(), ret_type, context);
		if (needsCast) {
			sw.write("static_cast" + chevrons(Emitter.translateType(mi.returnType(), true)) + "(");
		}

		sw.begin(0);
		String dangling = "";
		boolean already_static = false;
		String targetMethodName = mangled_method_name(n.name().id().toString());
		boolean isInterfaceInvoke = false;
		boolean needsNullCheck = needsNullCheck(target);
		if (!n.isTargetImplicit()) {
		    // explicit target.
		    if (target instanceof X10Special_c && ((X10Special_c)target).kind().equals(X10Special_c.SUPER)) {
		        sw.write(Emitter.translateType(t));
		        sw.write("::");
		        already_static = true;
		    } else if (target instanceof Expr) {
		        if (mi.flags().isStatic()) {
		            sw.write("((void)");
		            n.printSubExpr((Expr) target, false, sw, tr);
		            sw.write(",");
		            sw.write(Emitter.translateType(t));
		            sw.write("::");
		            dangling = ")";
		            already_static = true;
		        } else {
		            if (t.isClass()) {
		                X10ClassType clsType = (X10ClassType)t.toClass();
		                if (clsType.flags().isInterface()) {
		                    invokeInterface(n, (Expr) target, n.arguments(), make_ref(REFERENCE_TYPE), clsType, mi, needsNullCheck);
		                    sw.end();
		                    if (needsCast) {
		                        sw.write(")");
		                    }
		                    return; // FIXME: unify with the regular return
		                }
		            } else if (xts.isParameterType(t)) {
		                if (mi.container().isClass() && mi.container().toClass().flags().isInterface()) {
		                    invokeInterface(n, (Expr) target, n.arguments(), Emitter.translateType(t), mi.container(), mi, true);
		                    sw.end();
		                    if (needsCast) {
		                        sw.write(")");
		                    }
		                    return; // FIXME: unify with the regular return
		                }
		            }

		            boolean assoc = !(target instanceof New_c || target instanceof Binary_c);
		            if (!isInterfaceInvoke) {
		                if (needsNullCheck) sw.write("x10aux::nullCheck(");
		                n.printSubExpr((Expr) target, assoc, sw, tr);
		                if (needsNullCheck) sw.write(")");
		                sw.write("->");
		            }
		        }
		    } else if (target instanceof TypeNode || target instanceof AmbReceiver) {
		        n.print(target, sw, tr);
		        sw.write("::");
		        already_static = true;
		    }
		}

		boolean virtual_dispatch = true;
		if (mi.typeParameters().size() == 0) {
		    // Attempting to devirtualize generic instance methods breaks xlC.
		    // Not clear if this is because we aren't generating the right magic incantation
		    // to do the invocation or if it is a bug in xlC.  
		    // Until that is clear, just don't try to do the optimization in this case.
		    if (t.isClass()) {
		        X10ClassType ct = (X10ClassType)t.toClass();
		        X10ClassDef cd = ct.x10Def();
		        if (cd.flags().isFinal()) {
		            virtual_dispatch = false;
		        }
		    }
		    if (mi.flags().isFinal()) {
		        virtual_dispatch = false;
		    }
		}
		if (!virtual_dispatch && !already_static ) {
		    // disable virtual dispatch
		    sw.write(Emitter.translateType(t));
		    sw.write("::");
		}
		if (context.inTemplate() && mi.typeParameters().size() != 0) {
		    sw.write("template ");
		}
		if (!isInterfaceInvoke) {
		    sw.write(targetMethodName);
		    emitter.printTemplateInstantiation(mi, sw);
		}
		sw.write("(");
		printCallActuals(n, context, xts, mi, n.arguments());
		sw.write(")");
		sw.write(dangling);
		sw.end();

		if (needsCast) {
			sw.write(")");
		}
	}

	private void invokeInterface(Node_c n, Expr target, List<Expr> args, String dispType, Type contType,
	                             MethodInstance mi, boolean needsNullCheck)
	{
	    X10CPPContext_c context = (X10CPPContext_c) tr.context();
	    TypeSystem xts = tr.typeSystem();
	    Type rt = mi.returnType();
	    assert (target != null); // has to be explicit target.
	    assert (contType instanceof X10ClassType); // have to dispatch to an interface type.
	    X10ClassType clsType = (X10ClassType) contType;
	    assert (clsType.flags().isInterface()); // have to dispatch to an interface type.

	    ITable itable= context.getITable(clsType);
	    String targetMethodName = itable.mangledName(mi);
	    sw.write(Emitter.translateType(clsType, false)+"::"+targetMethodName+"(");
	    if (needsNullCheck) sw.write("x10aux::nullCheck(");
	    n.print(target, sw, tr);
	    if (needsNullCheck) sw.write(")");
	    if (mi.formalTypes().size()>0) sw.write(", ");
	    printCallActuals(n, context, xts, mi, args);
	    sw.write(")");
	}

	private void printCallActuals(Node_c n, X10CPPContext_c context, TypeSystem xts, MethodInstance mi,
	                              List<Expr> args)
	{
		int counter;
		if (args.size() > 0) {
			sw.allowBreak(2, 2, "", 0); // miser mode
			sw.begin(0);
			counter = 0;
			for (Iterator<Expr> i = args.iterator(); i.hasNext(); ) {
				Expr e = i.next();
				n.print(e, sw, tr);
				if (i.hasNext()) {
					sw.write(",");
					sw.allowBreak(0, " ");
				}
				counter++;
			}
			sw.end();
		}
	}


	public void visit(Field_c n) {
		X10CPPContext_c context = (X10CPPContext_c) tr.context();
		Receiver target = n.target();
		Type t = target.type();

		TypeSystem xts = (TypeSystem) t.typeSystem();
		X10FieldInstance fi = (X10FieldInstance) n.fieldInstance();

		X10FieldDef fd = fi.x10Def();
		if (target instanceof TypeNode) {
		    assert (fi.flags().isStatic());
		    TypeNode tn = (TypeNode) target;
		    if (t instanceof ParameterType) {
		        // Rewrite to the class declaring the field.
		        target = tn.typeRef(fd.container());
		        n = (Field_c) n.target(target);
		    }
		    if (t.isClass()) {
		        X10ClassType ct = (X10ClassType)t.toClass();
		        target = tn.typeRef(Types.ref(getStaticMemberContainer(ct.x10Def())));
		    }
		}

		String pat = getCppImplForDef(fd);
		if (pat != null) {
			Map<String,Object> components = new HashMap<String,Object>();
			components.put("this", target);
			components.put("0", target);
			emitter.nativeSubst("Native", components, tr, pat, sw);
			return;
		}

		sw.begin(0);
		// TODO: capture constant fields as variables
		if (!n.flags().isStatic()) {
			X10CPPContext_c c = (X10CPPContext_c) tr.context();
			if (target instanceof X10Special_c && ((X10Special_c)target).isSelf()) {
				assert (false) : ("Loki knows why we got here...");
				//sw.write((context.Self() == null) ? "self" : context.Self());
				//sw.write("->");
			}
		}
		String name = n.name().id().toString();
		assert (target != null);
		if (target instanceof Expr) {
			if (fi.flags().isStatic()) {
				sw.write("((void)");
				n.printSubExpr((Expr) target, false, sw, tr);
				sw.write(",");
				sw.write(Emitter.translateType(target.type()));
				sw.write("::");
				sw.allowBreak(2, 3, "", 0);
				sw.write(mangled_field_name(name));
				sw.write(")");
				sw.end();
				return;
			} else {
			    boolean needsNullCheck = !Types.isX10Struct(t) && needsNullCheck(target);
				boolean assoc = !(target instanceof New_c || target instanceof Binary_c);
				if (needsNullCheck) sw.write("x10aux::nullCheck(");
				n.printSubExpr((Expr) target, assoc, sw, tr);
				if (needsNullCheck) sw.write(")");
			}
		}
		else if (target instanceof TypeNode || target instanceof AmbReceiver) {
			n.print(target, sw, tr);
		}
		if (n.fieldInstance().flags().isStatic()) {
			sw.write("::");
		} else {
			sw.write("->");
		}
		sw.allowBreak(2, 3, "", 0);
		if (!n.fieldInstance().flags().isStatic()) {
            if (target instanceof X10Special_c && ((X10Special_c)target).kind().equals(X10Special_c.SUPER)) {
                sw.write(Emitter.translateType(context.currentClass().superClass())+"::");
            }
		    sw.write(mangled_field_name(name));
		} else {
		    if (isPerProcess((X10Def) n.fieldInstance().def())) {
		        sw.write(mangled_field_name(name));
		    } else {
		        sw.write(mangled_field_name(name+STATIC_FIELD_ACCESSOR_SUFFIX) + "()");
		    }
		}
		sw.end();
	}


	public void visit(Local_c n) {
		X10CPPContext_c c = (X10CPPContext_c) tr.context();
		LocalInstance var = n.localInstance();
		if (c.isInsideClosure())
			c.saveEnvVariableInfo(n.name().toString());
		sw.write(mangled_non_method_name(var.name().toString()));
	}

	public void visit(New_c n) {
		X10CPPContext_c context = (X10CPPContext_c) tr.context();
		TypeSystem xts = (TypeSystem)context.typeSystem();
		ConstructorInstance constructor = n.constructorInstance();
		boolean stackAllocate = false;
        boolean embed = false;

        // Danger Will Robinson! Give programmer plenty of rope to hang themselves!!
        // If there's a @StackAllocate annotation on a new expression, then do what
        // the programmer asked us to and stack allocate the storage for the object.
        // If the programmer was incorrect about the lifetime of the object, then
        // the program will almost certainly crash in some unexpected way.
        Type annotation = xts.StackAllocate();
        if (!((X10Ext) n.ext()).annotationMatching(annotation).isEmpty()) {
            stackAllocate = true;
            //		        System.err.println("@StackAllocate " + n);
        }
        Type annotation2 = xts.Embed();
        if (!((X10Ext) n.ext()).annotationMatching(annotation2).isEmpty()) {
            embed = true;
            //              System.err.println("@StackAllocate " + n);
        }
		
		if (n.qualifier() != null)
			throw new InternalCompilerError("Qualified new not supported");
		if (n.body() != null)
			throw new InternalCompilerError("Anonymous innner classes should have been removed.");

		if (stackAllocate) {
		    sw.write(context.getStackAllocName()+"."+CONSTRUCTOR+"(");
		} else if (embed) {
		    sw.write("&"+context.getEmbeddedFieldName()+";");
		    sw.newline();
		    sw.write(context.getEmbeddedFieldName()+"."+CONSTRUCTOR+"(");
		} else {
		    sw.write(Emitter.translateType(n.objectType().type())+"::"+MAKE+"(");
		}
		    
		sw.begin(0);
		for (Iterator<Expr> i = n.arguments().iterator(); i.hasNext(); ) {
			Expr e = i.next();
			n.print(e, sw, tr);
			if (i.hasNext()) {
				sw.write(",");
				sw.allowBreak(0, " ");
			}
		}
		sw.write(")");
		sw.end();
	}


	public void visit(FloatLit_c n) {
		String val;
		if (n.kind() == FloatLit_c.FLOAT)
			val = Float.toString((float) n.value()) + "f";
		else if (n.kind() == FloatLit_c.DOUBLE)
			val = Double.toString(n.value());
		else
			throw new InternalCompilerError("Unrecognized FloatLit kind " + n.kind());
		sw.write(val);
	}

	public void visit(IntLit_c n) {
	    String val;
        switch (n.kind()) {
        case ULONG:
	        if (n.boundary())
	            val = "0x" + Long.toHexString(n.value()).toUpperCase() + "llu";
	        else if (n.value() < 0)
	            val = "0x" + Long.toHexString(n.value()).toUpperCase() + "llu";
	        else
	            val = Long.toString(n.value()) + "ull";
            break;
        case UINT:
	        if (n.value() >= 0x80000000L)
	            val = "0x" + Long.toHexString(n.value()).toUpperCase() + "u";
	        else if (n.boundary())
	            val = "0x" + Long.toHexString(-n.value()).toUpperCase() + "u";
	        else
	            val = Long.toString((int) n.value()) + "u";
            break;
        case LONG:
	        if (n.boundary())
	            val = "0x" + Long.toHexString(n.value()).toUpperCase() + "llu";
	        else
	            val = Long.toString(n.value()) + "ll";
            break;
        case INT:
	        if (n.value() >= 0x80000000L)
	            val = "0x" + Long.toHexString(n.value()).toUpperCase() + "u";
	        else if (n.boundary())
	            val = "0x" + Long.toHexString(-n.value()).toUpperCase() + "u";
	        else
	            val = Long.toString((int) n.value());
            break;

        case BYTE:
        case SHORT:
        case UBYTE:
        case USHORT:
            val = Long.toString((int) n.value());
            break;

        default:
	        throw new InternalCompilerError("Unrecognized IntLit kind " + n.kind());
        }
	    sw.write("("); sw.begin(0);
	    sw.write("(" + Emitter.translateType(n.type(), true) + ")");
	    sw.write(val);
	    sw.end(); sw.write(")");
	}

	public void visit(NullLit_c n) {
		sw.write("X10_NULL");
	}

	public void visit(StringLit_c n) {
        TypeSystem xts = tr.typeSystem();

        boolean nativeString = false;
        try {
            Type annotation = xts.systemResolver().findOne(QName.make("x10.compiler.NativeString"));
            if (!((X10Ext) n.ext()).annotationMatching(annotation).isEmpty()) {
                nativeString = true;
//                System.err.println("@NativeString " + n);
            }
        } catch (SemanticException e) { 
            /* Ignore exception when looking for annotation */  
        }

        if (nativeString) {
            sw.write("\"");
            sw.write(StringUtil.escape(n.stringValue()));
            sw.write("\"");
        } else {
            sw.write("x10aux::string_utils::lit(\"");
            sw.write(StringUtil.escape(n.stringValue()));
            sw.write("\")");
        }
	}

	public void visit(CharLit_c lit) {
		sw.write("((x10_char)'"+StringUtil.escape(lit.charValue())+"')");
	}

	public void visit(BooleanLit_c lit) {
		sw.write(lit.toString());
	}


	public void visit(Id_c n) {
		sw.write(mangled_non_method_name(n.id().toString()));
	}


	public void visit(X10Cast_c c) {
		TypeNode tn = c.castType();
		assert tn instanceof CanonicalTypeNode;

		switch (c.conversionType()) {
		case CHECKED:
		case PRIMITIVE:
		case SUBTYPE:
        case UNCHECKED:

			if (tn instanceof X10CanonicalTypeNode) {
				X10CanonicalTypeNode xtn = (X10CanonicalTypeNode) tn;

                Type t = Types.baseType(xtn.type());
                Type f = Types.baseType(c.expr().type());

                Type t_ = Types.stripConstraints(t);
                Type f_ = Types.stripConstraints(f);

                TypeSystem xts = tr.typeSystem();
                Context context = (Context) tr.context();

                if (xts.typeEquals(f_, t_, context)) {
                    c.printSubExpr(c.expr(), true, sw, tr);
                } else if (c.conversionType()==Converter.ConversionType.SUBTYPE && xts.isSubtype(f_, t_, context)) {
                    // If it is an upcast, we can implement as a class_cast_unchecked.
                    // However we still need to do something for two reasons
                    //   (a) if it is a struct, then the upcast will autobox
                    //   (b) if it is not a struct, we might still need the cast to
                    //       get the right C++ types so that overload resolution will work.
                    sw.write("x10aux::class_cast_unchecked");
                    sw.write(chevrons(Emitter.translateType(t_, true)) + "(");
                    c.printSubExpr(c.expr(), true, sw, tr);
                    sw.write(")");
                } else {
				    if (c.conversionType()==Converter.ConversionType.UNCHECKED) {
				        sw.write("x10aux::class_cast_unchecked");
				    } else {
				        sw.write("x10aux::class_cast");
				    }
				    sw.write(chevrons(Emitter.translateType(t_, true)) + "(");
				    c.printSubExpr(c.expr(), true, sw, tr);
				    sw.write(")");
				}
			} else {
				throw new InternalCompilerError("Ambiguous TypeNode survived type-checking.", tn.position());
			}
			break;

		case UNBOXING:
			throw new InternalCompilerError("Unknown conversion type after type-checking.", c.position());
		case UNKNOWN_IMPLICIT_CONVERSION:
			throw new InternalCompilerError("Unknown conversion type after type-checking.", c.position());
		case UNKNOWN_CONVERSION:
			throw new InternalCompilerError("Unknown conversion type after type-checking.", c.position());
		case BOXING:
			throw new InternalCompilerError("Boxing conversion should have been rewritten.", c.position());
		}
	}

	public void visit(SubtypeTest_c n) {
		X10CPPContext_c context = (X10CPPContext_c) tr.context();
        if (n.equals()) {
            sw.write("x10aux::sametype");
            sw.write(chevrons(
                Emitter.translateType(n.subtype().type(), true) + "," +
                Emitter.translateType(n.supertype().type(), true)
            ));
        } else {
            sw.write("x10aux::subtypeof");
            sw.write(chevrons(
                Emitter.translateType(n.subtype().type(), true) + "," +
                Emitter.translateType(n.supertype().type(), true)
            ));
		}
		sw.write("()");
	}

	public void visit(HasZeroTest_c n) {
	    X10CPPContext_c context = (X10CPPContext_c) tr.context();
	    sw.write("x10aux::haszero");
	    sw.write(chevrons(
	            Emitter.translateType(n.parameter().type(), true)
	    ));
	    sw.write("()");
	}

	public void visit(X10Instanceof_c n) {
		X10CPPContext_c context = (X10CPPContext_c) tr.context();

		if (refsAsPointers) {
			sw.write("!!dynamic_cast");
			sw.write(chevrons(Emitter.translateType(n.compareType().type(), true)));
			sw.write("(");
			sw.begin(0);
			n.printSubExpr(n.expr(), sw, tr);
			sw.end();
			sw.write(")");
			return;
		}
		sw.write("x10aux::instanceof");
		sw.write(chevrons(Emitter.translateType(n.compareType().type(), true)));
		sw.write("(");
		sw.begin(0);
		n.printSubExpr(n.expr(), false, sw, tr);
		sw.end();
		sw.write(")");
	}

	public void visit(Throw_c n) {
		X10CPPContext_c context = (X10CPPContext_c) tr.context();
		sw.write("x10aux::throwException(x10aux::nullCheck(");
		n.print(n.expr(), sw, tr);
		sw.write("));");
	}

	public void visit(Try_c n) {
		X10CPPContext_c context = (X10CPPContext_c) tr.context();
		TypeSystem xts =  tr.typeSystem();
		
        X10Ext_c ext = (X10Ext_c) n.ext();
        if (ext.initVals != null) {
            Set<LocalDef> asyncInits = context.findData(SharedVarsMethods.ASYNC_INIT_VALS_KEY);
            if (asyncInits == null) {
                asyncInits = CollectionFactory.newHashSet(ext.initVals);
                context.addData(SharedVarsMethods.ASYNC_INIT_VALS_KEY, asyncInits);
            } else {
                asyncInits.addAll(ext.initVals);
            }
        }
		
		if (n.finallyBlock() != null) {
			sw.write("try {");
			sw.newline(0); sw.begin(0);
		}
		sw.write("try");
		assert (n.tryBlock() instanceof Block_c);
		n.printSubStmt(n.tryBlock(), sw, tr);
		sw.newline(0);
        
		// [IP] C++ will not catch ref types properly, as there is no hierarchy.
		// So, we have to do the dispatching ourselves.
		sw.newline();
		String refVar = "__ref__" + getUniqueId_();
		sw.write("catch (x10aux::__ref& " + refVar + ") {");
		sw.newline(4); sw.begin(0);
		if (n.catchBlocks().size() > 0) {
		    String excVar = "__exc" + refVar;
		    // Note that the following c-style cast only works because Throwable is
		    // *not* an interface and thus is not virtually inheritted.  If it
		    // were, we would have to static_cast the exception to Throwable on
		    // throw (otherwise we would need to offset by an unknown quantity).
		    String exception_ref = Emitter.translateType(xts.Throwable(), true);
		    sw.write(exception_ref+"& " + excVar + " = ("+exception_ref+"&)" + refVar + ";");
		    context.setExceptionVar(excVar);
		    for (Catch cb : n.catchBlocks()) {
		        sw.newline(0);
		        n.printBlock(cb, sw, tr);
		    }
		}
		sw.newline();
		sw.write("throw;");
		sw.end(); sw.newline();
		sw.write("}");
		if (n.finallyBlock() != null) {
			sw.end(); sw.newline();
			sw.write("} catch (...) {");
			sw.newline(4); sw.begin(0);
			n.printBlock(n.finallyBlock(), sw, tr);
			sw.newline();
			sw.write("throw;");
			sw.end(); sw.newline();
			sw.write("}");
			sw.newline();
			n.printBlock(n.finallyBlock(), sw, tr);
		}
	}

	public void visit(Catch_c n) {
        X10CPPContext_c context = (X10CPPContext_c) tr.context();
		String excVar = context.getExceptionVar();
		sw.newline();
		sw.write("if (");
		String type = Emitter.translateType(n.formal().type().type(), true);
        if (n.formal().type().type().typeEquals(tr.typeSystem().Throwable(), context)) {
            sw.write("true");
        } else if (refsAsPointers) {
			sw.write("!!dynamic_cast" + chevrons(type) + "(" + excVar + ")");
		} else {
			sw.write("x10aux::instanceof" + chevrons(type) + "(" + excVar + ")");
		}
		sw.write(") {");
		sw.newline(4); sw.begin(0);
		n.printBlock(n.formal(), sw, tr);
		sw.write(" =");
		sw.allowBreak(2, " ");
		sw.write("static_cast" + chevrons(type) + "(" + excVar + ");");
		sw.newline(0);
		n.print(n.body(), sw, tr);
		sw.end(); sw.newline();
		sw.write("} else");
	}

    public void visit(ParExpr_c n) {
        n.print(n.expr(), sw, tr);
    }

    public void visit(Conditional_c n) {
        n.printSubExpr(n.cond(), false, sw, tr);
        sw.unifiedBreak(2);
        sw.write("? (");
        sw.begin(0);
        n.printSubExpr(n.consequent(), true, sw, tr);
        sw.end();
        sw.write(")");
        sw.unifiedBreak(2);
        sw.write(": (");
        sw.begin(0);
        n.printSubExpr(n.alternative(), true, sw, tr);
        sw.end();
        sw.write(")");
    }

    public void visit(X10Special_c n) {
        X10CPPContext_c context = (X10CPPContext_c) tr.context();

        if (context.isInsideClosure()) {
            assert n.kind().equals(X10Special_c.THIS) || n.kind().equals(X10Special_c.SUPER);
            sw.write(SAVED_THIS);
            context.saveEnvVariableInfo(THIS);
        } else {
            if (Types.isX10Struct(n.type())) {
                sw.write("(*"+n.kind().toString()+")");
            } else {
                if (n.kind().equals(X10Special_c.THIS)) {
                    sw.write("(("+Emitter.translateType(n.type(),true)+")"+n.kind()+")");
                } else {
                    assert n.kind().equals(X10Special_c.SUPER);
                    sw.write("(("+Emitter.translateType(context.currentClass().superClass(),true)+")this)");
                }
            }
        }
    }

    public static String getClosureName(String className, int id) {
        // TODO: factor out into a constant
        return className+"__closure__"+id;
    }

    private boolean envContainsVar(List<VarInstance<? extends VarDef>> env, VarInstance<? extends VarDef> vi) {
        for (VarInstance<? extends VarDef> e : env) {
            if (e.def() == vi.def()) return true;
        }
        return false;
    }
    public void visit(Closure_c n) {
        X10CPPContext_c c = (X10CPPContext_c) tr.context();

        emitter.enterClosure(c);

        ClosureDef closureDef = n.closureDef();
        CodeInstance<?> ci = closureDef.methodContainer().get();
        X10ClassType hostClassType = (X10ClassType) closureDef.typeContainer().get();
        X10ClassDef hostClassDef = hostClassType.x10Def();
        TypeSystem xts = tr.typeSystem();

        List<Type> freeTypeParams = new ArrayList<Type>();
        while (ci instanceof ClosureInstance)
            ci = ((ClosureDef) ci.def()).methodContainer().get();
        if (ci instanceof MethodInstance) {
            MethodInstance xmi = (MethodInstance) ci;
            // in X10, static methods do not inherit the template params of their classes
            if (!xmi.flags().isStatic())
                freeTypeParams.addAll(hostClassDef.typeParameters());
            freeTypeParams.addAll(xmi.typeParameters());
        } else if (ci instanceof InitializerInstance) {
            InitializerInstance ii = (InitializerInstance) ci;
            if (!ii.def().flags().isStatic())
                freeTypeParams.addAll(hostClassDef.typeParameters());
        } else {
            // could be a constructor or other non-static thing
            freeTypeParams.addAll(hostClassDef.typeParameters());
        }

        String hostClassName = translate_mangled_FQN(Emitter.fullName(hostClassType).toString(), "_");

        c.setInsideClosure(true);

        int id = getConstructorId(c);

        String cname = getClosureName(hostClassName, id);

        boolean in_template_closure = false;

        StringBuffer cnamet_ = new StringBuffer(cname);
        String prefix = "<";
        for (Type t : freeTypeParams) {
            in_template_closure = true;
            cnamet_.append(prefix + Emitter.translateType(t));
            prefix = ",";
        }
        if (in_template_closure) cnamet_.append(" >");
        String cnamet = cnamet_.toString();
                
        // Prepend this stream to closures.  Closures are created from the outside in.
        // Thus, later closures can be used by earlier ones, but not vice versa.
        ClassifiedStream saved_closures = c.closures;
        ClassifiedStream saved_generic_closures = c.genericFunctionClosures;
        ClassifiedStream inc_s;
        if (in_template_closure) {
            inc_s = sw.getNewStream(c.genericFunctionClosures.ext, c.genericFunctionClosures, true);
            c.closures = inc_s;
            c.genericFunctionClosures = inc_s;
        } else {
            inc_s = sw.getNewStream(c.closures.ext, c.closures, true);
            c.closures = inc_s;
        }
        sw.pushCurrentStream(inc_s);

        StreamWrapper inc = sw;
        
        // A stream to put definitions of static variables.
        // If the def is templatized, it has to go in the inc stream.
        // If the def is not templatized, it has to go in the CC stream (even if sw is the Header stream).
        ClassifiedStream defn_s =  in_template_closure ? inc.currentStream() : c.staticDefinitions;

        String headerGuard = getHeaderGuard(cname);
        inc.write("#ifndef "+headerGuard+"_CLOSURE"); inc.newline();
        inc.write("#define "+headerGuard+"_CLOSURE"); inc.newline();

        Type retType = n.returnType().type();
        X10ClassType sup = (X10ClassType) ClosureSynthesizer.closureBaseInterfaceDef(xts,0, n.formals().size(), retType.isVoid()).asType();
        List<Type> supArgs = new ArrayList<Type>();
        for (Formal formal : n.formals())
            supArgs.add(formal.type().typeRef().get());
        if (!retType.isVoid())
            supArgs.add(retType);
        String superType = Emitter.translateType(sup.typeArguments(supArgs));
        String superTypeRef = Emitter.translateType(sup.typeArguments(supArgs), true);

        inc.write("#include <x10/lang/Closure.h>"); inc.newline();
        String header = getHeader(sup);
        inc.write("#include <"+header+">"); inc.newline();
        // class header
        if (!freeTypeParams.isEmpty())
            emitter.printTemplateSignature(freeTypeParams, inc);
        inc.write("class "+cname+" : public "+CLOSURE_TYPE+" {");
        inc.newline(4); inc.begin(0);
        inc.write("public:") ; inc.newline(); inc.forceNewline();

        /* ITables declarations */
        inc.write("static "+(in_template_closure ? "typename " : "")+superType+(in_template_closure ? "::template itable " : "::itable")+chevrons(cnamet)+" _itable;"); inc.newline();
        inc.write("static x10aux::itable_entry _itables[2];"); inc.newline(); inc.forceNewline();
        inc.write("virtual x10aux::itable_entry* _getITables() { return _itables; }"); inc.newline(); inc.forceNewline();

        inc.write("// closure body"); inc.newline();
        inc.write(Emitter.translateType(retType, true)+" "+Emitter.mangled_method_name(ClosureCall.APPLY.toString())+"(");
        prefix = "";
        for (Formal formal : n.formals()) {
            inc.write(prefix);
            n.print(formal, inc, tr);
            prefix = ", ";
        }
        inc.write(") ");
        n.print(n.body(), inc, tr);
        inc.newline(); inc.forceNewline();

        inc.write("// captured environment"); inc.newline();
        List<VarInstance<?>> refs = computeBoxedRefs(c, closureDef);

        List<VarInstance<? extends VarDef>> env = closureDef.capturedEnvironment();
        for (VarInstance<?> vi : c.variables) {
            if (!envContainsVar(env, vi) && !vi.name().toString().equals(THIS)) {
                // Sanity check
                String msg = "Closure "+n+" at "+n.position()+" captures "+vi+" which is not in the environment";
                Warnings.issue(tr.job(), msg, n.position());
            }
        }
        
		if (((X10CPPCompilerOptions)tr.job().extensionInfo().getOptions()).x10_config.DEBUG)
		{
			String key = ((StreamWrapper)inc).getStreamName(StreamWrapper.CC);
			Map<String, LineNumberMap> fileToLineNumberMap = c.<Map<String, LineNumberMap>>findData(X10CPPTranslator.FILE_TO_LINE_NUMBER_MAP);
		    if (fileToLineNumberMap != null) 
		    {
		        final LineNumberMap lineNumberMap = fileToLineNumberMap.get(key);
		        if (lineNumberMap != null)
		        {
		        	int numMembers = c.variables.size();
		        	for (int i = 0; i < numMembers; i++) 
		        	{
		        		VarInstance<?> var = c.variables.get(i);
		        		String name = var.name().toString();
		        		if (name.equals(THIS)) 
		    				name = SAVED_THIS;
		        		lineNumberMap.addClosureMember(name, var.type().toString(), cname, c.currentCode().position().file(), c.currentCode().position().line(), c.currentCode().position().endLine());
		        	}
		        	if (numMembers > 0)
		        	{
			        	for (int i=0; i< n.formals().size(); i++)
			        	{
			        		Formal f = n.formals().get(i);
			        		lineNumberMap.addLocalVariableMapping(f.name().toString(), f.type().toString(), c.currentCode().position().line(), c.currentCode().position().endLine(), c.currentCode().position().file(), true, -2, false);
			        	}
		        	}
		        }
		    }
		}

        emitter.printDeclarationList(inc, c, c.variables, refs);
        inc.forceNewline();

        inc.write("x10aux::serialization_id_t "+SERIALIZE_ID_METHOD+"() {");
        inc.newline(4); inc.begin(0);
        inc.write("return "+SERIALIZATION_ID_FIELD+";"); inc.end(); inc.newline();
        inc.write("}"); inc.newline(); inc.forceNewline();

        generateClosureSerializationFunctions(c, cnamet, inc, n.body(), refs);

//        inc.write(cname+"("+SERIALIZATION_MARKER+") { }");
//        inc.newline(); inc.forceNewline();

        inc.write(cname+"(");
        for (int i = 0; i < c.variables.size(); i++) {
            if (i > 0) inc.write(", ");
            VarInstance<?> var = (VarInstance<?>) c.variables.get(i);
            String name = var.name().toString();
            if (name.equals(THIS))
                name = SAVED_THIS;
            else name = mangled_non_method_name(name);
            if (refs.contains(var)) {
                inc.write(make_captured_lval(var.type()) + " " + name);
            } else {
                inc.write(Emitter.translateType(var.type(), true) + " " + name);
            }
        }
        inc.write(")");
        inc.begin(0);
        // FIXME: factor out this loop
        for (int i = 0 ; i < c.variables.size() ; i++) {
            VarInstance<?> var = (VarInstance<?>) c.variables.get(i);
            String name = var.name().toString();
            if (name.equals(THIS))
                name = SAVED_THIS;
            else name = mangled_non_method_name(name);
            if (i > 0) inc.write(", "); else inc.write(" : ");
            inc.write(name + "(" + name + ")");
        }
        inc.end();
        inc.write(" { }"); inc.newline(); inc.forceNewline();

        inc.write("static const x10aux::serialization_id_t "+SERIALIZATION_ID_FIELD+";");
        inc.newline(); inc.forceNewline();

        inc.write("static const x10aux::RuntimeType* getRTT() {"+
                  " return x10aux::getRTT"+chevrons(superType)+"(); }");
        inc.newline();
        inc.write("virtual const x10aux::RuntimeType *_type() const {"+
                  " return x10aux::getRTT"+chevrons(superType)+"(); }");
        inc.newline(); inc.forceNewline();

        inc.write(Emitter.translateType(xts.String(), true)+" toString() {");
        inc.newline(4); inc.begin(0);
        inc.write("return x10aux::string_utils::lit(this->toNativeString());");
        inc.end(); inc.newline();
        inc.write("}");
        inc.newline(); inc.forceNewline();

        inc.write("const char* toNativeString() {");
        inc.newline(4); inc.begin(0);
        inc.write("return \""+StringUtil.escape(n.position().nameAndLineString())+"\";");
        inc.end(); inc.newline();
        inc.write("}");
        inc.end(); inc.newline(); inc.forceNewline();

        inc.write("};"); inc.newline(); inc.forceNewline();

        if (in_template_closure)
            emitter.printTemplateSignature(freeTypeParams, defn_s);
        // TODO: To workaround XTENLANG-467, we are explicitly qualifying inherited member functions here.
        // This is less than ideal, since it can introduce subtle bugs when the C++ code is refactored
        // (an overridden member function will not be called from the itable, which is very non-intuitive).
        // As soon as XTENLANG-467 is fixed, take out the explicit qualifications and let C++ member lookup do its job...
        defn_s.writeln((in_template_closure ? "typename ": "")+superType+(in_template_closure ? "::template itable ": "::itable")+chevrons(cnamet)+
        			cnamet+"::_itable(&"+REFERENCE_TYPE+"::equals, &"+CLOSURE_TYPE+"::hashCode, &"+
        			cnamet+"::"+Emitter.mangled_method_name(ClosureCall.APPLY.toString())+", &"+
        			cnamet+"::toString, &"+CLOSURE_TYPE+"::typeName);");

        if (in_template_closure)
            emitter.printTemplateSignature(freeTypeParams, defn_s);
		defn_s.write("x10aux::itable_entry "+cnamet+"::_itables[2] = {");
		defn_s.write("x10aux::itable_entry(&x10aux::getRTT"+chevrons(superType)+", &"+cnamet+"::_itable),");
		defn_s.write("x10aux::itable_entry(NULL, NULL)};"); defn_s.newline(); defn_s.forceNewline();

		int kind = 0;
		try {
			if (!((X10Ext)(n.body()).ext()).annotationMatching(xts.systemResolver().findOne(QName.make("x10.compiler.AsyncClosure"))).isEmpty()) {
				kind = 1;
			}
			if (!((X10Ext)(n).ext()).annotationMatching(xts.systemResolver().findOne(QName.make("x10.compiler.AsyncClosure"))).isEmpty()) {
				kind = 1;
			}
		} catch (SemanticException e) {
		}
		try {
			if (!((X10Ext)(n.body()).ext()).annotationMatching(xts.systemResolver().findOne(QName.make("x10.compiler.RemoteInvocation"))).isEmpty()) {
				kind = 2;
			}
			if (!((X10Ext)(n).ext()).annotationMatching(xts.systemResolver().findOne(QName.make("x10.compiler.RemoteInvocation"))).isEmpty()) {
				kind = 2;
			}
		} catch (SemanticException e) {
		}
		generateClosureDeserializationIdDef(defn_s, cnamet, freeTypeParams, hostClassName, n.body(), kind);

		inc.write("#endif // "+headerGuard+"_CLOSURE"); inc.newline();

        sw.popCurrentStream();
        c.closures = saved_closures;
        c.genericFunctionClosures = saved_generic_closures;

        // create closure instantiation (not in inc but where the closure was defined)
        // note that we alloc using the typeof the superType but we pass in the correct size
        // this is because otherwise alloc may (when debugging is on) try to examine the
        // RTT of the closure (which doesn't exist)

        // first get the template arguments (if any)
        prefix="<";
        StringBuffer sb = new StringBuffer();
        for (Type t : freeTypeParams) {
            sb.append(prefix+Emitter.translateType(t, true));
            prefix = ",";
        }
        if (prefix.equals(",")) sb.append(">");
        String templateArgs = sb.toString();

        boolean stackAllocateClosure = ((X10CPPContext_c)c).closureOuter.stackAllocateClosure;
        if (!stackAllocateClosure) {
            sw.write(make_ref(superType)+"("+make_ref(cnamet));
            sw.write("(new (x10aux::alloc"+chevrons(superType)+"(sizeof("+cname+templateArgs+")))");
        }
        sw.write(cname+templateArgs+"(");
        for (int i = 0; i < c.variables.size(); i++) {
            if (i > 0) sw.write(", ");
            VarInstance<?> var = (VarInstance<?>) c.variables.get(i);
            String name = var.name().toString();
            if (name.equals(THIS)) {
                // FIXME: Hack upon hack...
                if (((X10CPPContext_c)c.pop()).isInsideClosure())  { 
                    name = SAVED_THIS;
                } else if (xts.isStruct(var.type())) {
                    name = STRUCT_THIS;
                }
            } else {
                name = mangled_non_method_name(name);
            }
            if (refs.contains(var)) {
                sw.write("&("+name+")");
            } else {
                sw.write(name);
            }
        }
        sw.write(")");
        if (!stackAllocateClosure) {
            sw.write("))");
        }

        c.finalizeClosureInstance();
        emitter.exitClosure(c);
    }

    protected List<VarInstance<?>> computeBoxedRefs(X10CPPContext_c c, ClosureDef closureDef) {
        List<Type> ats = closureDef.annotationsNamed(QName.make("x10.compiler.Ref"));
        List<VarInstance<?>> refs = new ArrayList<VarInstance<?>>();
        for (Type at : ats) {
            Expr exp = ((X10ParsedClassType_c) at).propertyInitializer(0);
            if (exp instanceof X10Local_c) {
                refs.add(((X10Local_c) exp).varInstance());
            }
        }
        
        for (VarInstance<?> var : c.variables) {
            VarDef def = var.def();
            if ((def instanceof X10LocalDef)) {
                X10LocalDef ld = ((X10LocalDef)def);
                if (!ld.flags().isFinal()) {
                    refs.add(var);
                } else if (ld.isAsyncInit()) {
                    Set<LocalDef> currentAsyncInits = c.findData(SharedVarsMethods.ASYNC_INIT_VALS_KEY);
                    if (currentAsyncInits != null && currentAsyncInits.contains(ld)) {
                        refs.add(var);
                    }
                }
            }
        }
        return refs;
    }

    protected void generateClosureSerializationFunctions(X10CPPContext_c c, String cnamet, StreamWrapper inc, 
                                                         Block block, List<VarInstance<?>> refs) {
        inc.write("void "+SERIALIZE_BODY_METHOD+"("+SERIALIZATION_BUFFER+" &buf) {");
        inc.newline(4); inc.begin(0);
        // FIXME: factor out this loop
        for (int i = 0; i < c.variables.size(); i++) {
            if (i > 0) inc.newline();
            VarInstance<?> var = (VarInstance<?>) c.variables.get(i);
            String name = var.name().toString();
            if (name.equals(THIS)) {
                name = SAVED_THIS;
            } else {
                name = mangled_non_method_name(name);
            }
            inc.write("buf.write(this->" + name + ");");
        }
        inc.end(); inc.newline();
        inc.write("}"); inc.newline(); inc.forceNewline();

        inc.write("template<class __T> static "+make_ref("__T")+" "+DESERIALIZE_METHOD+"("+DESERIALIZATION_BUFFER+" &buf) {");
        inc.newline(4); inc.begin(0);
        inc.writeln(cnamet+"* storage = x10aux::alloc"+chevrons(cnamet)+"();");
        inc.writeln("buf.record_reference("+make_ref(cnamet)+"(storage));");
        
        // FIXME: factor out this loop
        for (int i = 0; i < c.variables.size(); i++) {
            VarInstance<?> var = (VarInstance<?>) c.variables.get(i);
            Type t = var.type();
            String type;
            if (refs.contains(var)) {
                type = make_captured_lval(t);
            } else {
                type = Emitter.translateType(t, true);
            }
            String name = var.name().toString();
            if (name.equals(THIS)) {
                name = SAVED_THIS;
            } else {
                name = mangled_non_method_name(name);
            }
            inc.write(type + " that_"+name+" = buf.read"+chevrons(type)+"();");
            inc.newline();
        }
        inc.write(make_ref(cnamet)+" this_ = new (storage) "+cnamet+"(");
        // FIXME: factor out this loop
        for (int i = 0; i < c.variables.size(); i++) {
            VarInstance<?> var = (VarInstance<?>) c.variables.get(i);
            String name = var.name().toString();
            if (name.equals(THIS))
                name = SAVED_THIS;
            else name = mangled_non_method_name(name);
            if (i > 0) inc.write(", ");
            inc.write("that_"+name);
        }
        inc.write(");");
        inc.newline();
        inc.write("return this_;"); inc.end(); inc.newline();
        inc.write("}"); inc.newline(); inc.forceNewline();
    }

    protected String closure_kind_strs[] = new String[] {
    		"x10aux::CLOSURE_KIND_NOT_ASYNC",
    		"x10aux::CLOSURE_KIND_SIMPLE_ASYNC",
    		"x10aux::CLOSURE_KIND_GENERAL_ASYNC"
    };

    protected void generateClosureDeserializationIdDef(ClassifiedStream defn_s, String cnamet, List<Type> freeTypeParams, String hostClassName, Block block, int kind) {
        TypeSystem xts =  tr.typeSystem();
        boolean in_template_closure = freeTypeParams.size()>0;
        if (in_template_closure)
            emitter.printTemplateSignature(freeTypeParams, defn_s);
        defn_s.write("const x10aux::serialization_id_t "+cnamet+"::"+SERIALIZATION_ID_FIELD+" = ");
        defn_s.newline(4);
        String template = in_template_closure ? "template " : "";
        defn_s.write("x10aux::DeserializationDispatcher::addDeserializer("+
                  cnamet+"::"+template+DESERIALIZE_METHOD+
                  chevrons("x10::lang::Reference")+","+closure_kind_strs[kind]+");");
        defn_s.newline(); defn_s.forceNewline();
    }

	private Closure_c getClosureLiteral(Expr target) {
	    if (target instanceof Closure_c)
	        return (Closure_c) target;
	    if (target instanceof ParExpr_c)
	        return getClosureLiteral(((ParExpr_c)target).expr());
	    return null;
	}

    // ClosureCall_c really means "call operator() on me, and if I happen to be a closure literal understand that means invoking my body"
    // So we have to handle 3 different cases: 
    //    (a) closure literal that for some odd reason wasn't inlined (should not really happen...)
    //    (b) a function type
    //    (c) an class (anonymous or not) that has an operator()
	public void visit(ClosureCall_c c) {
        X10CPPContext_c context = (X10CPPContext_c) tr.context();
		Expr target = c.target();
		Type t = target.type();
		boolean needsNullCheck = needsNullCheck(target);
        Closure_c lit = getClosureLiteral(target);
		if (lit != null) {
		    // Optimize to stack-allocated closure and non-virtual dispatch
		    context.setStackAllocateClosure(true);
		    c.printSubExpr(target, sw, tr);
		    context.setStackAllocateClosure(false);
		    sw.write("."+Emitter.mangled_method_name(ClosureCall.APPLY.toString())+"(");
		} else {
		    if (t.isClass() && t.toClass().isAnonymous()) {
		        ClassType tc = t.toClass();
		        if (tc.interfaces().size() > 0) {
		            t = tc.interfaces().get(0);
		        } else {
		            t = tc.superClass();
		        }
		    }
		    
		    if (t.isClass() && t.toClass().flags().isInterface()) {
		        MethodInstance ami = null;
		        TypeSystem xts = tr.typeSystem();
		        try {
		            List<Type> actualTypes = new ArrayList<Type>();
		            for (Expr a : c.arguments()) {
		                actualTypes.add(a.type());
		            }
		            ami = xts.findMethod(t, xts.MethodMatcher(c.type(), ClosureCall.APPLY, actualTypes, context));  // todo: double check this code
		        } catch (SemanticException e) {
		            e.printStackTrace();
		            assert (false);
		        }
		        invokeInterface(c, target, c.arguments(), make_ref(REFERENCE_TYPE), t.toClass(), ami, needsNullCheck);
		        return;
		    } else {
		        if (needsNullCheck) sw.write("x10aux::nullCheck(");
		        c.printSubExpr(target, sw, tr);
		        if (needsNullCheck) sw.write(")");
		        sw.write("->"+Emitter.mangled_method_name(ClosureCall.APPLY.toString())+"(");
		    }
		}

		sw.begin(0);
		boolean first = true;
		for (Expr e : c.arguments()) {
			if (!first) {
			    sw.write(",");
			    sw.allowBreak(0, " ");
			}
			c.print(e, sw, tr);
			first = false;
		}
		sw.end();
		sw.write(")");
	}


	public void visit(X10CanonicalTypeNode_c n) {
		Type t = n.type();
		if (t == null)
		    throw new InternalCompilerError("Unknown type");

		sw.write(Emitter.translateType(t));
	}


	public void visit(X10Unary_c n) {
	    X10CPPContext_c context = (X10CPPContext_c) tr.context();

	    Expr left = n.expr();
	    Type l = left.type();
	    TypeSystem xts = (TypeSystem) tr.typeSystem();
	    NodeFactory nf = tr.nodeFactory();
	    Unary.Operator op = n.operator();

	    if (op == Unary.POST_DEC || op == Unary.POST_INC || op == Unary.PRE_DEC || op == Unary.PRE_INC) { // TODO
	        visit((Unary_c)n);
	        return;
	    }
	    if (l.isNumeric()) { // TODO: get rid of this special case by defining native operators
	        visit((Unary_c)n);
	        return;
	    }
	    if (l.isBoolean()) { // TODO: get rid of this special case by defining native operators
	        visit((Unary_c)n);
	        return;
	    }
	    assert (false) : ("User-defined unary operators should have been desugared earier");

	    // FIXME: move this to the Desugarer
	    Name methodName = X10Unary_c.unaryMethodName(op);
	    Expr receiver = left;

	    if (methodName == null)
	        throw new InternalCompilerError("No method to implement " + n, n.position());

	    try {
	        List<Type> types = Arrays.asList(new Type[] { });
	        MethodInstance mi = xts.findMethod(receiver.type(),
	                xts.MethodMatcher(receiver.type(), methodName, types, context));
	        List<Expr> args = Arrays.asList(new Expr[] { });
	        n.print(nf.Call(n.position(), receiver, nf.Id(n.position(), methodName),
	                args).methodInstance(mi).type(mi.returnType()), sw, tr);
	    } catch (SemanticException e) { }
	}

	public void visit(Unary_c n) {
		Unary_c.Operator operator = n.operator();
		Expr expr = n.expr();
		if (operator == Unary_c.NEG && expr instanceof IntLit) {
		    IntLit_c lit = (IntLit_c) expr;
		    IntLit.Kind kind = lit.kind().toSigned();
		    n.printSubExpr(lit.value(-lit.longValue()).kind(kind), true, sw, tr);
		}
		else if (operator.isPrefix()) {
			sw.write(operator.toString());
			n.printSubExpr(expr, false, sw, tr);
		}
		else {
			n.printSubExpr(expr, false, sw, tr);
			sw.write(operator.toString());
		}
	}


	public void visit(X10Binary_c n) {
	    X10CPPContext_c context = (X10CPPContext_c) tr.context();

	    Expr left = n.left();
	    Type l = left.type();
	    Expr right = n.right();
	    Type r = right.type();
	    TypeSystem xts = (TypeSystem) tr.typeSystem();
	    NodeFactory nf = tr.nodeFactory();
	    Binary.Operator op = n.operator();

	    if (op == Binary.EQ || op == Binary.NE) { // FIXME: get rid of this special case
	        sw.write("("); sw.begin(0);
	        if (op == Binary.NE) {
	            sw.write("!");
	        }
	        sw.write(STRUCT_EQUALS+"("); sw.begin(0);
	        n.printSubExpr(left, sw, tr);
 	        sw.write(",");
	        sw.allowBreak(0, " ");
	        n.printSubExpr(right, sw, tr);

	        sw.end(); sw.write(")");
	        sw.end(); sw.write(")");
	        return;
	    }
	    if (l.isNumeric() && r.isNumeric()) { // TODO: get rid of this special case by defining native operators
	        visit((Binary_c)n);
	        return;
	    }
	    if (l.isBoolean() && r.isBoolean()) { // TODO: get rid of this special case by defining native operators
	        visit((Binary_c)n);
	        return;
	    }
	    if (op == Binary.ADD && (l.isSubtype(xts.String(), context) || r.isSubtype(xts.String(), context))) { // TODO: get rid of this special case by defining native operators
	        visit((Binary_c)n);
	        return;
	    }
	    assert (false) : ("User-defined binary operators should have been desugared earier");

	    // FIXME: move this to the Desugarer
	    boolean inv = n.invert();
	    Name methodName = inv ? X10Binary_c.invBinaryMethodName(op) : X10Binary_c.binaryMethodName(op);
	    Expr receiver = inv ? right : left;
	    Expr arg = inv ? left : right;

	    if (methodName == null)
	        throw new InternalCompilerError("No method to implement " + n, n.position());

	    try {
	        List<Type> types = Arrays.asList(new Type[] { arg.type() });
	        MethodInstance mi = xts.findMethod(receiver.type(),
	                xts.MethodMatcher(receiver.type(), methodName, types, context));
	        List<Expr> args = Arrays.asList(new Expr[] { arg });
	        n.print(nf.Call(n.position(), receiver, nf.Id(n.position(), methodName),
	                args).methodInstance(mi).type(mi.returnType()), sw, tr);
	    } catch (SemanticException e) { }
	}

	public void visit(Binary_c n) {
		String opString = n.operator().toString();

		// Boolean short-circuiting operators are ok
		TypeSystem xts = tr.typeSystem();
		assert (opString.equals("&&") || opString.equals("||"))
		    : "visiting "+n.getClass()+" at "+n.position()+": "+n;

		n.printSubExpr(n.left(), true, sw, tr);
		sw.write(" ");
		sw.write(opString);
		sw.allowBreak(0, " ");
		n.printSubExpr(n.right(), false, sw, tr);
	}

    // allow overriding in subclasses (i.e. CUDACodeGenerator)
    // [DC] FIXME: ASTQuery.getCppRepParam still uses CPP_NATIVE_STRING directly
    protected String[] getCurrentNativeStrings() { return new String[] {CPP_NATIVE_STRING}; }

	String getCppImplForDef(X10Def o) {
		return getCppImplForDef(o, null);
	}
	String getCppImplForDef(X10Def o, String[] langbox) {
	    TypeSystem xts = (TypeSystem) o.typeSystem();
	    Type annotation = xts.NativeType();
	    String[] our_langs = getCurrentNativeStrings();
	    for (String our_lang : our_langs) {
	        List<Type> as = o.annotationsMatching(annotation);
	        for (Type at : as) {
	            assertNumberOfInitializers(at, 2);
	            String lang = getStringPropertyInit(at, 0);
	            if (lang != null && lang.equals(our_lang)) {
	                String lit = getStringPropertyInit(at, 1);
	                if (langbox!=null) langbox[0] = our_lang;
	                return lit;
	            }
	        }
	    }
	    return null;
	}

    private String getCppImplForStmt(Stmt n) {
        TypeSystem xts = (TypeSystem) tr.typeSystem();
        if (n.ext() instanceof X10Ext) {
            X10Ext ext = (X10Ext) n.ext();
            Type annotation = xts.NativeType();
            List<X10ClassType> as = ext.annotationMatching(annotation);
            String[] our_langs = getCurrentNativeStrings();
            for (String our_lang : our_langs) {
                for (Type at : as) {
                    assertNumberOfInitializers(at, 2);
                    String lang = getStringPropertyInit(at, 0);
                    if (lang != null && lang.equals(our_lang)) {
                        String lit = getStringPropertyInit(at, 1);
                        return lit;
                    }
                }
            }
        }
        return null;
    }

	// FIXME: generic native methods will break
	void emitNativeAnnotation(String pat, List<ParameterType> typeParams, List<Type> typeArgs, Object receiver, List<String> params, List<? extends Object> args, List<ParameterType> typeParams2, List<Type> typeArgs2) {
		assert (receiver != null);

		//Object[] components = new Object[1+3*types.size() + args.size() + 3*typeArguments.size()];
		Map<String,Object> components = new HashMap<String,Object>();

		components.put("this", receiver);
		components.put("0", receiver);

		// [DC] these ought to still work, but there may now be a better solution
	    if (receiver instanceof X10Special_c && ((X10Special_c)receiver).kind() == X10Special_c.SUPER) {
	        pat = pat.replaceAll("\\(#0\\)->", Emitter.translateType(tr.context().currentClass().superClass())+"::"); // FIXME: HACK
	        pat = pat.replaceAll("\\(#0\\)", "("+Emitter.translateType(((X10Special_c)receiver).type(), true)+"((#0*)this))"); // FIXME: An even bigger HACK (remove when @Native migrates to the body)
            pat = pat.replaceAll("\\(#this\\)->", Emitter.translateType(tr.context().currentClass().superClass())+"::"); // FIXME: HACK
            pat = pat.replaceAll("\\(#this\\)", "("+Emitter.translateType(((X10Special_c)receiver).type(), true)+"((#0*)this))"); // FIXME: An even bigger HACK (remove when @Native migrates to the body)
	    }

	    int i = 1;
	    Iterator<ParameterType> typeParams_ = typeParams.iterator();
	    for (Type at : typeArgs) {
			components.put(typeParams_.next().name().toString(), at);
			components.put(Integer.toString(i++), at);
			components.put(Integer.toString(i++), "/"+"*"+" UNUSED "+"*"+"/");
			components.put(Integer.toString(i++), "/"+"*"+" UNUSED "+"*"+"/");
	    }
	    int j=0;
	    Iterator<String> params_ = params.iterator();
	    for (Object e : args) {
			components.put(params_.next(), e);
			components.put(Integer.toString(i++), e);
	    }
	    Iterator<ParameterType> typeParams2_ = typeParams2.iterator();
	    for (Type at : typeArgs2) {
			components.put(typeParams2_.next().name().toString(), at);
			components.put(Integer.toString(i++), at);
			components.put(Integer.toString(i++), "/"+"*"+" UNUSED "+"*"+"/");
			components.put(Integer.toString(i++), "/"+"*"+" UNUSED "+"*"+"/");
	    }
	    emitter.nativeSubst("Native", components, tr, pat, sw);
	}

	private static boolean isPODType(Type t) {
	    return (t.isBoolean() || t.isByte() || t.isShort() || t.isInt() || t.isLong() ||
	            t.isFloat() || t.isDouble());
	}

	public void visit(Tuple_c c) {
	    TypeSystem xts =  tr.typeSystem();
	    Context context = tr.context();

		// Handles Array initializer.
		Type T = Types.getParameterType(c.type(), 0);		
		String type = Emitter.translateType(c.type());
		String tmp = getId();
		// [DC] this cast is needed to ensure everything has a ref type
		// otherwise overloads don't seem to work properly
		sw.write("("+make_ref(type)+")");

		sw.write("(__extension__ ({");
		sw.newline(4); sw.begin(0);
		sw.write(make_ref(type)+" "+tmp+"(");

		sw.write("x10::array::Array"+chevrons(Emitter.translateType(T, true)));
		sw.writeln("::_make("+c.arguments().size()+"));");
		int count = 0;
		for (Expr e : c.arguments()) {
		    sw.write(tmp+"->"+Emitter.mangled_method_name(SettableAssign.SET.toString())+"(");
		    sw.writeln((count++)+", ");
		    c.printSubExpr(e, false, sw, tr);
		    sw.writeln(");");
		}
		sw.write(tmp);
		X10CPPCompilerOptions opts = (X10CPPCompilerOptions) tr.job().extensionInfo().getOptions();
		if (!opts.x10_config.ALLOW_STATEMENT_EXPRESSIONS) {
		    // FIXME: HACK around a compiler bug in GCC 4.1
		    sw.write(".operator->()");
		}
		sw.write(";");
		sw.end(); sw.newline();
		sw.write("}))");
	}

    protected boolean isPerProcess(X10Def def) {
        try {
            Type t = tr.typeSystem().systemResolver().findOne(QName.make("x10.compiler.PerProcess"));
            return !def.annotationsMatching(t).isEmpty();
        } catch (SemanticException e) {
            return false;
        }
    }
} // end of MessagePassingCodeGenerator
// vim:tabstop=4:shiftwidth=4:expandtab