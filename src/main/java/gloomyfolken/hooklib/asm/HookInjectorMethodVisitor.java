package gloomyfolken.hooklib.asm;

import gloomyfolken.hooklib.api.Shift;
import gloomyfolken.hooklib.helper.Logger;
import gloomyfolken.hooklib.minecraft.HookLibPlugin;
import gloomyfolken.hooklib.minecraft.MinecraftClassTransformer;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gloomyfolken.hooklib.asm.AsmUtils.isPatternSensitive;

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

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        super.visitLocalVariable(name, desc, signature, start, end, index);
        if (hook.isRequiredPrintLocalVariables())
            Logger.instance.info(methodName + ":  @LocalVariable(" + index + ") " + Type.getType(desc).getClassName() + " " + name);
    }

    /**
     * Вставляет хук в байткод.
     */
    protected final void visitHook() {
        if (!cv.visitingHook) {
            cv.visitingHook = true;
            hook.inject(this);
            cv.visitingHook = false;
            cv.markInjected(hook);
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

                            Type[] argumentTypes = Type.getArgumentTypes(desc);
                            for (int i = argumentTypes.length - 1; i >= 0; i--) {
                                visitInsn(getPopOpcode(argumentTypes[i]));
                            }
                            if (opcode != Opcodes.INVOKESTATIC)
                                visitInsn(Opcodes.POP);

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

    private static int getPopOpcode(Type argumentType) {
        return argumentType == Type.LONG_TYPE || argumentType == Type.DOUBLE_TYPE ? Opcodes.POP2 : Opcodes.POP;
    }

    static class ExpressionVisitor extends MethodNode {
        private final MethodVisitor targetVisitor;
        private final AsmHook hook;
        private final HookInjectorClassVisitor cv;
        private final List<AbstractInsnNode> expressionPattern;
        private final int ordinal;
        private final Shift shift;
        private final Type patternType;

        public ExpressionVisitor(MethodVisitor mv, int access, String name, String desc, String signature, String[] exceptions,
                                 AsmHook hook, HookInjectorClassVisitor cv,
                                 List<AbstractInsnNode> expressionPattern, int ordinal, Shift shift, Type patternType) {
            super(Opcodes.ASM5, access, name, desc, signature, exceptions);
            targetVisitor = mv;
            this.hook = hook;
            this.cv = cv;
            this.expressionPattern = expressionPattern;
            this.ordinal = ordinal;
            this.shift = shift;
            this.patternType = patternType;
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
            super.visitLocalVariable(name, desc, signature, start, end, index);
            if (hook.isRequiredPrintLocalVariables())
                Logger.instance.info(this.name + ":  @LocalVariable(" + index + ") " + Type.getType(desc).getClassName() + " " + name);
        }

        @Override
        public void visitEnd() {
            List<Pair<AbstractInsnNode, AbstractInsnNode>> foundNodes = findSimilarCode();

            if (ordinal == -1) {
                for (Pair<AbstractInsnNode, AbstractInsnNode> e : foundNodes)
                    insertExpressionInjectCall(e);

                if (foundNodes.size() > 0)
                    cv.markInjected(hook);

            } else {
                if (foundNodes.size() > ordinal) {
                    insertExpressionInjectCall(foundNodes.get(ordinal));
                    cv.markInjected(hook);
                }
            }

            accept(targetVisitor);
        }

        private void insertExpressionInjectCall(Pair<AbstractInsnNode, AbstractInsnNode> found) {

            switch (shift) {
                case BEFORE:
                    instructions.insertBefore(found.getLeft(), hook.injectNode(this));

                    break;
                case INSTEAD:

                    /*
                    for (Type in : patternType.getArgumentTypes()) {
                        instructions.insertBefore(found.getLeft(), new InsnNode(getPopOpcode(in)));
                    }*/

                    instructions.insert(found.getRight(), hook.injectNode(this));
                    instructions.insert(found.getRight(), new InsnNode(getPopOpcode(patternType.getReturnType())));

                    /*
                    AbstractInsnNode i = found.getLeft();
                    while (i != found.getRight().getNext()) {
                        AbstractInsnNode next = i.getNext();
                        instructions.remove(i);
                        i = next;
                    }
                    */

                    //N значений есть на стеке до инструкций
                    //1 или 0 значений есть на стеке после инструкций
                    //нужно добавить N операций POP или POP2 вместо инструкций

                    /*
                    InsnList pops = new InsnList();
                    int stackCount = 0;
                    for (AbstractInsnNode insn : expressionPattern) {
                        List<InsnNode> in = getInAmount(insn);
                        int out = getOutAmount(insn);
                        for (int i = stackCount; i < in.size(); i++) {
                            pops.add(in.get(i));
                        }
                        stackCount += out;
                    }*/

                    break;
                case AFTER:
                    instructions.insert(found.getRight(), hook.injectNode(this));

                    break;
            }
        }

        private List<Pair<AbstractInsnNode, AbstractInsnNode>> findSimilarCode() {
            List<Pair<AbstractInsnNode, AbstractInsnNode>> r = new ArrayList<>();
            Map<Integer, Integer> colorCompliance = new HashMap<>();

            AbstractInsnNode start = null;
            int findingPosition = 0;

            AbstractInsnNode current = instructions.getFirst();
            while (current != null) {
                if (isPatternSensitive(current)) {
                    AbstractInsnNode currentExpectation = expressionPattern.get(findingPosition);

                    if (findingPosition == 0)
                        start = current;

                    if (equalsWithVarColor(current, currentExpectation, colorCompliance)) {
                        findingPosition++;

                        if (findingPosition == expressionPattern.size()) {
                            r.add(Pair.of(start, current));
                            findingPosition = 0;
                            colorCompliance.clear();
                        }

                        current = current.getNext();

                    } else {
                        findingPosition = 0;
                        colorCompliance.clear();
                        current = start.getNext();
                    }
                } else
                    current = current.getNext();
            }

            return r;
        }

        private boolean equalsWithVarColor(AbstractInsnNode current, AbstractInsnNode currentExpectation, Map<Integer, Integer> colorCompliance) {
            if (current instanceof VarInsnNode && currentExpectation instanceof VarInsnNode) {
                if (current.getOpcode() == currentExpectation.getOpcode()) {
                    VarInsnNode currentExpectation1 = (VarInsnNode) currentExpectation;
                    VarInsnNode current1 = (VarInsnNode) current;

                    Integer ePair = colorCompliance.get(currentExpectation1.var);
                    boolean colorEquals;
                    if (ePair == null) {
                        if (!colorCompliance.containsValue(current1.var)) {
                            colorCompliance.put(currentExpectation1.var, current1.var);
                            colorEquals = true;
                        } else
                            colorEquals = false;
                    } else
                        colorEquals = ePair == current1.var;
                    return colorEquals;
                } else
                    return false;
            } else {
                return current.getType() == currentExpectation.getType() &&
                        EqualsBuilder.reflectionEquals(current, currentExpectation, "prev", "next", "index", "previousInsn", "nextInsn");
            }
        }
    }
}
