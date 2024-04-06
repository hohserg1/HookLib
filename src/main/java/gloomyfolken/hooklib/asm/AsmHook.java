package gloomyfolken.hooklib.asm;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import gloomyfolken.hooklib.api.HookPriority;
import gloomyfolken.hooklib.api.ReturnSolve;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.DOUBLE;
import static org.objectweb.asm.Type.FLOAT;
import static org.objectweb.asm.Type.LONG;
import static org.objectweb.asm.Type.*;

/**
 * Класс, отвечающий за установку одного хука в один метод.
 * Терминология:
 * hook (хук) - вызов вашего статического метода из стороннего кода (майнкрафта, форджа, других модов)
 * targetMethod (целевой метод) - метод, куда вставляется хук
 * targetClass (целевой класс) - класс, где находится метод, куда вставляется хук
 * hookMethod (хук-метод) - ваш статический метод, который вызывается из стороннего кода
 * hookClass (класс с хуком) - класс, в котором содержится хук-метод
 */
public class AsmHook implements AsmInjection, Cloneable {

    private String targetClassName; // через точки
    private String targetMethodName;
    private List<Type> targetMethodParameters = new ArrayList<Type>(2);
    private Type targetMethodReturnType; //если не задано, то не проверяется

    private String hooksClassName; // через точки
    private String hookMethodName;
    // -1 - значение return
    private List<Integer> transmittableVariableIds = new ArrayList<Integer>(2);
    private List<Type> hookMethodParameters = new ArrayList<Type>(2);
    private Type hookMethodReturnType = Type.VOID_TYPE;
    private boolean hasReturnValueParameter; // если в хук-метод передается значение из return

    private ReturnCondition returnCondition = ReturnCondition.NEVER;

    private HookInjectorFactory injectorFactory = HookInjectorFactory.BeginFactory.INSTANCE;
    private HookPriority priority = HookPriority.NORMAL;

    // может быть без возвращаемого типа
    private String targetMethodDescription1;
    private String targetMethodDescription2;

    private String hookMethodDescription;

    private boolean createMethod;
    private boolean isMandatory = true;
    private boolean requiredPrintLocalVariables = false;

    public String getTargetClassName() {
        return targetClassName;
    }

    private String getTargetClassInternalName() {
        return targetClassName.replace('.', '/');
    }

    private String getHookClassInternalName() {
        return hooksClassName.replace('.', '/');
    }

    protected boolean isTargetMethod(String name, String desc) {
        if (!name.equals(targetMethodName))
            return false;

        if (targetMethodReturnType == null)
            return desc.startsWith(targetMethodDescription1);

        return desc.equals(targetMethodDescription1) || desc.equals(targetMethodDescription2);
    }

    public boolean needToCreate() {
        return createMethod;
    }

    public boolean isMandatory() {
        return isMandatory;
    }

    protected boolean isRequiredPrintLocalVariables() {
        return requiredPrintLocalVariables;
    }

    protected HookInjectorFactory getInjectorFactory() {
        return injectorFactory;
    }

    private boolean hasHookMethod() {
        return true;
    }

    public void create(HookInjectorClassVisitor classVisitor) {
        ClassMetadataReader.MethodReference superMethod = classVisitor.transformer.classMetadataReader
                .findVirtualMethod(getTargetClassInternalName(), targetMethodName, targetMethodDescription1);
        // юзаем название суперметода, потому что findVirtualMethod может вернуть метод с другим названием
        MethodVisitor mv = classVisitor.visitMethod(Opcodes.ACC_PUBLIC,
                superMethod == null ? targetMethodName : superMethod.name, targetMethodDescription1, null, null);
        if (mv instanceof HookInjectorMethodVisitor) {
            HookInjectorMethodVisitor inj = (HookInjectorMethodVisitor) mv;
            inj.visitCode();
            inj.visitLabel(new Label());
            if (superMethod == null) {
                injectDefaultValue(inj, targetMethodReturnType);
            } else {
                injectSuperCall(inj, superMethod);
            }
            injectReturn(inj, targetMethodReturnType);
            inj.visitLabel(new Label());
            inj.visitMaxs(0, 0);
            inj.visitEnd();
        } else {
            throw new IllegalArgumentException("Hook injector not created");
        }
    }

