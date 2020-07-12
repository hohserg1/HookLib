package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.asm.HookContainer;
import gloomyfolken.hooklib.config.Config;
import gloomyfolken.hooklib.experimental.utils.annotation.AnnotationMap;
import gloomyfolken.hooklib.experimental.utils.annotation.AnnotationUtils;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModClassLoader;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static gloomyfolken.hooklib.experimental.utils.SideOnlyUtils.isValidSide;
import static org.objectweb.asm.ClassReader.SKIP_CODE;
import static org.objectweb.asm.Opcodes.ASM5;

public class MainHookLoader extends HookLoader {

    // включает саму HookLib'у. Делать это можно только в одном из HookLoader'ов.
    // При желании, можно включить gloomyfolken.hooklib.minecraft.HookLibPlugin и не указывать здесь это вовсе.
    @Override
    public String[] getASMTransformerClass() {
        return new String[]{PrimaryClassTransformer.class.getName()};
    }

    @Override
    public void registerHooks() {
        Config.loadConfig();
        findHookContainers().forEach(i -> {
            String containerName = i.getLeft();
            if (Config.enableTestHooks || !containerName.equals("gloomyfolken.hooklib.example.TestHooks"))
                getTransformer().registerHookContainer(containerName, i.getRight());
        });
    }

    private List<Pair<String, byte[]>> findHookContainers() {

        List<File> jarCandidates = new ArrayList<>(10);
        List<File> classCandidates = new ArrayList<>(100);
        List<Pair<String, byte[]>> result = new ArrayList<>(1);

        if (Config.useModsDirCandidates)
            addFromModsDir(jarCandidates);

        if (Config.useClasspathCandidates)
            addFromClasspath(jarCandidates, classCandidates);

        for (File jar : jarCandidates)
            try {
                ZipFile zipFile = new ZipFile(jar);

                Enumeration<? extends ZipEntry> entries = zipFile.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && entry.getName().endsWith(".class"))
                        findHooksInStream(result, zipFile.getInputStream(entry));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        for (File classFile : classCandidates)
            try {
                findHooksInStream(result, FileUtils.openInputStream(classFile));
            } catch (IOException e) {
                e.printStackTrace();
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

    private void addFromModsDir(List<File> jarCandidates) {
        File modsDir = new File("./mods/");

        File[] jarFiles = modsDir.listFiles(pathname -> pathname.getName().endsWith(".jar"));

        if (jarFiles != null)
            jarCandidates.addAll(Arrays.asList(jarFiles));
    }

    private void findHooksInStream(List<Pair<String, byte[]>> result, InputStream stream) throws IOException {
        byte[] targetArray = IOUtils.toByteArray(stream);
        ClassNode classNode = new ClassNode(ASM5);
        new ClassReader(targetArray).accept(classNode, SKIP_CODE);
        AnnotationMap annotationMap = AnnotationUtils.annotationOf(classNode);
        if(annotationMap.contains(HookContainer.class) && isValidSide(annotationMap))
            result.add(Pair.of(classNode.name.replace('/', '.'), targetArray));
    }
}
