package gloomyfolken.hooklib.asm.injections;

import gloomyfolken.hooklib.asm.HookInjectorClassVisitor;
import gloomyfolken.hooklib.asm.HookInjectorFactory;
import gloomyfolken.hooklib.asm.HookInjectorMethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import static gloomyfolken.hooklib.asm.injections.AsmMethodLens.methodAccessorSuffix;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.DOUBLE;
import static org.objectweb.asm.Type.LONG;

public class AsmMethodLensHook implements AsmMethodInjection {
    private final String hookClassName;
    private final String hookMethodLensName;
    private final String hookMethodLensDescription;

    private final String targetClassInternalName;
    private final String targetMethodName;
    private final String targetMethodDescription;

    private final boolean isMandatory;

    public AsmMethodLensHook(String hookClassName, String hookMethodLensName,
                             String hookMethodLensDescription, String targetClassName, String targetMethodName, String targetMethodDescription,
                             boolean isMandatory) {
        this.hookClassName = hookClassName.replace('/', '.');
        this.hookMethodLensName = hookMethodLensName;
        this.hookMethodLensDescription = hookMethodLensDescription;

        this.targetClassInternalName = targetClassName.replace('.', '/');
        this.targetMethodName = targetMethodName;
        this.targetMethodDescription = targetMethodDescription;

        this.isMandatory = isMandatory;
    }


    @Override
    public String getTargetClassName() {
        return hookClassName;
    }

    @Override
    public boolean isMandatory() {
        return isMandatory;
    }

    @Override
    public boolean isTargetMethod(String name, String desc) {
        return name.equals(hookMethodLensName) && desc.equals(hookMethodLensDescription);
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
        Type methodType = Type.getMethodType(hookMethodLensDescription);

        int variableId = 0;

        for (Type parameterType : methodType.getArgumentTypes()) {
            inj.visitVarInsn(parameterType.getOpcode(ILOAD), variableId);
            if (parameterType.getSort() == DOUBLE || parameterType.getSort() == LONG) {
                variableId += 2;
            } else {
                variableId++;
            }
        }

        inj.visitMethodInsn(INVOKESTATIC, targetClassInternalName, targetMethodName + methodAccessorSuffix, hookMethodLensDescription, false);

        inj.visitInsn(methodType.getReturnType().getOpcode(IRETURN));
    }

    @Override
    public InsnList injectNode(MethodNode methodNode, HookInjectorClassVisitor cv) {
        InsnList r = new InsnList();
        Type methodType = Type.getMethodType(hookMethodLensDescription);

        int variableId = 0;

        for (Type parameterType : methodType.getArgumentTypes()) {
            r.add(new VarInsnNode(parameterType.getOpcode(ILOAD), variableId));
            if (parameterType.getSort() == DOUBLE || parameterType.getSort() == LONG) {
                variableId += 2;
            } else {
                variableId++;
            }
        }

        r.add(new MethodInsnNode(INVOKESTATIC, targetClassInternalName, targetMethodName + methodAccessorSuffix, targetMethodDescription, false));

        return r;
    }

    @Override
    public boolean needToCreate() {
        return false;
    }

    @Override
    public void create(HookInjectorClassVisitor hookInjectorClassVisitor) {
    }
}
