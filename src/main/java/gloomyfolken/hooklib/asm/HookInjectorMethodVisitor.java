package gloomyfolken.hooklib.asm;

import gloomyfolken.hooklib.api.Shift;
import gloomyfolken.hooklib.helper.Logger;
import gloomyfolken.hooklib.minecraft.HookLibPlugin;
import gloomyfolken.hooklib.minecraft.MinecraftClassTransformer;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Класс, непосредственно вставляющий хук в метод.
 * Чтобы указать конкретное место вставки хука, нужно создать класс extends HookInjector.
 */
public abstract class HookInjectorMethodVisitor extends AdviceAdapter {

    protected final AsmHook hook;
    protected final HookInjectorClassVisitor cv;
    public final String methodName;
    public final Type methodType;
    public final boolean isStatic;

    protected HookInjectorMethodVisitor(MethodVisitor mv, int access, String name, String desc,
                                        AsmHook hook, HookInjectorClassVisitor cv) {
        super(Opcodes.ASM5, mv, access, name, desc);
        this.hook = hook;
        this.cv = cv;
        isStatic = (access & Opcodes.ACC_STATIC) != 0;
        this.methodName = name;
        this.methodType = Type.getMethodType(desc);
    }

    /**
     * Вставляет хук в байткод.
     */
    protected final void visitHook() {
        if (!cv.visitingHook) {
            cv.visitingHook = true;
            hook.inject(this);
            cv.visitingHook = false;
        }
    }

    MethodVisitor getBasicVisitor() {
        return mv;
    }

    static class OrderedVisitor extends HookInjectorMethodVisitor {

        private int ordinal;

        protected OrderedVisitor(MethodVisitor mv, int access, String name, String desc, AsmHook hook, HookInjectorClassVisitor cv, int ordinal) {
            super(mv, access, name, desc, hook, cv);
            this.ordinal = ordinal;
        }

        protected boolean canVisitOrderedHook() {
            if (this.ordinal == 0) {
                this.ordinal = -2;
                return true;
            }
            if (this.ordinal == -1)
                return true;
            if (this.ordinal > 0)
                this.ordinal--;
            return false;
        }

        protected boolean visitOrderedHook() {
            if (canVisitOrderedHook()) {
                visitHook();
                return true;
            } else
                return false;
        }
    }

    /**
     * Вставляет хук в начале метода.
     */
    static class BeginVisitor extends HookInjectorMethodVisitor {

        public BeginVisitor(MethodVisitor mv, int access, String name, String desc,
                            AsmHook hook, HookInjectorClassVisitor cv) {
            super(mv, access, name, desc, hook, cv);
        }

        @Override
        protected void onMethodEnter() {
            visitHook();
        }

    }

    /**
     * Вставляет хук на каждом выходе из метода, кроме выходов через throw.
     */
    static class ReturnVisitor extends OrderedVisitor {

        public ReturnVisitor(MethodVisitor mv, int access, String name, String desc,
                             AsmHook hook, HookInjectorClassVisitor cv, int ordinal) {
            super(mv, access, name, desc, hook, cv, ordinal);
        }

        @Override
        protected void onMethodExit(int opcode) {
            if (opcode != Opcodes.ATHROW)
                visitOrderedHook();
        }
    }

    static class MethodCallVisitor extends OrderedVisitor {

        private final String methodName;
        private final String methodDesc;
        private final Shift shift;

        protected MethodCallVisitor(MethodVisitor mv, int access, String name, String desc, AsmHook hook, HookInjectorClassVisitor cv,
                                    String methodName, String methodDesc, int ordinal, Shift shift) {
            super(mv, access, name, desc, hook, cv, ordinal);
            this.methodName = methodName;
            this.methodDesc = methodDesc;
            this.shift = shift;
        }

        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            String targetName = HookLibPlugin.getObfuscated() ? MinecraftClassTransformer.instance.getMethodNames().getOrDefault(MinecraftClassTransformer.getMemberId("func_", name), name) : name;
            if (methodName.equals(targetName) && (methodDesc.isEmpty() || desc.startsWith(methodDesc))) {
                switch (shift) {
                    case BEFORE:
                        visitOrderedHook();
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                        break;
                    case AFTER:
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                        visitOrderedHook();
                        break;
                    case INSTEAD:
                        if (canVisitOrderedHook()) {
                            for (int i = 0; i < (Type.getArgumentTypes(desc)).length + ((opcode == 184) ? 0 : 1); i++)
                                visitInsn(87);
                            visitHook();
                        } else
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                        break;
                }
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }
}
