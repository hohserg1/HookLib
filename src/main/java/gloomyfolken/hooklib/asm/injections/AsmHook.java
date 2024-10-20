package gloomyfolken.hooklib.asm.injections;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import gloomyfolken.hooklib.api.HookPriority;
import gloomyfolken.hooklib.api.ReturnSolve;
import gloomyfolken.hooklib.asm.*;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.DOUBLE;
import static org.objectweb.asm.Type.FLOAT;
import static org.objectweb.asm.Type.LONG;
import static org.objectweb.asm.Type.*;


public class AsmHook implements AsmMethodInjection, Cloneable {

    private String targetClassName; //dots, regular class name
    private String targetMethodName;
    private List<Type> targetMethodParameters = new ArrayList<Type>(2);
    @Nullable
    private Type targetMethodReturnType;

    private String hooksClassName; //dots, regular class name
    private String hookMethodName;
    // -1 is return value
    private List<Integer> transmittableVariableIds = new ArrayList<Integer>(2);
    private List<Type> hookMethodParameters = new ArrayList<Type>(2);
    private Type hookMethodReturnType = Type.VOID_TYPE;
    private boolean hasReturnValueParameter;

    private ReturnCondition returnCondition = ReturnCondition.NEVER;

    private HookInjectorFactory injectorFactory = HookInjectorFactory.BeginFactory.INSTANCE;
    private HookPriority priority = HookPriority.NORMAL;

    private String targetMethodDescription1;
    private String targetMethodDescription2;

    private String hookMethodDescription;

    private boolean createMethod;
    private boolean isMandatory = true;
    private boolean requiredPrintLocalVariables = false;

    public String getTargetClassName() {
        return targetClassName;
    }

    private String getTargetClassInternalName() {
        return targetClassName.replace('.', '/');
    }

    private String getHookClassInternalName() {
        return hooksClassName.replace('.', '/');
    }

    public boolean isTargetMethod(String name, String desc) {
        if (!name.equals(targetMethodName))
            return false;

        if (targetMethodReturnType == null)
            return desc.startsWith(targetMethodDescription1);

        return desc.equals(targetMethodDescription1) || desc.equals(targetMethodDescription2);
    }

    public boolean needToCreate() {
        return createMethod;
    }

    public boolean isMandatory() {
        return isMandatory;
    }

    public boolean isRequiredPrintLocalVariables() {
        return requiredPrintLocalVariables;
    }

    public HookInjectorFactory getInjectorFactory() {
        return injectorFactory;
    }

    private boolean hasHookMethod() {
        return true;
    }

    public void create(HookInjectorClassVisitor classVisitor) {
        ClassMetadataReader.MethodReference superMethod = classVisitor.transformer.classMetadataReader
                .findVirtualMethod(getTargetClassInternalName(), targetMethodName, targetMethodDescription1);
        //findVirtualMethod may return other name
        MethodVisitor mv = classVisitor.visitMethod(Opcodes.ACC_PUBLIC,
                superMethod == null ? targetMethodName : superMethod.name, targetMethodDescription1, null, null);
        if (mv instanceof HookInjectorMethodVisitor) {
            HookInjectorMethodVisitor inj = (HookInjectorMethodVisitor) mv;
            inj.visitCode();
            inj.visitLabel(new Label());
            if (superMethod == null) {
                injectDefaultValue(inj, targetMethodReturnType);
            } else {
                injectSuperCall(inj, superMethod);
            }
            injectReturn(inj, targetMethodReturnType);
            inj.visitLabel(new Label());
            inj.visitMaxs(0, 0);
            inj.visitEnd();
        } else {
            throw new IllegalArgumentException("Hook injector not created");
        }
    }

