
// Automatically generated by the command
// m4 ClockTest3.m4 > ClockTest3.x10
// Do not edit
/**
 * Clock test for  barrier functions
 * @author kemal 3/2005
 */
public class ClockTest3 {

	int val=0;
	static final int N=32;

	public boolean run() {
      		final clock c = clock.factory.clock();
		
		foreach (point [i]: 0:(N-1)) clocked(c) {
			async(here) clocked(c) finish async(here) {async(here) {atomic val++;}}
			next;
			int temp;
			atomic {temp=val;}
			if (temp != N) {
				throw new Error();
			}
			next;
			async(here) clocked(c) finish async(here) {async(here) {atomic val++;}}
			next;
		}
		next; next; next;
		int temp2;
		atomic {temp2=val;}
		if (temp2!=2*N) {
			throw new Error();
		}
		return true;
	}

	
    public static void main(String[] args) {
        final boxedBoolean b=new boxedBoolean();
        try {
                finish b.val=(new ClockTest3()).run();
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