    protected InsnList injectNode(MethodNode methodNode) {
        InsnList r = new InsnList();
        Type targetMethodReturnType = Type.getReturnType(methodNode.desc);

        int returnLocalId = -1;
        if (hasReturnValueParameter) {
            returnLocalId = methodNode.maxLocals;
            methodNode.maxLocals++;
            r.add(new VarInsnNode(targetMethodReturnType.getOpcode(ISTORE), returnLocalId)); //storeLocal
        }

        r.add(injectInvokeStaticNode(methodNode, returnLocalId, hookMethodName, hookMethodDescription));


        if (returnCondition == ReturnCondition.ALWAYS) {
            r.add(new InsnNode(targetMethodReturnType.getOpcode(IRETURN)));

        } else if (returnCondition == ReturnCondition.ON_SOLVE) {
            LabelNode doNotReturn = new LabelNode(new Label());

            int hookResultLocalId = methodNode.maxLocals;
            methodNode.maxLocals++;
            r.add(new VarInsnNode(hookMethodReturnType.getOpcode(ISTORE), hookResultLocalId));

            r.add(new VarInsnNode(hookMethodReturnType.getOpcode(ILOAD), hookResultLocalId));
            r.add(new TypeInsnNode(INSTANCEOF, Type.getDescriptor(ReturnSolve.Yes.class)));
            r.add(new JumpInsnNode(IFEQ, doNotReturn));

            if (targetMethodReturnType != VOID_TYPE) {
                r.add(new VarInsnNode(hookMethodReturnType.getOpcode(ILOAD), hookResultLocalId));
                r.add(new TypeInsnNode(CHECKCAST, Type.getInternalName(ReturnSolve.Yes.class)));
                Type boxed = objectToPrimitive.inverse().getOrDefault(targetMethodReturnType, targetMethodReturnType);
                r.add(new FieldInsnNode(GETFIELD, Type.getInternalName(ReturnSolve.Yes.class), "value", Type.getDescriptor(Object.class)));
                r.add(new TypeInsnNode(CHECKCAST, boxed.getInternalName()));
                if (boxed != targetMethodReturnType)
                    r.add(new MethodInsnNode(INVOKEVIRTUAL, boxed.getInternalName(), primitiveToUnboxingMethod.get(targetMethodReturnType), Type.getMethodDescriptor(targetMethodReturnType), false));
            }

            r.add(new InsnNode(targetMethodReturnType.getOpcode(IRETURN)));

            r.add(doNotReturn);
        }

        //кладем в стек значение, которое шло в return
        if (hasReturnValueParameter) {
            r.add(new VarInsnNode(targetMethodReturnType.getOpcode(ILOAD), returnLocalId));
        }

        return r;
    }

