package gloomyfolken.hooklib.asm;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import gloomyfolken.hooklib.asm.HookLogger.SystemOutLogger;
import gloomyfolken.hooklib.asm.model.lens.hook.AsmLens;
import gloomyfolken.hooklib.asm.model.method.hook.AsmHook;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.ASM5;

public class HookClassTransformer extends HookApplier {

    public static HookLogger logger = new SystemOutLogger();

    protected Multimap<String, AsmHook> hookMap = TreeMultimap.create();
    protected Multimap<String, AsmLens> lensMap = TreeMultimap.create();

    protected HashMap<String, List<AbstractInsnNode>> exprPatternsMap = new HashMap<>();
    public HookContainerParser containerParser = new HookContainerParser(this);

    public void registerHook(AsmHook hook) {
        hookMap.put(hook.getTargetClassName(), hook);
    }

    public void registerLens(AsmLens lens) {
        lensMap.put(lens.getTargetClassInternalName(), lens);
    }

    public void registerHookContainer(String className) {
        try {
            registerHookContainer(className, classMetadataReader.getClassData(className));
        } catch (IOException e) {
            logger.severe("Can not parse hooks container " + className, e);
            e.printStackTrace();
        }
    }

    Map<String, byte[]> tempRegistryMap = new HashMap<>();

    public void registerHookContainer(String className, byte[] classData) {
        tempRegistryMap.put(className, classData);
    }

    public void finishRegistry() {
        tempRegistryMap.forEach((className, classData) -> {
            containerParser.parseHooks(className, classData).forEach(this::registerHook);
            containerParser.parseLenses(className, classData).forEach(this::registerLens);
        });
    }

    public byte[] transform(String className, byte[] bytecode) {
        Collection<AsmHook> hooks = hookMap.get(className);
        Collection<AsmLens> lenses = lensMap.get(className);

        if (!hooks.isEmpty() || !lenses.isEmpty()) {
            //Collections.sort(hooks);
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

                hooks.stream().filter(AsmHook::isCreateMethod).forEach(ah -> {
                    createMethod(ah, classNode);
                    hooks.remove(ah);
                });

                for (FieldNode fieldNode : classNode.fields) {
                    if (!lenses.isEmpty()) {
                        List<AsmLens> forCurrectField = lenses.stream().filter(al -> al.isTargetField(fieldNode)).collect(Collectors.toList());
                        lenses.removeAll(forCurrectField);
                        forCurrectField.forEach(al -> applyLense(al, fieldNode));
                        forCurrectField.forEach(al -> logger.debug("Patching field " + al.getTargetClassInternalName() + "#" + al.getTargetFieldName()));
                    }
                }

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
                    throw new RuntimeException("Can not find anchorTarget method of mandatory hook " + notInjected);
                } else {
                    logger.warning("Can not find anchorTarget method of hook " + notInjected);
                }
            }
        }
        return bytecode;
    }

    protected boolean isTargetMethod(AsmHook ah, String name, String desc) {
        return ah.isTargetMethod(name, desc);
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
