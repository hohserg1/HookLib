package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.api.HookContainer;
import gloomyfolken.hooklib.helper.Logger;
import gloomyfolken.hooklib.helper.annotation.AnnotationMap;
import gloomyfolken.hooklib.helper.annotation.AnnotationUtils;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModClassLoader;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static gloomyfolken.hooklib.helper.SideOnlyUtils.isValidSide;
import static org.objectweb.asm.ClassReader.SKIP_CODE;
import static org.objectweb.asm.Opcodes.ASM5;

public class MainHookLoader extends HookLoader {
    @Override
    public String[] getASMTransformerClass() {
        return new String[]{PrimaryClassTransformer.class.getName()};
    }

    protected void registerHooks() {
        findHookContainers().forEach(classNode -> {
            String containerName = classNode.name.replace('/', '.');
            getTransformer().registerHookContainer(containerName, classNode);
        });
    }

    private List<ClassNode> findHookContainers() {

        List<File> jarCandidates = new ArrayList<>(10);
        List<File> classCandidates = new ArrayList<>(100);
        List<ClassNode> result = new ArrayList<>(1);

        if (Config.instance.useModsDirCandidates)
            addFromModsDir(jarCandidates);

        if (Config.instance.useClasspathCandidates)
            addFromClasspath(jarCandidates, classCandidates);

        for (File jar : jarCandidates)
            try {
                ZipFile zipFile = new ZipFile(jar);

                Enumeration<? extends ZipEntry> entries = zipFile.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && entry.getName().endsWith(".class"))
                        try (InputStream is = zipFile.getInputStream(entry)) {
                            if (is != null)
                                findHooksInStream(result, is);
                        } catch (Throwable e) {
                            if (e instanceof IllegalArgumentException &&
                                    e.getStackTrace()[0].getClassName().equals(ClassReader.class.getName()) &&
                                    e.getStackTrace()[0].getMethodName().equals("<init>")) {
                                Logger.instance.error("Failed to parse java9+ class " + jar + "#" + entry.getName());
                            } else
                                Logger.instance.error("Failed to parse class " + jar + "#" + entry.getName(), e);
                        }
                }
            } catch (IOException e) {
                Logger.instance.error("Failed to parse jar " + jar);
                e.printStackTrace();
            }

        for (File classFile : classCandidates)
            try (FileInputStream is = FileUtils.openInputStream(classFile)) {
                findHooksInStream(result, is);
            } catch (IOException e) {
                Logger.instance.error("Failed to parse class " + classFile, e);
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

    File modsDir = new File("./mods/");

    private void addFromModsDir(List<File> jarCandidates) {

        File[] jarFiles = modsDir.listFiles(pathname -> pathname.getName().endsWith(".jar"));

        if (jarFiles != null)
            jarCandidates.addAll(Arrays.asList(jarFiles));
    }

    private void findHooksInStream(List<ClassNode> result, InputStream stream) throws IOException {
        ClassNode classNode = new ClassNode(ASM5);
        new ClassReader(stream).accept(classNode, SKIP_CODE);
        AnnotationMap annotationMap = AnnotationUtils.annotationOf(classNode);
        if (annotationMap.contains(HookContainer.class) && isValidSide(annotationMap))
            result.add(classNode);
    }
}
