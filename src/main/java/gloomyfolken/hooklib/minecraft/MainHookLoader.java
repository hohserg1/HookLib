package gloomyfolken.hooklib.minecraft;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderException;
import cpw.mods.fml.common.ModClassLoader;
import gloomyfolken.hooklib.asm.HookCheckClassVisitor;
import gloomyfolken.hooklib.config.Config;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
                registerHookContainer(containerName, i.getRight());
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

    public File[] getParentSources() {
        List<URL> urls = ((LaunchClassLoader) getClass().getClassLoader()).getSources();
        File[] sources = new File[urls.size()];
        try {
            for (int i = 0; i < urls.size(); i++) {
                sources[i] = new File(urls.get(i).toURI());
            }
            return sources;
        } catch (URISyntaxException e) {
            FMLLog.log(Level.ERROR, e, "Unable to process our input to locate the minecraft code");
            throw new LoaderException(e);
        }
    }

    private void addFromClasspath(List<File> jarCandidates, List<File> classCandidates) {
        File[] minecraftSources = getParentSources();

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
        new ClassReader(targetArray)
                .accept(new HookCheckClassVisitor(
                        className -> result.add(Pair.of(className, targetArray))), 0);
    }
}
