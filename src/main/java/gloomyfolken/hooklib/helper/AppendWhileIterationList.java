package gloomyfolken.hooklib.helper;

import java.util.ArrayList;
import java.util.Collection;

public class AppendWhileIterationList<A> extends ArrayList<A> {

    public AppendWhileIterationList(Collection<? extends A> c) {
        super(c);
    }

    @Override
    public boolean add(A a) {
        int prevModCount = modCount;
        boolean r = super.add(a);
        modCount = prevModCount;
        return r;
    }
}
