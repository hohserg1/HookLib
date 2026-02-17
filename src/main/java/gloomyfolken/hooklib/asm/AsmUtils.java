package gloomyfolken.hooklib.asm;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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

    public static Set<Type> allPrimitives = ImmutableSet.of(VOID_TYPE, BOOLEAN_TYPE, CHAR_TYPE, BYTE_TYPE, SHORT_TYPE, INT_TYPE, FLOAT_TYPE, LONG_TYPE, DOUBLE_TYPE);

    public static boolean isPrimitive(Type t) {
        return t.getSort() < 9;
    }

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

    public static boolean isArray(Type type) {
        return type.getSort() == 9;
    }

    public static boolean isObject(Type type) {
        return type.getSort() == 10;
    }

    public static Type mapBy(Type type, Function<String, String> mappings) {
        if (AsmUtils.isPrimitive(type)) {
            return type;
        }

        if (AsmUtils.isArray(type)) {
            if (AsmUtils.isPrimitive(type.getElementType())) {
                return type;
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < type.getDimensions(); i++) {
                    sb.append("[");
                }
                sb.append("L");
                sb.append(mapBy(type.getElementType(), mappings).getInternalName());
                sb.append(";");
                return Type.getType(sb.toString());
            }
        } else if (AsmUtils.isObject(type)) {
            String unmappedName = mappings.apply(type.getInternalName());
            return Type.getType("L" + unmappedName + ";");
        } else {
            throw new IllegalArgumentException("Can not map method type!");
        }
    }

    @Value
    @AllArgsConstructor
    public static class OpcodeDetails {
        public int consumeFromStack;
        public int putToStack;
        public Function<AbstractInsnNode, Type> resultType;

        public OpcodeDetails(int consumeFromStack, int putToStack, Type constantResultType) {
            this(consumeFromStack, putToStack, __ -> constantResultType);
        }
    }

    public static OpcodeDetails getOpcodeDetails(int opcode) {
        OpcodeDetails r = opcodeDetails.get(opcode);
        if (r == null)
            throw new IllegalArgumentException("Unsupported opcode type: " + opcode + ". Plz report to https://github.com/hohserg1/HookLib/issues");
        return r;
    }

    private static Map<Integer, OpcodeDetails> opcodeDetails = ImmutableMap.<Integer, OpcodeDetails>builder()
        .put(ACONST_NULL, new OpcodeDetails(0, 1, getType(Object.class)))
        .put(ICONST_M1, new OpcodeDetails(0, 1, INT_TYPE))
        .put(ICONST_0, new OpcodeDetails(0, 1, INT_TYPE))
        .put(ICONST_1, new OpcodeDetails(0, 1, INT_TYPE))
        .put(ICONST_2, new OpcodeDetails(0, 1, INT_TYPE))
        .put(ICONST_3, new OpcodeDetails(0, 1, INT_TYPE))
        .put(ICONST_4, new OpcodeDetails(0, 1, INT_TYPE))
        .put(ICONST_5, new OpcodeDetails(0, 1, INT_TYPE))
        .put(LCONST_0, new OpcodeDetails(0, 1, LONG_TYPE))
        .put(LCONST_1, new OpcodeDetails(0, 1, LONG_TYPE))
        .put(FCONST_0, new OpcodeDetails(0, 1, FLOAT_TYPE))
        .put(FCONST_1, new OpcodeDetails(0, 1, FLOAT_TYPE))
        .put(FCONST_2, new OpcodeDetails(0, 1, FLOAT_TYPE))
        .put(DCONST_0, new OpcodeDetails(0, 1, DOUBLE_TYPE))
        .put(DCONST_1, new OpcodeDetails(0, 1, DOUBLE_TYPE))
        .put(BIPUSH, new OpcodeDetails(0, 1, BYTE_TYPE))
        .put(SIPUSH, new OpcodeDetails(0, 1, SHORT_TYPE))
        .put(LDC, new OpcodeDetails(0, 1, i -> {
            Object cst = ((LdcInsnNode) i).cst;
            if (cst instanceof Type) {
                Type t = (Type) cst;
                int s = t.getSort();
                if (s == 10) {
                    return getType(Class.class);
                } else {
                    return s == 11 ? getType(String.class) : getType(Class.class);
                }
            } else if (cst instanceof Handle) {
                throw new IllegalArgumentException("unsupported ldc: " + cst);
            } else {
                return getType(cst.getClass());
            }
        }))
        .put(ILOAD, new OpcodeDetails(0, 1, INT_TYPE))
        .put(LLOAD, new OpcodeDetails(0, 1, LONG_TYPE))
        .put(FLOAD, new OpcodeDetails(0, 1, FLOAT_TYPE))
        .put(DLOAD, new OpcodeDetails(0, 1, DOUBLE_TYPE))
        .put(ALOAD, new OpcodeDetails(0, 1, getType(Object.class)))
        .put(IALOAD, new OpcodeDetails(2, 1, INT_TYPE))
        .put(LALOAD, new OpcodeDetails(2, 1, LONG_TYPE))
        .put(FALOAD, new OpcodeDetails(2, 1, FLOAT_TYPE))
        .put(DALOAD, new OpcodeDetails(2, 1, DOUBLE_TYPE))
        .put(AALOAD, new OpcodeDetails(2, 1, getType(Object.class)))
        .put(BALOAD, new OpcodeDetails(2, 1, BYTE_TYPE))
        .put(CALOAD, new OpcodeDetails(2, 1, CHAR_TYPE))
        .put(SALOAD, new OpcodeDetails(2, 1, SHORT_TYPE))
        .put(ISTORE, new OpcodeDetails(1, 0, INT_TYPE))
        .put(LSTORE, new OpcodeDetails(1, 0, LONG_TYPE))
        .put(FSTORE, new OpcodeDetails(1, 0, FLOAT_TYPE))
        .put(DSTORE, new OpcodeDetails(1, 0, DOUBLE_TYPE))
        .put(ASTORE, new OpcodeDetails(1, 0, getType(Object.class)))
        .put(IASTORE, new OpcodeDetails(3, 0, VOID_TYPE))
        .put(LASTORE, new OpcodeDetails(3, 0, VOID_TYPE))
        .put(FASTORE, new OpcodeDetails(3, 0, VOID_TYPE))
        .put(DASTORE, new OpcodeDetails(3, 0, VOID_TYPE))
        .put(AASTORE, new OpcodeDetails(3, 0, VOID_TYPE))
        .put(BASTORE, new OpcodeDetails(3, 0, VOID_TYPE))
        .put(CASTORE, new OpcodeDetails(3, 0, VOID_TYPE))
        .put(SASTORE, new OpcodeDetails(3, 0, VOID_TYPE))
        .put(POP, new OpcodeDetails(1, 0, VOID_TYPE))
        .put(POP2, new OpcodeDetails(1, 0, VOID_TYPE))
        .put(DUP, new OpcodeDetails(1, 2, i -> {
            throw new IllegalStateException("instruction have no easy predictable result type" + i);
        }))
        .put(DUP_X1, new OpcodeDetails(2, 3, i -> {
            throw new IllegalStateException("instruction have no easy predictable result type" + i);
        }))
        .put(DUP_X2, new OpcodeDetails(3, 4, i -> {
            throw new IllegalStateException("instruction have no easy predictable result type" + i);
        }))
        .put(DUP2, new OpcodeDetails(1, 2, i -> {
            throw new IllegalStateException("instruction have no easy predictable result type" + i);
        }))
        .put(DUP2_X1, new OpcodeDetails(2, 3, i -> {
            throw new IllegalStateException("instruction have no easy predictable result type" + i);
        }))
        .put(DUP2_X2, new OpcodeDetails(3, 4, i -> {
            throw new IllegalStateException("instruction have no easy predictable result type" + i);
        }))
        .put(SWAP, new OpcodeDetails(2, 2, i -> {
            throw new IllegalStateException("instruction have no easy predictable result type" + i);
        }))
        .put(IADD, new OpcodeDetails(2, 1, INT_TYPE))
        .put(LADD, new OpcodeDetails(2, 1, LONG_TYPE))
        .put(FADD, new OpcodeDetails(2, 1, FLOAT_TYPE))
        .put(DADD, new OpcodeDetails(2, 1, DOUBLE_TYPE))
        .put(ISUB, new OpcodeDetails(2, 1, INT_TYPE))
        .put(LSUB, new OpcodeDetails(2, 1, LONG_TYPE))
        .put(FSUB, new OpcodeDetails(2, 1, FLOAT_TYPE))
        .put(DSUB, new OpcodeDetails(2, 1, DOUBLE_TYPE))
        .put(IMUL, new OpcodeDetails(2, 1, INT_TYPE))
        .put(LMUL, new OpcodeDetails(2, 1, LONG_TYPE))
        .put(FMUL, new OpcodeDetails(2, 1, FLOAT_TYPE))
        .put(DMUL, new OpcodeDetails(2, 1, DOUBLE_TYPE))
        .put(IDIV, new OpcodeDetails(2, 1, INT_TYPE))
        .put(LDIV, new OpcodeDetails(2, 1, LONG_TYPE))
        .put(FDIV, new OpcodeDetails(2, 1, FLOAT_TYPE))
        .put(DDIV, new OpcodeDetails(2, 1, DOUBLE_TYPE))
        .put(IREM, new OpcodeDetails(2, 1, INT_TYPE))
        .put(LREM, new OpcodeDetails(2, 1, LONG_TYPE))
        .put(FREM, new OpcodeDetails(2, 1, FLOAT_TYPE))
        .put(DREM, new OpcodeDetails(2, 1, DOUBLE_TYPE))
        .put(INEG, new OpcodeDetails(1, 1, INT_TYPE))
        .put(LNEG, new OpcodeDetails(1, 1, LONG_TYPE))
        .put(FNEG, new OpcodeDetails(1, 1, FLOAT_TYPE))
        .put(DNEG, new OpcodeDetails(1, 1, DOUBLE_TYPE))
        .put(ISHL, new OpcodeDetails(2, 1, INT_TYPE))
        .put(LSHL, new OpcodeDetails(2, 1, LONG_TYPE))
        .put(ISHR, new OpcodeDetails(2, 1, INT_TYPE))
        .put(LSHR, new OpcodeDetails(2, 1, LONG_TYPE))
        .put(IUSHR, new OpcodeDetails(2, 1, INT_TYPE))
        .put(LUSHR, new OpcodeDetails(2, 1, LONG_TYPE))
        .put(IAND, new OpcodeDetails(2, 1, INT_TYPE))
        .put(LAND, new OpcodeDetails(2, 1, LONG_TYPE))
        .put(IOR, new OpcodeDetails(2, 1, INT_TYPE))
        .put(LOR, new OpcodeDetails(2, 1, LONG_TYPE))
        .put(IXOR, new OpcodeDetails(2, 1, INT_TYPE))
        .put(LXOR, new OpcodeDetails(2, 1, LONG_TYPE))
        .put(IINC, new OpcodeDetails(0, 0, VOID_TYPE))
        .put(I2L, new OpcodeDetails(1, 1, LONG_TYPE))
        .put(I2F, new OpcodeDetails(1, 1, FLOAT_TYPE))
        .put(I2D, new OpcodeDetails(1, 1, DOUBLE_TYPE))
        .put(L2I, new OpcodeDetails(1, 1, INT_TYPE))
        .put(L2F, new OpcodeDetails(1, 1, FLOAT_TYPE))
        .put(L2D, new OpcodeDetails(1, 1, DOUBLE_TYPE))
        .put(F2I, new OpcodeDetails(1, 1, INT_TYPE))
        .put(F2L, new OpcodeDetails(1, 1, LONG_TYPE))
        .put(F2D, new OpcodeDetails(1, 1, DOUBLE_TYPE))
        .put(D2I, new OpcodeDetails(1, 1, INT_TYPE))
        .put(D2L, new OpcodeDetails(1, 1, LONG_TYPE))
        .put(D2F, new OpcodeDetails(1, 1, FLOAT_TYPE))
        .put(I2B, new OpcodeDetails(1, 1, BYTE_TYPE))
        .put(I2C, new OpcodeDetails(1, 1, CHAR_TYPE))
        .put(I2S, new OpcodeDetails(1, 1, SHORT_TYPE))
        .put(LCMP, new OpcodeDetails(2, 1, INT_TYPE))
        .put(FCMPL, new OpcodeDetails(2, 1, INT_TYPE))
        .put(FCMPG, new OpcodeDetails(2, 1, INT_TYPE))
        .put(DCMPL, new OpcodeDetails(2, 1, INT_TYPE))
        .put(DCMPG, new OpcodeDetails(2, 1, INT_TYPE))
        .put(GETSTATIC, new OpcodeDetails(0, 1, i -> getType(((FieldInsnNode) i).desc)))
        .put(GETFIELD, new OpcodeDetails(0, 1, i -> getType(((FieldInsnNode) i).desc)))
        .put(NEW, new OpcodeDetails(0, 1, i -> getObjectType(((TypeInsnNode) i).desc)))
        .put(NEWARRAY, new OpcodeDetails(1, 1, getType(Object.class)))
        .put(ANEWARRAY, new OpcodeDetails(1, 1, getType(Object.class)))
        .put(ARRAYLENGTH, new OpcodeDetails(1, 1, INT_TYPE))
        .put(CHECKCAST, new OpcodeDetails(1, 1, i -> getObjectType(((TypeInsnNode) i).desc)))
        .put(INSTANCEOF, new OpcodeDetails(1, 1, BOOLEAN_TYPE))
        .build();
}
