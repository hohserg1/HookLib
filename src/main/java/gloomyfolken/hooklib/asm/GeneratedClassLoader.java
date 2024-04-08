package gloomyfolken.hooklib.asm;

import java.util.HashMap;
import java.util.Map;

public class GeneratedClassLoader extends ClassLoader {

    public static GeneratedClassLoader instance = new GeneratedClassLoader();

    private GeneratedClassLoader() {
        super(GeneratedClassLoader.class.getClassLoader());
    }

    public static Class<?> initClass(String name, byte[] bytecode) throws ClassNotFoundException {
        instance.addClass(name, bytecode);
        return Class.forName(name, true, instance);
    }

    private Map<String, byte[]> forLoad = new HashMap<>();

    public void addClass(String name, byte[] bytecode) {
        forLoad.put(name, bytecode);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytecode = forLoad.get(name);
        if (bytecode != null)
            return defineClass(name, bytecode, 0, bytecode.length);

        return super.findClass(name);
    }
}
