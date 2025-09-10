package gloomyfolken.hooklib.minecraft;

import com.google.common.collect.*;
import gloomyfolken.hooklib.helper.*;

import java.io.*;
import java.util.*;

public enum Deobfuscation {
    instance;

    private SetMultimap<String, String> mcpToSrgMethods = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);
    private SetMultimap<String, String> mcpToSrgFields = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);

    Deobfuscation() {
        if (HookLibPlugin.getObfuscated()) {
            try {
                long timeStart = System.currentTimeMillis();
                mcpToSrgMethods = loadMethodNames("/methods.bin2");
                mcpToSrgFields = loadMethodNames("/fields.bin2");
                long time = System.currentTimeMillis() - timeStart;
                Logger.instance.debug("Mappings dictionary loaded in " + time + " ms");
            } catch (IOException e) {
                Logger.instance.error("Can not load obfuscated method names", e);
            }
        }
    }

    public Set<String> obfMethod(String deobfName) {
        return mcpToSrgMethods.get(deobfName);
    }

    public Set<String> obfField(String deobfName) {
        return mcpToSrgFields.get(deobfName);
    }

    private SetMultimap<String, String> loadMethodNames(String fileName) throws IOException {
        InputStream resourceStream = getClass().getResourceAsStream(fileName);
        if (resourceStream == null) throw new IOException("Methods dictionary not found");
        DataInputStream input = new DataInputStream(new BufferedInputStream(resourceStream));
        int numMethods = input.readInt();
        SetMultimap<String, String> map = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);
        for (int i = 0; i < numMethods; i++) {
            map.put(input.readUTF(), input.readUTF());
        }
        input.close();
        return map;
    }
}
