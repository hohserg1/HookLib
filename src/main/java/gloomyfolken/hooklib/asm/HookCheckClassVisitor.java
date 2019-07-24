package gloomyfolken.hooklib.asm;


import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.function.Consumer;

import static gloomyfolken.hooklib.asm.HookContainerParser.SIDE_ONLY_DESC;

public class HookCheckClassVisitor extends ClassVisitor {

    private static final String HOOK_CONTAINER_DESC = Type.getDescriptor(HookContainer.class);
    private final Consumer<String> addFoundClass;
    private String currentClassName;

    private boolean hookContainerFound = false;
    private HashMap<String, Object> sideOnlyValues = new HashMap<>();

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
            hookContainerFound = true;
        if (SIDE_ONLY_DESC.equals(desc))
            return new HookContainerParser.HookAnnotationVisitor(sideOnlyValues);
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public void visitEnd() {
        String currentSide = FMLLaunchHandler.side().toString();
        if (hookContainerFound && sideOnlyValues.getOrDefault("value", currentSide).equals(currentSide))
            addFoundClass.accept(currentClassName);
        super.visitEnd();
    }
}
