package gloomyfolken.hooklib.asm.model;

import gloomyfolken.hooklib.asm.*;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Builder
@Value
public class AsmHook implements Comparable<AsmHook> {

    String targetMethodName;
    String targetClassName;
    List<Type> targetMethodParameters;
    Type targetMethodReturnType;
    String targetMethodDescription;

    String hookMethodName;
    String hookClassName;
    List<Type> hookMethodParameters;
    List<Integer> hookMethodLocalCaptureIds;
    Type hookMethodReturnType;
    boolean hasReturnValueParameter;
    String hookMethodDescription;

    @Builder.Default
    boolean createMethod = false;
    @Builder.Default
    boolean isMandatory = false;
    @Builder.Default
    ReturnCondition returnCondition = ReturnCondition.NEVER;
    @Builder.Default
    HookPriority priority = HookPriority.NORMAL;

    @Builder.Default
    InjectionPoint point = InjectionPoint.HEAD;
    @Builder.Default
    Shift shift = Shift.AFTER;
    @Builder.Default
    String anchorTarget = "";
    @Builder.Default
    int ordinal = -1;

    public boolean isTargetMethod(String name, String desc) {
        return (targetMethodReturnType == null && desc.startsWith(targetMethodDescription) ||
                desc.equals(targetMethodDescription)) && name.equals(targetMethodName);
    }

    public String getPatchedMethodName() {
        return targetClassName + '#' + targetMethodName + targetMethodDescription;
    }

    public String getHookClassInternalName() {
        return hookClassName.replace('.', '/');
    }

    @Override
    public int compareTo(AsmHook o) {
        if (point.isPriorityInverted && o.point.isPriorityInverted) {
            return priority.ordinal() > o.priority.ordinal() ? -1 : 1;
        } else if (!point.isPriorityInverted && !o.point.isPriorityInverted) {
            return priority.ordinal() > o.priority.ordinal() ? 1 : -1;
        } else {
            return point.isPriorityInverted ? 1 : -1;
        }
    }

    public static class AsmHookBuilder {

        public AsmHookBuilder startArgumentsFill() {
            targetMethodParameters = new ArrayList<>();
            hookMethodParameters = new ArrayList<>();
            hookMethodLocalCaptureIds = new ArrayList<>();
            return this;
        }

        public AsmHookBuilder addThisToHookMethodParameters() {
            hookMethodParameters.add(TypeHelper.getType(targetClassName));
            hookMethodLocalCaptureIds.add(0);
            return this;
        }

        public AsmHookBuilder addReturnValueToHookMethodParameters() {
            if (targetMethodReturnType == Type.VOID_TYPE) {
                throw new IllegalStateException("Target method's return type is void, it does not make sense to " +
                        "transmit its return value to hook method.");
            }
            hookMethodParameters.add(targetMethodReturnType);
            hookMethodLocalCaptureIds.add(-1);
            hasReturnValueParameter = true;
            return this;
        }

        public AsmHookBuilder addHookMethodParameter(Type parameterType, int variableId) {
            hookMethodParameters.add(parameterType);
            hookMethodLocalCaptureIds.add(variableId);
            return this;
        }

        public AsmHookBuilder addTargetMethodParameter(Type parameterType) {
            targetMethodParameters.add(parameterType);

            return this;
        }

        public AsmHookBuilder finishArgumentsFill() {
            targetMethodDescription = getMethodDesc(targetMethodReturnType, targetMethodParameters);
            hookMethodDescription = Type.getMethodDescriptor(hookMethodReturnType, hookMethodParameters.toArray(new Type[0]));
            return this;
        }

        private String getMethodDesc(Type returnType, List<Type> paramTypes) {
            Type[] paramTypesArray = paramTypes.toArray(new Type[0]);
            if (returnType == null)
                return StringUtils.chop(Type.getMethodDescriptor(Type.VOID_TYPE, paramTypesArray));
            else
                return Type.getMethodDescriptor(returnType, paramTypesArray);
        }

        public AsmHookBuilder setAnchorForInject(HashMap<String, Object> anchor) {
            MapUtils.<Integer>maybeOfMapValue(anchor, "ordinal").ifPresent(this::ordinal);
            MapUtils.<String>maybeOfMapValue(anchor, "point").map(InjectionPoint::valueOf).ifPresent(this::point);
            MapUtils.<String>maybeOfMapValue(anchor, "shift").map(Shift::valueOf).ifPresent(this::shift);
            MapUtils.<String>maybeOfMapValue(anchor, "target").ifPresent(this::anchorTarget);

            return this;
        }

    }
}

