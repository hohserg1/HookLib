package gloomyfolken.hooklib.api;

public @interface FieldLens {
    String targetField() default "";

    boolean createField() default false;

    boolean isMandatory() default true;
}
