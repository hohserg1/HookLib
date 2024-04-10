package gloomyfolken.hooklib.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Use it with {@link Hook} annotation to insert hook-method call at other method call
 * <p>
 * For example, if target class looks like:
 * <blockquote><pre>{@code public class Bruh {
 *      public int kek(int arg) {
 *          ...
 *          someMethod(); //wanna inject hook here
 *          ...
 *      }
 * }}
 * </pre></blockquote>
 * Then hook should be:
 * <blockquote><pre>{@code @HookContainer
 * public class MyHooks {
 *      @Hook
 *      @OnMethodCall("someMethod")
 *      public static kek(Bruh self, int arg) {
 *          System.out.println("here!");
 *      }
 * }}</pre></blockquote>
 * And result code of target class in runtime will be:
 * <blockquote><pre>{@code public class Bruh {
 *      public int kek(int arg) {
 *          ...
 *          someMethod();
 *          MyHooks.kek(this, arg);
 *          ...
 *      }
 * }}
 * </pre></blockquote>
 */
@Target(ElementType.METHOD)
public @interface OnMethodCall {
    /**
     * Name of some method which call will be used as injection point
     */
    String value();

    /**
     * Determines what's arguments and return type of {@link #value} method
     * <p>
     * Useful if target method code have calls of few different methods with same name
     */
    String desc() default "";

    /**
     * It's possible to insert hook-method call with different shift around found calls of {@link #value} method
     * <p>
     * {@link Shift#BEFORE} will insert before call
     * <p>
     * {@link Shift#INSTEAD} will insert instead call. If {@link #value} method have non-void return type, hook-method should return same type for replace value.
     * <p>
     * {@link Shift#AFTER} will insert after call.
     */
    Shift shift() default Shift.AFTER;

    /**
     * Determines which call of some method will be used in order.
     * <p>
     * 0 for first call.
     * <p>
     * 1 for secord call.
     * <p>
     * -1 for all calls.
     */
    int ordinal() default -1;

}
