package x10.lang;

import x10.base.TypeArgument;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Implementation of X10 Clocks.  There are some differences between
 * this implementation and what the spec says.  Most notably, clocks
 * are not a final class but an interface.  This is to allow the compiler
 * to generate subclasses that use the delegate pattern in order to
 * implement clocked final variables.  In essence, the compiler creates
 * a subclass of Clock.Delegate that contains the clocked final variables,
 * accessor methods and an advance method.  Then, instead of the original
 * clock the subclass of Clock.Delegate can be passed around to give
 * access to the clocked final variables.  Note that the compiler needs
 * to mangle a bit with the field accesses to ensure that reads read from
 * one set of variables (current) whereas writes go to another set (next).
 * The Clock.Delegate.advance() implementations merely copy the values from
 * the next set to the current set.
 * 
 * In the Clock interface methods that map to reserved words in the 
 * X10 source language are prefixed with a 'do' since some of them 
 * conflict with reserved words in Java.
 * 
 * @author Christian Grothoff, Christoph von Praun
 */
public abstract class Clock implements TypeArgument {

    private static int nextId_ = 0;
    private int id_;
    
    protected Clock() {
        synchronized (getClass()) {
            id_ = nextId_++;
        }
    }
    	
    /**
     * Register the current activity with this clock.  It is an error
     * to register an activity with a clock that is already registered.
     * Note that the activity that created the clock is automatically
     * registered!
     */
    public abstract void register();

    /**
     * Register another activity with this clock.  It is an error
     * to register an activity with a clock that is already registered.
     */
    public abstract void register(Activity a);
    
    /**
     * Execute the given activity.  The clock will not advance
     * into the next phase until the given activity and all
     * activities transitively started by a have completed.
     * 
     * @param a an activity to run; the activity is started here
     *  (Runtime.here()!) and the clock will not advance until
     *  the activity completes.
     */
    public abstract void doNow(Activity a);
    
    /**
     * Notify the clock that this activity has completed its
     * clocked activities in this cycle, that is the current
     * activity will not issue any further calls to 'now' until
     * it calls 'next'.
     *
     */
    public abstract void doContinue();
    
    /**
     * @return true if the activity has already dropped this
     *   clock (or if it never was registered).
     */
    public abstract boolean dropped();
    
    /**
     * Drop this activity from the clock.  Afterwards the
     * activity may no longer use continue or now on this clock.
     * Other activities will no longer be blocked waiting for 
     * the current activity to complete the phase.  
     * 
     * @return true if the activity has already dropped this
     *   clock (or if it never was registered).
     */
    public abstract boolean drop();
    
    /**
     * Drop the given activity from the clock.  Afterwards the
     * activity may no longer use continue or now on this clock.
     * Other activities will no longer be blocked waiting for 
     * the current activity to complete the phase.  
     * 
     * @return true if the activity has already dropped this
     *   clock (or if it never was registered).
     */
    public abstract boolean drop(Activity a);
    
    /**
     * Block until all clocks that this activity is registered with
     * have called continue (or next since next implies continue on
     * all registered clocks). 
     * 
     * Note that the semantics of this implementation are different
     * than what is stated in the X10 Report in that the programmer
     * does not specify the clocks but next applies to all clocks
     * that the activity is registered with.
     */
    public abstract void doNext();

    public void doNext(Clock[] clocks) {
        assert clocks != null;
        if (clocks.length > 1) {
            Arrays.sort(clocks, new Comparator() {
                public int compare(java.lang.Object o1, java.lang.Object o2) {
                    int ret = 0;
                    assert (o1 instanceof Clock);
                    assert (o2 instanceof Clock);
                    Clock c1 = (Clock) o1;
                    Clock c2 = (Clock) o2;
                    if (c1.id_ < c2.id_)
                        ret = -1;
                    else if (c1.id_ > c2.id_)
                        ret = 1;
                    return ret;
                }
                public boolean equals(java.lang.Object o) {
                    return (o == Clock.this);
                }
            });
        }
        
        for (int i=0; i < clocks.length; ++ i) {
            clocks[i].doNext();
        }
    }
    
    /**
     * Register a callback that is to be called whenever the clock
     * advances into the next phase.
     * 
     * @param al the listener to notify
     */
    public abstract void registerAdvanceListener(AdvanceListener al);
    
    /**
     * Callback method used by the Clock to notify all listeners
     * that the Clock is advancing into the next phase.
     * 
     * @author Christian Grothoff
     */
    public interface AdvanceListener {
        public void notifyAdvance();
    }
    
} // end of Clock