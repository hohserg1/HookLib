package gloomyfolken.hooklib.asm;

import org.objectweb.asm.MethodVisitor;

public enum InjectionPoint {

    /**
     * Начало метода
     */
    HEAD(false, HookInjectorMethodVisitor.ByAnchor::new),

    /**
     * Конец метода
     */
    RETURN(true, HookInjectorMethodVisitor.ByAnchor::new),

    /**
     * Когда происходит вызов другого метода где-то в теле хукнутого
     */
    METHOD_CALL(false, HookInjectorMethodVisitor.ByAnchor::new);

    public final boolean isPriorityInverted;
    public final HookInjectorFactory factory;

    InjectionPoint(boolean isPriorityInverted, HookInjectorFactory factory) {

        this.isPriorityInverted = isPriorityInverted;
        this.factory = factory;
    }

    interface HookInjectorFactory {
        public MethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc, AsmHook hook, HookInjectorClassVisitor hookInjectorClassVisitor);
    }
}
