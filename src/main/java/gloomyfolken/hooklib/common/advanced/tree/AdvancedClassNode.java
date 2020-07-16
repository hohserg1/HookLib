package gloomyfolken.hooklib.common.advanced.tree;

import gloomyfolken.hooklib.common.annotations.AnnotationMap;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.ASM5;

/**
 * Продвинутые фичи, типо, улучшенный парсинг аннотаций
 */
public class AdvancedClassNode extends ClassNode {

    public AnnotationMap annotations = new AnnotationMap();

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AdvancedAnnotationNode an = new AdvancedAnnotationNode(desc, visible);
        annotations.put(desc, an);
        if (visible) {
            if (visibleAnnotations == null)
                visibleAnnotations = new ArrayList<>(1);

            visibleAnnotations.add(an);
        } else {
            if (invisibleAnnotations == null)
                invisibleAnnotations = new ArrayList<>(1);

            invisibleAnnotations.add(an);
        }
        return an;
    }

    public List<AdvancedMethodNode> methods = new ArrayList<>();

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        AdvancedMethodNode mn = new AdvancedMethodNode(access, name, desc, signature, exceptions);
        methods.add(mn);
        super.methods.add(mn);
        return mn;
    }

    public List<AdvancedFieldNode> fields = new ArrayList<>();

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        AdvancedFieldNode fn = new AdvancedFieldNode(access, name, desc, signature, value);
        fields.add(fn);
        super.fields.add(fn);
        return fn;
    }

    /**
     * default constructors
     */
    public AdvancedClassNode() {
        super(ASM5);
    }

    public AdvancedClassNode(int api) {
        super(api);
    }
}
