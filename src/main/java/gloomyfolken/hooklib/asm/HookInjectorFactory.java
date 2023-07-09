package gloomyfolken.hooklib.asm;

import gloomyfolken.hooklib.api.Shift;
import org.objectweb.asm.MethodVisitor;

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

    abstract HookInjectorMethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc,
                                                          AsmHook hook, HookInjectorClassVisitor cv);


    static class BeginFactory extends HookInjectorFactory {

        public static final BeginFactory INSTANCE = new BeginFactory();

        private BeginFactory() {
        }

        @Override
        public HookInjectorMethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc,
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
        public HookInjectorMethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc,
                                                            AsmHook hook, HookInjectorClassVisitor cv) {
            return new HookInjectorMethodVisitor.ReturnVisitor(mv, access, name, desc, hook, cv, ordinal);
        }
    }

    static class MethodCallFactory extends HookInjectorFactory {
        public final String methodName;
        public final String methodDesc;
        public final int ordinal;
        public final Shift shift;

        MethodCallFactory(String methodName, String methodDesc, int ordinal, Shift shift) {
            this.methodName = methodName;
            this.methodDesc = methodDesc;
            this.ordinal = ordinal;
            this.shift = shift;
        }

        @Override
        HookInjectorMethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc, AsmHook hook, HookInjectorClassVisitor cv) {
            return new HookInjectorMethodVisitor.MethodCallVisitor(mv, access, name, desc, hook, cv, methodName, methodDesc, ordinal, shift);
        }
    }

}
