package gloomyfolken.hooklib.asm.injections;

import gloomyfolken.hooklib.asm.HookInjectorClassVisitor;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Type;

import java.util.List;

@Value
public class AsmFixPrivateClassArguments implements AsmInjection {
    String targetClassName;
    String targetMethodName;
    String targetMethodDesc;
    List<Pair<Integer, Type>> argsToReplace;
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
        for (Pair<Integer, Type> e : argsToReplace) {
            int index = e.getLeft();
            Type validType = e.getRight();
            argumentTypes[index] = validType;
        }
        return Type.getMethodDescriptor(methodType.getReturnType(), argumentTypes);
    }

    @Override
    public int compareTo(AsmInjection o) {
        if (o == this)
            return 0;
        return -1;
    }
}
