package gloomyfolken.hooklib.experimental.utils.annotation;

import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AnnotationMap {
    public Map<String, Object> map;

    AnnotationMap(Map<String, Object> map) {
        this.map = map;
    }

    AnnotationMap() {
        map = new HashMap<>();
    }

    public <A extends Annotation> A get(String desc) {
        return (A) map.get(desc);
    }

    public <A extends Annotation> A get(Class<A> annotationClass) {
        return get(Type.getDescriptor(annotationClass));
    }

    public <A extends Annotation> Optional<A> maybeGet(Class<A> annotationClass) {
        return Optional.ofNullable(get(annotationClass));
    }

    public <A extends Annotation> boolean contains(Class<A> annotationClass) {
        return map.containsKey(Type.getDescriptor(annotationClass));
    }
}
