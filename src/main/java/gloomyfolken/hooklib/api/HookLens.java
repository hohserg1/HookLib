package gloomyfolken.hooklib.api;

public @interface HookLens {
    public String name() default "";

    public boolean boxing() default false;

    public boolean createField() default false;
}