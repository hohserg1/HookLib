package gloomyfolken.hooklib.asm;

import com.google.common.collect.ImmutableList;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;

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

    protected void createMethod(AsmHook ah, ClassNode classNode) {
    }

    protected void applyHook(AsmHook ah, MethodNode methodNode) {
        String anchorTarget = ah.getAnchorTarget();

        InsnList instructions = methodNode.instructions;
        InsnList addition = determineAddition(ah, methodNode);

        switch (ah.getAnchorPoint()) {
            case HEAD:
                instructions.insert(addition);
                break;
            case METHOD_CALL: {
                ListIterator<AbstractInsnNode> iterator = instructions.iterator();
                while (iterator.hasNext()) {
                    AbstractInsnNode node = iterator.next();
                    if (node instanceof MethodInsnNode) {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) node;
                        if (areMethodNamesEquals(methodInsnNode.name, anchorTarget)) {
                            switch (ah.getShift()) {
                                case BEFORE: {
                                    instructions.insertBefore(node, addition);
                                }
                                break;
                                case INSTEAD: {
                                    instructions.insertBefore(node, addition);

                                    //remove values for method call from stack
                                    int redundantStackSize = getMethodType(methodInsnNode.desc).getArgumentTypes().length +
                                            (methodInsnNode.getOpcode() == INVOKESTATIC ? 0 : 1);
                                    InsnList pop = new InsnList();
                                    for (int i = 0; i < redundantStackSize; i++)
                                        pop.add(new InsnNode(POP));
                                    instructions.insertBefore(node, pop);

                                    instructions.remove(node);
                                }
                                break;
                                case AFTER: {
                                    instructions.insert(node, addition);
                                }
                                break;
                            }

                        }
                    }
                }
            }
            break;
            case EXPRESSION: {

            }
            break;
            case RETURN: {
                ListIterator<AbstractInsnNode> iterator = instructions.iterator();
                while (iterator.hasNext()) {
                    AbstractInsnNode node = iterator.next();
                    if (returnOpcodes.contains(node.getOpcode()))
                        instructions.insertBefore(node, addition);
                }
            }
            break;
        }


        /*
        ImmutableList<ConcretePoint> target = determineConcretePoints(ah.getAnchorPoint(), ah.getAnchorTarget(), methodNode);

        InsertMethod im = determineInsertMethod(ah.getAnchor());
        InsnList addition = determineAddition(ah, methodNode);

        int ordinal = ah.getAnchorOrdinal();
        if (ordinal == -1)
            target.forEach(p -> im.insert(methodNode.instructions, p, addition));
        else if (target.size() > ordinal)
            im.insert(methodNode.instructions, target.get(ordinal), addition);
        else
            HookClassTransformer.logger.warning("Ordinal of hook " + ah.getHookClassName() + "#" + ah.getHookMethodName() + " greater that number of available similar injection points");*/
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

        if (ah.hasReturnValueParameter()) {
            returnLocalId = methodNode.maxLocals;
            methodNode.maxLocals++;
            r.add(new VarInsnNode(ah.getTargetMethodReturnType().getOpcode(ISTORE), returnLocalId));
        }

        for (int i = 0; i < ah.getHookMethodParameters().size(); i++) {
            Type parameterType = ah.getHookMethodParameters().get(i);
            int variableId = ah.getTransmittableVariableIds().get(i);

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
