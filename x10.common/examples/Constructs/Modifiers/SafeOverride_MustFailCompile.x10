import harness.x10Test;

/**
 * Check that a safe method can be overridden only by a safe method.
 * @author vj  9/2006
 */
public class SafeOverride_MustFailCompile extends x10Test {

    class T1 {
      public safe void m() {
      }
    }
    class T2 extends T1 {
      public void m() { // should give a compile error.
      }
      }
   
	public boolean run() {
		
		return true;
	}

	public static void main(String[] args) {
		new SafeOverride_MustFailCompile().execute();
	}

	
}
