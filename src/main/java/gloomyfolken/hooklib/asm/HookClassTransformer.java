package gloomyfolken.hooklib.asm;

import com.google.common.collect.ImmutableList;
import gloomyfolken.hooklib.asm.HookLogger.SystemOutLogger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

public class HookClassTransformer {

    public static HookLogger logger = new SystemOutLogger();
    protected HashMap<String, List<AsmHook>> hooksMap = new HashMap<>();
    private HookContainerParser containerParser = new HookContainerParser(this);
    protected ClassMetadataReader classMetadataReader = new ClassMetadataReader();

    public void registerHook(AsmHook hook) {
        if (hooksMap.containsKey(hook.getTargetClassName())) {
            hooksMap.get(hook.getTargetClassName()).add(hook);
        } else {
            List<AsmHook> list = new ArrayList<>(2);
            list.add(hook);
            hooksMap.put(hook.getTargetClassName(), list);
        }
    }

    public void registerHookContainer(String className) {
        containerParser.parseHooks(className);
    }

    public void registerHookContainer(String className, byte[] classData) {
        containerParser.parseHooks(className, classData);
    }

    public void registerHookContainer(byte[] classData) {
        containerParser.parseHooks(classData);
    }

    public byte[] transform(String className, byte[] bytecode) {
        List<AsmHook> hooks = hooksMap.get(className);

        if (hooks != null) {
            Collections.sort(hooks);
            logger.debug("Injecting hooks into class " + className);
            try {
                /*
                 Начиная с седьмой версии джавы, сильно изменился процесс верификации байткода.
                 Ради этого приходится включать автоматическую генерацию stack map frame'ов.
                 На более старых версиях байткода это лишняя трата времени.
                 Подробнее здесь: http://stackoverflow.com/questions/25109942
                */
                int majorVersion = ((bytecode[6] & 0xFF) << 8) | (bytecode[7] & 0xFF);
                boolean java7 = majorVersion > 50;


                ClassReader cr = new ClassReader(bytecode);
                ClassWriter cw = createClassWriter(java7 ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS);

                ClassNode classNode = new ClassNode(ASM5);
                cr.accept(classNode, java7 ? ClassReader.SKIP_FRAMES : ClassReader.EXPAND_FRAMES);

                for (MethodNode methodNode : classNode.methods)
                    if (!hooks.isEmpty()) {
                        List<AsmHook> forCurrentMethod = hooks.stream().filter(ah -> isTargetMethod(ah, methodNode.name, methodNode.desc)).collect(Collectors.toList());
                        hooks.removeAll(forCurrentMethod);
                        forCurrentMethod.forEach(ah -> applyHook(ah, methodNode));
                        forCurrentMethod.forEach(ah -> logger.debug("Patching method " + ah.getPatchedMethodName()));
                    }

                hooks.stream().filter(AsmHook::getCreateMethod).forEach(ah -> createMethod(ah, classNode));

                classNode.accept(cw);

                //HookInjectorClassVisitor hooksWriter = createInjectorClassVisitor(cw, hooks);
                //cr.accept(hooksWriter, java7 ? ClassReader.SKIP_FRAMES : ClassReader.EXPAND_FRAMES);

                bytecode = cw.toByteArray();
            } catch (Exception e) {
                logger.severe("A problem has occurred during transformation of class " + className + ".");
                logger.severe("Attached hooks:");
                for (AsmHook hook : hooks) {
                    logger.severe(hook.toString());
                }
                logger.severe("Stack trace:", e);
            }

            for (AsmHook notInjected : hooks) {
                if (notInjected.isMandatory()) {
                    throw new RuntimeException("Can not find target method of mandatory hook " + notInjected);
                } else {
                    logger.warning("Can not find target method of hook " + notInjected);
                }
            }
        }
        return bytecode;
    }

    private void createMethod(AsmHook ah, ClassNode classNode) {
    }

