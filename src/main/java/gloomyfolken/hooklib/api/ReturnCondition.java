package gloomyfolken.hooklib.api;

/**
 * В зависимости от этого значения после вызова хук-метода может быть вызван return.
 */

public enum ReturnCondition {

    /**
     * return не вызывается никогда.
     */
    NEVER(false),

    /**
     * return вызывается всегда.
     */
    ALWAYS(false),

    /**
     * return вызывается, если хук-метод вернул new ResultSolve(true, <value>).
     */
    ON_SOLVE(true);

    public final boolean requiresCondition;

    ReturnCondition(boolean requiresCondition) {
        this.requiresCondition = requiresCondition;
    }

}
