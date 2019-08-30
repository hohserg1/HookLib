package gloomyfolken.hooklib.asm;

public class ResultSolve<A> {
    final A value;
    final boolean needReturn;

    public ResultSolve(boolean needReturn, A value) {
        this.value = value;
        this.needReturn = needReturn;
    }
}
