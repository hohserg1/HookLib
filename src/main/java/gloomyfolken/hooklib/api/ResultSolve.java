package gloomyfolken.hooklib.api;

public class ResultSolve<A> {
    public final boolean needReturn;
    public final A value;

    public ResultSolve(boolean needReturn, A value) {
        this.value = value;
        this.needReturn = needReturn;
    }
}
