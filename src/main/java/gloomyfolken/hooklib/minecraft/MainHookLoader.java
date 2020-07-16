package gloomyfolken.hooklib.minecraft;

import com.google.common.collect.ImmutableMultimap;
import gloomyfolken.hooklib.common.advanced.tree.AdvancedClassNode;
import gloomyfolken.hooklib.common.model.field.lens.AsmLens;
import gloomyfolken.hooklib.common.model.method.hook.AsmHook;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static gloomyfolken.hooklib.minecraft.HookContainerIndexer.*;

public class MainHookLoader implements IFMLLoadingPlugin {

    public final ImmutableMultimap<String, AsmHook> hooks;
    public final ImmutableMultimap<String, AsmLens> lenses;

    public MainHookLoader() {
        preloadUsedClasses();
        System.out.println("Start hooks indexing");
        List<AdvancedClassNode> hookContainers = findHookContainers();
        hooks = findHooks(hookContainers);
        lenses = findLenses(hookContainers);
        System.out.println("End hooks indexing");
    }

    private void preloadUsedClasses() {
        PreLoader.getClassesRequiredByTransformer(getASMTransformerClass())
                .forEach(className -> {
                    try {
                        System.out.println("Preloaded for HookLib transformer " + className);
                        Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{ScannerClassTransformer.class.getName()};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        System.out.println(data);

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
