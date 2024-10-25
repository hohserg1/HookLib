package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.helper.Logger;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public enum Deobfuscation {
    instance;

    private Map<Integer, String> methodNames = new HashMap<>();
    private Map<Integer, String> fieldNames = new HashMap<>();

    Deobfuscation() {
        if (HookLibPlugin.getObfuscated()) {
            try {
                long timeStart = System.currentTimeMillis();
                methodNames = loadMethodNames("/methods.bin");
                fieldNames = loadMethodNames("/fields.bin");
                long time = System.currentTimeMillis() - timeStart;
                Logger.instance.debug("Mappings dictionary loaded in " + time + " ms");
            } catch (IOException e) {
                Logger.instance.error("Can not load obfuscated method names", e);
            }
        }
    }

    public String deobfMethod(String name) {
        return methodNames.getOrDefault(getMemberId("func_", name), name);
    }

    public String deobfField(String name) {
        return fieldNames.getOrDefault(getMemberId("field_", name), name);
    }

    private int getMemberId(String prefix, String srgName) {
        if (srgName.startsWith(prefix)) {
            int first = srgName.indexOf('_');
            int second = srgName.indexOf('_', first + 1);
            return Integer.valueOf(srgName.substring(first + 1, second));
        } else {
            return -1;
        }
    }

    private HashMap<Integer, String> loadMethodNames(String fileName) throws IOException {
        InputStream resourceStream = getClass().getResourceAsStream(fileName);
        if (resourceStream == null) throw new IOException("Methods dictionary not found");
        DataInputStream input = new DataInputStream(new BufferedInputStream(resourceStream));
        int numMethods = input.readInt();
        HashMap<Integer, String> map = new HashMap<Integer, String>(numMethods);
        for (int i = 0; i < numMethods; i++) {
            map.put(input.readInt(), input.readUTF());
        }
        input.close();
        return map;
    }
}
