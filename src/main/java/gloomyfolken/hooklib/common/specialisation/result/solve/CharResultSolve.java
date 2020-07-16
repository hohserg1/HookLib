package gloomyfolken.hooklib.common.specialisation.result.solve;

public class CharResultSolve {
    public final boolean needReturn;
    public final char value;

    public CharResultSolve(boolean needReturn, char value) {
        this.value = value;
        this.needReturn = needReturn;
    }
}