package gloomyfolken.hooklib.asm.injections;

import gloomyfolken.hooklib.asm.AsmUtils;
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

public class AsmMethodLens implements AsmMethodInjectionObserving {
    static final String methodAccessorSuffix = "$hook$lens$invoke";

    private final String targetClassName;
    private final String targetMethodName;
    private final String targetMethodDescription;
    private final String invokerMethodDesc;
    private final boolean isMandatory;

    private boolean found = false;
    private boolean isStaticMethod = false;

    public AsmMethodLens(String targetClassName, String targetMethodName, String targetMethodDescription, String invokerMethodDesc, boolean isMandatory) {
        this.targetClassName = targetClassName;
        this.targetMethodName = targetMethodName;
        this.targetMethodDescription = targetMethodDescription;
        this.invokerMethodDesc = invokerMethodDesc;
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
    public boolean isTargetMethod(String name, String desc) {
        return name.equals(targetMethodName) && desc.equals(targetMethodDescription);
    }

    @Override
    public HookInjectorFactory getInjectorFactory() {
        return HookInjectorFactory.ObservingFactory.INSTANCE;
    }

    @Override
    public void visitedMethod(int access, String name, String desc, String signature, String[] exceptions) {
        found = true;
        isStaticMethod = AsmUtils.isStatic(access);
    }

    @Override
    public boolean needToCreate() {
        return true;
    }

    @Override
    public void create(HookInjectorClassVisitor classVisitor) {
        if (!found)
            return;

        MethodVisitor mv = classVisitor.visitMethod(ACC_PUBLIC | ACC_STATIC, targetMethodName + methodAccessorSuffix, invokerMethodDesc, null, null);
        Type methodType = Type.getMethodType(targetMethodDescription);

        mv.visitCode();
        mv.visitLabel(new Label());

        if (!isStaticMethod)
            mv.visitVarInsn(ALOAD, 0);

        int variableId = 1;

        for (Type parameterType : methodType.getArgumentTypes()) {
            mv.visitVarInsn(parameterType.getOpcode(ILOAD), variableId);
            if (parameterType.getSort() == DOUBLE || parameterType.getSort() == LONG) {
                variableId += 2;
            } else {
                variableId++;
            }
        }

        int invokeOpcode = isStaticMethod ? INVOKESTATIC : INVOKEVIRTUAL;
        mv.visitMethodInsn(invokeOpcode, getTargetClassInternalName(), targetMethodName, targetMethodDescription, false);

        mv.visitInsn(methodType.getReturnType().getOpcode(IRETURN));

        mv.visitLabel(new Label());
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        classVisitor.markInjected(this);
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
