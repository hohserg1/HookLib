package gloomyfolken.hooklib.asm;

import org.objectweb.asm.tree.InsnList;

public interface InsertMethod {
    void insert(InsnList list, ConcretePoint point, InsnList addition);
}
