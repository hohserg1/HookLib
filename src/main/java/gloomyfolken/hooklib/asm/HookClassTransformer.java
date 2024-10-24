package gloomyfolken.hooklib.asm;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import gloomyfolken.hooklib.asm.injections.AsmInjection;
import gloomyfolken.hooklib.helper.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.objectweb.asm.ClassReader.SKIP_CODE;
import static org.objectweb.asm.Opcodes.ASM5;

public class HookClassTransformer {
    protected ListMultimap<String, AsmInjection> hooksMap = ArrayListMultimap.create(10, 2);
    public ClassMetadataReader classMetadataReader = new ClassMetadataReader();

    public void registerAllHooks(ListMultimap<String, AsmInjection> hooks) {
        hooksMap.putAll(hooks);
    }

    public void registerHook(AsmInjection hook) {
        hooksMap.put(hook.getTargetClassName(), hook);
    }

    public void registerHookContainer(String className) {
        try {
            ClassNode classNode = new ClassNode(ASM5);
            new ClassReader(classMetadataReader.getClassData(className)).accept(classNode, SKIP_CODE);
            HookContainerParser.parseHooks(classNode).forEachOrdered(this::registerHook);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] transform(String className, byte[] bytecode) {
        if (hooksMap.containsKey(className)) {
            List<AsmInjection> hooks = hooksMap.get(className);
            Set<AsmInjection> injectedHooks;
            Collections.sort(hooks);
            Logger.instance.debug("Injecting hooks into class " + className);
            try {
                if (bytecode == null) {
                    Logger.instance.error("wtf bytecode null " + className + ". skipping");
                    new RuntimeException().printStackTrace();
                    return bytecode;
                }
                //special flags for java7+. https://stackoverflow.com/questions/25109942
                int majorVersion = ((bytecode[6] & 0xFF) << 8) | (bytecode[7] & 0xFF);
                boolean java7 = majorVersion > 50;


                ClassReader cr = new ClassReader(bytecode);
                ClassWriter cw = createClassWriter(java7 ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS);
                HookInjectorClassVisitor hooksWriter = createInjectorClassVisitor(cw, hooks);
                cr.accept(hooksWriter, java7 ? ClassReader.SKIP_FRAMES : ClassReader.EXPAND_FRAMES);
                bytecode = cw.toByteArray();
                injectedHooks = hooksWriter.injectedHooks;
            } catch (Exception e) {
                throw new RuntimeException("A problem has occurred during transformation of class " + className + ". Plz report to https://github.com/hohserg1/HookLib/issues\n" +
                        "Attached hooks: [\n" +
                        hooksToString(hooks) + "\n" +
                        "]\n" +
                        "Stack trace:", e);
            }

            List<AsmInjection> mandatoryMissed = new ArrayList<>();

            for (AsmInjection hook : hooks) {
                if (!injectedHooks.contains(hook))
                    if (hook.isMandatory()) {
                        mandatoryMissed.add(hook);
                    } else {
                        Logger.instance.warning("Can not find target method of hook " + hook);
                    }
            }

            if (!mandatoryMissed.isEmpty()) {
                throw new RuntimeException("Can not find target method of mandatory hooks: [\n" +
                        hooksToString(mandatoryMissed) +
                        "\n]"
                );
            }
        }

        return bytecode;
    }

    private String hooksToString(List<AsmInjection> mandatoryMissed) {
        return mandatoryMissed.stream().map(AsmInjection::toString).collect(Collectors.joining("\n"));
    }

    protected HookInjectorClassVisitor createInjectorClassVisitor(ClassVisitor finalizeVisitor, List<AsmInjection> hooks) {
        return new HookInjectorClassVisitor(this, finalizeVisitor, hooks);
    }

    protected ClassWriter createClassWriter(int flags) {
        return new SafeClassWriter(classMetadataReader, flags);
    }


}
