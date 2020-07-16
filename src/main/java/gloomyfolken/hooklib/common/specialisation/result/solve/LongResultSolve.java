package gloomyfolken.hooklib.common.specialisation.result.solve;

public class LongResultSolve {
    public final boolean needReturn;
    public final long value;

    public LongResultSolve(boolean needReturn, long value) {
        this.value = value;
        this.needReturn = needReturn;
    }
}