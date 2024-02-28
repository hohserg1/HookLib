package gloomyfolken.hooklib.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Allow to capture return value.
 * <p>
 * Use it with {@link OnReturn}.
 * <p>
 * Add additional argument with type of target method return type to hook-method and mark with this annotation.
 */
@Target(ElementType.PARAMETER)
public @interface ReturnValue {
}
