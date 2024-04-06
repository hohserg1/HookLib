package gloomyfolken.hooklib.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Possible return type of hook-method, which allows a hook-method to determine whether a target-method needs to do return
 *
 * @param <A> return type of target method, can be marked by {@link Primitive} annotation for strictly primitive type
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
     * Type parameter of {@link ReturnSolve} can be marked by this annotation
     * <p>
     * Have sense only with {@link Hook#createMethod}, otherwise HookLib will try to find target method with primitive return type anyway
     */
    @Target(ElementType.TYPE_USE)
    @interface Primitive {
    }
}
