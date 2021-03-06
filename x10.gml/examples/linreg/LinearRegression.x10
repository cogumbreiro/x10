/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2011-2014.
 *  (C) Copyright Sara Salem Hamouda 2014.
 */
package linreg;

import x10.matrix.Vector;
import x10.matrix.ElemType;
import x10.util.Timer;
import x10.util.ArrayList;
import x10.matrix.distblock.DistBlockMatrix;
import x10.matrix.distblock.DupVector;
import x10.matrix.distblock.DistVector;

import x10.matrix.util.Debug;
import x10.util.resilient.iterative.PlaceGroupBuilder;

import x10.util.resilient.iterative.ResilientIterativeApp;
import x10.util.resilient.iterative.ResilientExecutor;
import x10.util.resilient.iterative.ApplicationSnapshotStore;
import x10.util.Team;

/**
 * Parallel linear regression based on GML distributed
 * dense/sparse matrix
 */
public class LinearRegression implements ResilientIterativeApp {
    public static val MAX_SPARSE_DENSITY = 0.1f;
    static val lambda = 1e-6 as Float; // regularization parameter
    
    /** Matrix of training examples */
    public val V:DistBlockMatrix;
    /** Vector of training regression targets */
    public val y:DistVector(V.M);
    /** Learned model weight vector, used for future predictions */
    public val w:Vector(V.N);
    
    public val maxIterations:Long;
    
    val d_p:DupVector(V.N);
    val Vp:DistVector(V.M);
    
    val r:Vector(V.N);
    val d_q:DupVector(V.N);
    
    private val checkpointFreq:Long;
    
    var norm_r2:ElemType;
    var lastCheckpointNorm:ElemType;
    var lastCheckpointR:Vector;
    var lastCheckpointW:Vector;    
    var iter:Long;
    
    //----Profiling-----
    public var parCompT:Long=0;
    public var seqCompT:Long=0;
    public var commT:Long;
    private val nzd:Float;
    private var places:PlaceGroup;
    private var team:Team;
        
    public def this(v:DistBlockMatrix, y:DistVector(v.M), it:Long, chkpntIter:Long, sparseDensity:Float, places:PlaceGroup, team:Team) {
        maxIterations = it;
        this.V = v;
        this.y = y;
        
        Vp = DistVector.make(V.M, V.getAggRowBs(), places, team);
        
        r  = Vector.make(V.N);
        d_p= DupVector.make(V.N, places, team);
        
        d_q= DupVector.make(V.N, places, team);
        
        w  = Vector.make(V.N);
        
        this.checkpointFreq = chkpntIter;
        
        nzd = sparseDensity;
        this.places = places;
        this.team = team;
    }
    
    public def isFinished() {
        return iter >= maxIterations;
    }
    
    public def run() {
        val dupR = DupVector.make(V.N, places, team);
        // 4: r=-(t(V) %*% y)
        dupR.mult(y, V);
        dupR.local().copyTo(r);
        // 5: p=-r
        r.copyTo(d_p.local());
        // 4: r=-(t(V) %*% y)
        r.scale(-1.0 as ElemType);
        // 6: norm_r2=sum(r*r)
        norm_r2 = r.dot(r);
        
        new ResilientExecutor(checkpointFreq, places).run(this);
        
        parCompT = dupR.getCalcTime() + d_q.getCalcTime() + Vp.getCalcTime();
        commT = dupR.getCommTime() + d_q.getCommTime() + d_p.getCommTime() + Vp.getCommTime();

        return w;
    }
    
    public def step() {
        d_p.sync();
        
        // Parallel computing
        
        // 10: q=((t(V) %*% (V %*% p)) )

        d_q.mult(Vp.mult(V, d_p), V);
        
        // Sequential computing
        
        var ct:Long = Timer.milliTime();
        //q = q + lambda*p
        val p = d_p.local();
        val q = d_q.local();
        q.scaleAdd(lambda, p);
        
        // 11: alpha= norm_r2/(t(p)%*%q);
        val alpha = norm_r2 / p.dotProd(q);
        
        // 12: w=w+alpha*p;
        w.scaleAdd(alpha, p);
        
        // 13: old norm r2=norm r2;
        val old_norm_r2 = norm_r2;
        
        // 14: r=r+alpha*q;
        r.scaleAdd(alpha, q);

        // 15: norm_r2=sum(r*r);
        norm_r2 = r.dot(r);

        // 16: beta=norm_r2/old_norm_r2;
        val beta = norm_r2/old_norm_r2;
        
        // 17: p=-r+beta*p;
        p.scale(beta).cellSub(r);
        
        seqCompT += Timer.milliTime() - ct;
        
        iter++;
    }
    
    public def checkpoint(resilientStore:ApplicationSnapshotStore) {
        resilientStore.startNewSnapshot();
        resilientStore.saveReadOnly(V);
        resilientStore.save(d_p);
        resilientStore.save(d_q);
        resilientStore.commit();
        lastCheckpointNorm = norm_r2;
        lastCheckpointR = r.clone();
        lastCheckpointW = w.clone();
    }
    
    /**
     * Restore from the snapshot with new PlaceGroup
     */
    public def restore(newPg:PlaceGroup, store:ApplicationSnapshotStore, lastCheckpointIter:Long, newAddedPlaces:ArrayList[Place]) {
        val newTeam = new Team(newPg);
        val newRowPs = newPg.size();
        val newColPs = 1;
        //remake all the distributed data structures
        if (nzd < MAX_SPARSE_DENSITY) {
            V.remakeSparse(newRowPs, newColPs, nzd, newPg, newAddedPlaces);
        } else {
            V.remakeDense(newRowPs, newColPs, newPg, newAddedPlaces);
        }
        d_p.remake(newPg, newTeam, newAddedPlaces);
        Vp.remake(V.getAggRowBs(), newPg, newTeam, newAddedPlaces);
        d_q.remake(newPg, newTeam, newAddedPlaces);
        store.restore();
        
        //adjust the iteration number, the norm value and vectors
        iter = lastCheckpointIter;
        norm_r2 = lastCheckpointNorm;
        r.copyTo(lastCheckpointR);
        w.copyTo(lastCheckpointW);
        places = newPg;
        Console.OUT.println("Restore succeeded. Restarting from iteration["+iter+"] norm["+norm_r2+"] ...");
    }
}
