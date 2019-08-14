package gloomyfolken.hooklib.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.tree.InsnList;

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

    /**
     * Вставляет хук в произвольном методе
     */

    public static InjectionPoint.HookInjectorFactory MethodCallInjector = (asmHook, methodNode) -> {
    };

    /**
     * Вставляет хук в начале метода.
     */
    public static InjectionPoint.HookInjectorFactory HeadInjector = (asmHook, methodNode) -> {
        InsnList r=new InsnList();


        methodNode.instructions.insertBefore(methodNode.instructions.getFirst(),r);

    };

    /**
     * Вставляет хук на каждом выходе из метода, кроме выходов через throw.
     */
    public static InjectionPoint.HookInjectorFactory ReturnInjector = (asmHook, methodNode) -> {
    };

}
