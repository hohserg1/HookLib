package gloomyfolken.hooklib.api;

public enum InjectionPoint {

    /**
     * Начало метода
     */
    HEAD(false),

    /**
     * Конец метода
     */
    RETURN(true),

    /**
     * Когда происходит вызов другого метода где-то в теле хукнутого
     */
    METHOD_CALL(false),

    /**
     * Когда происходит вычисление выражения
     */
    EXPRESSION(false);

    public final boolean isPriorityInverted;

    InjectionPoint(boolean isPriorityInverted) {
        this.isPriorityInverted = isPriorityInverted;
    }
}
