package gloomyfolken.hooklib.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
public @interface OnMethodCall {
    String value();

    String desc() default "";

    int ordinal() default -1;

    Shift shift() default Shift.AFTER;

}
