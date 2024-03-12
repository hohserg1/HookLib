package gloomyfolken.hooklib.api;

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
}