    public InsnList injectNode(MethodNode methodNode, HookInjectorClassVisitor cv) {
        InsnList r = new InsnList();
        Type targetMethodReturnType = Type.getReturnType(methodNode.desc);

        int returnLocalId = -1;
        if (hasReturnValueParameter) {
            returnLocalId = methodNode.maxLocals;
            methodNode.maxLocals++;
            r.add(new VarInsnNode(targetMethodReturnType.getOpcode(ISTORE), returnLocalId));
        }

        r.add(injectInvokeStaticNode(methodNode, returnLocalId, hookMethodName, hookMethodDescription));


        if (returnCondition == ReturnCondition.ALWAYS) {
            r.add(new InsnNode(targetMethodReturnType.getOpcode(IRETURN)));

        } else if (returnCondition == ReturnCondition.ON_SOLVE) {
            LabelNode doNotReturn = new LabelNode(new Label());

            int hookResultLocalId = methodNode.maxLocals;
            methodNode.maxLocals++;
            r.add(new VarInsnNode(hookMethodReturnType.getOpcode(ISTORE), hookResultLocalId));

            r.add(new VarInsnNode(hookMethodReturnType.getOpcode(ILOAD), hookResultLocalId));
            r.add(new TypeInsnNode(INSTANCEOF, Type.getDescriptor(ReturnSolve.Yes.class)));
            r.add(new JumpInsnNode(IFEQ, doNotReturn));

            if (targetMethodReturnType != VOID_TYPE) {
                r.add(new VarInsnNode(hookMethodReturnType.getOpcode(ILOAD), hookResultLocalId));
                r.add(new TypeInsnNode(CHECKCAST, Type.getInternalName(ReturnSolve.Yes.class)));
                Type boxed = objectToPrimitive.inverse().getOrDefault(targetMethodReturnType, targetMethodReturnType);
                r.add(new FieldInsnNode(GETFIELD, Type.getInternalName(ReturnSolve.Yes.class), "value", Type.getDescriptor(Object.class)));
                r.add(new TypeInsnNode(CHECKCAST, boxed.getInternalName()));
                if (boxed != targetMethodReturnType)
                    r.add(new MethodInsnNode(INVOKEVIRTUAL, boxed.getInternalName(), primitiveToUnboxingMethod.get(targetMethodReturnType), Type.getMethodDescriptor(targetMethodReturnType), false));
            }

            r.add(new InsnNode(targetMethodReturnType.getOpcode(IRETURN)));

            r.add(doNotReturn);
        }

        if (hasReturnValueParameter) {
            r.add(new VarInsnNode(targetMethodReturnType.getOpcode(ILOAD), returnLocalId));
        }

        return r;
    }

    public void inject(HookInjectorMethodVisitor inj) {
        Type targetMethodReturnType = inj.methodType.getReturnType();

        int returnLocalId = -1;
        if (hasReturnValueParameter) {
            returnLocalId = inj.newLocal(targetMethodReturnType);
            inj.visitVarInsn(targetMethodReturnType.getOpcode(ISTORE), returnLocalId);
        }

        injectInvokeStatic(inj, returnLocalId, hookMethodName, hookMethodDescription);

        if (returnCondition == ReturnCondition.ALWAYS) {
            injectReturn(inj, targetMethodReturnType);

        } else if (returnCondition == ReturnCondition.ON_SOLVE) {
            Label doNotReturn = inj.newLabel();

            int hookResultLocalId = inj.newLocal(hookMethodReturnType);
            inj.visitVarInsn(hookMethodReturnType.getOpcode(ISTORE), hookResultLocalId);

            inj.visitVarInsn(hookMethodReturnType.getOpcode(ILOAD), hookResultLocalId);
            inj.visitTypeInsn(INSTANCEOF, Type.getInternalName(ReturnSolve.Yes.class));
            inj.visitJumpInsn(IFEQ, doNotReturn);

            if (targetMethodReturnType != VOID_TYPE) {
                inj.visitVarInsn(hookMethodReturnType.getOpcode(ILOAD), hookResultLocalId);
                inj.visitTypeInsn(CHECKCAST, Type.getInternalName(ReturnSolve.Yes.class));
                Type boxed = objectToPrimitive.inverse().getOrDefault(targetMethodReturnType, targetMethodReturnType);
                inj.visitFieldInsn(GETFIELD, Type.getInternalName(ReturnSolve.Yes.class), "value", Type.getDescriptor(Object.class));
                inj.visitTypeInsn(CHECKCAST, boxed.getInternalName());
                if (boxed != targetMethodReturnType)
                    inj.visitMethodInsn(INVOKEVIRTUAL, boxed.getInternalName(), primitiveToUnboxingMethod.get(targetMethodReturnType), Type.getMethodDescriptor(targetMethodReturnType), false);
            }
            injectReturn(inj, targetMethodReturnType);

            inj.visitLabel(doNotReturn);
        }

        if (hasReturnValueParameter) {
            injectLoad(inj, targetMethodReturnType, returnLocalId);
        }
    }

