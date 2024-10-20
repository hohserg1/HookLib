package gloomyfolken.hooklib.asm;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

public class AsmUtils {
    public static BiMap<Type, Type> objectToPrimitive = ImmutableBiMap.<Type, Type>builder()
            .put(Type.getType(Void.class), VOID_TYPE)
            .put(Type.getType(Boolean.class), BOOLEAN_TYPE)
            .put(Type.getType(Character.class), CHAR_TYPE)
            .put(Type.getType(Byte.class), BYTE_TYPE)
            .put(Type.getType(Short.class), SHORT_TYPE)
            .put(Type.getType(Integer.class), INT_TYPE)
            .put(Type.getType(Float.class), FLOAT_TYPE)
            .put(Type.getType(Long.class), LONG_TYPE)
            .put(Type.getType(Double.class), DOUBLE_TYPE)
            .build();

    public static Map<Type, String> primitiveToUnboxingMethod = ImmutableBiMap.<Type, String>builder()
            .put(BOOLEAN_TYPE, "booleanValue")
            .put(CHAR_TYPE, "charValue")
            .put(BYTE_TYPE, "byteValue")
            .put(SHORT_TYPE, "shortValue")
            .put(INT_TYPE, "intValue")
            .put(FLOAT_TYPE, "floatValue")
            .put(LONG_TYPE, "longValue")
            .put(DOUBLE_TYPE, "doubleValue")
            .build();

    private static Set<Integer> returnOpcodes = new HashSet<>(Arrays.asList(
            IRETURN,
            LRETURN,
            FRETURN,
            DRETURN,
            ARETURN,
            RETURN
    ));

    public static boolean isStatic(MethodNode methodNode) {
        return isStatic(methodNode.access);
    }

    public static boolean isStatic(int access) {
        return (access & Opcodes.ACC_STATIC) != 0;
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
