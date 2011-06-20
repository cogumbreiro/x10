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

package polyglot.ext.x10.types;

import java.util.Collections;
import java.util.List;

import polyglot.types.Flags;
import polyglot.types.LocalDef_c;
import polyglot.types.QName;
import polyglot.types.Ref;
import polyglot.types.Name;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.VarDef_c.ConstantValue;
import polyglot.util.Position;
import polyglot.util.TypedList;

/**
 * An X10ConstructorInstance_c varies from a ConstructorInstance_c only in that it
 * maintains a returnType. If an explicit returnType is not declared in the constructor
 * then the returnType is simply a noClause variant of the container.
 * @author vj
 *
 */
public class X10LocalDef_c extends LocalDef_c implements X10LocalDef {
    public X10LocalDef_c(TypeSystem ts, Position pos,
            Flags flags, 
            Ref<? extends Type> type,
            Name name) {
        super(ts, pos, flags, type, name);
    }
    
    public String toString() {
	ConstantValue cv = constantRef.getCached();
	String cvStr = "";
	
	if (cv != null && cv.isConstant()) {
		Object v = cv.value();
		if (v instanceof String) {
			String s = (String) v;
	
			if (s.length() > 8) {
				s = s.substring(0, 8) + "...";
			}
	
			v = "\"" + s + "\"";
		}
	
		cvStr = " = " + v;
	}
	
	return "local " + flags.translate() + name + ": " + type + cvStr;
    }

    // BEGIN ANNOTATION MIXIN
    List<Ref<? extends Type>> annotations;

    public List<Ref<? extends Type>> defAnnotations() {
	if (annotations == null) return Collections.EMPTY_LIST;
        return Collections.unmodifiableList(annotations);
    }
    
    public void setDefAnnotations(List<Ref<? extends Type>> annotations) {
        this.annotations = TypedList.<Ref<? extends Type>>copyAndCheck(annotations, Ref.class, true);
    }
    
    public List<Type> annotations() {
        return X10TypeObjectMixin.annotations(this);
    }
    
    public List<Type> annotationsMatching(Type t) {
        return X10TypeObjectMixin.annotationsMatching(this, t);
    }
    
    public List<Type> annotationsNamed(QName fullName) {
        return X10TypeObjectMixin.annotationsNamed(this, fullName);
    }
    // END ANNOTATION MIXIN
}