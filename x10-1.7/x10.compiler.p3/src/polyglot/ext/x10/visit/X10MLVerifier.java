/**
 * 
 */
/*
 *
 * (C) Copyright IBM Corporation 2006-2008.
 *
 *  This file is part of X10 Language.
 *
 */

package polyglot.ext.x10.visit;

import polyglot.ast.Call;
import polyglot.ast.ClassDecl;
import polyglot.ast.ConstructorCall;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.FieldDecl;
import polyglot.ast.MethodDecl;
import polyglot.ast.New;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.SourceFile;
import polyglot.ast.TypeNode;
import polyglot.ext.x10.ast.X10MLSourceFile;
import polyglot.frontend.Job;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.types.UnknownType;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;

/**
 * @author nystrom
 *
 */
public class X10MLVerifier extends ContextVisitor {

	/**
	 * @param job
	 * @param ts
	 * @param nf
	 */
	public X10MLVerifier(Job job, TypeSystem ts, NodeFactory nf) {
		super(job, ts, nf);
	}
	
	public Node leaveCall(Node old, Node n, NodeVisitor v) throws SemanticException {
		// We should redo type-checking, but it's currently not idempotent,
		// so will fail.
		
		// For now, we just check a few easy things.
		
		if (n.position() == null) {
			n = n.position(Position.COMPILER_GENERATED);
		}
		
		// Just check that we're dealing with an XML file.
		if (n instanceof SourceFile) {
			if (! (n instanceof X10MLSourceFile)) {
				throw new InternalCompilerError("Non-XML file loaded via XML parser.", n.position());
			}
			return n;
		}
		
		if (n instanceof TypeNode) {
			TypeNode tn = (TypeNode) n;
			if (tn.type() == null || tn.type() instanceof UnknownType) {
				throw new SemanticException("Missing or invalid type " + tn, n.position());
			}
		}
		
		if (n instanceof Expr) {
			Expr e = (Expr) n;
			if (e.type() == null || e.type() instanceof UnknownType) {
				throw new SemanticException("Missing or invalid type for expression " + e, n.position());
			}
		}
		
		if (n instanceof ClassDecl) {
			ClassDecl fd = (ClassDecl) n;
			if (fd.classDef() == null) {
				throw new SemanticException("Missing type for class declaration " + fd, n.position());
			}
		}
		
		if (n instanceof Call) {
			Call f = (Call) n;
			if (f.methodInstance() == null) {
				throw new SemanticException("Missing or invalid MethodInstance for call " + f, n.position());
			}
		}
		
		if (n instanceof FieldDecl) {
			FieldDecl fd = (FieldDecl) n;
			if (fd.fieldDef() == null) {
				throw new SemanticException("Missing or invalid FieldInstance for declaration " + fd, n.position());
			}
		}
		
		if (n instanceof Field) {
			Field f = (Field) n;
			if (f.fieldInstance() == null) {
				throw new SemanticException("Missing or invalid FieldInstance for field access " + f, n.position());
			}
		}
		
		if (n instanceof MethodDecl) {
			MethodDecl fd = (MethodDecl) n;
			if (fd.methodDef() == null) {
				throw new SemanticException("Missing or invalid MethodInstance for declaration " + fd, n.position());
			}
		}
		
		if (n instanceof Call) {
			Call f = (Call) n;
			if (f.methodInstance() == null) {
				throw new SemanticException("Missing or invalid MethodInstance for call " + f, n.position());
			}
		}
		
		if (n instanceof ConstructorDecl) {
			ConstructorDecl fd = (ConstructorDecl) n;
			if (fd.constructorDef() == null) {
				throw new SemanticException("Missing or invalid ConstructorInstance for declaration " + fd, n.position());
			}
		}
		
		if (n instanceof ConstructorCall) {
			ConstructorCall f = (ConstructorCall) n;
			if (f.constructorInstance() == null) {
				throw new SemanticException("Missing or invalid ConstructorInstance for constructor call " + f, n.position());
			}
		}
		
		if (n instanceof New) {
			New f = (New) n;
			if (f.constructorInstance() == null) {
				throw new SemanticException("Missing or invalid ConstructorInstance for new expression " + f, n.position());
			}
			if (f.body() != null && f.anonType() == null) {
			    throw new SemanticException("Missing or invalid anonymous type ClassDef for new expression " + f, n.position());
			}
		}
		
		return n;
	}

}