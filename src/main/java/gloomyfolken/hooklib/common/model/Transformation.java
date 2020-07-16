package gloomyfolken.hooklib.common.model;


import org.objectweb.asm.tree.ClassNode;

public interface Transformation {

    void apply(ClassNode targetClass);

    String targetClass();

}
