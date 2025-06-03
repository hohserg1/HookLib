package gloomyfolken.hooklib.asm;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import gloomyfolken.hooklib.asm.injections.*;
import gloomyfolken.hooklib.helper.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

public class HookInjectorClassVisitor extends ClassVisitor {

    final Multimap<String, AsmFieldLens> fieldHooks;
    final Multimap<String, AsmFixFirstArgument> methodPreHooks;
    final Multimap<String, AsmMethodInjection> methodHooks;
    final Optional<AsmClassAccessFix> classAccessFix;
    Set<AsmInjection> injectedHooks = new HashSet<>(1);
    boolean visitingHook;
    public HookClassTransformer transformer;
    private final List<AsmInjection> allHooks;

    String superName;

    public HookInjectorClassVisitor(HookClassTransformer transformer, ClassVisitor finalizeVisitor, List<AsmInjection> hooks) {
        super(Opcodes.ASM5, finalizeVisitor);

        this.methodPreHooks = hooks.stream()
            .filter(a -> a instanceof AsmFixFirstArgument)
            .map(a -> (AsmFixFirstArgument) a)
            .collect(Multimaps.toMultimap(AsmFixFirstArgument::getTargetMethodName, Function.identity(), ArrayListMultimap::create));

        this.methodHooks = hooks.stream()
            .filter(a -> a instanceof AsmMethodInjection)
            .map(a -> (AsmMethodInjection) a)
            .collect(Multimaps.toMultimap(AsmMethodInjection::getTargetMethodName, Function.identity(), ArrayListMultimap::create));

        this.fieldHooks = hooks.stream()
            .filter(a -> a instanceof AsmFieldLens)
            .map(a -> (AsmFieldLens) a)
            .collect(Multimaps.toMultimap(AsmFieldLens::getTargetFieldName, Function.identity(), ArrayListMultimap::create));

        classAccessFix = hooks.stream().filter(a -> a instanceof AsmClassAccessFix).map(a -> (AsmClassAccessFix) a).findAny();

        this.transformer = transformer;
        this.allHooks = hooks;
    }

    public void markInjected(AsmInjection injection) {
        injectedHooks.add(injection);
    }

    @Override
    public void visit(int version, int access, String name,
                      String signature, String superName, String[] interfaces) {
        this.superName = superName;
        if (classAccessFix.isPresent()) {
            access |= ACC_PUBLIC;
            markInjected(classAccessFix.get());
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        for (AsmFieldLens lens : fieldHooks.get(deobfField(name))) {
            if (lens.checkDescription(desc)) {
                access &= ~ACC_FINAL;

                lens.foundExistedField(name, access, desc);

                Logger.instance.debug("Patching field " + lens.getPatchedFieldName());

                return super.visitField(access, name, desc, signature, value);
            }
        }
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        for (AsmFixFirstArgument preHook : methodPreHooks.get(name)) {
            if (preHook.checkDescription(deobfDescription(desc))) {
                markInjected(preHook);
                desc = preHook.transformDescription(desc);
                break;
            }
        }
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        String desc1 = deobfDescription(desc);
        for (AsmMethodInjection hook : methodHooks.get(deobfMethod(name))) {
            if (hook.checkDescription(desc1) && !injectedHooks.contains(hook)) {
                MethodVisitor prevMV = mv;
                mv = hook.getInjectorFactory().createHookInjector(mv, access, name, desc, signature, exceptions, hook, this);
                if (prevMV != mv)
                    Logger.instance.debug("Patching method " + hook.getPatchedMethodName(name, desc));
                else
                    Logger.instance.debug("Observing method " + hook.getPatchedMethodName(name, desc));
            }

        }
        return mv;
    }

    @Override
    public void visitEnd() {
        for (AsmInjection injection : allHooks) {
            if (injection.needToCreate() && !injectedHooks.contains(injection)) {
                injection.create(this);
            }
        }
        super.visitEnd();
    }

    protected String deobfMethod(String name) {
        return name;
    }

    protected String deobfDescription(String desc) {
        return desc;
    }

    protected String deobfField(String name) {
        return name;
    }
}
