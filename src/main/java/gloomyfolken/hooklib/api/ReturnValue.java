package gloomyfolken.hooklib.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Перехватывает значение, которое изначально шло в return, и передает его хук-методу.
 * Говоря более формально, передает последнее значение в стаке.
 * Можно использовать только когда injectOnExit() == true и целевой метод возвращает не void.
 */
@Target(ElementType.PARAMETER)
public @interface ReturnValue {
}
