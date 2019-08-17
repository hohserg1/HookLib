package gloomyfolken.hooklib.asm;

import com.google.common.collect.ImmutableList;
import gloomyfolken.hooklib.asm.model.AsmHook2;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

public class HookApplier {
    private ImmutableList<Integer> returnOpcodes = ImmutableList.of(
            IRETURN,
            LRETURN,
            FRETURN,
            DRETURN,
            ARETURN,
            RETURN
    );

    protected void createMethod(AsmHook2 ah, ClassNode classNode) {
    }

    protected void applyHook(AsmHook2 ah, MethodNode methodNode) {
        String anchorTarget = ah.getAnchorTarget();
        int ordinal = ah.getOrdinal();

        InsnList instructions = methodNode.instructions;

        switch (ah.getPoint()) {
            case HEAD:
                instructions.insert(determineAddition(ah, methodNode));
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

                if (ordinal == -1) {
                    methodNodes.forEach(methodInsnNodeConsumer);
                } else {
                    Optional<MethodInsnNode> target = methodNodes.limit(ordinal + 1).skip(ordinal).findFirst();

                    if (target.isPresent())
                        target.ifPresent(methodInsnNodeConsumer);
                    else
                        HookClassTransformer.logger.warning("Ordinal of hook " + ah.getHookClassName() + "#" + ah.getHookMethodName() + " greater that number of available similar injection points");
                }
            }
            break;
            case EXPRESSION: {
                String evalPatternName=anchorTarget;

            }
            break;
            case RETURN: {
                Stream<AbstractInsnNode> returnNodes = streamOfInsnList(instructions).filter(n -> returnOpcodes.contains(n.getOpcode()));
                if (ordinal == -1)
                    returnNodes.forEach(n -> instructions.insertBefore(n, determineAddition(ah, methodNode)));
                else {
                    Optional<AbstractInsnNode> target = returnNodes.limit(ordinal + 1).skip(ordinal).findFirst();

                    if (target.isPresent())
                        instructions.insertBefore(target.get(), determineAddition(ah, methodNode));
                    else
                        HookClassTransformer.logger.warning("Ordinal of hook " + ah.getHookClassName() + "#" + ah.getHookMethodName() + " greater that number of available similar injection points");
                }
            }
            break;
        }
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

    private InsnList determineAddition(AsmHook2 ah, MethodNode methodNode) {
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

    private InsnList createLocalCapturing(AsmHook2 ah, MethodNode methodNode) {
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

    protected boolean areMethodNamesEquals(String name1, String name2) {
        return name1.equals(name2);
    }
}
