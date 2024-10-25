package gloomyfolken.hooklib.asm;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import gloomyfolken.hooklib.asm.injections.AsmInjection;
import gloomyfolken.hooklib.helper.AppendWhileIterationList;
import gloomyfolken.hooklib.helper.Logger;
import gloomyfolken.hooklib.minecraft.HookLoader;
import gloomyfolken.hooklib.minecraft.PrimaryClassTransformer;
import gloomyfolken.hooklib.minecraft.TransformingStage;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HookClassTransformer implements IClassTransformer {

    protected boolean active = true;
    public static HookClassTransformer last = null;

    private static final ListMultimap<String, AsmInjection> hooksMap = ArrayListMultimap.create(10, 2);
    public ClassMetadataReader classMetadataReader = HookLoader.getDeobfuscationMetadataReader();

    public TransformingStage stage = new PrimaryClassTransformer();

    public HookClassTransformer() {
        if (last != null)
            last.deactivate();
        last = this;
    }

    protected void deactivate() {
        active = false;
    }

    public static void registerAllHooks(ListMultimap<String, AsmInjection> hooks) {
        hooksMap.putAll(hooks);
    }

    private static List<IClassTransformer> transformers = null;
    private static int prevSize = -1;

    private void initTransformerList() {
        if (transformers == null) {
            try {
                ClassLoader classLoader = HookClassTransformer.class.getClassLoader();
                if (classLoader instanceof LaunchClassLoader) {
                    Field field = LaunchClassLoader.class.getDeclaredField("transformers");
                    field.setAccessible(true);
                    List<IClassTransformer> originalList = (List<IClassTransformer>) field.get(classLoader);

                    List<IClassTransformer> replacementList = new AppendWhileIterationList<>(originalList);

                    transformers = replacementList;

                    field.set(classLoader, replacementList);

                    if (transformers.get(transformers.size() - 1) == this)
                        prevSize = transformers.size();

                } else {
                    throw new IllegalStateException("HookLib was not loaded by LaunchClassLoader. Hooks will not be injected.");
                }
            } catch (Throwable e) {
                throw new RuntimeException("failed to get LaunchClassLoader#transformers", e);
            }
        }
    }

    private void raiseUpHookClassTransformer() {
        initTransformerList();
        if (prevSize != transformers.size()) {
            transformers.add(new HookClassTransformer());
            prevSize = transformers.size();
        }
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        return transform(transformedName, basicClass);
    }

    public byte[] transform(String className, byte[] bytecode) {
        if (!active)
            return bytecode;

        raiseUpHookClassTransformer();

        if (!active)
            return bytecode;

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
        return stage.createInjectorClassVisitor(this, finalizeVisitor, hooks);
    }

    protected ClassWriter createClassWriter(int flags) {
        return new SafeClassWriter(classMetadataReader, flags);
    }


}
