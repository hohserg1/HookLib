package gloomyfolken.hooklib.asm;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

public interface AsmMethodInjection extends AsmInjection {
    boolean isTargetMethod(String name, String desc);

    HookInjectorFactory getInjectorFactory();

    boolean isRequiredPrintLocalVariables();

    void inject(HookInjectorMethodVisitor inj);

    InsnList injectNode(MethodNode methodNode, HookInjectorClassVisitor cv);

    String getPatchedMethodName(String actualName, String actualDescription);
}
