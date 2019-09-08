package gloomyfolken.hooklib.asm;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import gloomyfolken.hooklib.asm.model.method.hook.AsmHook;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

public class HookApplier {
    public ClassMetadataReader classMetadataReader = new ClassMetadataReader();
    private ImmutableList<Integer> returnOpcodes = ImmutableList.of(
            IRETURN,
            LRETURN,
            FRETURN,
            DRETURN,
            ARETURN,
            RETURN
    );

    public void createMethod(AsmHook ah, ClassNode classNode) {
    }

    public void applyHook(AsmHook ah, MethodNode methodNode) {
        String anchorTarget = ah.getAnchorTarget();
        int ordinal = ah.getOrdinal();

        InsnList instructions = methodNode.instructions;

        switch (ah.getPoint()) {
            case HEAD: {
                String superClass = classMetadataReader.getSuperClass(ah.getTargetClassName());

                AbstractInsnNode afterSuperConstructor = streamOfInsnList(instructions)
                        .filter(n -> n instanceof MethodInsnNode)
                        .map(n -> (MethodInsnNode) n)
                        .filter(n -> n.getOpcode() == INVOKESPECIAL && n.owner.equals(superClass) && n.name.equals("<init>"))
                        .findFirst()
                        .map(n -> (AbstractInsnNode) n)
                        .orElseGet(instructions::getFirst);

                instructions.insert(afterSuperConstructor, determineAddition(ah, methodNode));
            }
            break;
            case METHOD_CALL: {
                Stream<MethodInsnNode> methodNodes = streamOfInsnList(instructions)
                        .filter(n -> n instanceof MethodInsnNode)
                        .map(n -> (MethodInsnNode) n)
                        .filter(n -> areMethodNamesEquals(n.name, anchorTarget));

                Consumer<MethodInsnNode> methodInsnNodeConsumer = n -> {
                    switch (ah.getShift()) {
                        case BEFORE: {
                            instructions.insertBefore(n, determineAddition(ah, methodNode));
                        }
                        break;
                        case INSTEAD: {
                            instructions.insertBefore(n, determineAddition(ah, methodNode));

                            //remove values for method call from stack
                            int redundantStackSize = getMethodType(n.desc).getArgumentTypes().length +
                                    (n.getOpcode() == INVOKESTATIC ? 0 : 1);
                            InsnList pop = new InsnList();
                            for (int i = 0; i < redundantStackSize; i++)
                                pop.add(new InsnNode(POP));
                            instructions.insertBefore(n, pop);

                            instructions.remove(n);
                        }
                        break;
                        case AFTER: {
                            instructions.insert(n, determineAddition(ah, methodNode));
                        }
                        break;
                    }
                };

                if (ordinal == -1)
                    methodNodes.forEach(methodInsnNodeConsumer);
                else {
                    Optional<MethodInsnNode> target = methodNodes.limit(ordinal + 1).skip(ordinal).findFirst();

                    if (target.isPresent())
                        target.ifPresent(methodInsnNodeConsumer);
                    else
                        warnOrdinalMiss(ah);
                }
            }
            break;
            case EXPRESSION: {
                try {
                    ClassNode hookClassNode = new ClassNode(ASM5);
                    classMetadataReader.acceptVisitor(ah.getHookClassReflectName(), hookClassNode);
                    Optional<MethodNode> maybeEvalPatternNode = hookClassNode.methods.stream().filter(mn -> mn.name.equals(anchorTarget)).findAny();
                    if (maybeEvalPatternNode.isPresent()) {
                        MethodNode evalPatternMethodNode = maybeEvalPatternNode.get();

                        List<AbstractInsnNode> pattern = Arrays.stream(evalPatternMethodNode.instructions.toArray())
                                .filter(n -> !(n instanceof LabelNode) && !(n instanceof LineNumberNode) && !(n instanceof FrameNode) && !isReturn(n))
                                .collect(Collectors.toList());

                        Stream<AbstractInsnNode> foundNodes = findSimilarCode(instructions, pattern).stream();

                        if (ordinal == -1)
                            foundNodes.forEach(n -> instructions.insert(n, determineAddition(ah, methodNode)));
                        else {
                            Optional<AbstractInsnNode> target = foundNodes.limit(ordinal + 1).skip(ordinal).findFirst();

                            if (target.isPresent())
                                target.ifPresent(n -> instructions.insert(n, determineAddition(ah, methodNode)));
                            else
                                warnOrdinalMiss(ah);

                        }

                    } else
                        HookClassTransformer.logger.warning("Evaluation expression " + ah.getHookClassReflectName() + "#" + anchorTarget + " not found");

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            break;
            case RETURN: {
                Stream<AbstractInsnNode> methodNodes = streamOfInsnList(instructions).filter(n -> returnOpcodes.contains(n.getOpcode()));

                if (ordinal == -1)
                    methodNodes.forEach(n -> instructions.insertBefore(n, determineAddition(ah, methodNode)));
                else {
                    Optional<AbstractInsnNode> target = methodNodes.limit(ordinal + 1).skip(ordinal).findFirst();

                    if (target.isPresent())
                        instructions.insertBefore(target.get(), determineAddition(ah, methodNode));
                    else
                        warnOrdinalMiss(ah);
                }
            }
            break;
        }
    }

    private void warnOrdinalMiss(AsmHook ah) {
        HookClassTransformer.logger.warning("Ordinal of hook " + ah.getHookClassReflectName() + "#" + ah.getHookMethodName() + " greater that number of available similar injection points");
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

    private static List<AbstractInsnNode> findSimilarCode(InsnList instructions, List<AbstractInsnNode> pattern) {
        List<AbstractInsnNode> r = new ArrayList<>();
        BiMap<Integer, Integer> colorCompliance = HashBiMap.create();
        AbstractInsnNode[] findingArea = instructions.toArray();

        int findingPosition = 0;
        for (int i = 0; i < findingArea.length; i++) {

            AbstractInsnNode current = findingArea[i];
            AbstractInsnNode currentExpectation = pattern.get(findingPosition);

            if (equalsWithVarColor(current, currentExpectation, colorCompliance))
                findingPosition++;
            else
                findingPosition = 0;

            if (findingPosition == pattern.size()) {
                System.out.println("FOUND!!1!");
                r.add(current);
                findingPosition = 0;
                colorCompliance.clear();
            }
        }

        return r;
    }

    private boolean isReturn(AbstractInsnNode n) {
        return returnOpcodes.contains(n.getOpcode());
    }

    private static InsnList copy(InsnList insnList) {
        InsnList r = new InsnList();
        for (int i = 0; i < insnList.size(); i++)
            r.add(insnList.get(i).clone(new HashMap<>()));

        return r;
    }

    private Stream<AbstractInsnNode> streamOfInsnList(InsnList instructions) {
        ListIterator<AbstractInsnNode> iterator = instructions.iterator();
        return Stream.iterate(instructions.getFirst(), AbstractInsnNode::getNext).limit(instructions.size())
                .collect(Collectors.toList()).stream();
    }

    private InsnList determineAddition(AsmHook ah, MethodNode methodNode) {
        InsnList r = new InsnList();

        r.add(createLocalCapturing(ah, methodNode));

        r.add(new MethodInsnNode(INVOKESTATIC, ah.getHookClassInternalName(), ah.getHookMethodName(), ah.getHookMethodDescription(), false));

        switch (ah.getReturnCondition()) {
            case NEVER:
                if (ah.getHookMethodReturnType() != VOID_TYPE)
                    r.add(new InsnNode(POP));
                break;
            case ALWAYS:
                int opcode = ah.getHookMethodReturnType().getOpcode(IRETURN);
                r.add(new InsnNode(opcode));
                break;
        }
        return r;
    }

    private InsnList createLocalCapturing(AsmHook ah, MethodNode methodNode) {
        InsnList r = new InsnList();

        int returnLocalId = -1;

        if (ah.isHasReturnValueParameter()) {
            returnLocalId = methodNode.maxLocals;
            methodNode.maxLocals++;
            r.add(new VarInsnNode(ah.getTargetMethodReturnType().getOpcode(ISTORE), returnLocalId));
        }

        for (int i = 0; i < ah.getHookMethodParameters().size(); i++) {
            Type parameterType = ah.getHookMethodParameters().get(i);
            int variableId = ah.getHookMethodLocalCaptureIds().get(i);

            if ((methodNode.access & Opcodes.ACC_STATIC) != 0) {
                // если попытка передачи this из статического метода, то передаем null
                if (variableId == 0) {
                    r.add(new InsnNode(Opcodes.ACONST_NULL));
                    continue;
                }
                // иначе сдвигаем номер локальной переменной
                if (variableId > 0) variableId--;
            }

            if (variableId == -1)
                variableId = returnLocalId;

            r.add(createLocalLoad(parameterType, variableId));
        }
        return r;
    }

    private VarInsnNode createLocalLoad(Type parameterType, int variableId) {
        int opcode;
        if (parameterType == INT_TYPE || parameterType == BYTE_TYPE || parameterType == CHAR_TYPE ||
                parameterType == BOOLEAN_TYPE || parameterType == SHORT_TYPE) {
            opcode = ILOAD;
        } else if (parameterType == LONG_TYPE) {
            opcode = LLOAD;
        } else if (parameterType == FLOAT_TYPE) {
            opcode = FLOAD;
        } else if (parameterType == DOUBLE_TYPE) {
            opcode = DLOAD;
        } else {
            opcode = ALOAD;
        }
        return new VarInsnNode(opcode, variableId);
    }

    public boolean areMethodNamesEquals(String name1, String name2) {
        return name1.equals(name2);
    }
}
