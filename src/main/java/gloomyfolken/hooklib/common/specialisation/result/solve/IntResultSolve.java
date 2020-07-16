package gloomyfolken.hooklib.common.specialisation.result.solve;

public class IntResultSolve {
    public final boolean needReturn;
    public final int value;

    public IntResultSolve(boolean needReturn, int value) {
        this.value = value;
        this.needReturn = needReturn;
    }
}
