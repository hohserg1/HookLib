package gloomyfolken.hooklib.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Mark hook-method with this annotation to get possible additional arguments which marked with @LocalVariable with right id's
 *
 * @see LocalVariable
 */
@Target(ElementType.METHOD)
public @interface PrintLocalVariables {
}