    protected void inject(HookInjectorMethodVisitor inj) {
        Type targetMethodReturnType = inj.methodType.getReturnType();

        // сохраняем значение, которое было передано return в локальную переменную
        int returnLocalId = -1;
        if (hasReturnValueParameter) {
            returnLocalId = inj.newLocal(targetMethodReturnType);
            inj.visitVarInsn(targetMethodReturnType.getOpcode(ISTORE), returnLocalId); //storeLocal
        }

        injectInvokeStatic(inj, returnLocalId, hookMethodName, hookMethodDescription);

        if (returnCondition == ReturnCondition.ALWAYS) {
            injectReturn(inj, targetMethodReturnType);

        } else if (returnCondition == ReturnCondition.ON_SOLVE) {
            Label doNotReturn = inj.newLabel();

            int hookResultLocalId = inj.newLocal(hookMethodReturnType);
            inj.visitVarInsn(hookMethodReturnType.getOpcode(ISTORE), hookResultLocalId);

            inj.visitVarInsn(hookMethodReturnType.getOpcode(ILOAD), hookResultLocalId);
            inj.visitTypeInsn(INSTANCEOF, Type.getInternalName(ReturnSolve.Yes.class));
            inj.visitJumpInsn(IFEQ, doNotReturn);

            if (targetMethodReturnType != VOID_TYPE) {
                inj.visitVarInsn(hookMethodReturnType.getOpcode(ILOAD), hookResultLocalId);
                inj.visitTypeInsn(CHECKCAST, Type.getInternalName(ReturnSolve.Yes.class));
                Type boxed = objectToPrimitive.inverse().getOrDefault(targetMethodReturnType, targetMethodReturnType);
                inj.visitFieldInsn(GETFIELD, Type.getInternalName(ReturnSolve.Yes.class), "value", Type.getDescriptor(Object.class));
                inj.visitTypeInsn(CHECKCAST, boxed.getInternalName());
                if (boxed != targetMethodReturnType)
                    inj.visitMethodInsn(INVOKEVIRTUAL, boxed.getInternalName(), primitiveToUnboxingMethod.get(targetMethodReturnType), Type.getMethodDescriptor(targetMethodReturnType), false);
            }
            injectReturn(inj, targetMethodReturnType);

            inj.visitLabel(doNotReturn);
        }

        //кладем в стек значение, которое шло в return
        if (hasReturnValueParameter) {
            injectLoad(inj, targetMethodReturnType, returnLocalId);
        }
    }

    private void injectLoad(HookInjectorMethodVisitor inj, Type parameterType, int variableId) {
        int opcode;
        if (parameterType == INT_TYPE || parameterType == BYTE_TYPE || parameterType == CHAR_TYPE ||
                parameterType == BOOLEAN_TYPE || parameterType == SHORT_TYPE) {
            opcode = ILOAD;
        } else if (parameterType == LONG_TYPE) {
            opcode = LLOAD;
        } else if (parameterType == FLOAT_TYPE) {
            opcode = FLOAD;
        } else if (parameterType == DOUBLE_TYPE) {
            opcode = DLOAD;
        } else {
            opcode = ALOAD;
        }
        inj.visitVarInsn(opcode, variableId);
    }

    private void injectSuperCall(HookInjectorMethodVisitor inj, ClassMetadataReader.MethodReference method) {
        int variableId = 0;
        for (int i = 0; i <= targetMethodParameters.size(); i++) {
            Type parameterType = i == 0 ? TypeHelper.getType(targetClassName) : targetMethodParameters.get(i - 1);
            injectLoad(inj, parameterType, variableId);
            if (parameterType.getSort() == DOUBLE || parameterType.getSort() == LONG) {
                variableId += 2;
            } else {
                variableId++;
            }
        }
        inj.visitMethodInsn(INVOKESPECIAL, method.owner, method.name, method.desc, false);
    }

    private void injectDefaultValue(HookInjectorMethodVisitor inj, Type targetMethodReturnType) {
        switch (targetMethodReturnType.getSort()) {
            case Type.VOID:
                break;
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                inj.visitInsn(Opcodes.ICONST_0);
                break;
            case FLOAT:
                inj.visitInsn(Opcodes.FCONST_0);
                break;
            case LONG:
                inj.visitInsn(Opcodes.LCONST_0);
                break;
            case DOUBLE:
                inj.visitInsn(Opcodes.DCONST_0);
                break;
            default:
                inj.visitInsn(Opcodes.ACONST_NULL);
                break;
        }
    }

    private void injectReturn(HookInjectorMethodVisitor inj, Type targetMethodReturnType) {
        inj.visitInsn(targetMethodReturnType.getOpcode(IRETURN));
    }

    private InsnList injectInvokeStaticNode(MethodNode methodNode, int returnLocalId, String name, String desc) {
        InsnList r = new InsnList();
        for (int i = 0; i < hookMethodParameters.size(); i++) {
            Type parameterType = hookMethodParameters.get(i);
            int variableId = transmittableVariableIds.get(i);
            if (AsmUtils.isStatic(methodNode)) {
                // если попытка передачи this из статического метода, то передаем null
                if (variableId == 0) {
                    r.add(new InsnNode(Opcodes.ACONST_NULL));
                    continue;
                }
                // иначе сдвигаем номер локальной переменной
                if (variableId > 0) variableId--;
            }
            if (variableId == -1) variableId = returnLocalId;
            r.add(new VarInsnNode(parameterType.getOpcode(ILOAD), variableId));
        }

        r.add(new MethodInsnNode(INVOKESTATIC, getHookClassInternalName(), name, desc, false));

        return r;
    }

