package gloomyfolken.hooklib.common.advanced.tree;

import gloomyfolken.hooklib.common.annotations.AnnotationMap;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.ASM5;

public class AdvancedMethodNode extends MethodNode {
    public AdvancedMethodNode(int access, String name, String desc, String signature, String[] exceptions) {
        super(ASM5, access, name, desc, signature, exceptions);
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

    public List<AnnotationMap> parameterAnnotations = Stream.generate(AnnotationMap::new).limit(Type.getArgumentTypes(desc).length).collect(Collectors.toList());

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        AdvancedAnnotationNode an = new AdvancedAnnotationNode(desc, visible);


        parameterAnnotations.get(parameter).put(desc, an);

        if (visible) {
            if (visibleParameterAnnotations == null)
                visibleParameterAnnotations = (List<AnnotationNode>[]) new List<?>[params];

            if (visibleParameterAnnotations[parameter] == null) {
                visibleParameterAnnotations[parameter] = new ArrayList<>(
                        1);
            }
            visibleParameterAnnotations[parameter].add(an);
        } else {
            if (invisibleParameterAnnotations == null)
                invisibleParameterAnnotations = (List<AnnotationNode>[]) new List<?>[params];

            if (invisibleParameterAnnotations[parameter] == null) {
                invisibleParameterAnnotations[parameter] = new ArrayList<>(
                        1);
            }
            invisibleParameterAnnotations[parameter].add(an);
        }
        return an;
    }
}
