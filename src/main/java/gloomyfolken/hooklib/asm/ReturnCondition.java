package gloomyfolken.hooklib.asm;

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
     * return вызывается, если хук-метод вернул true.
     * Нельзя применить, если хук-метод не возвращает тип boolean.
     */
    ON_SOLVE(true);

    public final boolean requiresCondition;

    ReturnCondition(boolean requiresCondition) {
        this.requiresCondition = requiresCondition;
    }

}
