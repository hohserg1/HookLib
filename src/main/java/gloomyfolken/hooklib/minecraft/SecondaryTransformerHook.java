package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.api.Hook;
import gloomyfolken.hooklib.api.HookContainer;
import gloomyfolken.hooklib.api.OnBegin;
import gloomyfolken.hooklib.asm.HookClassTransformer;
import net.minecraftforge.fml.common.Loader;

@HookContainer
public class SecondaryTransformerHook {

    /**
     * Register MinecraftClassTransformer as last transformer for apply it after all others transformers
     */
    @Hook
    @OnBegin
    public static void injectData(Loader loader, Object... data) {
        HookClassTransformer.last.stage = new MinecraftClassTransformer();
    }
}
