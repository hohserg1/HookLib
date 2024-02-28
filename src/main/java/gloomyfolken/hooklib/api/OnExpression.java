package gloomyfolken.hooklib.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Use it with {@link Hook} annotation for insert hook-method call at specific point in code of target method.
 * <p>
 * Specific point determines by code pattern which need to write in separated method in same hook container and specify that method name by {@link #expressionPattern} parameter
 * <p>
 * For example, if target class looks like:
 * <blockquote><pre>{@code public class Bruh {
 *      public int kek(int arg) {
 *          ...
 *          String someVar = "lol" + arg; //wanna inject hook here
 *          ...
 *          return someVar.length();
 *      }
 * }}
 * </pre></blockquote>
 * Then hook should be:
 * <blockquote><pre>{@code @HookContainer
 * public class MyHooks {
 *      @Hook
 *      @OnExpression(expressionPattern = "someVarCalculation")
 *      public static kek(Bruh self, int arg) {
 *          System.out.println("here!");
 *      }
 *      public static String someVarCalculation(int arg){
 *          return "lol" + arg;
 *      }
 * }}</pre></blockquote>
 * And result code of target class in runtime will be:
 * <blockquote><pre>{@code public class Bruh {
 *      public int kek(int arg) {
 *          ...
 *          String someVar = "lol" + arg;
 *          MyHooks.kek(this, arg);
 *          ...
 *          return someVar.length();
 *      }
 * }}
 * </pre></blockquote>
 * <p>
 * HookLib will find code in target method which *similar* to code in {@link #expressionPattern}.
 * <p>
 * Similar meaning that local variables may have different id's in pattern, but same linkage.
 * <p>
 * For example, such pattern:
 * <blockquote><pre>{@code
 *  public static String somePattern(int a, int b){
 *      return a + b; //sum of two different variables
 *  }
 * }</pre></blockquote>
 * And such target method code:
 * <blockquote><pre>{@code
 *  public int kek(int arg) {
 *      ...
 *      int c = arg + arg;  //it will not match, sum two times same variable
 *      int d = arg + c;    //it will match, sum of two different variables
 *      ...
 *  }
 * }
 * </pre></blockquote>
 */
@Target(ElementType.METHOD)
public @interface OnExpression {
    /**
     * Name of method in same hook container which code will be used as pattern for finding similar code in target method.
     */
    String expressionPattern();

    /**
     * It's possible to insert hook-method call with different shift around found expression
     * <p>
     * {@link Shift#BEFORE} will insert before expression
     * <p>
     * {@link Shift#INSTEAD} will insert instead expression. If expression have non-void result type, hook-method should return same type  for replace value.
     * <p>
     * {@link Shift#AFTER} will insert after expression.
     */
    Shift shift() default Shift.AFTER;

    /**
     * Determines which suitable expression will be used as injection point in order.
     * <p>
     * 0 for first expression.
     * <p>
     * 1 for secord expression.
     * <p>
     * -1 for all expressions.
     */
    int ordinal() default -1;
}
