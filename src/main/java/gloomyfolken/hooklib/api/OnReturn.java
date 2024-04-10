package gloomyfolken.hooklib.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Use it with {@link Hook} annotation to insert hook-method call at return's of target method
 */
@Target(ElementType.METHOD)
public @interface OnReturn {
    /**
     * Determines which return point will be used in order.
     * <p>
     * 0 for first return.
     * <p>
     * 1 for secord return.
     * <p>
     * -1 for all return's.
     */
    int ordinal() default -1;
}
