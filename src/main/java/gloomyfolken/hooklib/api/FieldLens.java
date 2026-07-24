package gloomyfolken.hooklib.api;

import java.lang.annotation.*;

/**
 * Use it to access to private field of some class
 * <p>
 * For example, if target class looks like:
 * <pre>{@code public class Bruh {
 *      private String kek;
 *      ...
 * }}
 * </pre>
 * Then hook-lens for `kek` field should be:
 * <pre>{@code @HookContainer
 * public class MyHooks {
 *      @FieldLens
 *      public static FieldAccessor<Bruh, String> kek;
 * }}</pre></blockquote>
 * Then you can somewhere in your code:
 * <pre>{@code Bruh bruh = ...;
 * System.out.println(MyHooks.kek.get(bruh));
 * MyHooks.kek.set(bruh, "my kek");}
 * </pre>
 * <p>
 * note: if it is needing to access to field which added by mixin, set isMandatory=false, because mixin will apply hook before themself for test
 *
 * @see FieldAccessor
 */
@Target(ElementType.FIELD)
public @interface FieldLens {
    /**
     * Determines target field name.
     * <p>
     * If empty string, hook-lens field name will be used.
     */
    String targetField() default "";

    /**
     * It's possible to create new field in target class.
     * <p>
     * Useful for store some additional data in target class.
     */
    boolean createField() default false;

    /**
     * Game will crash if HookLib can't find target field
     * <p>
     * Turn it to false if your hook-lens is optional
     * <p>
     * Optional hook-lenses will be only warned if not possible to inject.
     */
    boolean isMandatory() default true;
}
