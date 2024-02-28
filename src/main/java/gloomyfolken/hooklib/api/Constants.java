package gloomyfolken.hooklib.api;

public interface Constants {
    /**
     * It's name of constructor method. Use it with {@link Hook#targetMethod} and {@link MethodLens#targetMethod()}
     * <p>
     * For example, if target class looks like:
     * <blockquote><pre>{@code public class Bruh {
     *      public Bruh(int arg) {
     *          //wanna inject hook here
     *          ...
     *      }
     * }}
     * </pre></blockquote>
     * Then hook should be:
     * <blockquote><pre>{@code @Hook(targetMethod = CONSTRUCTOR_NAME)
     * @OnBegin
     * public static constructorHook(Bruh self, int arg) {
     *      System.out.println("here!");
     * }}</pre></blockquote>
     */
    String CONSTRUCTOR_NAME = "<init>";

    /**
     * It's name of constructor method. Use it with {@link Hook#targetMethod}
     * <p>
     * For example, if target class looks like:
     * <blockquote><pre>{@code public class Bruh {
     *      static {
     *          //wanna inject hook here
     *          ...
     *      }
     * }
     * }</pre></blockquote>
     * Then hook should be:
     * <blockquote><pre>{@code @Hook(targetMethod = STATIC_INITIALIZER_NAME)
     * @OnBegin
     * public static staticInitHook(Bruh self, int arg) {
     *      System.out.println("here!");
     * }}</pre></blockquote>
     */
    String STATIC_INITIALIZER_NAME = "<clinit>";
}
