package gloomyfolken.hooklib.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public class AsmLensHook implements AsmInjection {
    private final String hookClassName;
    private final String hookMethodName;
    private final String hookMethodDesc;
    private final String targetClassName;
    private final String targetFieldName;
    private final Type targetFieldType;
    private final boolean isGetter;
    private final boolean isMandatory;

    public AsmLensHook(String hookClassName, String hookMethodName, String hookMethodDesc, String targetClassName, String targetFieldName, Type targetFieldType, boolean isGetter, boolean isMandatory) {
        this.hookClassName = hookClassName.replace('/', '.');
        this.hookMethodName = hookMethodName;
        this.hookMethodDesc = hookMethodDesc;
        this.targetClassName = targetClassName.replace('.', '/');
        this.targetFieldName = targetFieldName;
        this.targetFieldType = targetFieldType;
        this.isGetter = isGetter;
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


    public String getPatchedMethodName() {
        return hookClassName + '#' + hookMethodName + hookMethodDesc;
    }

    @Override
    public boolean needToCreate() {
        return false;
    }

    @Override
    public void create(HookInjectorClassVisitor hookInjectorClassVisitor) {
    }

    public boolean isTargetMethod(String name, String desc) {
        return name.equals(hookMethodName) && hookMethodDesc.equals(desc);
    }

    public MethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc, AsmLensHook hook, HookInjectorClassVisitor hookInjectorClassVisitor) {
        return new AdviceAdapter(Opcodes.ASM5, mv, access, name, desc) {
            @Override
            protected void onMethodEnter() {
                if (isGetter) {
                    visitVarInsn(Opcodes.ALOAD, 0);
                    visitFieldInsn(Opcodes.GETFIELD, targetClassName, targetFieldName, targetFieldType.getDescriptor());
                    visitInsn(targetFieldType.getOpcode(Opcodes.IRETURN));
                } else {
                    visitVarInsn(Opcodes.ALOAD, 0);
                    visitVarInsn(targetFieldType.getOpcode(Opcodes.ILOAD), 1);
                    visitFieldInsn(Opcodes.PUTFIELD, targetClassName, targetFieldName, targetFieldType.getDescriptor());
                    visitInsn(targetFieldType.getOpcode(Opcodes.RETURN));
                }
            }
        };
    }
}
