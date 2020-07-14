package gloomyfolken.hooklib.common;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import static org.objectweb.asm.Opcodes.ASM5;

public class AsmHelper {

    public static ClassNode classNodeOf(byte[] bytes) {
        ClassReader classReader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode(ASM5);
        classReader.accept(classNode, ClassReader.SKIP_FRAMES);
        return classNode;
    }
}
