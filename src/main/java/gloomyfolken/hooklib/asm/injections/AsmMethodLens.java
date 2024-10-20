package gloomyfolken.hooklib.asm.injections;

import gloomyfolken.hooklib.asm.HookInjectorClassVisitor;
import gloomyfolken.hooklib.asm.HookInjectorFactory;
import gloomyfolken.hooklib.asm.HookInjectorMethodVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.DOUBLE;
import static org.objectweb.asm.Type.LONG;

public class AsmMethodLens implements AsmMethodInjection {
    private final String targetClassName;
    private final String targetMethodName;
    private final String targetMethodDescription;
    private final boolean isTargetMethodStatic;
    private final boolean isMandatory;

    static final String methodAccessorSuffix = "$hook$lens";

    public AsmMethodLens(String targetClassName, String targetMethodName, String targetMethodDescription, boolean isTargetMethodStatic, boolean isMandatory) {
        this.targetClassName = targetClassName;
        this.targetMethodName = targetMethodName;
        this.targetMethodDescription = targetMethodDescription;
        this.isTargetMethodStatic = isTargetMethodStatic;
        this.isMandatory = isMandatory;
    }

    @Override
    public String getTargetClassName() {
        return targetClassName;
    }

    @Override
    public boolean isMandatory() {
        return isMandatory;
    }

    @Override
    public boolean needToCreate() {
        return true;
    }

    @Override
    public void create(HookInjectorClassVisitor classVisitor) {
        int access = ACC_PUBLIC;
        if (isTargetMethodStatic)
            access |= ACC_STATIC;

        MethodVisitor mv = classVisitor.visitMethod(access, targetMethodName + methodAccessorSuffix, targetMethodDescription, null, null);
        if (mv instanceof HookInjectorMethodVisitor) {
            Type methodType = Type.getMethodType(targetMethodDescription);

            HookInjectorMethodVisitor inj = (HookInjectorMethodVisitor) mv;
            inj.visitCode();
            inj.visitLabel(new Label());

            int variableId = isTargetMethodStatic ? 0 : 1;

            if (!isTargetMethodStatic)
                inj.visitVarInsn(ALOAD, 0);

            for (Type parameterType : methodType.getArgumentTypes()) {
                inj.visitVarInsn(parameterType.getOpcode(ILOAD), variableId);
                if (parameterType.getSort() == DOUBLE || parameterType.getSort() == LONG) {
                    variableId += 2;
                } else {
                    variableId++;
                }
            }

            int invokeOpcode = isTargetMethodStatic ? INVOKESTATIC : INVOKEVIRTUAL;
            inj.visitMethodInsn(invokeOpcode, getTargetClassInternalName(), targetMethodName, targetMethodDescription, false);

            inj.visitInsn(methodType.getReturnType().getOpcode(IRETURN));

            inj.visitLabel(new Label());
            inj.visitMaxs(0, 0);
            inj.visitEnd();

        } else {
            throw new IllegalArgumentException("Hook injector not created");
        }
    }

    @Override
    public boolean isTargetMethod(String name, String desc) {
        return name.equals(targetMethodName + methodAccessorSuffix) && desc.equals(targetMethodDescription);
    }

    @Override
    public HookInjectorFactory getInjectorFactory() {
        return HookInjectorFactory.BeginFactory.INSTANCE;
    }

    @Override
    public boolean isRequiredPrintLocalVariables() {
        return false;
    }

    @Override
    public void inject(HookInjectorMethodVisitor inj) {
    }

    @Override
    public InsnList injectNode(MethodNode methodNode, HookInjectorClassVisitor cv) {
        return new InsnList();
    }
}
