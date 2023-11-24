package gloomyfolken.hooklib.asm;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import gloomyfolken.hooklib.helper.Logger;
import gloomyfolken.hooklib.minecraft.Config;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.objectweb.asm.ClassReader.SKIP_CODE;
import static org.objectweb.asm.Opcodes.ASM5;

public class HookClassTransformer {
    protected ListMultimap<String, AsmInjection> hooksMap = ArrayListMultimap.create(10, 2);
    protected ClassMetadataReader classMetadataReader = new ClassMetadataReader();

    public void registerAllHooks(ListMultimap<String, AsmInjection> hooks) {
        hooksMap.putAll(hooks);
    }

    public void registerHook(AsmInjection hook) {
        hooksMap.put(hook.getTargetClassName(), hook);
    }

    public void registerHookContainer(String className) {
        try {
            ClassNode classNode = new ClassNode(ASM5);
            new ClassReader(classMetadataReader.getClassData(className)).accept(classNode, SKIP_CODE);
            HookContainerParser.parseHooks(classNode).forEachOrdered(this::registerHook);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] transform(String className, byte[] bytecode) {
        if (hooksMap.containsKey(className)) {
            List<AsmInjection> hooks = hooksMap.get(className);
            Set<AsmInjection> injectedHooks;
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
                        hooks
                );
                cr.accept(hooksWriter, java7 ? ClassReader.SKIP_FRAMES : ClassReader.EXPAND_FRAMES);
                bytecode = cw.toByteArray();
                injectedHooks = hooksWriter.injectedHooks;
            } catch (Exception e) {
                injectedHooks = ImmutableSet.of();
                Logger.instance.error("A problem has occurred during transformation of class " + className + ".");
                Logger.instance.error("Attached hooks:");
                for (AsmInjection hook : hooks) {
                    Logger.instance.error("    " + hook.toString());
                }
                Logger.instance.error("Stack trace:", e);
            }

            for (AsmInjection hook : hooks) {
                if (!injectedHooks.contains(hook))
                    if (hook.isMandatory()) {
                        throw new RuntimeException("Can not find target method of mandatory hook " + hook);
                    } else {
                        Logger.instance.warning("Can not find target method of hook " + hook);
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
    protected HookInjectorClassVisitor createInjectorClassVisitor(ClassVisitor finalizeVisitor, List<AsmInjection> hooks) {
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
