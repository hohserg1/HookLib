package gloomyfolken.hooklib.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This is WIP feature!
 * <p>
 * Use it to call private method of some class
 * <p>
 * <p>
 * For example, if target class looks like:
 * <blockquote><pre>{@code public class Bruh {
 *      private String kek(int arg){
 *          ...
 *      }
 *      ...
 * }}
 * </pre></blockquote>
 * Then hook-lens for `kek` method should be:
 * <blockquote><pre>{@code @HookContainer
 * public class MyHooks {
 *      @MethodLens
 *      public static String kek(Bruh instance, int arg){
 *          //any code here, will be replaced by HookLib anyway
 *          throw new NotImplementedException();
 *      }
 * }}</pre></blockquote>
 * Then you can somewhere in your code:
 * <blockquote><pre>{@code Bruh bruh = ...;
 * System.out.println(MyHooks.kek(bruh, 10));}
 * </pre></blockquote>
 */
@Target(ElementType.METHOD)
public @interface MethodLens {
    /**
     * Determines target method name.
     * If empty string, hook-lens method name will be used.
     * <p>
     * It's useful if it's need to access to private constructor.
     *
     * @see Constants#CONSTRUCTOR_NAME
     */
    String targetMethod() default "";

    /**
     * Game will crash if HookLib can't find target method for hook-lens.
     * <p>
     * Turn it to false if your hook-lens is optional.
     * <p>
     * Optional hook-lenses will be only warned if not possible to inject.
     */
    boolean isMandatory() default true;
}
