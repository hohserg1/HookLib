package gloomyfolken.hooklib.common.model.method.hook;

import gloomyfolken.hooklib.api.InjectionPoint;
import gloomyfolken.hooklib.api.Shift;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@ToString
@EqualsAndHashCode
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
public class Anchor {
    InjectionPoint point;

    Shift shift;

    String target;

    int ordinal;
}
