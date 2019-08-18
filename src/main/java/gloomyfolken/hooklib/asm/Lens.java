package gloomyfolken.hooklib.asm;

public abstract class Lens<A, B> {

    public abstract void set(A a, B b);

    public abstract B get(A a);

}
