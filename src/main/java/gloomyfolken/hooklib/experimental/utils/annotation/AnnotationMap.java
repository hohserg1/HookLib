package gloomyfolken.hooklib.experimental.utils.annotation;

import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class AnnotationMap {
    private Map<String, Object> map;

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
}
