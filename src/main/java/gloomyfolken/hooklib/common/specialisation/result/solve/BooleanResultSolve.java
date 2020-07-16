package gloomyfolken.hooklib.common.specialisation.result.solve;

public class BooleanResultSolve {
    public final boolean needReturn;
    public final boolean value;

    public BooleanResultSolve(boolean needReturn, boolean value) {
        this.value = value;
        this.needReturn = needReturn;
    }
}