/**
 * @author bdlucas
 */
public class PlaceCheckGenericClass {

    class C[S] {
        property p:int = 0;
        val x:S;
        var y:S;
        def foo() {}
        def foo(x:S) {}
        final def foo[T](x:T) {}
        def this(s:S) {
            x = s;
            y = s;
        }
    }

    val c = new C[String]("0");

    public def run01(): boolean = {
        val f = future (Place.places(1)) {
            try {
                val a = c.p;
            } catch (e:BadPlaceException) {
                return true;
            }
            x10.io.Console.OUT.println("01 fails");
            return false;
        };
        return f.force();
    }

    public def run02(): boolean = {
        val f = future (Place.places(1)) {
            try {
                val a = c.x;
            } catch (e:BadPlaceException) {
                return true;
            }
            x10.io.Console.OUT.println("02 fails");
            return false;
        };
        return f.force();
    }

    public def run03(): boolean = {
        val f = future (Place.places(1)) {
            try {
                val a = c.y;
            } catch (e:BadPlaceException) {
                return true;
            }
            x10.io.Console.OUT.println("03 fails");
            return false;
        };
        return f.force();
    }

    public def run04(): boolean = {
        val f = future (Place.places(1)) {
            try {
                c.foo();
            } catch (e:BadPlaceException) {
                return true;
            }
            x10.io.Console.OUT.println("04 fails");
            return false;
        };
        return f.force();
    }

    public def run05(): boolean = {
        val f = future (Place.places(1)) {
            try {
                c.foo("1");
            } catch (e:BadPlaceException) {
                return true;
            }
            x10.io.Console.OUT.println("05 fails");
            return false;
        };
        return f.force();

    }

    public def run06(): boolean = {
        val f = future (Place.places(1)) {
            try {
                c.foo(1.0);
            } catch (e:BadPlaceException) {
                return true;
            }
            x10.io.Console.OUT.println("06 fails");
            return false;
        };
        return f.force();
    }

    public def run(): boolean {
    	if (Place.MAX_PLACES == 1) {
    		x10.io.Console.OUT.println("not enough places to run this test");
    		return false;
    	}
    	return run01() && run02() && run03() && run04() && run05() && run06();
	}

    //
    //
    //

    public static def main(var args: Rail[String]): void = {
        new PlaceCheckGenericClass().run();
    }
}