    private void injectLoad(HookInjectorMethodVisitor inj, Type parameterType, int variableId) {
        int opcode;
        if (parameterType == INT_TYPE || parameterType == BYTE_TYPE || parameterType == CHAR_TYPE ||
                parameterType == BOOLEAN_TYPE || parameterType == SHORT_TYPE) {
            opcode = ILOAD;
        } else if (parameterType == LONG_TYPE) {
            opcode = LLOAD;
        } else if (parameterType == FLOAT_TYPE) {
            opcode = FLOAD;
        } else if (parameterType == DOUBLE_TYPE) {
            opcode = DLOAD;
        } else {
            opcode = ALOAD;
        }
        inj.visitVarInsn(opcode, variableId);
    }

    private void injectSuperCall(HookInjectorMethodVisitor inj, ClassMetadataReader.MethodReference method) {
        int variableId = 0;
        for (int i = 0; i <= targetMethodParameters.size(); i++) {
            Type parameterType = i == 0 ? TypeHelper.getType(targetClassName) : targetMethodParameters.get(i - 1);
            injectLoad(inj, parameterType, variableId);
            if (parameterType.getSort() == DOUBLE || parameterType.getSort() == LONG) {
                variableId += 2;
            } else {
                variableId++;
            }
        }
        inj.visitMethodInsn(INVOKESPECIAL, method.owner, method.name, method.desc, false);
    }

    private void injectDefaultValue(HookInjectorMethodVisitor inj, Type targetMethodReturnType) {
        switch (targetMethodReturnType.getSort()) {
            case Type.VOID:
                break;
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                inj.visitInsn(Opcodes.ICONST_0);
                break;
            case FLOAT:
                inj.visitInsn(Opcodes.FCONST_0);
                break;
            case LONG:
                inj.visitInsn(Opcodes.LCONST_0);
                break;
            case DOUBLE:
                inj.visitInsn(Opcodes.DCONST_0);
                break;
            default:
                inj.visitInsn(Opcodes.ACONST_NULL);
                break;
        }
    }

    private void injectReturn(HookInjectorMethodVisitor inj, Type targetMethodReturnType) {
        inj.visitInsn(targetMethodReturnType.getOpcode(IRETURN));
    }

    private InsnList injectInvokeStaticNode(MethodNode methodNode, int returnLocalId, String name, String desc) {
        InsnList r = new InsnList();
        for (int i = 0; i < hookMethodParameters.size(); i++) {
            Type parameterType = hookMethodParameters.get(i);
            int variableId = transmittableVariableIds.get(i);
            if (AsmUtils.isStatic(methodNode)) {
                if (variableId == 0) {
                    r.add(new InsnNode(Opcodes.ACONST_NULL));
                    continue;
                }
                //shift transmittable vars if static
                if (variableId > 0) variableId--;
            }
            if (variableId == -1) variableId = returnLocalId;
            r.add(new VarInsnNode(parameterType.getOpcode(ILOAD), variableId));
        }

        r.add(new MethodInsnNode(INVOKESTATIC, getHookClassInternalName(), name, desc, false));

        return r;
    }

