define(`Now',
`async(here) clocked$1 finish async(here) $2')
// Automatically generated by the command
// m4 ClockTest4.m4 > ClockTest4.x10
// Do not edit
import harness.x10Test;

/**
 * Clock test for barrier functions.
 * foreach loop body represented with a method.
 *
 * @author kemal 3/2005
 */
public class ClockTest4 extends x10Test {

	int val = 0;
	const int N = 32;

	public boolean run() {
		final clock c = clock.factory.clock();

		foreach (point [i]: [1:(N-1)]) clocked(c) {
			foreachBody(i,c);
		}
		foreachBody(0,c);
		int temp2;
		atomic { temp2 = val; }
		chk(temp2 == 0);
		return true;
	}

	void foreachBody(final int i, final clock c) {
		Now((c), { async(here) { atomic val += i; } })
		next;
		int temp;
		atomic { temp = val; }
		chk(temp == N*(N-1)/2);
		next;
		Now((c), { async(here) { atomic val -= i; } })
		next;
	}

	public static void main(String[] args) {
		new ClockTest4().executeAsync();
	}
}
