package gloomyfolken.hooklib.asm;

import gloomyfolken.hooklib.api.Shift;
import lombok.Value;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.List;

/**
 * Фабрика, задающая тип инжектора хуков. Фактически, от выбора фабрики зависит то, в какие участки кода попадёт хук.
 * "Из коробки" доступно два типа инжекторов: MethodEnter, который вставляет хук на входе в метод,
 * и MethodExit, который вставляет хук на каждом выходе.
 */
public abstract class HookInjectorFactory {

    /**
     * Метод AdviceAdapter#visitInsn() - штука странная. Там почему-то вызов следующего MethodVisitor'a
     * производится после логики, а не до, как во всех остальных случаях. Поэтому для MethodExit приоритет
     * хуков инвертируется.
     */
    protected boolean isPriorityInverted = false;

    abstract MethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc, String signature, String[] exceptions,
                                              AsmHook hook, HookInjectorClassVisitor cv);


    static class BeginFactory extends HookInjectorFactory {

        public static final BeginFactory INSTANCE = new BeginFactory();

        private BeginFactory() {
        }

        @Override
        public MethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc, String signature, String[] exceptions,
                                                AsmHook hook, HookInjectorClassVisitor cv) {
            return new HookInjectorMethodVisitor.BeginVisitor(mv, access, name, desc, hook, cv);
        }

    }

    static class ReturnFactory extends HookInjectorFactory {
        public final int ordinal;

        public ReturnFactory(int ordinal) {
            this.ordinal = ordinal;
            isPriorityInverted = true;
        }

        @Override
        public MethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc, String signature, String[] exceptions,
                                                AsmHook hook, HookInjectorClassVisitor cv) {
            return new HookInjectorMethodVisitor.ReturnVisitor(mv, access, name, desc, hook, cv, ordinal);
        }
    }

    @Value
    static class MethodCallFactory extends HookInjectorFactory {
        public String methodName;
        public String methodDesc;
        public int ordinal;
        public Shift shift;

        @Override
        MethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc, String signature, String[] exceptions,
                                         AsmHook hook, HookInjectorClassVisitor cv) {
            return new HookInjectorMethodVisitor.MethodCallVisitor(mv, access, name, desc, hook, cv, methodName, methodDesc, ordinal, shift);
        }
    }

    @Value
    static class ExpressionFactory extends HookInjectorFactory {
        public List<AbstractInsnNode> expressionPattern;
        public Shift shift;
        public int ordinal;
        public Type patternType;

        @Override
        MethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc, String signature, String[] exceptions,
                                         AsmHook hook, HookInjectorClassVisitor cv) {
            return new HookInjectorMethodVisitor.ExpressionVisitor(mv, access, name, desc, signature, exceptions,
                    hook, cv, expressionPattern, ordinal, shift, patternType);
        }
    }

}
