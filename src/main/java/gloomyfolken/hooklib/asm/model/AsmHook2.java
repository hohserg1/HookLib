package gloomyfolken.hooklib.asm.model;

import gloomyfolken.hooklib.asm.HookPriority;
import gloomyfolken.hooklib.asm.ReturnCondition;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class AsmHook2 implements Comparable<AsmHook2> {
    TargetMethodSpec targetMethod;
    HookSpec hookMethod;

    boolean createMethod;
    boolean isMandatory;
    ReturnCondition returnCondition;
    HookPriority priority;

    Anchor anchor;

    @Override
    public int compareTo(AsmHook2 o) {
        if (anchor.getPoint().isPriorityInverted && o.anchor.getPoint().isPriorityInverted) {
            return priority.ordinal() > o.priority.ordinal() ? -1 : 1;
        } else if (!anchor.getPoint().isPriorityInverted && !o.anchor.getPoint().isPriorityInverted) {
            return priority.ordinal() > o.priority.ordinal() ? 1 : -1;
        } else {
            return anchor.getPoint().isPriorityInverted ? 1 : -1;
        }
    }
}

