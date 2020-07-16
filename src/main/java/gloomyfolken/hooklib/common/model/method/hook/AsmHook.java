package gloomyfolken.hooklib.common.model.method.hook;

import gloomyfolken.hooklib.api.HookPriority;
import gloomyfolken.hooklib.api.ReturnCondition;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.objectweb.asm.Type;

import java.util.List;

@ToString
@EqualsAndHashCode
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
public class AsmHook implements Comparable {

    String targetClassName;
    String targetMethodName;

    String hookClassName;
    String hookMethodName;
    List<HookParameter> hookMethodParameters;
    List<Type> targetMethodParameters;

    boolean createMethod;
    boolean isMandatory;
    ReturnCondition returnCondition;
    HookPriority priority;
    Anchor anchor;

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
