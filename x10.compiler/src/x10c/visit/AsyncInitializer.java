package x10c.visit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import polyglot.ast.ArrayAccess_c;
import polyglot.ast.Assign;
import polyglot.ast.Block;
import polyglot.ast.Block_c;
import polyglot.ast.Eval;
import polyglot.ast.Eval_c;
import polyglot.ast.Expr;
import polyglot.ast.Expr_c;
import polyglot.ast.Field;
import polyglot.ast.Id;
import polyglot.ast.IntLit;
import polyglot.ast.LocalAssign;
import polyglot.ast.IntLit.Kind;
import polyglot.ast.Local;
import polyglot.ast.LocalDecl;
import polyglot.ast.Local_c;
import polyglot.ast.Loop;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Receiver;
import polyglot.ast.Stmt;
import polyglot.ast.Stmt_c;
import polyglot.ast.Term;
import polyglot.ast.Try;
import polyglot.frontend.Job;
import polyglot.types.Flags;
import polyglot.types.LocalDef;
import polyglot.types.LocalDef_c;
import polyglot.types.LocalInstance;
import polyglot.types.Name;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.types.VarDef;
import polyglot.types.VarDef_c;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import x10.ast.Closure;
import x10.ast.Closure_c;
import x10.ast.X10Call;
import x10.ast.X10Call_c;
import x10.ast.X10CanonicalTypeNode;
import x10.ast.X10Formal_c;
import x10.ast.X10LocalAssign_c;
import x10.ast.X10LocalDecl_c;
import x10.ast.X10Local_c;
import x10.ast.X10NodeFactory;
import x10.ast.X10Special;
import x10.extension.X10Ext;
import x10.extension.X10Ext_c;
import x10.types.ParameterType;
import x10.types.X10ClassType;
import x10.types.X10Flags;
import x10.types.X10MethodInstance;
import x10.types.X10TypeMixin;
import x10.types.X10TypeSystem;
import x10c.ast.BackingArray;
import x10c.ast.BackingArrayAccess;
import x10c.ast.BackingArrayAccessAssign;
import x10c.ast.BackingArrayNewArray;
import x10c.ast.X10CBackingArrayAccess_c;
import x10c.ast.X10CNodeFactory_c;
import x10c.types.X10CTypeSystem_c;

public class AsyncInitializer extends ContextVisitor {

    private final X10CTypeSystem_c xts;
    private final X10CNodeFactory_c xnf;

    // mapping corresponding box var id for init val
    private Map<VarDef, Id> initValToId = new HashMap<VarDef, Id>();
    private int nestLevel = 0;
    private int pcnt;

    public AsyncInitializer(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
        xts = (X10CTypeSystem_c) ts;
        xnf = (X10CNodeFactory_c) nf;
    }

    private boolean isFinishBlock(Try n) {
        X10Ext_c ext = (X10Ext_c) n.ext();
        Set<VarDef> asyncInitVal = ext.asyncInitVal;
        return (asyncInitVal != null);
    }
    private boolean isAsyncBlock(X10Call n) {
        X10MethodInstance mi = (X10MethodInstance) n.methodInstance();
        if (mi.container().isClass() && ((X10ClassType) mi.container().toClass()).
                fullName().toString().equals("x10.lang.Runtime")) {
            if (mi.signature().startsWith("runAsync"))
                return true;
        }
        return false;
    }

