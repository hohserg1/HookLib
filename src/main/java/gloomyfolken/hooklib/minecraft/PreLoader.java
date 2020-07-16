package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.common.AsmHelper;
import gloomyfolken.hooklib.common.ClassMetadataReader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PreLoader {
    static Set<String> getClassesRequiredByTransformer(String[] transformerClass) {
        return Arrays.stream(transformerClass)
                .flatMap(className -> {
                    try {
                        return Stream.of(ClassMetadataReader.instance.getClassData(className));
                    } catch (IOException e) {
                        e.printStackTrace();
                        return Stream.empty();
                    }
                })
                .map(bytes -> AsmHelper.classNodeOf(bytes, ClassReader.SKIP_FRAMES))
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
                .map(className -> className.replace('/', '.'))
                .collect(Collectors.toSet());
    }
}
