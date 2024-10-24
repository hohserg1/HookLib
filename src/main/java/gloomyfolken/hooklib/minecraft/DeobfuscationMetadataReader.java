package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.asm.ClassMetadataReader;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * ClassMetadataReader with support of Minecraft obfuscation
 */
public class DeobfuscationMetadataReader extends ClassMetadataReader {

    private static Method runTransformers;

    static {
        try {
            runTransformers = LaunchClassLoader.class.getDeclaredMethod("runTransformers",
                    String.class, String.class, byte[].class);
            runTransformers.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] getClassData(String className) throws IOException {
        byte[] bytes = super.getClassData(unmap(className.replace('.', '/')));
        return deobfuscateClass(className, bytes);
    }

    @Override
    protected boolean checkSameMethod(String sourceName, String sourceDesc, String targetName, String targetDesc) {
        return checkSameMethod(sourceName, targetName) && sourceDesc.equals(targetDesc);
    }

    /**
     * Forge and other coremods may add new methods which may be need to override, so finding in transformed bytecode
     */
    @Override
    protected MethodReference getMethodReferenceASM(String type, String methodName, String desc, boolean privateToo) throws IOException {
        FindMethodClassVisitor cv = new FindMethodClassVisitor(methodName, desc, privateToo);
        byte[] bytes = getTransformedBytes(type);
        acceptVisitor(bytes, cv);
        return cv.found ? new MethodReference(type, cv.targetAccess, cv.targetName, cv.targetDesc) : null;
    }

    static byte[] deobfuscateClass(String className, byte[] bytes) {
        if (HookLoader.getDeobfuscationTransformer() != null) {
            bytes = HookLoader.getDeobfuscationTransformer().transform(className, className, bytes);
        }
        return bytes;
    }

    private static byte[] getTransformedBytes(String type) throws IOException {
        String obfName = unmap(type);
        byte[] bytes = Launch.classLoader.getClassBytes(obfName);
        if (bytes == null) {
            throw new RuntimeException("Bytes for " + obfName + " not found");
        }
        try {
            bytes = (byte[]) runTransformers.invoke(Launch.classLoader, obfName, type, bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }

    private static String unmap(String type) {
        if (HookLibPlugin.getObfuscated()) {
            return FMLDeobfuscatingRemapper.INSTANCE.unmap(type);
        }
        return type;
    }

    private static boolean checkSameMethod(String srgName, String mcpName) {
        if (HookLibPlugin.getObfuscated() && MinecraftClassTransformer.instance != null) {
            int methodId = MinecraftClassTransformer.getMemberId("func_", srgName);
            String remappedName = MinecraftClassTransformer.instance.getMethodNames().get(methodId);
            if (remappedName != null && remappedName.equals(mcpName)) {
                return true;
            }
        }
        return srgName.equals(mcpName);
    }

}
