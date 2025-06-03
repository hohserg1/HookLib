package gloomyfolken.hooklib.asm.injections;

import gloomyfolken.hooklib.asm.HookInjectorClassVisitor;
import lombok.Value;

@Value
public class AsmClassAccessFix implements AsmInjection {
    String targetClassName;

    @Override
    public String getTargetClassName() {
        return targetClassName;
    }

    @Override
    public boolean isMandatory() {
        return true;
    }

    @Override
    public boolean needToCreate() {
        return false;
    }

    @Override
    public void create(HookInjectorClassVisitor hookInjectorClassVisitor) {
    }
}
