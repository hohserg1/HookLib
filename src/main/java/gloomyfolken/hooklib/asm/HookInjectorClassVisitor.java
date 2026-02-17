package gloomyfolken.hooklib.asm;

import com.google.common.collect.*;
import gloomyfolken.hooklib.asm.injections.*;
import gloomyfolken.hooklib.helper.*;
import gloomyfolken.hooklib.minecraft.*;
import org.apache.commons.lang3.tuple.*;
import org.objectweb.asm.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static org.objectweb.asm.Opcodes.*;

public class HookInjectorClassVisitor extends ClassVisitor {

    final Multimap<String, AsmFieldLens> fieldHooks;
    final Multimap<String, AsmFixPrivateClassArguments> methodPreHooks;
    final Multimap<String, AsmMethodInjection> methodHooks;
    final Optional<AsmClassAccessFix> classAccessFix;
    Set<AsmInjection> injectedHooks = new HashSet<>(1);
    boolean visitingHook;
    public HookClassTransformer transformer;
    private final List<AsmInjection> allHooks;

    String superName;

    public HookInjectorClassVisitor(HookClassTransformer transformer, ClassVisitor finalizeVisitor, List<AsmInjection> hooks) {
        super(Opcodes.ASM5, finalizeVisitor);

        this.methodPreHooks = collect(hooks, AsmFixPrivateClassArguments.class, AsmFixPrivateClassArguments::getTargetMethodName, __ -> ImmutableSet.of());

        this.methodHooks = collect(hooks, AsmMethodInjection.class, AsmMethodInjection::getTargetMethodName, Deobfuscation.instance::obfMethod);

        this.fieldHooks = collect(hooks, AsmFieldLens.class, AsmFieldLens::getTargetFieldName, Deobfuscation.instance::obfField);

        classAccessFix = hooks.stream().filter(a -> a instanceof AsmClassAccessFix).map(a -> (AsmClassAccessFix) a).findAny();

        this.transformer = transformer;
        this.allHooks = hooks;
    }

    private <Injection> Multimap<String, Injection> collect(List<AsmInjection> hooks, Class<Injection> filter, Function<Injection, String> targetMemberName,
                                                            Function<String, Set<String>> obfuscation) {
        return hooks.stream()
            .filter(filter::isInstance)
            .map(filter::cast)
            .flatMap(a -> {
                String deobfName = targetMemberName.apply(a);
                return Stream.concat(obfuscation.apply(deobfName).stream(), Stream.of(deobfName)).distinct().map(name -> Pair.of(name, a));
            })
            .collect(Multimaps.toMultimap(Pair::getLeft, Pair::getRight, ArrayListMultimap::create));
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
        for (AsmFieldLens lens : fieldHooks.get(name)) {
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
        for (AsmFixPrivateClassArguments preHook : methodPreHooks.get(name)) {
            if (preHook.checkDescription(deobfDescription(desc))) {
                markInjected(preHook);
                desc = preHook.transformDescription(desc);
                break;
            }
        }
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        String desc1 = deobfDescription(desc);
        for (AsmMethodInjection hook : methodHooks.get(name)) {
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

    protected String deobfDescription(String desc) {
        return desc;
    }

}
