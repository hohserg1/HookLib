package gloomyfolken.hooklib.asm.injections;

import gloomyfolken.hooklib.asm.*;
import org.objectweb.asm.tree.*;

public interface AsmMethodInjection extends AsmInjection {

    String getTargetMethodName();

    boolean checkDescription(String desc);

    HookInjectorFactory getInjectorFactory();

    boolean isRequiredPrintLocalVariables();

    void inject(HookInjectorMethodVisitor inj);

    InsnList injectNode(MethodNode methodNode, HookInjectorClassVisitor cv);

    default String getPatchedMethodName(String actualName, String actualDescription) {
        return getTargetClassName() + '#' + getTargetMethodName() + actualDescription + " (actually named " + actualName + ")";
    }
}
