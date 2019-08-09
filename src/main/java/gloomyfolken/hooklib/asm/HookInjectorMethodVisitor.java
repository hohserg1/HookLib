package gloomyfolken.hooklib.asm;

import gloomyfolken.hooklib.minecraft.HookLibPlugin;
import gloomyfolken.hooklib.minecraft.MinecraftClassTransformer;
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
    protected Integer ordinal;

    protected HookInjectorMethodVisitor(MethodVisitor mv, int access, String name, String desc,
                                        AsmHook hook, HookInjectorClassVisitor cv) {
        super(Opcodes.ASM5, mv, access, name, desc);
        this.hook = hook;
        this.cv = cv;
        isStatic = (access & Opcodes.ACC_STATIC) != 0;
        methodName = name;
        methodType = Type.getMethodType(desc);

        ordinal = hook.getAnchorOrdinal();
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

    protected boolean visitOrderedHook() {
        if (ordinal == 0) {
            visitHook();
            ordinal = -2;
            return true;
        } else if (ordinal == -1) {
            visitHook();
            return true;
        } else if (ordinal > 0)
            ordinal -= 1;
        return false;
    }

    MethodVisitor getBasicVisitor() {
        return mv;
    }

    /**
     * Вставляет хук в произвольном методе
     */

    public static class MethodCallInjector extends HookInjectorMethodVisitor {

        public MethodCallInjector(MethodVisitor mv, int access, String name, String desc, AsmHook hook, HookInjectorClassVisitor cv) {
            super(mv, access, name, desc, hook, cv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            String targetName =
                    HookLibPlugin.isObfuscated()
                            ? MinecraftClassTransformer.instance.getMethodNames().getOrDefault(MinecraftClassTransformer.getMethodId(name), name)
                            : name;
            if (hook.getAnchorTarget().equals(targetName))
                switch (hook.getShift()) {

                    case BEFORE:
                        visitOrderedHook();
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                        break;
                    case AFTER:
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                        visitOrderedHook();
                        break;
                    case INSTEAD:
                        if (visitOrderedHook())
                            for (int i = 0; i < Type.getArgumentTypes(desc).length + 1; i++)
                                visitInsn(Opcodes.POP);
                        else
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                        break;
                }
            else
                super.visitMethodInsn(opcode, owner, name, desc, itf);

        }
    }

    /**
     * Вставляет хук в начале метода.
     */
    public static class Headinjector extends HookInjectorMethodVisitor {

        public Headinjector(MethodVisitor mv, int access, String name, String desc,
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
    public static class ReturnInjector extends HookInjectorMethodVisitor {

        public ReturnInjector(MethodVisitor mv, int access, String name, String desc,
                              AsmHook hook, HookInjectorClassVisitor cv) {
            super(mv, access, name, desc, hook, cv);
        }

        @Override
        protected void onMethodExit(int opcode) {
            if (opcode != Opcodes.ATHROW)
                visitOrderedHook();
        }
    }

}
