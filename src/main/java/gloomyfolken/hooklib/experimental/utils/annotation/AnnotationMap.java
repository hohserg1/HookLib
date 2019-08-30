package gloomyfolken.hooklib.experimental.utils.annotation;

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

    public <A> A get(String desc) {
        return (A) map.get(desc);
    }
}
