public class hook {
	public static abstract class Lens<A, B> {

		public abstract void set(A a, B b);

		public abstract B get(A a);

	}
	
    public static Lens<String, Integer> testField = null;
	
	public static void test(Lens<Integer, Integer> lol){}
}