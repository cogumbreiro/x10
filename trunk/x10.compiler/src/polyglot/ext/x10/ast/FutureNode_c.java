/*
 * Created on Nov 26, 2004
 *
 * 
 */
package polyglot.ext.x10.ast;

import polyglot.ext.jl.ast.TypeNode_c;
import polyglot.ext.x10.types.FutureType_c;

import polyglot.util.Position;
import polyglot.util.CodeWriter;

import polyglot.ast.TypeNode;
import polyglot.ast.Node;

import polyglot.types.SemanticException;
import polyglot.types.Type;

import polyglot.visit.TypeChecker;
import polyglot.visit.NodeVisitor;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.TypeBuilder;
import polyglot.visit.PrettyPrinter;

import polyglot.main.Report;




/** A FutureNode is an TypeNode that has been marked with a future
 * qualifier.  ote that the base TypeNode may be ambiguous and may need to be resolved.
 * TODO: Check if the buildTypes phase should be used on this node.
 * TODO: Find a better way of handling the constructin of FutureType.
 * @author vj
 *
 */
public class FutureNode_c extends TypeNode_c implements FutureNode {

	// Recall the field this.type is defined at the supertype.
	// The Typenode representing the argument type X.
    protected TypeNode base;

    public FutureNode_c(Position pos, TypeNode base) {
    	super( pos );
    	this.base = base;
    }


    public TypeNode base() {
    	return this.base;
    }
    public FutureNode_c base( TypeNode base ) {
    	FutureNode_c n = (FutureNode_c) copy();
    	n.base = base;
    	return n;
    }

  protected FutureNode_c reconstruct( TypeNode base ) {
      return (base != this.base) ? base( base ) :  this;
  }

    // Fri Nov 26 22:31:31 2004 vj I dont understand yet what this does...
    public Node buildTypes(TypeBuilder tb) throws SemanticException {
    	/*
    	Report.report(5,"[FutureNode_c] Building type |" + this + "|:");
    	// new SemanticException("", position()).printStackTrace( System.out);
    	Type myType = tb.typeSystem().unknownType(position());
		Report.report(5,"[FutureNode_c] ... yields type |" + myType + "|(hashcode:"+ myType.hashCode()+ ").");
    	Node result = type( myType );
    	*/ 
    	return this;
    }


    public Node visitChildren( NodeVisitor v ) {
    	TypeNode base = (TypeNode) visitChild( this.base, v);
    	return reconstruct( base );
    }

    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
    	// Report.report(5,"[FutureNode_c] Disambiguating |" + this + "|(#" + this.hashCode() +"):");
    	TypeNode newType = (TypeNode) base.disambiguate( sc );
    	// Report.report(5,"[FutureNode_c] ... yields type |" + type + "|.");
    	Type baseType = newType.type();
    	if (null == baseType ) {
    		throw new SemanticException("The type constructor future cannot be applied to a <null> type", 
        			position());
    	}
    	this.type = new FutureType_c( baseType.typeSystem(), position(), baseType);
    	Node result = reconstruct( newType );
    	// Report.report(5,"[FutureNode_c] ... returns |" + result +"|(#" + result.hashCode() +").");
    	return result; 
       }

    /**
     * Typecheck the type-argument. 
     */
    public Node typeCheck( TypeChecker tc) throws SemanticException {
    	// Report.report(5,"[FutureNode_c] Type checking |" + this +"|:");
    	Node n = base.typeCheck( tc );
    	// Report.report(5,"[FutureNode_c] ... yields node |" + n +"|.");
    	if (n instanceof TypeNode) {
    	 TypeNode arg = (TypeNode) n;
    	 Type argType = arg.type();
    	 // TypeNode result = new CanonicalFutureTypeNode_c( position(), arg.type());
    	 this.type = new FutureType_c( argType.typeSystem(), position(), argType);
    	 // this.base = null?
		 // Report.report(5, "[FutureNode_c] ... sets type to |" + this.type + "|.");
		 // Report.report(5, "[FutureNode_c] ... returns |" + this + "|.");
    	 return this;
    	}
    	throw new SemanticException("Argument to future type-constructor does not type-check" + position());
    }

    public String toString() {
    	return "future " + (base == null ? "??" : base.toString());
  }
    /**
     * Write out Java code for this node.
     */
    public void prettyPrint(CodeWriter w, PrettyPrinter ignore) {             
        w.write("x10.lang.Future " /*+ base */);
      } 

    // translate??
    // prettyPrint?
    // dump?
}
