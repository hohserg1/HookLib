package gloomyfolken.hooklib.asm.model;

import java.util.HashMap;
import java.util.Optional;

public class MapUtils {
    public static <A> Optional<A> maybeOfMapValue(HashMap<String, Object> map, String key) {
        return Optional.ofNullable((A) map.get(key));
    }
}
