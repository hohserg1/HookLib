package gloomyfolken.hooklib.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
public @interface LocalVariable {
    int value() default onlyOneWithSameType;

    public static final int onlyOneWithSameType = -1;
    public static final int returnValue = -2;
}