    private void injectInvokeStatic(HookInjectorMethodVisitor inj, int returnLocalId, String name, String desc) {
        for (int i = 0; i < hookMethodParameters.size(); i++) {
            Type parameterType = hookMethodParameters.get(i);
            int variableId = transmittableVariableIds.get(i);
            if (inj.isStatic) {
                // если попытка передачи this из статического метода, то передаем null
                if (variableId == 0) {
                    inj.visitInsn(Opcodes.ACONST_NULL);
                    continue;
                }
                // иначе сдвигаем номер локальной переменной
                if (variableId > 0) variableId--;
            }
            if (variableId == -1) variableId = returnLocalId;
            injectLoad(inj, parameterType, variableId);
        }

        inj.visitMethodInsn(INVOKESTATIC, getHookClassInternalName(), name, desc, false);
    }

    public String getPatchedMethodName() {
        return targetClassName + '#' + targetMethodName + targetMethodDescription1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AsmHook: ");

        sb.append(targetClassName).append('#').append(targetMethodName);
        sb.append(targetMethodDescription1);
        sb.append(" -> ");
        sb.append(hooksClassName).append('#').append(hookMethodName);
        sb.append(hookMethodDescription);

        sb.append(", ReturnCondition=" + returnCondition);
        sb.append(", InjectorFactory: " + injectorFactory.getClass().getName());
        sb.append(", CreateMethod = " + createMethod);

        return sb.toString();
    }

    @Override
    public int compareTo(AsmInjection o) {
        if (o instanceof AsmHook) {
            AsmHook otherHook = (AsmHook) o;
            if (injectorFactory.isPriorityInverted && otherHook.injectorFactory.isPriorityInverted) {
                return priority.ordinal() > otherHook.priority.ordinal() ? -1 : 1;
            } else if (!injectorFactory.isPriorityInverted && !otherHook.injectorFactory.isPriorityInverted) {
                return priority.ordinal() > otherHook.priority.ordinal() ? 1 : -1;
            } else {
                return injectorFactory.isPriorityInverted ? 1 : -1;
            }
        } else
            return AsmInjection.super.compareTo(o);
    }

    public static Builder newBuilder() {
        return new AsmHook().new Builder();
    }

    public class Builder extends AsmHook {

        private Builder() {

        }

        /**
         * --- ОБЯЗАТЕЛЬНО ВЫЗВАТЬ ---
         * Определяет название класса, в который необходимо установить хук.
         *
         * @param className Название класса с указанием пакета, разделенное точками.
         *                  Например: net.minecraft.world.World
         */
        public Builder setTargetClass(String className) {
            AsmHook.this.targetClassName = className;
            return this;
        }

        /**
         * --- ОБЯЗАТЕЛЬНО ВЫЗВАТЬ ---
         * Определяет название метода, в который необходимо вставить хук.
         * Если нужно пропатчить конструктор, то в названии метода нужно указать <init>.
         *
         * @param methodName Название метода.
         *                   Например: getBlockId
         */
        public Builder setTargetMethod(String methodName) {
            AsmHook.this.targetMethodName = methodName;
            return this;
        }

