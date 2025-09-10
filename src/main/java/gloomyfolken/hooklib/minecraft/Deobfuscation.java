package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.helper.*;

import java.io.*;
import java.util.*;

public enum Deobfuscation {
    instance;

    private Map<String, String> mcpToSrgMethods = new HashMap<>();
    private Map<String, String> mcpToSrgFields = new HashMap<>();

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

    public String obfMethod(String deobfName) {
        return mcpToSrgMethods.getOrDefault(deobfName, deobfName);
    }

    public String obfField(String deobfName) {
        return mcpToSrgFields.getOrDefault(deobfName, deobfName);
    }

    private Map<String, String> loadMethodNames(String fileName) throws IOException {
        InputStream resourceStream = getClass().getResourceAsStream(fileName);
        if (resourceStream == null) throw new IOException("Methods dictionary not found");
        DataInputStream input = new DataInputStream(new BufferedInputStream(resourceStream));
        int numMethods = input.readInt();
        Map<String, String> map = new HashMap<>(numMethods);
        for (int i = 0; i < numMethods; i++) {
            map.put(input.readUTF(), input.readUTF());
        }
        input.close();
        return map;
    }
}
