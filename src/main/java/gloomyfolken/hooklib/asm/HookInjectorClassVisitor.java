package gloomyfolken.hooklib.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class HookInjectorClassVisitor extends ClassVisitor {

    List<AsmInjection> hooks;
    List<AsmInjection> injectedHooks = new ArrayList<>(1);
    boolean visitingHook;
    HookClassTransformer transformer;

    String superName;

    public HookInjectorClassVisitor(HookClassTransformer transformer, ClassVisitor finalizeVisitor, List<AsmInjection> hooks) {
        super(Opcodes.ASM5, finalizeVisitor);
        this.hooks = hooks;
        this.transformer = transformer;
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
            if (injection instanceof AsmLens) {
                AsmLens lens = (AsmLens) injection;
                if (isTargetField(lens, name, desc) && !injectedHooks.contains(lens)) {
                    injectedHooks.add(lens);

                    return super.visitField(
                            access | ACC_PUBLIC & ~ACC_PRIVATE & ~ACC_PROTECTED & ~ACC_FINAL,
                            name, desc, signature, value
                    );
                }
            }
        }
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        for (AsmInjection injection : hooks) {
            if (injection instanceof AsmHook) {
                AsmHook hook = (AsmHook) injection;
                if (isTargetMethod(hook, name, desc) && !injectedHooks.contains(hook)) {
                    // добавляет MethodVisitor в цепочку
                    mv = hook.getInjectorFactory().createHookInjector(mv, access, name, desc, hook, this);
                    injectedHooks.add(hook);
                }
            } else if (injection instanceof AsmLensHook) {
                AsmLensHook hook = (AsmLensHook) injection;
                if (isTargetMethod(hook, name, desc) && !injectedHooks.contains(hook)) {
                    // добавляет MethodVisitor в цепочку
                    mv = hook.createHookInjector(mv, access, name, desc, hook, this);
                    injectedHooks.add(hook);
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

    private boolean isTargetMethod(AsmLensHook hook, String name, String desc) {
        return hook.isTargetMethod(name, desc);
    }

    protected boolean isTargetMethod(AsmHook hook, String name, String desc) {
        return hook.isTargetMethod(name, desc);
    }

    protected boolean isTargetField(AsmLens lens, String name, String desc) {
        return lens.isTargetField(name, desc);
    }
}
