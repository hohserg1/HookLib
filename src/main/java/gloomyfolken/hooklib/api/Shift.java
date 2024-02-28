package gloomyfolken.hooklib.api;

/**
 * Use it with {@link OnExpression#shift} and {@link OnMethodCall#shift} for shift injection point around interested code
 */
public enum Shift {
    BEFORE, AFTER, INSTEAD
}
