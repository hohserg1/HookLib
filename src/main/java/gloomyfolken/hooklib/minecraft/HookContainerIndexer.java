package gloomyfolken.hooklib.minecraft;

import com.google.common.collect.ImmutableMultimap;
import gloomyfolken.hooklib.api.*;
import gloomyfolken.hooklib.common.AsmHelper;
import gloomyfolken.hooklib.common.advanced.tree.AdvancedClassNode;
import gloomyfolken.hooklib.common.advanced.tree.AdvancedFieldNode;
import gloomyfolken.hooklib.common.advanced.tree.AdvancedMethodNode;
import gloomyfolken.hooklib.common.model.field.lens.AsmLens;
import gloomyfolken.hooklib.common.model.method.hook.Anchor;
import gloomyfolken.hooklib.common.model.method.hook.AsmHook;
import jdk.internal.org.objectweb.asm.Type;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModClassLoader;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import static java.util.stream.Collectors.toList;
import static org.objectweb.asm.ClassReader.SKIP_CODE;

public class HookContainerIndexer {
    public static ImmutableMultimap<String, AsmLens> findLenses(List<AdvancedClassNode> hookContainers) {
        return hookContainers.stream()
                .flatMap(classNode -> classNode.fields.stream()
                        .filter(fn -> fn.annotations.contains(HookLens.class))
                        .map(fn -> parseLens(classNode, fn)))
                .collect(
                        ImmutableMultimap::<String, AsmLens>builder,
                        (builder, ah) -> builder.put(ah.targetClassName, ah),
                        (builder1, builder2) -> builder1.putAll(builder2.build())
                ).build();

    }

    public static ImmutableMultimap<String, AsmHook> findHooks(List<AdvancedClassNode> hookContainers) {
        return hookContainers.stream()
                .flatMap(classNode -> classNode.methods.stream()
                        .filter(mn -> mn.annotations.contains(Hook.class))
                        .map(mn -> parseHook(classNode, mn)))
                .collect(
                        ImmutableMultimap::<String, AsmHook>builder,
                        (builder, ah) -> builder.put(ah.targetClassName, ah),
                        (builder1, builder2) -> builder1.putAll(builder2.build())
                ).build();
    }

    public static List<AdvancedClassNode> findHookContainers() {
        return getClasses()
                .filter(classNode -> classNode.annotations.contains(HookContainer.class))
                .filter(cn -> !cn.annotations.contains(SideOnly.class) || cn.annotations.get(SideOnly.class).value() == FMLLaunchHandler.side())
                .collect(toList());
    }

    private static AsmLens parseLens(AdvancedClassNode classNode, AdvancedFieldNode fn) {
        HookLens annotation = fn.annotations.get(HookLens.class).value();
        String targetFieldName = annotation.name().isEmpty() ? fn.name : annotation.name();
        assert (fn.desc.equals(Type.getDescriptor(Lens.class)));//todo: replace assert by more good exception

        String[] typeParameters = fn.signature.substring(fn.signature.indexOf('<') + 1, fn.signature.lastIndexOf('>')).split(";");

        String targetClassName = Type.getType(typeParameters[0] + ";").getClassName();
        String targetFieldTypeName = Type.getType(typeParameters[1] + ";").getClassName();

        System.out.println("test " + targetClassName + "#" + targetFieldName + ": " + targetFieldTypeName);
        return new AsmLens(classNode.name, fn.name, targetClassName, targetFieldName, targetFieldTypeName, null, annotation.boxing(), annotation.createField());
    }

    private static AsmHook parseHook(AdvancedClassNode classNode, AdvancedMethodNode methodNode) {
        Hook value = methodNode.annotations.get(Hook.class).value();
        At at = value.at();
        return new AsmHook("", null, null, null, null, null, value.createMethod(), value.isMandatory(), value.returnCondition(), value.priority(),
                new Anchor(at.point(), at.shift(), at.target(), at.ordinal()));
    }

    private static Stream<AdvancedClassNode> getClasses() {
        File modsDir = new File("./mods/");

        File[] jarFiles = modsDir.listFiles(pathname -> pathname.getName().endsWith(".jar"));


        ModClassLoader modClassLoader = Loader.instance().getModClassLoader();

        File[] minecraftSources = modClassLoader.getParentSources();

        Stream<File> jarCandidates = Stream.concat(
                (jarFiles != null ? Arrays.stream(jarFiles) : Stream.empty()),
                Arrays.stream(minecraftSources).filter(File::isFile)
        );

        Stream<File> classCandidates = Arrays.stream(minecraftSources)
                .filter(File::isDirectory)
                .flatMap(source -> FileUtils.listFiles(source, new String[]{"class"}, true).stream());

        Stream<byte[]> classesInJar = jarCandidates
                .flatMap(maybeMapper(ZipFile::new))
                .flatMap(zipFile -> zipFile.stream()
                        .filter(entry -> !entry.isDirectory() && entry.getName().endsWith(".class"))
                        .flatMap(maybeMapper(zipFile::getInputStream))
                        .flatMap(maybeMapper(IOUtils::toByteArray)));

        Stream<byte[]> classesInClasspath = classCandidates
                .flatMap(maybeMapper(FileUtils::openInputStream))
                .flatMap(maybeMapper(IOUtils::toByteArray));

        return Stream.concat(classesInJar, classesInClasspath).map(bytes -> AsmHelper.classNodeOf(bytes, SKIP_CODE));
    }

    private interface ThrowableFunction<A, B> {
        B apply(A a) throws IOException;
    }

    private static <A, B> Function<A, Stream<B>> maybeMapper(ThrowableFunction<A, B> openInputStream) {
        return a -> {
            try {
                return Stream.of(openInputStream.apply(a));
            } catch (IOException e) {
                e.printStackTrace();
                return Stream.empty();
            }
        };
    }

}
