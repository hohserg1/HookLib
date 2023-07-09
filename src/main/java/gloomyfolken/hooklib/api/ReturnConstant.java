package gloomyfolken.hooklib.api;

public @interface ReturnConstant {

    boolean booleanValue() default false;

    byte byteValue() default 0;

    short shortValue() default 0;

    int intValue() default 0;

    long longValue() default 0L;

    float floatValue() default 0.0F;

    double doubleValue() default 0.0D;

    char charValue() default 0;

    String stringValue() default "";
}
