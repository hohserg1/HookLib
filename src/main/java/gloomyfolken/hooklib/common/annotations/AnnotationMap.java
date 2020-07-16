package gloomyfolken.hooklib.common.annotations;

import gloomyfolken.hooklib.common.advanced.tree.AdvancedAnnotationNode;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AnnotationMap {

    private Map<String, AdvancedAnnotationNode> map = new HashMap<>();

    public AdvancedAnnotationNode get(String desc) {
        return map.get(desc);
    }

    public <A extends Annotation> AdvancedAnnotationNode get(Class<A> annotationClass) {
        return get(Type.getDescriptor(annotationClass));
    }

    public <A extends Annotation> Optional<AdvancedAnnotationNode> maybeGet(Class<A> annotationClass) {
        return Optional.ofNullable(get(annotationClass));
    }

    public <A extends Annotation> boolean contains(Class<A> annotationClass) {
        return contains(Type.getDescriptor(annotationClass));
    }

    public boolean contains(String annotationClassDesc) {
        return map.containsKey(annotationClassDesc);
    }

    public void put(String desc, AdvancedAnnotationNode an) {
        map.put(desc, an);
    }

    public int size() {
        return map.size();
    }
}
