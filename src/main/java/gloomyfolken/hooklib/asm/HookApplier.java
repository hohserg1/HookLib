package gloomyfolken.hooklib.asm;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import gloomyfolken.hooklib.asm.model.lens.hook.AsmLens;
import gloomyfolken.hooklib.asm.model.method.hook.AsmHook;
import gloomyfolken.hooklib.minecraft.MainHookLoader;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.objectweb.asm.Label;
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

    public void applyLense(AsmLens al, FieldNode fieldNode) {
    }


    public ClassMetadataReader classMetadataReader = new ClassMetadataReader();
    private ImmutableList<Integer> returnOpcodes = ImmutableList.of(
            IRETURN,
            LRETURN,
            FRETURN,
            DRETURN,
            ARETURN,
            RETURN
    );

    private void injectDefaultValue(MethodNode newMethod, Type targetMethodReturnType) {
        switch (targetMethodReturnType.getSort()) {
            case Type.VOID:
                break;
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                newMethod.visitInsn(Opcodes.ICONST_0);
                break;
            case Type.FLOAT:
                newMethod.visitInsn(Opcodes.FCONST_0);
                break;
            case Type.LONG:
                newMethod.visitInsn(Opcodes.LCONST_0);
                break;
            case Type.DOUBLE:
                newMethod.visitInsn(Opcodes.DCONST_0);
                break;
            default:
                newMethod.visitInsn(Opcodes.ACONST_NULL);
                break;
        }
    }

    private void injectSuperCall(MethodNode newMethod, ClassMetadataReader.MethodReference superMethod, AsmHook ah) {
        int variableId = 0;
        for (int i = 0; i <= ah.getTargetMethodParameters().size(); i++) {
            Type parameterType = i == 0 ? TypeHelper.getType(ah.getTargetClassName()) : ah.getTargetMethodParameters().get(i - 1);
            newMethod.instructions.add(createLocalLoad(parameterType, variableId));
            if (parameterType.getSort() == Type.DOUBLE || parameterType.getSort() == Type.LONG) {
                variableId += 2;
            } else {
                variableId++;
            }
        }
        newMethod.visitMethodInsn(INVOKESPECIAL, superMethod.owner, superMethod.name, superMethod.desc, false);
    }
    private void injectReturn(MethodNode inj, Type targetMethodReturnType) {
        if (targetMethodReturnType == INT_TYPE || targetMethodReturnType == SHORT_TYPE ||
                targetMethodReturnType == BOOLEAN_TYPE || targetMethodReturnType == BYTE_TYPE
                || targetMethodReturnType == CHAR_TYPE) {
            inj.visitInsn(IRETURN);
        } else if (targetMethodReturnType == LONG_TYPE) {
            inj.visitInsn(LRETURN);
        } else if (targetMethodReturnType == FLOAT_TYPE) {
            inj.visitInsn(FRETURN);
        } else if (targetMethodReturnType == DOUBLE_TYPE) {
            inj.visitInsn(DRETURN);
        } else if (targetMethodReturnType == VOID_TYPE) {
            inj.visitInsn(RETURN);
        } else {
            inj.visitInsn(ARETURN);
        }
    }

    public void createMethod(AsmHook ah, ClassNode classNode) {
        ClassMetadataReader.MethodReference superMethod = MainHookLoader.getTransformer().classMetadataReader
                .findVirtualMethod(ah.getTargetClassName().replace('.', '/'), ah.getTargetMethodName(), ah.getTargetMethodDescription());
        // юзаем название суперметода, потому что findVirtualMethod может вернуть метод с другим названием
        MethodNode newMethod = new MethodNode(ASM5, superMethod == null ? ah.getTargetMethodName() : superMethod.name, ah.getTargetMethodDescription(), null, null);
        classNode.methods.add(newMethod);

        newMethod.visitCode();
        newMethod.visitLabel(new Label());
        if (superMethod == null)
            injectDefaultValue(newMethod, ah.getTargetMethodReturnType());
        else
            injectSuperCall(newMethod, superMethod, ah);
        injectReturn(newMethod,ah.getTargetMethodReturnType());
        newMethod.visitLabel(new Label());
        newMethod.visitMaxs(0, 0);
        newMethod.visitEnd();
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
