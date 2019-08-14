package gloomyfolken.hooklib.asm;

import org.objectweb.asm.tree.MethodNode;

public enum InjectionPoint {

    /**
     * Начало метода
     */
    HEAD(false, HookInjectorMethodVisitor.HeadInjector),

    /**
     * Конец метода
     */
    RETURN(true, HookInjectorMethodVisitor.ReturnInjector),

    /**
     * Когда происходит вызов другого метода где-то в теле хукнутого
     */
    METHOD_CALL(false, HookInjectorMethodVisitor.MethodCallInjector);

    public final boolean isPriorityInverted;
    public final HookInjectorFactory factory;

    InjectionPoint(boolean isPriorityInverted, HookInjectorFactory factory) {

        this.isPriorityInverted = isPriorityInverted;
        this.factory = factory;
    }

    interface HookInjectorFactory {
        public void apply(AsmHook asmHook, MethodNode methodNode);
    }
}
