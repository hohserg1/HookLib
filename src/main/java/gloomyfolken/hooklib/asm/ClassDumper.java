package gloomyfolken.hooklib.asm;

import gloomyfolken.hooklib.helper.Logger;
import gloomyfolken.hooklib.minecraft.Config;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public enum ClassDumper {
    instance;

    ClassDumper() {
        if (Config.instance.dumpChangedClasses)
            if (Config.instance.dumpLocation.exists()) {
                try {
                    FileUtils.deleteDirectory(Config.instance.dumpLocation);
                } catch (IOException e) {
                    Logger.instance.error("Unable to delete previous hooked classes dump folder");
                    e.printStackTrace();
                }
            }
    }

    public void dumpClass(String className, byte[] bytecode) {
        if (!Config.instance.dumpChangedClasses)
            return;
        File classLocation = new File(Config.instance.dumpLocation, className.replace('.', '/') + ".class");
        classLocation.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(classLocation)) {
            if (Config.instance.logDumpedClasses)
                Logger.instance.info("Saving hooked class " + className + " to " + classLocation);
            fos.write(bytecode);
        } catch (IOException e) {
            Logger.instance.warning("Unable to save hooked class " + className);
            e.printStackTrace();
        }
    }

}
