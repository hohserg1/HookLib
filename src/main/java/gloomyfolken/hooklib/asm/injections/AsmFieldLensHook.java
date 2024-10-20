package gloomyfolken.hooklib.asm.injections;

import gloomyfolken.hooklib.api.Constants;
import gloomyfolken.hooklib.asm.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.*;

public class AsmFieldLensHook implements AsmMethodInjection {
    private final String hookClassName;
    private final String hookFieldLensName;
    private final String targetClassName;
    private final String targetFieldName;
    private final Type targetFieldType;
    private final boolean isMandatory;

    public AsmFieldLensHook(String hookClassName, String hookFieldLensName, String targetClassName, String targetFieldName, Type targetFieldType, boolean isMandatory) {
        this.hookClassName = hookClassName.replace('/', '.');
        this.hookFieldLensName = hookFieldLensName;
        this.targetClassName = targetClassName.replace('.', '/');
        this.targetFieldName = targetFieldName;
        this.targetFieldType = targetFieldType;
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
    public boolean needToCreate() {
        return true;
    }

    @Override
    public void create(HookInjectorClassVisitor classVisitor) {
        MethodVisitor mv = classVisitor.visitMethod(ACC_STATIC, Constants.STATIC_INITIALIZER_NAME, Type.getMethodDescriptor(Type.VOID_TYPE), null, null);
        if (mv instanceof HookInjectorMethodVisitor) {
            HookInjectorMethodVisitor inj = (HookInjectorMethodVisitor) mv;
            inj.visitCode();
            inj.visitLabel(new Label());
            inj.visitInsn(RETURN);
            inj.visitMaxs(0, 0);
            inj.visitEnd();

        } else {
            throw new IllegalArgumentException("Hook injector not created");
        }
    }

    public boolean isTargetMethod(String name, String desc) {
        return name.equals(Constants.STATIC_INITIALIZER_NAME);
    }

    @Override
    public HookInjectorFactory getInjectorFactory() {
        return new HookInjectorFactory.ReturnFactory(-1);
    }

    @Override
    public boolean isRequiredPrintLocalVariables() {
        return false;
    }

    @Override
    public void inject(HookInjectorMethodVisitor inj) {
        String lensClassName = hookClassName + "$" + hookFieldLensName + "$lens";

        generateLensClass(lensClassName, inj.cv.transformer.classMetadataReader);

        inj.visitLdcInsn(lensClassName);
        inj.visitInsn(ICONST_1);
        inj.visitFieldInsn(GETSTATIC, "gloomyfolken/hooklib/asm/GeneratedClassLoader", "instance", "Lgloomyfolken/hooklib/asm/GeneratedClassLoader;");
        inj.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
        inj.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "newInstance", "()Ljava/lang/Object;", false);
        inj.visitTypeInsn(CHECKCAST, "gloomyfolken/hooklib/api/FieldAccessor");
        inj.visitFieldInsn(PUTSTATIC, hookClassName.replace('.', '/'), hookFieldLensName, "Lgloomyfolken/hooklib/api/FieldAccessor;");
    }

    private void generateLensClass(String lensClassName, ClassMetadataReader classMetadataReader) {
        String lensClassInternalName = lensClassName.replace('.', '/');


        ClassWriter classWriter = new SafeClassWriter(classMetadataReader, ClassWriter.COMPUTE_FRAMES);
        MethodVisitor methodVisitor;

        String targetClassDescriptor = Type.getObjectType(targetClassName).getDescriptor();
        String lensClassDescriptor = Type.getObjectType(lensClassInternalName).getDescriptor();
        classWriter.visit(
                V1_8,
                ACC_PUBLIC | ACC_SUPER,
                lensClassInternalName,
                "Ljava/lang/Object;Lgloomyfolken/hooklib/api/FieldAccessor<" + targetClassDescriptor + targetFieldType.getDescriptor() + ">;",
                Type.getInternalName(Object.class),
                new String[]{"gloomyfolken/hooklib/api/FieldAccessor"}
        );

        classWriter.visitSource(".dynamic", null);

        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLineNumber(7, label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false);
            methodVisitor.visitInsn(RETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("this", lensClassDescriptor, null, label0, label1, 0);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
        }
        String getDescriptor = Type.getMethodDescriptor(targetFieldType, Type.getObjectType(targetClassName));
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "get", getDescriptor, null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLineNumber(11, label0);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitFieldInsn(GETFIELD, targetClassName, targetFieldName, targetFieldType.getDescriptor());
            methodVisitor.visitInsn(ARETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("this", lensClassDescriptor, null, label0, label1, 0);
            methodVisitor.visitLocalVariable("instance", targetClassDescriptor, null, label0, label1, 1);
            methodVisitor.visitMaxs(1, 2);
            methodVisitor.visitEnd();
        }
        String setDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType(targetClassName), targetFieldType);
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "set", setDescriptor, null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLineNumber(16, label0);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitFieldInsn(PUTFIELD, targetClassName, targetFieldName, targetFieldType.getDescriptor());
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLineNumber(17, label1);
            methodVisitor.visitInsn(RETURN);
            Label label2 = new Label();
            methodVisitor.visitLabel(label2);
            methodVisitor.visitLocalVariable("this", lensClassDescriptor, null, label0, label2, 0);
            methodVisitor.visitLocalVariable("instance", targetClassDescriptor, null, label0, label2, 1);
            methodVisitor.visitLocalVariable("newValue", targetFieldType.getDescriptor(), null, label0, label2, 2);
            methodVisitor.visitMaxs(2, 3);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, "set", "(Ljava/lang/Object;Ljava/lang/Object;)V", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLineNumber(7, label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitTypeInsn(CHECKCAST, targetClassName);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitTypeInsn(CHECKCAST, targetFieldType.getInternalName());
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, lensClassInternalName, "set", setDescriptor, false);
            methodVisitor.visitInsn(RETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("this", lensClassDescriptor, null, label0, label1, 0);
            methodVisitor.visitMaxs(3, 3);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, "get", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLineNumber(7, label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitTypeInsn(CHECKCAST, targetClassName);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, lensClassInternalName, "get", getDescriptor, false);
            methodVisitor.visitInsn(ARETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("this", lensClassDescriptor, null, label0, label1, 0);
            methodVisitor.visitMaxs(2, 2);
            methodVisitor.visitEnd();
        }
        classWriter.visitEnd();

        GeneratedClassLoader.instance.addClass(lensClassName, classWriter.toByteArray());
    }

    @Override
    public InsnList injectNode(MethodNode methodNode, HookInjectorClassVisitor cv) {
        return null;
    }

    @Override
    public String getPatchedMethodName(String actualName, String actualDescription) {
        return hookClassName + '#' + Constants.STATIC_INITIALIZER_NAME;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AsmLensHook: ");

        sb.append(targetClassName).append('#').append(targetFieldName).append(": ");
        sb.append(targetFieldType);
        sb.append(" -> ");
        sb.append(hookClassName).append('#').append(hookFieldLensName);

        return sb.toString();
    }
}
