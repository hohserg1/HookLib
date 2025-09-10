package gloomyfolken.hooklib.minecraft;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import gloomyfolken.hooklib.api.FieldLens;
import gloomyfolken.hooklib.api.HookContainer;
import gloomyfolken.hooklib.api.OnExpression;
import gloomyfolken.hooklib.api.PrivateClass;
import gloomyfolken.hooklib.asm.HookClassTransformer;
import gloomyfolken.hooklib.asm.HookContainerParser;
import gloomyfolken.hooklib.asm.injections.AsmInjection;
import gloomyfolken.hooklib.helper.KeepHookLibLastList;
import gloomyfolken.hooklib.helper.Logger;
import gloomyfolken.hooklib.helper.annotation.AnnotationMap;
import gloomyfolken.hooklib.helper.annotation.AnnotationUtils;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModClassLoader;
import net.minecraftforge.fml.relauncher.CoreModManager;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.*;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static gloomyfolken.hooklib.helper.SideOnlyUtils.isValidSide;
import static org.objectweb.asm.ClassReader.SKIP_CODE;
import static org.objectweb.asm.Opcodes.ASM5;

public class MainHookLoader extends HookLoader {

    private boolean transformersListReplaced = false;

    @Override
    public String[] getASMTransformerClass() {
        if (!transformersListReplaced) {
            transformersListReplaced = true;
            ClassLoader classLoader = MainHookLoader.class.getClassLoader();
            if (classLoader instanceof LaunchClassLoader) {
                try {
                    Field field = LaunchClassLoader.class.getDeclaredField("transformers");
                    field.setAccessible(true);
                    List<IClassTransformer> originalList = (List<IClassTransformer>) field.get(classLoader);
                    List<IClassTransformer> replacementList = new KeepHookLibLastList<>(originalList);
                    field.set(classLoader, replacementList);
                } catch (NoSuchFieldException possibleFine) {

                } catch (Throwable e) {
                    throw new RuntimeException("unexpected exception while hooking up transformers collection", e);
                }
            } else {
                throw new IllegalStateException("HookLib was not loaded by LaunchClassLoader");
            }
        }

        return new String[]{HookClassTransformer.class.getName()};
    }

    protected void registerHooks() {
        Multimap<Class<? extends Annotation>, ClassNode> hookAnnotatedClasses = findHookAnnotatedClasses();

        HookContainerParser parser = new HookContainerParser(hookAnnotatedClasses.get(PrivateClass.class));

        HookClassTransformer.registerAllHooks(parser.makePrivateClassImageHooks());

        HookClassTransformer.registerAllHooks(
            hookAnnotatedClasses.get(HookContainer.class).stream()
                .flatMap(parser::parseHooks)
                .distinct()
                .collect(Multimaps.toMultimap(AsmInjection::getTargetClassName, Function.identity(), ArrayListMultimap::create))
        );
    }

    private Multimap<Class<? extends Annotation>, ClassNode> findHookAnnotatedClasses() {
        List<File> jarCandidates = new ArrayList<>(10);
        List<File> classCandidates = new ArrayList<>(100);
        Multimap<Class<? extends Annotation>, ClassNode> result = Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);

        addFromModsDir(jarCandidates, new File("./mods/"));
        addFromModsDir(jarCandidates, new File("./mods/" + ForgeVersion.mcVersion));


        if (Config.instance.useClasspathCandidates)
            addFromClasspath(jarCandidates, classCandidates);

        Set<File> jarWithHooks = new HashSet<>();

        for (File jar : jarCandidates)
            try {
                Logger.instance.info("Finding hooks in jar: " + jar);
                ZipFile zipFile = new ZipFile(jar);

                Enumeration<? extends ZipEntry> entries = zipFile.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && entry.getName().endsWith(".class"))
                        try (InputStream is = zipFile.getInputStream(entry)) {
                            if (is != null) {
                                if (findHooksInStream(result, is)) {
                                    jarWithHooks.add(jar);
                                }
                            }
                        } catch (Throwable e) {
                            if (e instanceof IllegalArgumentException &&
                                e.getStackTrace()[0].getClassName().equals(ClassReader.class.getName()) &&
                                e.getStackTrace()[0].getMethodName().equals("<init>")) {
                                Logger.instance.error("Failed to parse java9+ class " + jar + "#" + entry.getName());
                            } else
                                Logger.instance.error("Failed to parse class " + jar + "#" + entry.getName(), e);
                        }
                }
            } catch (Throwable e) {
                Logger.instance.error("Failed to parse jar " + jar);
                e.printStackTrace();
            }

        for (File classFile : classCandidates)
            try (FileInputStream is = FileUtils.openInputStream(classFile)) {
                findHooksInStream(result, is);
            } catch (IOException e) {
                Logger.instance.error("Failed to parse class " + classFile, e);
            }

        for (File jar : jarWithHooks) {
            Logger.instance.info("Jar contains hooks, adding to classpath: " + jar);
            try {
                ((LaunchClassLoader) getClass().getClassLoader()).addURL(jar.toURI().toURL());
                CoreModManager.getReparseableCoremods().add(jar.getName());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            if (HookLibPlugin.getObfuscated())
                CoreModManager.getReparseableCoremods().add(jar.getName());
        }

        return result;
    }

    private void addFromClasspath(List<File> jarCandidates, List<File> classCandidates) {
        ModClassLoader modClassLoader = Loader.instance().getModClassLoader();

        File[] minecraftSources = modClassLoader.getParentSources();

        for (File source : minecraftSources) {
            if (source.isFile()) {
                jarCandidates.add(source);
            } else if (source.isDirectory()) {
                Collection<File> classFiles = FileUtils.listFiles(source, new String[]{"class"}, true);
                classCandidates.addAll(classFiles);
            }
        }
    }

    private void addFromModsDir(List<File> jarCandidates, File folder) {

        File[] jarFiles = folder.listFiles(pathname -> pathname.getName().endsWith(".jar"));

        if (jarFiles != null)
            jarCandidates.addAll(Arrays.asList(jarFiles));
    }

    private boolean findHooksInStream(Multimap<Class<? extends Annotation>, ClassNode> result, InputStream stream) throws IOException {
        ClassNode classNode = new ClassNode(ASM5);
        ClassReader classReader = new ClassReader(stream);
        classReader.accept(classNode, SKIP_CODE);
        AnnotationMap annotationMap = AnnotationUtils.annotationOf(classNode);
        if (annotationMap.contains(HookContainer.class) && isValidSide(annotationMap)) {
            if (needToParseFully(classNode)) {
                classNode = new ClassNode(ASM5);
                classReader.accept(classNode, 0);
            }
            result.put(HookContainer.class, classNode);
            return true;
        }
        if (annotationMap.contains(PrivateClass.class) && isValidSide(annotationMap)) {
            result.put(PrivateClass.class, classNode);
        }
        return false;
    }

    private boolean needToParseFully(ClassNode classNode) {
        return haveExpressionHooks(classNode) || haveCreationFieldLenses(classNode);
    }

    private boolean haveExpressionHooks(ClassNode classNode) {
        return classNode.methods.stream().map(AnnotationUtils::annotationOf).anyMatch(a -> a.contains(OnExpression.class));
    }

    private boolean haveCreationFieldLenses(ClassNode classNode) {
        return classNode.fields.stream().map(AnnotationUtils::annotationOf).anyMatch(a -> a.contains(FieldLens.class) && a.get(FieldLens.class).createField());
    }
}
