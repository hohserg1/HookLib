package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.common.AsmHelper;
import gloomyfolken.hooklib.common.ClassMetadataReader;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.objectweb.asm.tree.AbstractInsnNode;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class MainHookLoader implements IFMLLoadingPlugin {

    public MainHookLoader() {
        putClassesToFML();
        //System.out.println(DeobfHelper.class);

    }

    private void putClassesToFML() {
        Arrays.stream(getASMTransformerClass())
                .flatMap(className -> {
                    try {
                        return Stream.of(ClassMetadataReader.instance.getClassData(className));
                    } catch (IOException e) {
                        e.printStackTrace();
                        return Stream.empty();
                    }
                })
                .map(AsmHelper::classNodeOf)
                .flatMap(cn ->
                        cn.methods.stream().flatMap(mn -> {
                            Stream<AbstractInsnNode> instructions = StreamSupport.stream(
                                    Spliterators.spliteratorUnknownSize(mn.instructions.iterator(), Spliterator.ORDERED),
                                    false);
                            return instructions.flatMap(in -> {
                                try {
                                    return Stream.of((String) in.getClass().getDeclaredField("owner").get(in));
                                } catch (NoSuchFieldException | IllegalAccessException e) {
                                    return Stream.empty();
                                }
                            });
                        })
                )
                .map(claccName -> claccName.replace('/', '.'))
                .collect(Collectors.toSet())
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
