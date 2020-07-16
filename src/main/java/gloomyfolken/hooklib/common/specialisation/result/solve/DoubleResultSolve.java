package gloomyfolken.hooklib.common.specialisation.result.solve;

public class DoubleResultSolve {
    public final boolean needReturn;
    public final double value;

    public DoubleResultSolve(boolean needReturn, double value) {
        this.value = value;
        this.needReturn = needReturn;
    }
}