package gloomyfolken.hooklib.experimental.utils.annotation;

import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class AnnotationMap {
    private Map<String, Supplier<Object>> suppliers;
    private Map<String, Object> cahce = new HashMap<>();

    AnnotationMap(Map<String, Supplier<Object>> map) {
        suppliers = map;
    }

    AnnotationMap() {
        suppliers = new HashMap<>();
    }

    public <A extends Annotation> A get(String desc) {
        return (A) cahce.computeIfAbsent(desc, __ -> {
            Supplier<Object> supplier = suppliers.get(desc);
            if (supplier != null)
                return supplier.get();
            else
                return null;
        });
    }

    public <A extends Annotation> A get(Class<A> annotationClass) {
        return get(Type.getDescriptor(annotationClass));
    }

    public <A extends Annotation> Optional<A> maybeGet(Class<A> annotationClass) {
        return Optional.ofNullable(get(annotationClass));
    }

    public <A extends Annotation> boolean contains(Class<A> annotationClass) {
        return suppliers.containsKey(Type.getDescriptor(annotationClass));
    }
}
