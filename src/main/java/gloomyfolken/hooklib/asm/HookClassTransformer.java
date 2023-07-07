package gloomyfolken.hooklib.asm;

import gloomyfolken.hooklib.helper.Logger;
import gloomyfolken.hooklib.minecraft.Config;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class HookClassTransformer {
    protected HashMap<String, List<AsmHook>> hooksMap = new HashMap<String, List<AsmHook>>();
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

    public void registerHookContainer(String className, ClassNode classNode) {
        containerParser.parseHooks(className, classNode);
    }

    public void registerHookContainer(String className) {
        containerParser.parseHooks(className);
    }

    public byte[] transform(String className, byte[] bytecode) {
        List<AsmHook> hooks = hooksMap.get(className);

        if (hooks != null) {
            Collections.sort(hooks);
            Logger.instance.debug("Injecting hooks into class " + className);
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
                HookInjectorClassVisitor hooksWriter = createInjectorClassVisitor(
                        Config.instance.useCheckClassAdapter ? new CheckClassAdapter(cw) : cw,
                        hooks);
                cr.accept(hooksWriter, java7 ? ClassReader.SKIP_FRAMES : ClassReader.EXPAND_FRAMES);
                bytecode = cw.toByteArray();
                for (AsmHook hook : hooksWriter.injectedHooks) {
                    Logger.instance.debug("Patching method " + hook.getPatchedMethodName());
                }
                hooks.removeAll(hooksWriter.injectedHooks);
            } catch (Exception e) {
                Logger.instance.error("A problem has occurred during transformation of class " + className + ".");
                Logger.instance.error("Attached hooks:");
                for (AsmHook hook : hooks) {
                    Logger.instance.error("    " + hook.toString());
                }
                Logger.instance.error("Stack trace:", e);
            }

            for (AsmHook notInjected : hooks) {
                if (notInjected.isMandatory()) {
                    throw new RuntimeException("Can not find target method of mandatory hook " + notInjected);
                } else {
                    Logger.instance.warning("Can not find target method of hook " + notInjected);
                }
            }
        }

        /*
        LoadedIndex.instance.index.add(className);
        if (LoadedIndex.instance.init)
            Connector.notified(className);*/

        return bytecode;
    }

    /**
     * Создает ClassVisitor для списка хуков.
     * Метод можно переопределить, если в ClassVisitor'e нужна своя логика для проверки,
     * является ли метод целевым (isTargetMethod())
     *
     * @param finalizeVisitor ClassWriter, который должен стоять в цепочке после этого ClassVisitor'a
     * @param hooks           Список хуков, вставляемых в класс
     * @return ClassVisitor, добавляющий хуки
     */
    protected HookInjectorClassVisitor createInjectorClassVisitor(ClassVisitor finalizeVisitor, List<AsmHook> hooks) {
        return new HookInjectorClassVisitor(this, finalizeVisitor, hooks);
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
