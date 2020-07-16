package gloomyfolken.hooklib.common.advanced.tree;

import gloomyfolken.hooklib.common.annotations.AnnotationMap;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.tree.FieldNode;

import java.util.ArrayList;

import static org.objectweb.asm.Opcodes.ASM5;

public class AdvancedFieldNode extends FieldNode {
    public AdvancedFieldNode(int access, String name, String desc, String signature, Object value) {
        super(ASM5,access, name, desc, signature, value);
    }

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
}
