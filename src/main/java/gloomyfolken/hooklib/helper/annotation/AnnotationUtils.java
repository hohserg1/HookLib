package gloomyfolken.hooklib.helper.annotation;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Supplier;

public class AnnotationUtils {
    public static AnnotationMap annotationOf(ClassNode classNode) {
        return getAnnotationMap(classNode.invisibleAnnotations, classNode.visibleAnnotations);
    }


    public static AnnotationMap annotationOf(MethodNode methodNode) {
        return getAnnotationMap(methodNode.invisibleAnnotations, methodNode.visibleAnnotations);
    }

    public static AnnotationMap annotationOf(FieldNode fieldNode) {
        return getAnnotationMap(fieldNode.invisibleAnnotations, fieldNode.visibleAnnotations);
    }

    public static AnnotationMap annotationOfParameter(MethodNode methodNode, int parameter) {
        int length = Type.getArgumentTypes(methodNode.desc).length;
        List<AnnotationNode>[] defaultValue = (List<AnnotationNode>[]) new List<?>[length];
        return getAnnotationMap(
                notNull(methodNode.invisibleParameterAnnotations, defaultValue)[parameter],
                notNull(methodNode.visibleParameterAnnotations, defaultValue)[parameter]);
    }

    private static <A> A notNull(A value, A defaultValue) {
        return Optional.ofNullable(value).orElse(defaultValue);
    }

    private static AnnotationMap getAnnotationMap(List<AnnotationNode> invisibleAnnotations, List<AnnotationNode> visibleAnnotations) {
        HashMap<String, Supplier<Object>> map = new HashMap<>();

        notNullList(invisibleAnnotations).forEach(node -> map.put(node.desc, () -> createInstance(node)));
        notNullList(visibleAnnotations).forEach(node -> map.put(node.desc, () -> createInstance(node)));

        return new AnnotationMap(map);
    }

    private static <A> List<A> notNullList(List<A> list) {
        return notNull(list, Collections.emptyList());
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
                new AnnotationInvocationHandler(annotationType, values));
    }

}
