package gloomyfolken.hooklib.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

public class AsmUtils {
    private static Set<Integer> returnOpcodes = new HashSet<>(Arrays.asList(
            IRETURN,
            LRETURN,
            FRETURN,
            DRETURN,
            ARETURN,
            RETURN
    ));

    public static boolean isStatic(MethodNode methodNode) {
        return (methodNode.access & Opcodes.ACC_STATIC) != 0;
    }

    public static boolean isPublic(MethodNode methodNode) {
        return (methodNode.access & Opcodes.ACC_PUBLIC) != 0;
    }

    public static boolean isReturn(AbstractInsnNode n) {
        return returnOpcodes.contains(n.getOpcode());
    }

    public static boolean isPatternSensitive(AbstractInsnNode n) {
        return !(n instanceof LineNumberNode) && !(n instanceof FrameNode) && !(n instanceof LabelNode);
    }
}
