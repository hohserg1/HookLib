package gloomyfolken.hooklib.minecraft;

import com.google.common.collect.ImmutableMultimap;
import gloomyfolken.hooklib.api.*;
import gloomyfolken.hooklib.common.AsmHelper;
import gloomyfolken.hooklib.common.advanced.tree.AdvancedClassNode;
import gloomyfolken.hooklib.common.advanced.tree.AdvancedFieldNode;
import gloomyfolken.hooklib.common.advanced.tree.AdvancedMethodNode;
import gloomyfolken.hooklib.common.annotations.AnnotationMap;
import gloomyfolken.hooklib.common.model.field.lens.AsmLens;
import gloomyfolken.hooklib.common.model.method.hook.Anchor;
import gloomyfolken.hooklib.common.model.method.hook.AsmHook;
import gloomyfolken.hooklib.common.model.method.hook.HookParameter;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModClassLoader;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.Type;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
                        .flatMap(maybeMapper(fn -> parseLens(classNode, fn))))
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
                        .flatMap(maybeMapper(mn -> parseHook(classNode, mn))))
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

    private static AsmLens parseLens(AdvancedClassNode classNode, AdvancedFieldNode fn) throws IllegalHookException {
        HookLens annotation = fn.annotations.get(HookLens.class).value();
        String targetFieldName = annotation.name().isEmpty() ? fn.name : annotation.name();

        checkValid(fn.desc.equals(Type.getDescriptor(Lens.class)), "Hook lens must be Lens<O,F>");

        String[] typeParameters = fn.signature.substring(fn.signature.indexOf('<') + 1, fn.signature.lastIndexOf('>')).split(";");

        String targetClassName = Type.getType(typeParameters[0] + ";").getClassName();
        String targetFieldTypeName = Type.getType(typeParameters[1] + ";").getClassName();

        return new AsmLens(classNode.name, fn.name, targetClassName, targetFieldName, targetFieldTypeName, null, annotation.boxing(), annotation.createField());
    }

    private static AsmHook parseHook(AdvancedClassNode classNode, AdvancedMethodNode methodNode) throws IllegalHookException {
        Hook hook = methodNode.annotations.get(Hook.class).value();
        At at = hook.at();

        String targetMethodName = hook.targetMethod().isEmpty() ? methodNode.name : hook.targetMethod();

        Type methodType = Type.getMethodType(methodNode.desc);
        Type[] argumentTypes = methodType.getArgumentTypes();

        checkValid(argumentTypes.length > 0, "Hook method must have one or more arguments");

        String targetClassName = argumentTypes[0].getClassName();

        List<HookParameter> hookMethodParameters = new ArrayList<>();
        List<Type> targetMethodParameters = new ArrayList<>();

        int currentParameterId = 1;
        Type targetMethodReturnType = null;

        for (int i = 1; i < argumentTypes.length; i++) {
            Type argType = argumentTypes[i];
            AnnotationMap parameterAnnotation = methodNode.parameterAnnotations.get(i);
            if (parameterAnnotation.contains(Hook.LocalVariable.class))
                hookMethodParameters.add(new HookParameter(argType, parameterAnnotation.get(Hook.LocalVariable.class).<Hook.LocalVariable>value().value()));
            else if (parameterAnnotation.contains(Hook.ReturnValue.class)) {
                hookMethodParameters.add(new HookParameter(argType, -1));
                targetMethodReturnType = argType;
            } else {
                hookMethodParameters.add(new HookParameter(argType, currentParameterId));
                targetMethodParameters.add(argType);
            }
        }


        return new AsmHook(targetClassName, targetMethodName, classNode.name, methodNode.name,
                Collections.unmodifiableList(hookMethodParameters), Collections.unmodifiableList(targetMethodParameters), targetMethodReturnType,
                hook.createMethod(), hook.isMandatory(), hook.returnCondition(), hook.priority(),
                new Anchor(at.point(), at.shift(), at.target(), at.ordinal()));
    }

    public static void checkValid(boolean ok, String error) throws IllegalHookException {
        if (!ok)
            throw new IllegalHookException(error);
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
        B apply(A a) throws Exception;
    }

    private static <A, B> Function<A, Stream<B>> maybeMapper(ThrowableFunction<A, B> openInputStream) {
        return a -> {
            try {
                return Stream.of(openInputStream.apply(a));
            } catch (Exception e) {
                e.printStackTrace();
                return Stream.empty();
            }
        };
    }

}