    private void injectInvokeStatic(HookInjectorMethodVisitor inj, int returnLocalId, String name, String desc) {
        for (int i = 0; i < hookMethodParameters.size(); i++) {
            Type parameterType = hookMethodParameters.get(i);
            int variableId = transmittableVariableIds.get(i);
            if (inj.isStatic) {
                if (variableId == 0) {
                    inj.visitInsn(Opcodes.ACONST_NULL);
                    continue;
                }
                //shift transmittable vars if static
                if (variableId > 0) variableId--;
            }
            if (variableId == -1) variableId = returnLocalId;
            injectLoad(inj, parameterType, variableId);
        }

        inj.visitMethodInsn(INVOKESTATIC, getHookClassInternalName(), name, desc, false);
    }

    public String getPatchedMethodName(String actualName, String actualDescription) {
        return targetClassName + '#' + actualName + actualDescription;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AsmHook: ");

        sb.append(targetClassName).append('#').append(targetMethodName);
        sb.append(targetMethodDescription1);
        sb.append(" -> ");
        sb.append(hooksClassName).append('#').append(hookMethodName);
        sb.append(hookMethodDescription);

        sb.append(", ReturnCondition=" + returnCondition);
        sb.append(", InjectorFactory: " + injectorFactory.getClass().getName());
        sb.append(", CreateMethod = " + createMethod);

        return sb.toString();
    }

    @Override
    public int compareTo(AsmInjection o) {
        if (o instanceof AsmHook) {
            AsmHook otherHook = (AsmHook) o;
            if (injectorFactory.isPriorityInverted && otherHook.injectorFactory.isPriorityInverted) {
                return priority.ordinal() > otherHook.priority.ordinal() ? -1 : 1;
            } else if (!injectorFactory.isPriorityInverted && !otherHook.injectorFactory.isPriorityInverted) {
                return priority.ordinal() > otherHook.priority.ordinal() ? 1 : -1;
            } else {
                return injectorFactory.isPriorityInverted ? 1 : -1;
            }
        } else
            return 0;
    }

    public static Builder newBuilder() {
        return new AsmHook().new Builder();
    }

    public class Builder extends AsmHook {

        private Builder() {

        }

        public Builder setTargetClass(String className) {
            AsmHook.this.targetClassName = className;
            return this;
        }

        public Builder setTargetMethod(String methodName) {
            AsmHook.this.targetMethodName = methodName;
            return this;
        }

        public Builder addTargetMethodParameters(Type... parameterTypes) {
            for (Type type : parameterTypes) {
                AsmHook.this.targetMethodParameters.add(type);
            }
            return this;
        }

        public Builder setTargetMethodReturnType(Type type) {
            AsmHook.this.targetMethodReturnType = type;
            return this;
        }

        public Builder setHookClass(String className) {
            AsmHook.this.hooksClassName = className;
            return this;
        }

        public Builder setHookMethod(String methodName) {
            AsmHook.this.hookMethodName = methodName;
            return this;
        }

        public Builder addHookMethodParameter(Type parameterType, int variableId) {
            AsmHook.this.hookMethodParameters.add(parameterType);
            AsmHook.this.transmittableVariableIds.add(variableId);
            return this;
        }

        public Builder addThisToHookMethodParameters() {
            AsmHook.this.hookMethodParameters.add(TypeHelper.getType(targetClassName));
            AsmHook.this.transmittableVariableIds.add(0);
            return this;
        }

        public Builder addReturnValueToHookMethodParameters() {
            if (AsmHook.this.targetMethodReturnType == Type.VOID_TYPE) {
                throw new IllegalStateException("Target method's return type is void, it does not make sense to " +
                        "transmit its return value to hook method.");
            }
            AsmHook.this.hookMethodParameters.add(AsmHook.this.targetMethodReturnType);
            AsmHook.this.transmittableVariableIds.add(-1);
            AsmHook.this.hasReturnValueParameter = true;
            return this;
        }

        public Builder setReturnCondition(ReturnCondition condition) {
            AsmHook.this.returnCondition = condition;
            return this;
        }

        public void setHookMethodReturnType(Type type) {
            AsmHook.this.hookMethodReturnType = type;
        }

