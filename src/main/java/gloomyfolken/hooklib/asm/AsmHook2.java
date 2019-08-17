package gloomyfolken.hooklib.asm;

public class AsmHook2 implements Comparable<AsmHook> {
    public final TargetMethodSpec targetMethod;
    public final HookSpec hookMethod;

    public final boolean createMethod;
    public final boolean isMandatory;
    public final ReturnCondition returnCondition;
    public final HookPriority priority;

    public final Anchor anchor;

    @Override
    public int compareTo(AsmHook o) {
        if (anchor.point.isPriorityInverted && o.anchor.point.isPriorityInverted) {
            return priority.ordinal() > o.priority.ordinal() ? -1 : 1;
        } else if (!anchor.point.isPriorityInverted && !o.anchor.point.isPriorityInverted) {
            return priority.ordinal() > o.priority.ordinal() ? 1 : -1;
        } else {
            return anchor.point.isPriorityInverted ? 1 : -1;
        }
    }
}

