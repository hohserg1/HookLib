package gloomyfolken.hooklib.asm.injections;

import gloomyfolken.hooklib.asm.HookInjectorClassVisitor;

public interface AsmInjection extends Comparable<AsmInjection> {
    String getTargetClassName();

    boolean isMandatory();

    boolean needToCreate();

    void create(HookInjectorClassVisitor hookInjectorClassVisitor);

    @Override
    default int compareTo(AsmInjection o) {
        return 0;
    }
}
