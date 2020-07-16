package gloomyfolken.hooklib.common.advanced.tree;

import gloomyfolken.hooklib.common.annotations.AnnotationInvocationHandler;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.*;

import static org.objectweb.asm.Opcodes.ASM5;

public class AdvancedAnnotationNode extends AnnotationNode {
    public final boolean visible;
    private Object value;

    public AdvancedAnnotationNode(String desc, boolean visible) {
        super(ASM5, desc);
        this.visible = visible;
    }

    public <A> A value() {
        value = createInstance(this);
        return (A) value;
    }

    private static Object createInstance(AnnotationNode annotationNode) {
        String className = Type.getType(annotationNode.desc).getClassName();
        try {

            HashMap<String, Object> map = new HashMap<>();

            Class<Annotation> annotationClass = (Class<Annotation>) Class.forName(className);

            List<Object> values = notNullList(annotationNode.values);
            for (int i = 0; i < values.size(); i += 2) {
                String name = (String) values.get(i);
                Object value = values.get(i + 1);

                Object insertableValue;

                if (value instanceof String[]) {
                    String[] enum1 = (String[]) value;
                    String enumType = Type.getType(enum1[0].replace('/', '.')).getClassName();
                    String enumValue = enum1[1];
                    Class<Enum> enumClass = (Class<Enum>) Class.forName(enumType);
                    insertableValue = Enum.valueOf(enumClass, enumValue);
                } else if (value instanceof AnnotationNode)
                    insertableValue = createInstance((AnnotationNode) value);
                else
                    insertableValue = value;

                map.put(name, insertableValue);
            }

            return annotation(annotationClass, map);
        } catch (ClassNotFoundException | NullPointerException e) {
            System.out.println("Error with annotation " + className + " parsing");
            //e.printStackTrace();
            return null;
        }
    }

    public static <A extends Annotation> A annotation(Class<A> annotationType, Map<String, Object> values) {
        return (A) Proxy.newProxyInstance(annotationType.getClassLoader(),
                new Class[]{annotationType},
                new AnnotationInvocationHandler(annotationType, values == null ? Collections.emptyMap() : values));
    }

    private static <A> A notNull(A value, A defaultValue) {
        return Optional.ofNullable(value).orElse(defaultValue);
    }

    private static <A> List<A> notNullList(List<A> list) {
        return notNull(list, Collections.emptyList());
    }
}
