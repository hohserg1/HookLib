package gloomyfolken.hooklib.common.model.method.hook;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

public interface HookParameterUsage {
    HookParameterUsage Simple = new HookParameterUsage() {
    };
    HookParameterUsage ReturnValue = new HookParameterUsage() {
    };

    @ToString
    @EqualsAndHashCode
    @AllArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
    final class VariableCapture {
        int pos;
    }
}
