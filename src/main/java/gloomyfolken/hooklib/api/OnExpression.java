package gloomyfolken.hooklib.api;

public @interface OnExpression {
    String expressionPattern();

    Shift shift() default Shift.AFTER;

    int ordinal() default -1;
}
