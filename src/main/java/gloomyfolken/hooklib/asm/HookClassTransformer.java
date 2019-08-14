package gloomyfolken.hooklib.asm;

import gloomyfolken.hooklib.asm.HookLogger.SystemOutLogger;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.ASM5;

public class HookClassTransformer {

    public static HookLogger logger = new SystemOutLogger();
    protected HashMap<String, List<AsmHook>> hooksMap = new HashMap<>();
    private HookContainerParser containerParser = new HookContainerParser(this);
    protected ClassMetadataReader classMetadataReader = new ClassMetadataReader();

    public void registerHook(AsmHook hook) {
        if (hooksMap.containsKey(hook.getTargetClassName())) {
            hooksMap.get(hook.getTargetClassName()).add(hook);
        } else {
            List<AsmHook> list = new ArrayList<AsmHook>(2);
            list.add(hook);
            hooksMap.put(hook.getTargetClassName(), list);
        }
    }

    public void registerHookContainer(String className) {
        containerParser.parseHooks(className);
    }

    public void registerHookContainer(String className,byte[] classData) {
        containerParser.parseHooks(className,classData);
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

                ClassNode classNode=new ClassNode(ASM5);
                cr.accept(classNode,java7 ? ClassReader.SKIP_FRAMES : ClassReader.EXPAND_FRAMES);

                for (MethodNode methodNode : classNode.methods)
                    if (!hooks.isEmpty()) {
                        List<AsmHook> forCurrentMethod = hooks.stream().filter(ah -> isTargetMethod(ah, methodNode.name, methodNode.desc)).collect(Collectors.toList());
                        hooks.removeAll(forCurrentMethod);
                        forCurrentMethod.forEach(ah -> applyHook(ah, methodNode));
                        forCurrentMethod.forEach(ah -> logger.debug("Patching method " + ah.getPatchedMethodName()));
                    }

                hooks.stream().filter(AsmHook::getCreateMethod).forEach(ah->createMethod(ah,classNode));

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
        ah.getAnchorPoint().factory.apply(ah,methodNode);
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
