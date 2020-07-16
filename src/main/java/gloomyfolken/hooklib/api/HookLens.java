package gloomyfolken.hooklib.api;

public @interface HookLens {

    String name() default "";

    boolean boxing() default false;

    boolean createField() default false;
}
