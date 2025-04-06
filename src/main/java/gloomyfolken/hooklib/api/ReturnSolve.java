package gloomyfolken.hooklib.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Possible return type of hook-method, which allows a hook-method to determine whether a target-method needs to do return
 *
 * @param <A> is a return type of target method, can be marked by {@link gloomyfolken.hooklib.api.Primitive} annotation for strictly primitive type
 */
public interface ReturnSolve<A> {
    static <A> ReturnSolve<A> yes(A value) {
        return new Yes<>(value);
    }

    static <A> ReturnSolve<A> no() {
        return no;
    }

    class Yes<A> implements ReturnSolve<A> {
        public final A value;

        public Yes(A value) {
            this.value = value;
        }
    }

    static ReturnSolve no = new ReturnSolve() {
    };

    /**
     * Deprecated: Use {@link gloomyfolken.hooklib.api.Primitive} instead
     */
    @Deprecated
    @Target(ElementType.TYPE_USE)
    @interface Primitive {
    }
}
