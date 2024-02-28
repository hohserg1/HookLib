package gloomyfolken.hooklib.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Use it with {@link Hook} annotation for insert hook-method call at begin of target method
 */
@Target(ElementType.METHOD)
public @interface OnBegin {
}
