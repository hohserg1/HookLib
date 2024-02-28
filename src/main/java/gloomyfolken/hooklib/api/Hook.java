package gloomyfolken.hooklib.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;


/**
 * Mark method with this annotation for make it hook-method.
 * <p>
 * Hook-methods calls will be inserted to target method, so you can handle some stuff in foreign code.
 * <p>
 * Also mark hook-method with one of annotations: @OnBegin, @OnExpression, @OnMethodCall or @OnReturn, it will determine injection point in target method code.
 * <p>
 * Target class will be determined by type of first argument. First argument will be `this` value of target method, or null if target method is static.
 * <p>
 * Name of target method will be determined by hook-method name or by `targetMethod` parameter of @Hook annotation if present.
 * <p>
 * Next arguments of hook-method should be same as target method.
 * <p>
 * Return type of hook-method can be ReturnSolve if it's need to override return logic sometimes at injection point.
 * <p>
 * Return type of hook-method can be void if it doesn't need to override return logic at injection point.
 * <p>
 * Return type of hook-method can be same with target method return type if it's need to always return at injection point. It doesn't work if target method return type is void.
 * <p>
 * For example, if target class looks like:
 * <blockquote><pre>{@code public class Bruh {
 *      public String kek(int arg) {
 *          //wanna inject hook here
 *          return "lol" + arg;
 *      }
 * }}
 * </pre></blockquote>
 * Then hook should be:
 * <blockquote><pre>{@code @HookContainer
 * public class MyHooks {
 *      @Hook
 *      @OnBegin
 *      public static kek(Bruh self, int arg) {
 *          System.out.println("here!");
 *      }
 * }}</pre></blockquote>
 * And result code of target class in runtime will be:
 * <blockquote><pre>{@code public class Bruh {
 *      public String kek(int arg) {
 *          MyHooks.kek(this, arg); //hook-method call inserted
 *          return "lol" + arg;
 *      }
 * }}
 * </pre></blockquote>
 */
@Target(ElementType.METHOD)
public @interface Hook {

    /**
     * Determines order of hook injection
     * <p>
     * Hooks with {@link HookPriority#HIGHEST} priority  will be called earlier
     * <p>
     * Hooks with {@link HookPriority#LOWEST} priority  will be called later
     */
    HookPriority priority() default HookPriority.NORMAL;

    /**
     * Determines target method name.
     * If empty string, hook-method name will be used.
     * <p>
     * It's useful if it's need to hook to constructor or static initializer.
     *
     * @see Constants#CONSTRUCTOR_NAME
     * @see Constants#STATIC_INITIALIZER_NAME
     */
    String targetMethod() default "";

    /**
     * Allow to create new method in target class.
     * <p>
     * Useful for override some method of super-class.
     * <p>
     * Super-method call be inserted to new method.
     */
    boolean createMethod() default false;


    /**
     * Game will crash if HookLib can't find target method or suitable injection point for mandatory hook.
     * <p>
     * Turn it to false if your hook-method is optional.
     * <p>
     * Optional hooks will be only warned if not possible to inject.
     */
    boolean isMandatory() default true;

}
