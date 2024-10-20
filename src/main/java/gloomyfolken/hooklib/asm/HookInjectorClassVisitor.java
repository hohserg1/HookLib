package gloomyfolken.hooklib.asm;

import gloomyfolken.hooklib.asm.injections.AsmFieldLens;
import gloomyfolken.hooklib.asm.injections.AsmInjection;
import gloomyfolken.hooklib.asm.injections.AsmMethodInjection;
import gloomyfolken.hooklib.helper.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ACC_FINAL;

public class HookInjectorClassVisitor extends ClassVisitor {

    List<AsmInjection> hooks;
    Set<AsmInjection> injectedHooks = new HashSet<>(1);
    boolean visitingHook;
    public HookClassTransformer transformer;

    String superName;

    public HookInjectorClassVisitor(HookClassTransformer transformer, ClassVisitor finalizeVisitor, List<AsmInjection> hooks) {
        super(Opcodes.ASM5, finalizeVisitor);
        this.hooks = hooks;
        this.transformer = transformer;
    }

    public void markInjected(AsmInjection injection) {
        injectedHooks.add(injection);
    }

    @Override
    public void visit(int version, int access, String name,
                      String signature, String superName, String[] interfaces) {
        this.superName = superName;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        for (AsmInjection injection : hooks) {
            if (injection instanceof AsmFieldLens) {
                AsmFieldLens lens = (AsmFieldLens) injection;
                if (isTargetField(lens, name, desc)) {
                    access &= ~ACC_FINAL;

                    lens.foundExistedField(access, desc);

                    Logger.instance.debug("Patching field " + ((AsmFieldLens) injection).getPatchedFieldName());

                    return super.visitField(access, name, desc, signature, value);
                }
            }
        }
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        for (AsmInjection injection : hooks) {
            if (injection instanceof AsmMethodInjection) {
                AsmMethodInjection hook = (AsmMethodInjection) injection;
                if (isTargetMethod(hook, name, desc) && !injectedHooks.contains(hook)) {
                    MethodVisitor prevMV = mv;
                    mv = hook.getInjectorFactory().createHookInjector(mv, access, name, desc, signature, exceptions, hook, this);
                    if (prevMV != mv)
                        Logger.instance.debug("Patching method " + hook.getPatchedMethodName(name, desc));
                    else
                        Logger.instance.debug("Observing method " + hook.getPatchedMethodName(name, desc));
                }
            }
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        for (AsmInjection injection : hooks) {
            if (injection.needToCreate() && !injectedHooks.contains(injection)) {
                injection.create(this);
            }
        }
        super.visitEnd();
    }

    protected boolean isTargetMethod(AsmMethodInjection hook, String name, String desc) {
        return hook.isTargetMethod(name, desc);
    }

    protected boolean isTargetField(AsmFieldLens lens, String name, String desc) {
        return lens.isTargetField(name, desc);
    }
}
