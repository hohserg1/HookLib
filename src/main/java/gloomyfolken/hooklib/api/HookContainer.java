package gloomyfolken.hooklib.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Mark class with this annotation for make HookLib can find hooks here
 *
 * @see Hook
 * @see FieldLens
 * @see MethodLens
 */
@Target(ElementType.TYPE)
public @interface HookContainer {
}
