package gloomyfolken.hooklib.asm.injections;

public interface AsmMethodInjectionObserving extends AsmMethodInjection {
    void visitedMethod(int access, String name, String desc, String signature, String[] exceptions);
}
