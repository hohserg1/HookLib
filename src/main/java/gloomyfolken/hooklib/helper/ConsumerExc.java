package gloomyfolken.hooklib.helper;

public interface ConsumerExc<A> {
    void accept(A v) throws Throwable;
}
