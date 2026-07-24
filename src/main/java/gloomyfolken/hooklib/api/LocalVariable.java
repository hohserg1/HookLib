package gloomyfolken.hooklib.api;

import java.lang.annotation.*;

/**
 * Allow capturing local variables.
 * <p>
 * Add additional argument to hook-method and mark with this annotation.
 * <p>
 * For example, if target class looks like:
 * <pre>{@code public class Bruh {
 *      public String kek(int arg) {
 *          int someVar = arg + 10;
 *          //wanna inject hook here and capture `someVar`
 *          return "lol" + someVar;
 *      }
 * }}
 * </pre>
 * Then hook should be:
 * <pre>{@code @HookContainer
 * public class MyHooks {
 *      @Hook
 *      @OnBegin
 *      public static void kek(Bruh self, int arg, @LocalVariable(id=2) int someVar) {
 *          System.out.println("someVar=" + someVar);
 *      }
 * }}</pre></blockquote>
 * And result code of target class in runtime will be:
 * <pre>{@code public class Bruh {
 *      public String kek(int arg) {
 *          int someVar = arg + 10;
 *          MyHooks.kek(this, arg, someVar);
 *          return "lol" + someVar;
 *      }
 * }}
 * </pre>
 *
 * @see PrintLocalVariables
 */
@Target(ElementType.PARAMETER)
public @interface LocalVariable {
    /**
     * This is id of local variable.
     * <p>
     * In example it was 2 because of:
     * <p>
     * this - 0
     * <p>
     * arg - 1
     * <p>
     * someVar - 2
     * <p>
     * But its may be hard to count variable id's, so its recommended to use {@link PrintLocalVariables}
     */
    int id();
}
