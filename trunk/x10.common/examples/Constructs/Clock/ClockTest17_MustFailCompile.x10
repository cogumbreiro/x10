/**
 * @author kemal 5/2005
 *
 *Clarification of language definition needed here:
 *Whether async clocked(c) S occurs inside a finish
 *body is hard to detect at compile time.
 *E.g. if the finish body contains an indirect function invocation
 *or a library routine.
 *How should this test behave?
 *
 *Currently this test causes a deadlock.
 *
 *Tentatively declaring that this is an error
 *that must be caught at compile time (may not be possible).
 *
 *
 */

/**
 * A class to invoke a 'function pointer' inside of finish
 */
class Y {
	static void test(foo f) {
		finish {
			f.apply(); // it is hard to determine f does an async clocked(c) S
		}
	}
}

public class ClockTest17_MustFailCompile {
	
	
	public boolean run() {
		/*A0*/
		final clock c0=clock.factory.clock();
		X x= new X();
		// f0 does not transmit clocks to subactivity
		foo f0= new foo() {
			public void apply() {
				async {
					System.out.println("hello from finish async S");
				}
			}    
		};
		// f1 transmits clocks to subactivity
		foo f1=new foo() {
			public void apply() { 
				/*Activity A1*/
				async clocked(c0){
					System.out.println("#1 before next");
					next;
					System.out.println("#1 after next");
				}
			}
		};
		
		foo[] fooArray=new foo[] {f0,f1};
		
		
		// This is invoking Y.test(f0) but not clear to a compiler
		Y.test(fooArray[x.zero()]);
		// Finish in Y.test completes and then the following executes.
		//No deadlock occurs here.
		System.out.println("#0a before next");
		next;
		System.out.println("#0a after next");
		
		// This is invoking Y.test(f1) but not clear to a compiler
		Y.test(fooArray[x.one()]);
		// Execution never reaches here (deadlock occurs) since:
		// A1 inside Y.test(f1) must first finish, but it
		// cannot since A0 has not executed next on clock c0 yet.
		System.out.println("#0b before next");
		next;
		System.out.println("#0b after next");
		
		return true;
	}
	
	public static void main(String[] args) {
		final boxedBoolean b=new boxedBoolean();
		try {
			finish b.val=(new ClockTest17_MustFailCompile()).run();
		} catch (Throwable e) {
			e.printStackTrace();
			b.val=false;
		}
		System.out.println("++++++ "+(b.val?"Test succeeded.":"Test failed."));
		x10.lang.Runtime.setExitCode(b.val?0:1);
	}
	static class boxedBoolean {
		boolean val=false;
	}
	
	
}

/**
 * An interface to use like a simple 'function pointer'
 *
 * foo f1=new foo(){public void apply() S1}; //assign body S1 to f1
 *
 * // values of free final variables of S1 are also captured in f1.
 *
 * f1.apply(); // invoke S1 indirectly using its captured
 *
 * // free variables 
 *
 */
interface foo {
	public void apply();
}

/** 
 * Dummy class to make static memory disambiguation difficult
 * for a typical compiler
 */
class X {
	public int[] z={1,0};
	int zero() { return z[z[z[1]]];} 
	int one() { return z[z[z[0]]];} 
	void modify() {z[0]+=1;}
}