        /**
         * --- ОБЯЗАТЕЛЬНО ВЫЗВАТЬ, ЕСЛИ У ЦЕЛЕВОГО МЕТОДА ЕСТЬ ПАРАМЕТРЫ ---
         * Добавляет один или несколько параметров к списку параметров целевого метода.
         * <p/>
         * Эти параметры используются, чтобы составить описание целевого метода.
         * Чтобы однозначно определить целевой метод, недостаточно только его названия - нужно ещё и описание.
         * <p/>
         * Примеры использования:
         * import static TypeHelper.*
         * //...
         * addTargetMethodParameters(Type.INT_TYPE)
         * Type worldType = getType("net.minecraft.world.World")
         * Type playerType = getType("net.minecraft.entity.player.EntityPlayer")
         * addTargetMethodParameters(worldType, playerType, playerType)
         *
         * @param parameterTypes Типы параметров целевого метода
         * @see TypeHelper
         */
        public Builder addTargetMethodParameters(Type... parameterTypes) {
            for (Type type : parameterTypes) {
                AsmHook.this.targetMethodParameters.add(type);
            }
            return this;
        }

        /**
         * Добавляет один или несколько параметров к списку параметров целевого метода.
         * Обёртка над addTargetMethodParameters(Type... parameterTypes), которая сама строит типы из названия.
         *
         * @param parameterTypeNames Названия классов параметров целевого метода.
         *                           Например: net.minecraft.world.World
         */

        public Builder addTargetMethodParameters(String... parameterTypeNames) {
            Type[] types = new Type[parameterTypeNames.length];
            for (int i = 0; i < parameterTypeNames.length; i++) {
                types[i] = TypeHelper.getType(parameterTypeNames[i]);
            }
            return addTargetMethodParameters(types);
        }

        /**
         * Изменяет тип, возвращаемый целевым методом.
         * Вовращаемый тип используется, чтобы составить описание целевого метода.
         * Чтобы однозначно определить целевой метод, недостаточно только его названия - нужно ещё и описание.
         * По умолчанию хук применяется ко всем методам, подходящим по названию и списку параметров.
         *
         * @param type Тип, возвращаемый целевым методом
         * @see TypeHelper
         */
        public Builder setTargetMethodReturnType(Type type) {
            AsmHook.this.targetMethodReturnType = type;
            return this;
        }

        /**
         * Изменяет тип, возвращаемый целевым методом.
         * Обёртка над setTargetMethodReturnType(Type returnType)
         *
         * @param returnType Название класса, экземпляр которого возвращает целевой метод
         */
        public Builder setTargetMethodReturnType(String returnType) {
            return setTargetMethodReturnType(TypeHelper.getType(returnType));
        }

        /**
         * --- ОБЯЗАТЕЛЬНО ВЫЗВАТЬ, ЕСЛИ НУЖЕН ХУК-МЕТОД, А НЕ ПРОСТО return SOME_CONSTANT ---
         * Определяет название класса, в котором находится хук-метод.
         *
         * @param className Название класса с указанием пакета, разделенное точками.
         *                  Например: net.myname.mymod.asm.MyHooks
         */
        public Builder setHookClass(String className) {
            AsmHook.this.hooksClassName = className;
            return this;
        }

        /**
         * --- ОБЯЗАТЕЛЬНО ВЫЗВАТЬ, ЕСЛИ НУЖЕН ХУК-МЕТОД, А НЕ ПРОСТО return SOME_CONSTANT ---
         * Определяет название хук-метода.
         * ХУК-МЕТОД ДОЛЖЕН БЫТЬ СТАТИЧЕСКИМ, А ПРОВЕРКИ НА ЭТО НЕТ. Будьте внимательны.
         *
         * @param methodName Название хук-метода.
         *                   Например: myFirstHook
         */
        public Builder setHookMethod(String methodName) {
            AsmHook.this.hookMethodName = methodName;
            return this;
        }

