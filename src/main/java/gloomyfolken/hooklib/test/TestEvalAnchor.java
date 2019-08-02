package gloomyfolken.hooklib.test;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;

public class TestEvalAnchor {
    public static void main(String[] args) throws IOException {
        if (true) {
            byte[] hook = identity(IOUtils.toByteArray(new FileInputStream(new File("./hook.class"))));
            byte[] test = identity(IOUtils.toByteArray(new FileInputStream(new File("./Test.class"))));

            System.out.println(Arrays.equals(test, identity(test)));
            Optional<List<AbstractInsnNode>> evaluation = analiseEvaluation(hook, "evaluation");
            Optional<List<AbstractInsnNode>> test2test = analiseEvaluation(test, "test");
            if (evaluation.isPresent() && evaluation.get().size() > 0) {
                List<AbstractInsnNode> nodeList = evaluation.get();

/*
            nodeList
                    .stream()
                    .map(TestEvalAnchor::nodeToString)
                    .forEach(System.out::println);

            System.out.println("test");

            test2test.get()
                    .stream()
                    .map(TestEvalAnchor::nodeToString)
                    .forEach(System.out::println);*/

                findSimilarCode(test, "test", "()V", nodeList);
            }
        }
    }

    private static void findSimilarCode(byte[] a, String methodName, String desc, List<AbstractInsnNode> nodeList) {
        ClassReader cr = new ClassReader(a);
        ClassNode cn = new ClassNode(Opcodes.ASM5);
        cr.accept(cn, ClassReader.SKIP_FRAMES);
        BiMap<Integer, Integer> colorCompliance = HashBiMap.create();
        cn.methods
                .stream()
                .filter(m -> m.name.equals(methodName) && m.desc.equals(desc)).findAny()
                .ifPresent(m -> {
                    AbstractInsnNode[] findingArea = m.instructions.toArray();
                    int findingPosition = 0;
                    for (int i = 0; i < findingArea.length; i++) {

                        AbstractInsnNode current = findingArea[i];
                        AbstractInsnNode currentExpectation = nodeList.get(findingPosition);

                        System.out.println(nodeToString(current) + "|" + nodeToString(currentExpectation));

                        boolean eq = equalsWithVarColor(current, currentExpectation, colorCompliance);
                        if (eq)
                            findingPosition++;
                        else
                            findingPosition = 0;

                        if (findingPosition == nodeList.size()) {
                            System.out.println("FOUND!!1!");
                            return;
                        }
                    }
                });
    }

    private static boolean equalsWithVarColor(AbstractInsnNode current, AbstractInsnNode currentExpectation, BiMap<Integer, Integer> colorCompliance) {
        if (current instanceof VarInsnNode && currentExpectation instanceof VarInsnNode) {
            if (current.getOpcode() == currentExpectation.getOpcode()) {
                VarInsnNode currentExpectation1 = (VarInsnNode) currentExpectation;
                VarInsnNode current1 = (VarInsnNode) current;

                Integer ePair = colorCompliance.get(currentExpectation1.var);
                boolean colorEquals;
                if (ePair == null) {
                    if (!colorCompliance.values().contains(current1.var)) {
                        colorCompliance.put(currentExpectation1.var, current1.var);
                        colorEquals = true;
                    } else
                        colorEquals = false;
                } else
                    colorEquals = ePair == current1.var;
                return colorEquals;
            } else
                return false;
        } else {
            return current.getType() == currentExpectation.getType() &&
                    EqualsBuilder.reflectionEquals(current, currentExpectation, "prev", "next", "index");
        }
    }

    private static String nodeToString(AbstractInsnNode node) {
        if (node instanceof VarInsnNode)
            return "VarInsnNode(" + Printer.OPCODES[node.getOpcode()] + ", " + ((VarInsnNode) node).var + ")";
        else if (node instanceof IntInsnNode)
            return "IntInsnNode(" + Printer.OPCODES[node.getOpcode()] + ", " + ((IntInsnNode) node).operand + ")";
        else if (node instanceof MethodInsnNode)
            return "MethodInsnNode(" + Printer.OPCODES[node.getOpcode()] + ", " + ((MethodInsnNode) node).owner + "#" + ((MethodInsnNode) node).name + ", " + ((MethodInsnNode) node).desc + ")";
        else if (node.getOpcode() >= 0)
            return "InsnNode(" + Printer.OPCODES[node.getOpcode()] + ")";
        else
            return node.getClass().getSimpleName() + "()";
    }

    private static Optional<List<AbstractInsnNode>> analiseEvaluation(byte[] a, String evalPatternName) {
        ClassReader cr = new ClassReader(a);
        ClassNode cn = new ClassNode(Opcodes.ASM5);
        cr.accept(cn, ClassReader.SKIP_FRAMES);
        return cn.methods
                .stream()
                .filter(m -> m.name.equals(evalPatternName))
                .findAny()
                .map(m ->
                        Arrays.stream(m.instructions.toArray())
                                .filter(n -> !(n instanceof LabelNode) && !(n instanceof LineNumberNode) && !(n instanceof FrameNode) && !isReturn(n))
                                .collect(Collectors.toList())
                );
    }

    private static Set<Integer> returnOpcodes = ImmutableSet.of(RETURN, IRETURN, ARETURN, DRETURN, FRETURN, LRETURN);

    private static boolean isReturn(AbstractInsnNode n) {
        return returnOpcodes.contains(n.getOpcode());
    }

    public static byte[] identity(byte[] a) {
        ClassReader cr = new ClassReader(a);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
        };
        cr.accept(cv, ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }
}
