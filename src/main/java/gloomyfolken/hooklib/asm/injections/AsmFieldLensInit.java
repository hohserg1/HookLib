package gloomyfolken.hooklib.asm.injections;

import gloomyfolken.hooklib.api.Constants;
import gloomyfolken.hooklib.asm.HookInjectorClassVisitor;
import gloomyfolken.hooklib.asm.HookInjectorFactory;
import gloomyfolken.hooklib.asm.HookInjectorFactory.BeginFactory;
import gloomyfolken.hooklib.asm.HookInjectorMethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;

public class AsmFieldLensInit implements AsmMethodInjection {
    private final String targetClassName;
    private final String targetFieldName;
    private final Type targetFieldType;
    private final boolean isMandatory;
    private final InsnList defaultValue;

    public AsmFieldLensInit(String targetClassName, String targetFieldName, Type targetFieldType, boolean isMandatory, InsnList defaultValue) {
        this.targetClassName = targetClassName;
        this.targetFieldName = targetFieldName;
        this.targetFieldType = targetFieldType;
        this.isMandatory = isMandatory;
        this.defaultValue = defaultValue;
        defaultValue.insertBefore(defaultValue.getFirst(), new VarInsnNode(Opcodes.ALOAD, 0));
        defaultValue.add(new FieldInsnNode(Opcodes.PUTFIELD, getTargetClassInternalName(), targetFieldName, targetFieldType.getDescriptor()));
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
        return name.equals(Constants.CONSTRUCTOR_NAME);
    }

    @Override
    public boolean needToCreate() {
        return false;
    }

    @Override
    public void create(HookInjectorClassVisitor hookInjectorClassVisitor) {

    }

    @Override
    public HookInjectorFactory getInjectorFactory() {
        return BeginFactory.INSTANCE;
    }

    @Override
    public boolean isRequiredPrintLocalVariables() {
        return false;
    }

    @Override
    public void inject(HookInjectorMethodVisitor inj) {
        ListIterator<AbstractInsnNode> it = defaultValue.iterator();
        while (it.hasNext()) {
            AbstractInsnNode i = it.next();
            i.accept(inj);
        }
    }

    @Override
    public InsnList injectNode(MethodNode methodNode, HookInjectorClassVisitor cv) {
        return defaultValue;
    }
}
