package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.api.At;
import gloomyfolken.hooklib.api.Hook;
import gloomyfolken.hooklib.api.HookContainer;
import gloomyfolken.hooklib.api.InjectionPoint;
import gloomyfolken.hooklib.common.AsmHelper;
import gloomyfolken.hooklib.common.SafeClassWriter;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.Loader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

@HookContainer(modid = ModTitle.hooklib_modid)
public class ScannerClassTransformer implements IClassTransformer {

    private static ScannerClassTransformer instance;

    public ScannerClassTransformer() {
        instance = this;
    }

    @Hook(at = @At(point = InjectionPoint.HEAD))
    public static void injectData(Loader loader, Object... data) {
        ClassLoader classLoader = ScannerClassTransformer.class.getClassLoader();
        if (classLoader instanceof LaunchClassLoader) {
            try {
                List<IClassTransformer> transformers = (List<IClassTransformer>) LaunchClassLoader.class.getDeclaredField("transformers").get(classLoader);
                transformers.remove(instance);//Помещаем трансформер хуклибы в конец списка, чтобы он применялся после DeobfuscationTransformer
                transformers.add(instance);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("HookLib was not loaded by LaunchClassLoader. Hooks will not be injected.");
        }
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        //System.out.println("transform " + name.equals(transformedName) + " name " + name + ", transformedName " + transformedName);

        ClassNode classNode = AsmHelper.classNodeOf(basicClass, ClassReader.SKIP_FRAMES);

        //System.out.println("methods " + classNode.methods.stream().map(m -> m.name).collect(Collectors.toList()));


        ClassWriter classWriter = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
}
