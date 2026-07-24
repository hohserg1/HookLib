package gloomyfolken.hooklib.minecraft;

import com.google.common.collect.*;
import gloomyfolken.hooklib.api.*;
import gloomyfolken.hooklib.asm.*;
import gloomyfolken.hooklib.asm.injections.*;
import gloomyfolken.hooklib.helper.*;
import gloomyfolken.hooklib.helper.annotation.*;
import net.minecraft.launchwrapper.*;
import net.minecraftforge.common.*;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.relauncher.*;
import org.apache.commons.io.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.function.*;
import java.util.zip.*;

import static gloomyfolken.hooklib.helper.SideOnlyUtils.*;
import static org.objectweb.asm.ClassReader.*;
import static org.objectweb.asm.Opcodes.*;

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
        Set<File> jarCandidates = new LinkedHashSet<>(10);
        Set<File> classCandidates = new LinkedHashSet<>(100);
        Multimap<Class<? extends Annotation>, ClassNode> result = Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);

        addFromModsDir(jarCandidates, new File("./mods/"));
        addFromModsDir(jarCandidates, new File("./mods/" + ForgeVersion.mcVersion));


        if (Config.instance.useClasspathCandidates)
            addFromClasspath(jarCandidates, classCandidates);

        Set<File> jarWithHooks = new HashSet<>();

        for (File jar : jarCandidates) {
            try {
                Logger.instance.info("Finding hooks in jar: " + jar);
                ZipFile zipFile = new ZipFile(jar);

                Enumeration<? extends ZipEntry> entries = zipFile.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
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
                }
            } catch (Throwable e) {
                Logger.instance.error("Failed to parse jar " + jar);
                e.printStackTrace();
            }
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

    private void addFromClasspath(Set<File> jarCandidates, Set<File> classCandidates) {
        ModClassLoader modClassLoader = Loader.instance().getModClassLoader();

        File[] minecraftSources = modClassLoader.getParentSources();

        for (File source : minecraftSources) {
            if (source.isFile()) {
                if (!KnownLibrariesZeroHooks.libsNames.contains(source.getName()) && !source.getAbsolutePath().startsWith(KnownLibrariesZeroHooks.jrePrefix)) {
                    jarCandidates.add(source);
                }
            } else if (source.isDirectory()) {
                Collection<File> classFiles = FileUtils.listFiles(source, new String[]{"class"}, true);
                classCandidates.addAll(classFiles);
            }
        }
    }

    private void addFromModsDir(Set<File> jarCandidates, File folder) {

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
