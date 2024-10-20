package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.api.Hook;
import gloomyfolken.hooklib.api.OnBegin;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.Loader;


public class SecondaryTransformerHook {

    /**
     * Register MinecraftClassTransformer as last transformer for apply it after all others transformers
     */
    @Hook
    @OnBegin
    public static void injectData(Loader loader, Object... data) {
        ClassLoader classLoader = SecondaryTransformerHook.class.getClassLoader();
        if (classLoader instanceof LaunchClassLoader) {
            ((LaunchClassLoader) classLoader).registerTransformer(MinecraftClassTransformer.class.getName());
        } else {
            System.out.println("HookLib was not loaded by LaunchClassLoader. Hooks will not be injected.");
        }
    }

}