    private void applyHook(AsmHook ah, MethodNode methodNode) {
        ImmutableList<ConcretePoint> target = determineConcretePoints(ah.getAnchorPoint(), ah.getAnchorTarget(), methodNode);
        InsertMethod im = determineInsertMethod(ah.getAnchorPoint(), ah.getShift());
        InsnList addition = determineAddition(ah, methodNode);
        int ordinal = ah.getAnchorOrdinal();
        if (ordinal == -1)
            target.forEach(p -> im.insert(methodNode.instructions, p, addition));
        else if (target.size() > ordinal)
            im.insert(methodNode.instructions, target.get(ordinal), addition);
        else
            logger.warning("Ordinal of hook " + ah.getHookClassName() + "#" + ah.getHookMethodName() + " greater that available similar injection points");
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
                r.add(new InsnNode(ah.getHookMethodReturnType().getOpcode(IRETURN)));
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
            if (variableId == -1) variableId = returnLocalId;
            r.add(createLocalLoad(parameterType, variableId));
        }
        return r;
    }

    public VarInsnNode createLocalLoad(Type parameterType, int variableId) {
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

    private InsertMethod determineInsertMethod(InjectionPoint anchorPoint, Shift shift) {
        switch (anchorPoint) {
            case HEAD:
                return (list, point, addition) -> list.insertBefore(list.getFirst(), addition);
            case RETURN:
                return (list, point, addition) -> list.insertBefore(point.from, addition);
            case METHOD_CALL:
                switch (shift) {
                    case BEFORE:
                        return (list, point, addition) -> list.insertBefore(point.from, addition);
                    case INSTEAD:
                        return (list, point, addition) -> {
                            list.insertBefore(point.from, addition);
                            AbstractInsnNode from = point.from;
                            for (int i = 0; i < point.size; i++) {
                                list.remove(from);
                                from = from.getNext();
                            }
                        };
                    case AFTER:
                        return (list, point, addition) -> {
                            AbstractInsnNode from = point.from;
                            for (int i = 1; i < point.size; i++)
                                from = from.getNext();

                            list.insert(from, addition);
                        };
                }
            default:
                throw new IllegalArgumentException("Unexpected value of InjectionPoint");
        }
    }

    private ImmutableList<Integer> returnOpcodes = ImmutableList.of(
            IRETURN,
            LRETURN,
            FRETURN,
            DRETURN,
            ARETURN,
            RETURN
    );

    private ImmutableList<ConcretePoint> determineConcretePoints(InjectionPoint anchorPoint, String anchorTarget, MethodNode methodNode) {
        InsnList instructions = methodNode.instructions;
        ImmutableList.Builder<ConcretePoint> builder = ImmutableList.builder();
        switch (anchorPoint) {
            case HEAD:
                return ImmutableList.of(new ConcretePoint(instructions.getFirst(), 1));
            case RETURN:
                for (int i = 0; i < instructions.size(); i++) {
                    AbstractInsnNode node = instructions.get(i);
                    if (returnOpcodes.contains(node.getOpcode()))
                        builder.add(new ConcretePoint(node, 1));
                }
                break;
            case METHOD_CALL:
                for (int i = 0; i < instructions.size(); i++) {
                    AbstractInsnNode node = instructions.get(i);
                    if (node instanceof MethodInsnNode && areMethodNamesEquals(((MethodInsnNode) node).name, anchorTarget))
                        builder.add(new ConcretePoint(node, 1));
                }
                break;
        }
        return builder.build();
    }

    protected boolean areMethodNamesEquals(String name1, String name2) {
        return name1.equals(name2);
    }

    protected boolean isTargetMethod(AsmHook ah, String name, String desc) {
        return ah.isTargetMethod(name, desc);
    }

    /**
     * Создает ClassVisitor для списка хуков.
     * Метод можно переопределить, если в ClassVisitor'e нужна своя логика для проверки,
     * является ли метод целевым (isTargetMethod())
     *
     * @param cw    ClassWriter, который должен стоять в цепочке после этого ClassVisitor'a
     * @param hooks Список хуков, вставляемых в класс
     * @return ClassVisitor, добавляющий хуки
     */
    protected HookInjectorClassVisitor createInjectorClassVisitor(ClassWriter cw, List<AsmHook> hooks) {
        return new HookInjectorClassVisitor(this, cw, hooks);
    }

    /**
     * Создает ClassWriter для сохранения трансформированного класса.
     * Метод можно переопределить, если в ClassWriter'e нужна своя реализация метода getCommonSuperClass().
     * Стандартная реализация работает для уже загруженных классов и для классов, .class файлы которых есть
     * в classpath, но они ещё не загружены. Во втором случае происходит загрузка (но не инициализация) классов.
     * Если загрузка классов является проблемой, то можно воспользоваться SafeClassWriter.
     *
     * @param flags Список флагов, которые нужно передать в конструктор ClassWriter'a
     * @return ClassWriter, сохраняющий трансформированный класс
     */
    protected ClassWriter createClassWriter(int flags) {
        return new SafeClassWriter(classMetadataReader, flags);
    }
}