    @Override
    protected NodeVisitor enterCall(Node parent, Node n) throws SemanticException {
        if (n instanceof X10Call) {
            X10Call call = (X10Call)n;
            X10MethodInstance mi = (X10MethodInstance) call.methodInstance();
            if (mi.container().isClass() && ((X10ClassType) mi.container().toClass()).
                    fullName().toString().equals("x10.lang.Runtime")) {
                // adjust nest level counter of finish block
                if (mi.signature().startsWith("startFinish"))
                    nestLevel++;
                else if (mi.signature().startsWith("stopFinish"))
                    nestLevel--;
            }
        }
        if (n instanceof Try) {
            X10Ext_c ext = (X10Ext_c) n.ext();
            Set<VarDef> asyncInitVal = ext.asyncInitVal;
            if (asyncInitVal != null) {
                if (nestLevel == 0) {
                    // outermost finish block -- initialize internal structure
                    initValToId.clear();
                    pcnt = 0;
                }

                // register into the map (curr nest level, box var)
                registerInternalMap(asyncInitVal);
            }
        }
        return super.enterCall(parent, n);
    }

    @Override
    protected Node leaveCall(Node parent, Node old, Node n, NodeVisitor v) throws SemanticException {
        if (!(n instanceof Try))
            return n;

        // collect local vars accessed within async
        Set<VarDef> asyncVar = collectLocalVarsToBox((Try)n);

        X10Ext_c ext = (X10Ext_c) n.ext();
        Set<VarDef> asyncInitVal = ext.asyncInitVal;
        if (asyncInitVal == null && asyncVar == null)
            return n;

        if (asyncVar != null) {
            // merge async init vals and async vars
            registerInternalMap(asyncVar);
            if (asyncInitVal == null) asyncInitVal = asyncVar;
            else asyncInitVal.addAll(asyncVar);
        }

        // box async init vals and async vars under the try-catch-finally block
        Try tcfBlock = replaceVariables((Try)n, asyncInitVal);
        if (nestLevel == 0)
            // after boxing all the nest levels
            tcfBlock = privatizeVariables(tcfBlock, asyncInitVal);

        List<Stmt> stmts = new ArrayList<Stmt>();
        // declare boxed vars
        for (VarDef var : asyncInitVal) {
            stmts.add(genBoxDeclaration(var, n));
        }
        stmts.add(tcfBlock);
        // store the boxed vars back into original final vars
        for (VarDef var : asyncInitVal) {
            stmts.add(genBackingStore(var, n));
        }

        Block bb = xnf.Block(n.position(), stmts);
        return bb;
    }

