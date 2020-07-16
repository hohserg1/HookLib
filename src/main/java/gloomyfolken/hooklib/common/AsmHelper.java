package gloomyfolken.hooklib.common;

import gloomyfolken.hooklib.common.advanced.tree.AdvancedClassNode;
import org.objectweb.asm.ClassReader;

public class AsmHelper {

    public static AdvancedClassNode classNodeOf(byte[] bytes, int flags) {
        ClassReader classReader = new ClassReader(bytes);
        AdvancedClassNode classNode = new AdvancedClassNode();
        classReader.accept(classNode, flags);
        return classNode;
    }
}
