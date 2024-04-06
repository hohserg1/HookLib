package gloomyfolken.hooklib.asm;


public enum ReturnCondition {

    NEVER(false),

    ALWAYS(false),

    ON_SOLVE(true);

    public final boolean requiresCondition;

    ReturnCondition(boolean requiresCondition) {
        this.requiresCondition = requiresCondition;
    }

}