    private Set<VarDef> collectLocalVarsToBox(Try tcfBlock) {
        // one pass scan of async blocks within finish and collect accesses of vars declared outside
        final Set<VarDef> asyncVar = new HashSet<VarDef>();
        final List<LocalDef> localDeclList = new ArrayList<LocalDef>();
        tcfBlock.visit(new NodeVisitor() {
            @Override
            public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
                if (n instanceof X10Call_c) {
                    X10Call call = (X10Call)n;
                    if (isAsyncBlock(call)) {
                        localDeclList.clear();
                        scanAsyncBlock(call, asyncVar, localDeclList);
                    }
                }
                return n;
            };
        });
        return asyncVar.isEmpty() ? null : asyncVar;
    }

    private void scanAsyncBlock(final X10Call call, final Set<VarDef> asyncVar, final List<LocalDef> localDeclList) {
        call.visit(new NodeVisitor() {
            @Override
            public Node override(Node parent, Node n) {
                if (n instanceof X10Call) {
                    X10Call currCall = (X10Call)n;
                    if (isAsyncBlock(currCall) && !currCall.equals(call))
                        // should not visit subtree of inner async block further (already done)
                        return n;
                }
                if (n instanceof Try) {
                    if (isFinishBlock((Try)n))
                        // should not visit subtree of inner finish block further (already done)
                        return n;
                }
                return null;
            }
            @Override
            public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
                if (n instanceof X10LocalDecl_c) {
                    X10LocalDecl_c ld = (X10LocalDecl_c)n;
                    if (ld.localDef().flags() == null || !ld.localDef().flags().equals(Flags.FINAL))
                        localDeclList.add(ld.localDef());
                }
                if (n instanceof X10Formal_c) {
                    X10Formal_c ld = (X10Formal_c)n;
                    if (ld.localDef().flags() == null || !ld.localDef().flags().equals(Flags.FINAL))
                        localDeclList.add(ld.localDef());
                }
                if (n instanceof X10Local_c) {
                    X10Local_c l = (X10Local_c)n;
                    Flags flags = l.localInstance().flags();
                    if (flags == null || !flags.equals(Flags.FINAL)) {
                        // check if this is not locally declared var
                        for (LocalDef localDefVar : localDeclList) {
                            if (localDefVar.asInstance().equals(l.localInstance()))
                                return n;
                        }
                        VarDef var = checkIfIncluded(l, asyncVar);
                        if (var == null)
                            asyncVar.add(new LocalDef_c(ts, n.position(), flags,
                                     Types.ref(l.localInstance().type()), l.localInstance().name()));
                    }
                }
                return n;
            }
        });
    }

    private Try replaceVariables(Try tcfBlock, final Set<VarDef> asyncInitVal) {
        // box async init vals
        tcfBlock = (Try)tcfBlock.visit(new NodeVisitor() {
            @Override
            public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
                if (n instanceof X10LocalAssign_c) {
                    X10LocalAssign_c la = (X10LocalAssign_c) n;
                    Type type = X10TypeMixin.baseType(la.type());
                    Expr left = la.left();

                    if (!(left instanceof Local)) {
                        return n;
                    }

                    VarDef initVal = checkIfIncluded((Local)left, asyncInitVal);
                    if (initVal == null)
                        return n;

                    // initialization to boxed variable
                    Id id = getBoxId(initVal);
                    LocalDef ldef = xts.localDef(n.position(), xts.Final(), Types.ref(type), id.id());
                    IntLit idx0 = xnf.IntLit(n.position(), IntLit.INT, 0);
                    return xnf.BackingArrayAccessAssign(n.position(), xnf.Local(n.position(), id).localInstance(ldef.asInstance()), 
                                 idx0, la.operator(), la.right()).type(type);
                }
                if (n instanceof X10Local_c) {
                    if ((parent instanceof X10LocalAssign_c) && n.equals(((X10LocalAssign_c)parent).left()))
                        // should be processed in the above
                        return n;

                    VarDef initVal = checkIfIncluded((Local)n, asyncInitVal);
                    if (initVal == null)
                        return n;

                    Type type = X10TypeMixin.baseType(((X10Local_c) n).type());

                    Id id = getBoxId(initVal);
                    LocalDef ldef = xts.localDef(n.position(), xts.Final(), Types.ref(type), id.id());
                    IntLit idx0 = xnf.IntLit(n.position(), IntLit.INT, 0);
                    return xnf.BackingArrayAccess(n.position(), xnf.Local(n.position(), id).localInstance(ldef.asInstance()), idx0, type);
                }
                return n;
            };
        });
        // tcfBlock.dump(System.err);
        return tcfBlock;
    }

    private Try privatizeVariables(Try tcfBlock, final Set<VarDef> asyncInitVal) {
        // setup internal structures
        final List<PVarInfo> privatizedVarList = new ArrayList<PVarInfo>();
        pcnt = 0;

        // tcfBlock.dump(System.err);
        Block newTryBlock = (Block)tcfBlock.tryBlock().visit(new NodeVisitor() {
            @Override
            public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
                if (n instanceof X10Call_c) {
                    X10Call call = (X10Call)n;
                    if (isAsyncBlock(call)) {
                        privatizedVarList.clear();
                        pcnt++;
                        collectInfo(call, privatizedVarList);
                        return doPrivatization(call, privatizedVarList);
                    }
                }
                return n;
            };
        });
        // newTryBlock.dump(System.err);
        tcfBlock = xnf.Try(tcfBlock.position(), newTryBlock, tcfBlock.catchBlocks(), tcfBlock.finallyBlock());
        return tcfBlock;
    }

    private void collectInfo(final X10Call call, final List<PVarInfo> privatizedVarList) {
        call.visit(new NodeVisitor() {
            @Override
            public Node override(Node parent, Node n) {
                if (n instanceof X10Call) {
                    X10Call currCall = (X10Call)n;
                    if (isAsyncBlock(currCall) && !currCall.equals(call))
                        // should not visit subtree of inner async block further (already done)
                        return n;
                }
                if (n instanceof Try) {
                    if (isFinishBlock((Try)n))
                        // should not visit subtree of inner finish block further (already done)
                        return n;
                }
                return null;
            }
            @Override
            public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
                if (n instanceof BackingArrayAccessAssign) {
                    BackingArrayAccessAssign ba = (BackingArrayAccessAssign)n;
                    VarDef initVal = checkInitValList((Local)((ArrayAccess_c)ba.left()).array());
                    if (initVal != null) {
                        registerPrivatizedList(privatizedVarList, initVal, true);
                    }
                }
                if (n instanceof BackingArrayAccess) {
                    BackingArrayAccess ba = (BackingArrayAccess)n;
                    VarDef initVal = checkInitValList((Local)ba.array());
                    if (initVal != null) {
                        // check left hand side
                        if (parent instanceof X10LocalAssign_c) {
                            Expr left = ((X10LocalAssign_c)parent).left();
                            if (left instanceof Local && initVal.name().equals(((Local)left).name().id())) {
                                // this is a backing store just generated in the above
                                return n;
                            }
                        }
                        registerPrivatizedList(privatizedVarList, initVal, false);
                    }
                }
                return n;
            };
        });
        // dumpPrivatizedList(privatizedVarList);
    }

    private void registerPrivatizedList(List<PVarInfo> privatizedVarList, VarDef initVal, boolean isAssign) {
        for (PVarInfo valInfo : privatizedVarList) {
            if (valInfo.initVal.equals(initVal)) {
                // found the match
                if (!isAssign) valInfo.refCount++;
                else valInfo.isAssign = true;
                return;
            }
        }
        privatizedVarList.add(new PVarInfo(initVal, isAssign));
    }

    private boolean checkPrivatizeCriteria(PVarInfo pvarInfo) {
        if (pvarInfo.initVal.flags().equals(Flags.FINAL))
            // local val
            return pvarInfo.refCount > 0;

        // local var: reference only case 
        // (currently cannot find location to write back updated private var)
        return !pvarInfo.isAssign && pvarInfo.refCount > 0;
    }

    private X10Call doPrivatization(final X10Call call, final List<PVarInfo> privatizedVarList) {
        if (privatizedVarList.isEmpty())
            // nothing to do for this runAsync
            return call;

        X10Call newCall = (X10Call)call.visit(new NodeVisitor() {
            @Override
            public Node override(Node parent, Node n) {
                if (n instanceof X10Call) {
                    X10Call currCall = (X10Call)n;
                    if (isAsyncBlock(currCall) && !currCall.equals(call))
                        // should not visit subtree of inner async block further (already done)
                        return n;
                }
                if (n instanceof Try) {
                    if (isFinishBlock((Try)n))
                        // should not visit subtree of inner finish block further (already done)
                        return n;
                }
                return null;
            }
            @Override
            public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
                if (n instanceof Closure_c) {
                    // private var declaration at each closure entry
                    List<Stmt> stmts = new ArrayList<Stmt>();
                    // for (VarDef initVal : asyncInitVal) {
                    for (PVarInfo pvarInfo : privatizedVarList) {
                        if (checkPrivatizeCriteria(pvarInfo))
                            // privatize only if reference exists
                            stmts.add(genPrivateVarDeclaration(pvarInfo, n));
                    }
                    if (stmts.isEmpty())
                        return n;

                    Closure_c c = (Closure_c) n;
                    stmts.add(c.body());
                    Block newBody = xnf.Block(n.position(), stmts);
                    return (Closure_c)c.body(newBody);
                }
                if (n instanceof Eval) {
                    Eval e = (Eval) n;
                    if (e.expr() instanceof BackingArrayAccessAssign) {
                        BackingArrayAccessAssign ba = (BackingArrayAccessAssign)(e.expr());

                        VarDef initVal = checkInitValList((Local)((ArrayAccess_c)ba.left()).array());
                        PVarInfo pvarInfo = checkPrivatizeVarList(initVal, privatizedVarList);
                        if (pvarInfo == null)
                            return n;

                        if (pvarInfo.initVal.flags().equals(Flags.FINAL)) {
                            // local val case: add assignment to private var
                            List<Stmt> stmts = new ArrayList<Stmt>();
                            stmts.add(e);
                            stmts.add(genPrivateVarAssign(pvarInfo, n));
                            return xnf.StmtSeq(n.position(), stmts);
                        } else {
                            // local var case: replace with private var
                            Id id = getPrivateId(pvarInfo, n);
                            Type type = ts.createLocalInstance(n.position(), Types.ref((LocalDef_c)initVal)).type();

                            // left-hand side (private var)
                            LocalDef ldef = xts.localDef(n.position(), xts.NoFlags(), Types.ref(type), id.id());
                            Local left = (Local) xnf.Local(n.position(), id).localInstance(ldef.asInstance()).type(type);

                            // creation of assignment node
                            Expr expr = xnf.LocalAssign(n.position(), left, Assign.ASSIGN, ba.right()).type(type);
                            return xnf.Eval(n.position(), expr);
                        }
                    }
                }
                if (n instanceof BackingArrayAccess) {
                    BackingArrayAccess ba = (BackingArrayAccess)n;
                    VarDef initVal = checkInitValList((Local)ba.array());
                    PVarInfo pvarInfo = checkPrivatizeVarList(initVal, privatizedVarList);
                    if (pvarInfo == null)
                        return n;

                    // replace with private var
                    Id id = getPrivateId(pvarInfo, n);
                    Type type = ts.createLocalInstance(n.position(), Types.ref((LocalDef_c)initVal)).type();
                    LocalDef ldef = xts.localDef(n.position(), xts.Final(), Types.ref(type), id.id());
                    return xnf.Local(n.position(), xnf.Id(n.position(), id.id())).localInstance(ldef.asInstance()).type(type);
                }
                return n;
            };
        });
        // newCall.dump(System.err);
        return newCall;
    }

    private void registerInternalMap(Set<VarDef> asyncInitVal) {
        List<VarDef> removeList = new ArrayList<VarDef>();
        for (VarDef initVal : asyncInitVal) {
            Id id = initValToId.get(initVal);
            if (id == null) {
                // register
                id = xnf.Id(Position.COMPILER_GENERATED, Name.makeFresh("$"+initVal.name()));
                initValToId.put(initVal, id);
            } else {
                // already registered in the outer finish
                removeList.add(initVal);
            }
        }
        for (VarDef initVal : removeList) {
            asyncInitVal.remove(initVal);
        }
    }

    private Id getBoxId(VarDef initVal) {
        Id id = initValToId.get(initVal);
        assert(id != null);
        return id;
    }

    private Id getPrivateId(PVarInfo pvarInfo, Node n) {
        // create an unique new name (not shadowed)
        if (pvarInfo.pvarId == null) {
            Id boxId = initValToId.get(pvarInfo.initVal);
            pvarInfo.pvarId = xnf.Id(n.position(), Name.make("p"+pcnt+boxId.toString()));
        }
        return pvarInfo.pvarId;
    }

    private PVarInfo checkPrivatizeVarList(VarDef initVal, List<PVarInfo> list) {
        for (PVarInfo pvarInfo : list) {
            if (initVal.equals(pvarInfo.initVal) && checkPrivatizeCriteria(pvarInfo))
                return pvarInfo;
        }
        return null;
    }

    private VarDef checkInitValList(Local lv) {
        String name = lv.toString();
        for (Map.Entry<VarDef,Id> entry : initValToId.entrySet()) {
            Id id = entry.getValue();
            if (id.toString().equals(name))
                return entry.getKey();
        }
        return null;
    }

    private VarDef checkIfIncluded(Local lv, Set<VarDef> asyncInitVal) {

        Name name1 = lv.name().id();
        Flags flags1 = lv.localInstance().flags();
        Type t1 = lv.localInstance().type();

        for (VarDef var : asyncInitVal) {
            Name name2 = ((LocalDef_c)var).name();
            Flags flags2 = ((LocalDef_c)var).flags();
            Type t2 = ts.createLocalInstance(lv.position(), Types.ref((LocalDef_c)var)).type();
            if (name1.toString().equals(name2.toString()) && flags1.equals(flags2) && t1.typeEquals(t2, context))
                // found the match
                return var;
        }
        return null;
    }

    private Type createArrayType(Type t) {
        return xts.createBackingArray(t.position(), Types.ref(t));
    }

    private Stmt genBoxDeclaration(VarDef initVal, Node n) {
        Type type = ts.createLocalInstance(n.position(), Types.ref((LocalDef_c)initVal)).type();
        Type arrayType = createArrayType(type);
        X10CanonicalTypeNode tnArray = xnf.X10CanonicalTypeNode(n.position(), arrayType);

        // right-hand side (array creation)
        List<Expr> dims = new ArrayList<Expr>();
        dims.add(xnf.IntLit(n.position(), IntLit.INT, 1));
        BackingArrayNewArray ba = xnf.BackingArrayNewArray(n.position(), tnArray, dims, 0, arrayType);

        // creation of declaration node
        Id id = getBoxId(initVal);
        LocalDef ldef = xts.localDef(n.position(), xts.Final(), Types.ref(arrayType), id.id());
        LocalDecl ld = xnf.LocalDecl(n.position(), xnf.FlagsNode(n.position(), Flags.FINAL), tnArray, id, ba)
                .localDef(ldef);

        if (initVal.flags().equals(Flags.FINAL))
            // local val case
            return ld;

        // local var case: set value from original var to boxed var
        ldef = xts.localDef(n.position(), xts.NoFlags(), Types.ref(type), id.id());
        Name name = ((LocalDef_c)initVal).name();
        Local right = (Local) xnf.Local(n.position(), xnf.Id(n.position(), name)).localInstance(ldef.asInstance()).type(type);

        IntLit idx0 = xnf.IntLit(n.position(), IntLit.INT, 0);
        Expr baa = xnf.BackingArrayAccessAssign(n.position(), xnf.Local(n.position(), id).localInstance(ldef.asInstance()), 
                                                idx0, Assign.ASSIGN, right).type(type);
        // returning a pair of statements
        List<Stmt> stmts = new ArrayList<Stmt>();
        stmts.add(ld);
        stmts.add(xnf.Eval(n.position(), baa));
        return xnf.StmtSeq(n.position(), stmts);
    }

    private Stmt genBackingStore(VarDef initVal, Node n) {
        Name name = ((LocalDef_c)initVal).name();
        Type type = ts.createLocalInstance(n.position(), Types.ref((LocalDef_c)initVal)).type();

        // right-hand side (boxed var)
        Id id = getBoxId(initVal);
        LocalDef rdef = xts.localDef(n.position(), xts.Final(), Types.ref(type), id.id());
        IntLit idx0 = xnf.IntLit(n.position(), IntLit.INT, 0);
        BackingArrayAccess right = xnf.BackingArrayAccess(n.position(), xnf.Local(n.position(), id).localInstance(rdef.asInstance()), 
                                                          idx0, type);
        // left-hand side (original final var)
        LocalDef ldef = xts.localDef(n.position(), xts.Final(), Types.ref(type), name);
        Local left = (Local) xnf.Local(n.position(), xnf.Id(n.position(), name)).localInstance(ldef.asInstance()).type(type);

        // creation of assignment node
        Expr expr = xnf.LocalAssign(n.position(), left, Assign.ASSIGN, right).type(type);
        Stmt stmt = xnf.Eval(n.position(), expr);

        // stmt.dump(System.err);
        return stmt;
    }

    private Stmt genPrivateVarDeclaration(PVarInfo pvarInfo, Node n) {
        // Name name = ((LocalDef_c)initVal).name();
        VarDef initVal = pvarInfo.initVal;
        boolean isAssign = pvarInfo.isAssign;
        Type type = ts.createLocalInstance(n.position(), Types.ref((LocalDef_c)initVal)).type();

        // creation of declaration node (private final var)
        Id id = getPrivateId(pvarInfo, n);
        LocalDef ldef = xts.localDef(n.position(), xts.Final(), Types.ref(type), id.id());
        LocalDecl ld;
        if (isAssign && initVal.flags().equals(Flags.FINAL)) {
            // no initial value
            ld = xnf.LocalDecl(n.position(), xnf.FlagsNode(n.position(), Flags.FINAL), 
                               xnf.X10CanonicalTypeNode(n.position(), type), xnf.Id(n.position(), id.id()))
                               .localDef(ldef);
        } else {
            // local var or reference only val: set initial value from boxed var
            BackingArrayAccess right = getBoxReference(initVal, type, n);
            // privatized var for local var (not val) should be non-final
            ld = xnf.LocalDecl(n.position(), xnf.FlagsNode(n.position(), initVal.flags()), 
                               xnf.X10CanonicalTypeNode(n.position(), type), xnf.Id(n.position(), id.id()), right)
                               .localDef(ldef);
        }
        // ld.dump(System.err);
        return ld;
    }

    private Stmt genPrivateVarAssign(PVarInfo pvarInfo, Node n) {
        VarDef initVal = pvarInfo.initVal;
        Type type = ts.createLocalInstance(n.position(), Types.ref((LocalDef_c)initVal)).type();

        // get right-hand side (boxed var)
        BackingArrayAccess right = getBoxReference(initVal, type, n);

        // left-hand side (private var)
        Id id = getPrivateId(pvarInfo, n);
        LocalDef ldef = xts.localDef(n.position(), xts.NoFlags(), Types.ref(type), id.id());
        Local left = (Local) xnf.Local(n.position(), xnf.Id(n.position(), id.id())).localInstance(ldef.asInstance()).type(type);

        // creation of assignment node
        Expr expr = xnf.LocalAssign(n.position(), left, Assign.ASSIGN, right).type(type);
        Stmt stmt = xnf.Eval(n.position(), expr);

        // stmt.dump(System.err);
        return stmt;
    }

    private BackingArrayAccess getBoxReference(VarDef initVal, Type type, Node n) {
        Id id = getBoxId(initVal);
        LocalDef rdef = xts.localDef(n.position(), xts.NoFlags(), Types.ref(type), id.id());
        IntLit idx0 = xnf.IntLit(n.position(), IntLit.INT, 0);
        BackingArrayAccess baa = xnf.BackingArrayAccess(n.position(), xnf.Local(n.position(), id).localInstance(rdef.asInstance()), 
                                                        idx0, type);
        return baa;
    }

    private void dumpPrivatizedList(List<PVarInfo> privatizedVarList) {
        System.out.println("-------------------");
        for (PVarInfo elem : privatizedVarList) {
            System.out.println(elem.toString());
        }
    }

    static class PVarInfo {
        VarDef initVal;     // corresponding asyncInitVal or asyncVar
        Id pvarId;          // unique id for each runAsync
        int refCount;
        boolean isAssign;

        PVarInfo(VarDef initVal, boolean isAssign) {
            this.initVal = initVal;
            this.isAssign = isAssign;
            this.refCount = isAssign ? 0 : 1;
        }

        public String toString() {
            return initVal.toString()+" refCount:"+refCount+" isAssign:"+isAssign;
        }
    }
}
