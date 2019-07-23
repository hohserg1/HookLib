package gloomyfolken.hooklib.asm;


import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.function.Consumer;

public class HookCheckClassVisitor extends ClassVisitor {

    private static final String HOOK_CONTAINER_DESC = Type.getDescriptor(HookContainer.class);
    private final Consumer<String> addFoundClass;
    private String currentClassName;

    public HookCheckClassVisitor(Consumer<String> addFoundClass) {
        super(Opcodes.ASM5);
        this.addFoundClass = addFoundClass;
    }

    @Override
    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
        currentClassName = name.replace('/', '.');
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (HOOK_CONTAINER_DESC.equals(desc))
            addFoundClass.accept(currentClassName);
        return super.visitAnnotation(desc, visible);
    }
}