        /**
         * --- ОБЯЗАТЕЛЬНО ВЫЗВАТЬ, ЕСЛИ У ХУК-МЕТОДА ЕСТЬ ПАРАМЕТРЫ ---
         * Добавляет параметр в список параметров хук-метода.
         * В байткоде не сохраняются названия параметров. Вместо этого приходится использовать их номера.
         * Например, в классе EntityLivingBase есть метод attackEntityFrom(DamageSource damageSource, float maybeDamage).
         * В нём будут использоваться такие номера параметров:
         * 1 - damageSource
         * 2 - maybeDamage
         * ВАЖНЫЙ МОМЕНТ: LONG И DOUBLE "ЗАНИМАЮТ" ДВА НОМЕРА.
         * Теоретически, кроме параметров в хук-метод можно передать и локальные переменные, но их
         * номера сложнее посчитать.
         * Например, в классе Entity есть метод setPosition(double x, double y, double z).
         * В нём будут такие номера параметров:
         * 1 - x
         * 2 - пропущено
         * 3 - y
         * 4 - пропущено
         * 5 - z
         * 6 - пропущено
         * <p/>
         * Код этого метода таков:
         * //...
         * float f = ...;
         * float f1 = ...;
         * //...
         * В таком случае у f будет номер 7, а у f1 - 8.
         * <p/>
         * Если целевой метод static, то не нужно начинать отсчет локальных переменных с нуля, номера
         * будут смещены автоматически.
         *
         * @param parameterType Тип параметра хук-метода
         * @param variableId    ID значения, передаваемого в хук-метод
         * @throws IllegalStateException если не задано название хук-метода или класса, который его содержит
         */
        public Builder addHookMethodParameter(Type parameterType, int variableId) {
            AsmHook.this.hookMethodParameters.add(parameterType);
            AsmHook.this.transmittableVariableIds.add(variableId);
            return this;
        }

        /**
         * Добавляет параметр в список параметров целевого метода.
         * Обёртка над addHookMethodParameter(Type parameterType, int variableId)
         *
         * @param parameterTypeName Название типа параметра хук-метода.
         *                          Например: net.minecraft.world.World
         * @param variableId        ID значения, передаваемого в хук-метод
         */
        public Builder addHookMethodParameter(String parameterTypeName, int variableId) {
            return addHookMethodParameter(TypeHelper.getType(parameterTypeName), variableId);
        }

        /**
         * Добавляет в список параметров хук-метода целевой класс и передает хук-методу this.
         * Если целевой метод static, то будет передано null.
         *
         * @throws IllegalStateException если не задан хук-метод
         */
        public Builder addThisToHookMethodParameters() {
            AsmHook.this.hookMethodParameters.add(TypeHelper.getType(targetClassName));
            AsmHook.this.transmittableVariableIds.add(0);
            return this;
        }

        /**
         * Добавляет в список параметров хук-метода тип, возвращаемый целевым методом и
         * передает хук-методу значение, которое вернёт return.
         * Более формально, при вызове хук-метода указывает в качестве этого параметра верхнее значение в стеке.
         * На практике основное применение -
         * Например, есть такой код метода:
         * int foo = bar();
         * return foo;
         * Или такой:
         * return bar()
         * <p/>
         * В обоих случаях хук-методу можно передать возвращаемое значение перед вызовом return.
         *
         * @throws IllegalStateException если целевой метод возвращает void
         * @throws IllegalStateException если не задан хук-метод
         */
        public Builder addReturnValueToHookMethodParameters() {
            if (AsmHook.this.targetMethodReturnType == Type.VOID_TYPE) {
                throw new IllegalStateException("Target method's return type is void, it does not make sense to " +
                        "transmit its return value to hook method.");
            }
            AsmHook.this.hookMethodParameters.add(AsmHook.this.targetMethodReturnType);
            AsmHook.this.transmittableVariableIds.add(-1);
            AsmHook.this.hasReturnValueParameter = true;
            return this;
        }

        public Builder setReturnCondition(ReturnCondition condition) {
            AsmHook.this.returnCondition = condition;
            return this;
        }

