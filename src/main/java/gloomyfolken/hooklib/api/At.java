package gloomyfolken.hooklib.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface At {
    /**
     * Тип точки инъекции
     */
    InjectionPoint point() default InjectionPoint.HEAD;

    /**
     * Сдвиг относительно точки инъекции
     */
    Shift shift() default Shift.AFTER;

    /**
     * Конкретизация, имя метода, например
     */
    String target() default "";

    /**
     * Какая по счету операция. -1, если все
     */
    int ordinal() default -1;
}
