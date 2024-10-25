package gloomyfolken.hooklib.helper;

import net.minecraftforge.fml.common.asm.transformers.ModAPITransformer;

import java.util.ArrayList;
import java.util.Collection;

public class KeepHookLibLastList<A> extends ArrayList<A> {
    public KeepHookLibLastList(Collection<? extends A> c) {
        super(c);
    }

    private A hookTransformer = null;

    @Override
    public boolean add(A a) {
        if (hookTransformer == null) {
            if (a.getClass().getName().equals("$wrapper.gloomyfolken.hooklib.asm.HookClassTransformer")) {
                hookTransformer = a;
            }
            super.add(a);
        } else {
            if (a instanceof ModAPITransformer) {
                super.add(a);
            } else {
                remove(size() - 1);
                super.add(a);
                super.add(hookTransformer);
            }
        }
        return true;
    }
}
