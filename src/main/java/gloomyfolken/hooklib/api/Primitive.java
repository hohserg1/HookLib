package gloomyfolken.hooklib.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Type parameter of {@link ReturnSolve} can be marked by this annotation
 * <p>
 * Have sense only with {@link Hook#createMethod}, otherwise HookLib will try to find target method with primitive return type anyway
 * <p>
 * Type parameter of {@link FieldAccessor} can be marked by this annotation
 * <p>
 * Hase sense only with {@link FieldLens#createField}, otherwise HookLib will try to find target field with primitive return type anyway
 */
@Target(ElementType.TYPE_USE)
public @interface Primitive {
}
