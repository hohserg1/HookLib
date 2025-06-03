package gloomyfolken.hooklib.asm.injections;

import gloomyfolken.hooklib.asm.HookInjectorClassVisitor;
import lombok.Value;
import org.objectweb.asm.Type;

@Value
public class AsmFixFirstArgument implements AsmInjection {
    String targetClassName;
    String targetMethodName;
    String targetMethodDesc;
    Type actualFirstArgType;
    boolean isMandatory;

    @Override
    public String getTargetClassName() {
        return targetClassName;
    }

    public boolean checkDescription(String desc) {
        return targetMethodDesc.equals(desc);
    }

    @Override
    public boolean isMandatory() {
        return isMandatory;
    }

    @Override
    public boolean needToCreate() {
        return false;
    }

    @Override
    public void create(HookInjectorClassVisitor hookInjectorClassVisitor) {
    }

    public String transformDescription(String desc) {
        Type methodType = Type.getMethodType(desc);
        Type[] argumentTypes = methodType.getArgumentTypes();
        argumentTypes[0] = actualFirstArgType;
        return Type.getMethodDescriptor(methodType.getReturnType(), argumentTypes);
    }

    @Override
    public int compareTo(AsmInjection o) {
        if (o == this)
            return 0;
        return -1;
    }
}
