package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.asm.*;
import gloomyfolken.hooklib.helper.Logger;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassVisitor;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This transformer uses after Minecraft classes have started loading
 * will be applied after all other transformers
 * kinda have no sense to separate it
 */
public class MinecraftClassTransformer extends HookClassTransformer implements IClassTransformer {

    public static MinecraftClassTransformer instance;
    private Map<Integer, String> methodNames;
    private Map<Integer, String> fieldNames;

    public MinecraftClassTransformer() {
        instance = this;

        if (HookLibPlugin.getObfuscated()) {
            try {
                long timeStart = System.currentTimeMillis();
                methodNames = loadMethodNames("/methods.bin");
                fieldNames = loadMethodNames("/fields.bin");
                long time = System.currentTimeMillis() - timeStart;
                Logger.instance.debug("Mappings dictionary loaded in " + time + " ms");
            } catch (IOException e) {
                Logger.instance.error("Can not load obfuscated method names", e);
            }
        }

        this.classMetadataReader = HookLoader.getDeobfuscationMetadataReader();

        this.hooksMap.putAll(PrimaryClassTransformer.instance.getHooksMap());
        PrimaryClassTransformer.instance.getHooksMap().clear();
        PrimaryClassTransformer.instance.registeredSecondTransformer = true;
    }

    private HashMap<Integer, String> loadMethodNames(String fileName) throws IOException {
        InputStream resourceStream = getClass().getResourceAsStream(fileName);
        if (resourceStream == null) throw new IOException("Methods dictionary not found");
        DataInputStream input = new DataInputStream(new BufferedInputStream(resourceStream));
        int numMethods = input.readInt();
        HashMap<Integer, String> map = new HashMap<Integer, String>(numMethods);
        for (int i = 0; i < numMethods; i++) {
            map.put(input.readInt(), input.readUTF());
        }
        input.close();
        return map;
    }

    @Override
    public byte[] transform(String oldName, String newName, byte[] bytecode) {
        return transform(newName, bytecode);
    }

    @Override
    protected HookInjectorClassVisitor createInjectorClassVisitor(ClassVisitor finalizeVisitor, List<AsmInjection> hooks) {
        return new HookInjectorClassVisitor(this, finalizeVisitor, hooks) {
            @Override
            protected boolean isTargetMethod(AsmMethodInjection hook, String name, String desc) {
                if (HookLibPlugin.getObfuscated()) {
                    String mcpName = methodNames.get(getMemberId("func_", name));
                    if (mcpName != null && super.isTargetMethod(hook, mcpName, desc)) {
                        return true;
                    }
                }
                return super.isTargetMethod(hook, name, desc);
            }

            @Override
            protected boolean isTargetField(AsmFieldLens lens, String name, String desc) {
                if (HookLibPlugin.getObfuscated()) {
                    String mcpName = fieldNames.get(getMemberId("field_", name));
                    if (mcpName != null && super.isTargetField(lens, mcpName, desc)) {
                        return true;
                    }
                }
                return super.isTargetField(lens, name, desc);
            }
        };
    }

    public Map<Integer, String> getMethodNames() {
        return methodNames;
    }

    public static int getMemberId(String prefix, String srgName) {
        if (srgName.startsWith(prefix)) {
            int first = srgName.indexOf('_');
            int second = srgName.indexOf('_', first + 1);
            return Integer.valueOf(srgName.substring(first + 1, second));
        } else {
            return -1;
        }
    }

}
