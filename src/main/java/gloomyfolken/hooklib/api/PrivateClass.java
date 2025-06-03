package gloomyfolken.hooklib.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Type parameter of {@link ReturnSolve}, of {@link FieldAccessor} and argument with type Object of hook-method can be marked by this annotation
 * to refer to some class which can be accessed as is.
 * <p>
 * For example, if target class looks like:
 * <blockquote><pre>{@code package a.b;
 * public class Bruh {
 *      private static class TargetClass {
 *          public String kek(int arg) {
 *              //wanna inject hook here
 *              return "lol" + arg;
 *          }
 *      }
 * }}
 * </pre></blockquote>
 * Then hook should be:
 * <blockquote><pre>{@code @HookContainer
 * public class MyHooks {
 *      @Hook
 *      @OnBegin
 *      public static void kek(@PrivateClass("a.b.Bruh$TargetClass") Object self, int arg) {
 *          System.out.println("hooking to private class!");
 *      }
 * }}</pre></blockquote>
 * <p>
 * Class can be marked by this annotation to reuse refer to some private class.
 * <blockquote><pre>{@code @PrivateClass("a.b.Bruh$TargetClass")
 * public class TargetClassImage{
 * }
 *
 * @HookContainer
 * public class MyHooks {
 *      @Hook
 *      @OnBegin
 *      public static void kek(TargetClassImage self, int arg) {
 *          System.out.println("hooking to private class!");
 *      }
 *
 *      @Hook(targetMethod = "kek")
 *      @OnReturn
 *      public static void kekReturn(TargetClassImage self, int arg) {
 *          System.out.println("hooking to private class 2!");
 *      }
 * }}</pre></blockquote>
 */
@Target({ElementType.PARAMETER, ElementType.TYPE_USE})
public @interface PrivateClass {
    /**
     * Canonical class name
     * <p>
     * Inner classes should be separated by "$" instead of dot "."
     */
    String value();
}