        /**
         * --- ОБЯЗАТЕЛЬНО ВЫЗВАТЬ, ЕСЛИ ЦЕЛЕВОЙ МЕТОД ВОЗВРАЩАЕТ НЕ void, И ВЫЗВАН setReturnCondition ---
         * Задает значение, которое возвращается при вызове return после вызова хук-метода.
         * Следует вызывать после setReturnCondition.
         * По умолчанию возвращается void.
         * Кроме того, если value == ReturnValue.HOOK_RETURN_VALUE, то этот метод изменяет тип возвращаемого
         * значения хук-метода на тип, указанный в setTargetMethodReturnType()
         *
         * @param value возвращаемое значение
         * @throws IllegalStateException    если returnCondition == NEVER (т. е. если setReturnCondition() не вызывался).
         *                                  Нет смысла указывать возвращаемое значение, если return не вызывается.
         * @throws IllegalArgumentException если value == ReturnValue.HOOK_RETURN_VALUE, а тип возвращаемого значения
         *                                  целевого метода указан как void (или setTargetMethodReturnType ещё не вызывался).
         *                                  Нет смысла использовать значение, которое вернул хук-метод, если метод возвращает void.
         */
        public Builder setReturnValue(ReturnSort value) {
            if (AsmHook.this.returnCondition == ReturnCondition.NEVER) {
                throw new IllegalStateException("Current return condition is ReturnCondition.NEVER, so it does not " +
                        "make sense to specify the return value.");
            }
            Type returnType = AsmHook.this.targetMethodReturnType;
            if (value != ReturnSort.VOID && returnType == VOID_TYPE && AsmHook.this.returnCondition == ReturnCondition.ALWAYS) {
                throw new IllegalArgumentException("Target method return value is void, so it does not make sense to " +
                        "return anything else.");
            }
            if (value == ReturnSort.VOID && returnType != VOID_TYPE) {
                throw new IllegalArgumentException("Target method return value is not void, so it is impossible " +
                        "to return VOID.");
            }
            if (value == ReturnSort.PRIMITIVE_CONSTANT && returnType != null && !isPrimitive(returnType)) {
                throw new IllegalArgumentException("Target method return value is not a primitive, so it is " +
                        "impossible to return PRIVITIVE_CONSTANT.");
            }
            if (value == ReturnSort.NULL && returnType != null && isPrimitive(returnType)) {
                throw new IllegalArgumentException("Target method return value is a primitive, so it is impossible " +
                        "to return NULL.");
            }

            //AsmHook.this.returnSort = value;
            if (value == ReturnSort.HOOK_RETURN_VALUE) {
                AsmHook.this.hookMethodReturnType = AsmHook.this.targetMethodReturnType;
            }
            return this;
        }

        /**
         * Возвращает тип возвращаемого значения хук-метода, если кому-то сложно "вычислить" его самостоятельно.
         *
         * @return тип возвращаемого значения хук-метода
         */
        public Type getHookMethodReturnType() {
            return hookMethodReturnType;
        }

        /**
         * Напрямую указывает тип, возвращаемый хук-методом.
         *
         * @param type
         */
        protected void setHookMethodReturnType(Type type) {
            AsmHook.this.hookMethodReturnType = type;
        }

        private boolean isPrimitive(Type type) {
            return type.getSort() > 0 && type.getSort() < 9;
        }

        /**
         * Задает фабрику, которая создаст инжектор для этого хука.
         * Если говорить более человеческим языком, то этот метод определяет, где будет вставлен хук:
         * в начале метода, в конце или где-то ещё.
         * Если не создавать своих инжекторов, то можно использовать две фабрики:
         * AsmHook.ON_ENTER_FACTORY (вставляет хук на входе в метод, используется по умолчанию)
         * AsmHook.ON_EXIT_FACTORY (вставляет хук на выходе из метода)
         *
         * @param factory Фабрика, создающая инжектор для этого хука
         */
        public Builder setInjectorFactory(HookInjectorFactory factory) {
            AsmHook.this.injectorFactory = factory;
            return this;
        }

        /**
         * Задает приоритет хука.
         * Хуки с большим приоритетом вызаваются раньше.
         */
        public Builder setPriority(HookPriority priority) {
            AsmHook.this.priority = priority;
            return this;
        }

        /**
         * Позволяет не только вставлять хуки в существующие методы, но и добавлять новые. Это может понадобиться,
         * когда нужно переопределить метод суперкласса. Если супер-метод найден, то тело генерируемого метода
         * представляет собой вызов супер-метода. Иначе это просто пустой метод или return false/0/null в зависимости
         * от возвращаемого типа.
         */
        public Builder setCreateMethod(boolean createMethod) {
            AsmHook.this.createMethod = createMethod;
            return this;
        }

