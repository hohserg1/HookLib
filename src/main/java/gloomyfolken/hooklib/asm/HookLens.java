package gloomyfolken.hooklib.asm;

public @interface HookLens {
    public String name() default "";

    public boolean boxing() default false;

    public boolean createField() default false;
}
