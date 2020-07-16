package gloomyfolken.hooklib.common.specialisation.result.solve;

public class FloatResultSolve {
    public final boolean needReturn;
    public final float value;

    public FloatResultSolve(boolean needReturn, float value) {
        this.value = value;
        this.needReturn = needReturn;
    }
}
