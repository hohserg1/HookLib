package gloomyfolken.hooklib.asm;

import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

public class TypeHelper {

    private static final Map<String, Type> primitiveTypes = new HashMap<String, Type>(9);

    static {
        primitiveTypes.put("void", Type.VOID_TYPE);
        primitiveTypes.put("boolean", Type.BOOLEAN_TYPE);
        primitiveTypes.put("byte", Type.BYTE_TYPE);
        primitiveTypes.put("short", Type.SHORT_TYPE);
        primitiveTypes.put("char", Type.CHAR_TYPE);
        primitiveTypes.put("int", Type.INT_TYPE);
        primitiveTypes.put("float", Type.FLOAT_TYPE);
        primitiveTypes.put("long", Type.LONG_TYPE);
        primitiveTypes.put("double", Type.DOUBLE_TYPE);
    }

    public static Type getType(String className) {
        Type primitive = primitiveTypes.get(className);
        return Type.getType(
                primitive != null ?
                        primitive.getDescriptor() :
                        "L" + className.replace(".", "/") + ";"
        );
    }

}