        /**
         * Позволяет объявить хук "обязательным" для запуска игры. В случае неудачи во время вставки такого хука
         * будет не просто выведено сообщение в лог, а крашнется игра.
         */
        public Builder setMandatory(boolean isMandatory) {
            AsmHook.this.isMandatory = isMandatory;
            return this;
        }

        public Builder setRequiredPrintLocalVariables(boolean requiredPrintLocalVariables) {
            AsmHook.this.requiredPrintLocalVariables = requiredPrintLocalVariables;
            return this;
        }

        private String getMethodDesc(Type returnType, List<Type> paramTypes) {
            Type[] paramTypesArray = paramTypes.toArray(new Type[0]);
            if (returnType == null) {
                String voidDesc = Type.getMethodDescriptor(Type.VOID_TYPE, paramTypesArray);
                return voidDesc.substring(0, voidDesc.length() - 1);
            } else {
                return Type.getMethodDescriptor(returnType, paramTypesArray);
            }
        }

        /**
         * Создает хук по заданным параметрам.
         *
         * @return полученный хук
         * @throws IllegalStateException если не был вызван какой-либо из обязательных методов
         */
        public AsmHook build() {
            AsmHook hook = AsmHook.this;

            if (hook.createMethod && hook.targetMethodReturnType == null) {
                hook.targetMethodReturnType = hook.hookMethodReturnType;
            }
            hook.targetMethodDescription1 = getMethodDesc(hook.targetMethodReturnType, hook.targetMethodParameters);
            Type maybePrimitive = objectToPrimitive.get(hook.targetMethodReturnType);
            if (maybePrimitive == null)
                hook.targetMethodDescription2 = hook.targetMethodDescription1;
            else {
                hook.targetMethodDescription2 = getMethodDesc(maybePrimitive, hook.targetMethodParameters);
            }

            hook.hookMethodDescription = Type.getMethodDescriptor(hook.hookMethodReturnType, hook.hookMethodParameters.toArray(new Type[0]));

            try {
                hook = (AsmHook) AsmHook.this.clone();
            } catch (CloneNotSupportedException impossible) {
            }

            if (hook.targetClassName == null) {
                throw new IllegalStateException("Target class name is not specified. " +
                        "Call setTargetClassName() before build().");
            }

            if (hook.targetMethodName == null) {
                throw new IllegalStateException("Target method name is not specified. " +
                        "Call setTargetMethodName() before build().");
            }

            if (!(hook.injectorFactory instanceof HookInjectorFactory.ReturnFactory) && hook.hasReturnValueParameter) {
                throw new IllegalStateException("Can not pass return value to hook method " +
                        "because hook location is not return insn.");
            }

            return hook;
        }
    }

    public static BiMap<Type, Type> objectToPrimitive = ImmutableBiMap.<Type, Type>builder()
            .put(Type.getType(Void.class), VOID_TYPE)
            .put(Type.getType(Boolean.class), BOOLEAN_TYPE)
            .put(Type.getType(Character.class), CHAR_TYPE)
            .put(Type.getType(Byte.class), BYTE_TYPE)
            .put(Type.getType(Short.class), SHORT_TYPE)
            .put(Type.getType(Integer.class), INT_TYPE)
            .put(Type.getType(Float.class), FLOAT_TYPE)
            .put(Type.getType(Long.class), LONG_TYPE)
            .put(Type.getType(Double.class), DOUBLE_TYPE)
            .build();

    public static Map<Type, String> primitiveToUnboxingMethod = ImmutableBiMap.<Type, String>builder()
            .put(BOOLEAN_TYPE, "booleanValue")
            .put(CHAR_TYPE, "charValue")
            .put(BYTE_TYPE, "byteValue")
            .put(SHORT_TYPE, "shortValue")
            .put(INT_TYPE, "intValue")
            .put(FLOAT_TYPE, "floatValue")
            .put(LONG_TYPE, "longValue")
            .put(DOUBLE_TYPE, "doubleValue")
            .build();
}