        public Builder setInjectorFactory(HookInjectorFactory factory) {
            AsmHook.this.injectorFactory = factory;
            return this;
        }

        public Builder setPriority(HookPriority priority) {
            AsmHook.this.priority = priority;
            return this;
        }

        public Builder setCreateMethod(boolean createMethod) {
            AsmHook.this.createMethod = createMethod;
            return this;
        }

        public Builder setMandatory(boolean isMandatory) {
            AsmHook.this.isMandatory = isMandatory;
            return this;
        }

        public Builder setRequiredPrintLocalVariables(boolean requiredPrintLocalVariables) {
            AsmHook.this.requiredPrintLocalVariables = requiredPrintLocalVariables;
            return this;
        }

        private String getMethodDesc(Type returnType, List<Type> paramTypes) {
            Type[] paramTypesArray = paramTypes.toArray(new Type[0]);
            if (returnType == null) {
                String voidDesc = Type.getMethodDescriptor(Type.VOID_TYPE, paramTypesArray);
                return voidDesc.substring(0, voidDesc.length() - 1);
            } else {
                return Type.getMethodDescriptor(returnType, paramTypesArray);
            }
        }

        public AsmHook build() {
            AsmHook hook = AsmHook.this;

            if (hook.createMethod && hook.targetMethodReturnType == null) {
                hook.targetMethodReturnType = hook.hookMethodReturnType;
            }
            hook.targetMethodDescription1 = getMethodDesc(hook.targetMethodReturnType, hook.targetMethodParameters);
            Type maybePrimitive = objectToPrimitive.get(hook.targetMethodReturnType);
            if (maybePrimitive == null)
                hook.targetMethodDescription2 = hook.targetMethodDescription1;
            else {
                hook.targetMethodDescription2 = getMethodDesc(maybePrimitive, hook.targetMethodParameters);
            }

            hook.hookMethodDescription = Type.getMethodDescriptor(hook.hookMethodReturnType, hook.hookMethodParameters.toArray(new Type[0]));

            try {
                hook = (AsmHook) AsmHook.this.clone();
            } catch (CloneNotSupportedException impossible) {
            }

            if (hook.targetClassName == null) {
                throw new IllegalStateException("Target class name is not specified. " +
                        "Call setTargetClassName() before build().");
            }

            if (hook.targetMethodName == null) {
                throw new IllegalStateException("Target method name is not specified. " +
                        "Call setTargetMethodName() before build().");
            }

            if (!(hook.injectorFactory instanceof HookInjectorFactory.ReturnFactory) && hook.hasReturnValueParameter) {
                throw new IllegalStateException("Can not pass return value to hook method " +
                        "because hook location is not return insn.");
            }

            return hook;
        }
    }

    public static BiMap<Type, Type> objectToPrimitive = ImmutableBiMap.<Type, Type>builder()
            .put(Type.getType(Void.class), VOID_TYPE)
            .put(Type.getType(Boolean.class), BOOLEAN_TYPE)
            .put(Type.getType(Character.class), CHAR_TYPE)
            .put(Type.getType(Byte.class), BYTE_TYPE)
            .put(Type.getType(Short.class), SHORT_TYPE)
            .put(Type.getType(Integer.class), INT_TYPE)
            .put(Type.getType(Float.class), FLOAT_TYPE)
            .put(Type.getType(Long.class), LONG_TYPE)
            .put(Type.getType(Double.class), DOUBLE_TYPE)
            .build();

    public static Map<Type, String> primitiveToUnboxingMethod = ImmutableBiMap.<Type, String>builder()
            .put(BOOLEAN_TYPE, "booleanValue")
            .put(CHAR_TYPE, "charValue")
            .put(BYTE_TYPE, "byteValue")
            .put(SHORT_TYPE, "shortValue")
            .put(INT_TYPE, "intValue")
            .put(FLOAT_TYPE, "floatValue")
            .put(LONG_TYPE, "longValue")
            .put(DOUBLE_TYPE, "doubleValue")
            .build();
}
