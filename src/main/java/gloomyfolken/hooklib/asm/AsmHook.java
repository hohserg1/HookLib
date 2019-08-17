package gloomyfolken.hooklib.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;
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
public class AsmHook implements Cloneable, Comparable<AsmHook> {

    public Anchor getAnchor() {
        return anchor;
    }

    private Anchor anchor = new Anchor();

    public static class Anchor {
        InjectionPoint point = InjectionPoint.HEAD;

        Shift shift = Shift.AFTER;

        String target = "";

        int ordinal = -1;
    }

    private String targetClassName; // через точки
    private String targetMethodName;
    private List<Type> targetMethodParameters = new ArrayList<>(2);

    public Type getTargetMethodReturnType() {
        return targetMethodReturnType;
    }

    private Type targetMethodReturnType; //если не задано, то не проверяется

    private String hooksClassName; // через точки
    private String hookMethodName;
    // -1 - значение return
    private List<Integer> transmittableVariableIds = new ArrayList<>(2);

    public List<Integer> getTransmittableVariableIds() {
        return transmittableVariableIds;
    }

    public List<Type> getHookMethodParameters() {
        return hookMethodParameters;
    }

    private List<Type> hookMethodParameters = new ArrayList<>(2);

    public Type getHookMethodReturnType() {
        return hookMethodReturnType;
    }

    private Type hookMethodReturnType = Type.VOID_TYPE;

    public boolean hasReturnValueParameter() {
        return hasReturnValueParameter;
    }

    private boolean hasReturnValueParameter; // если в хук-метод передается значение из return

    public ReturnCondition getReturnCondition() {
        return returnCondition;
    }

    private ReturnCondition returnCondition = ReturnCondition.NEVER;
    private ReturnValue returnValue = ReturnValue.VOID;
    private Object primitiveConstant;

    private HookPriority priority = HookPriority.NORMAL;

    // может быть без возвращаемого типа
    private String targetMethodDescription;
    private String hookMethodDescription;
    private String returnMethodName;
    // может быть без возвращаемого типа
    private String returnMethodDescription;

    private boolean createMethod;
    private boolean isMandatory;

    public InjectionPoint getAnchorPoint() {
        return anchor.point;
    }

    public String getAnchorTarget() {
        return anchor.target;
    }

    public Shift getShift() {
        return anchor.shift;
    }

    public Integer getAnchorOrdinal() {
        return anchor.ordinal;
    }

    protected String getTargetClassName() {
        return targetClassName;
    }

    private String getTargetClassInternalName() {
        return targetClassName.replace('.', '/');
    }

    public String getHookMethodName() {
        return hookMethodName;
    }

    public String getHookMethodDescription() {
        return hookMethodDescription;
    }

    public String getHookClassName() {
        return hooksClassName;
    }

    public String getHookClassInternalName() {
        return hooksClassName.replace('.', '/');
    }

    protected boolean isTargetMethod(String name, String desc) {
        return (targetMethodReturnType == null && desc.startsWith(targetMethodDescription) ||
                desc.equals(targetMethodDescription)) && name.equals(targetMethodName);
    }

    protected boolean getCreateMethod() {
        return createMethod;
    }

    protected boolean isMandatory() {
        return isMandatory;
    }

    private boolean hasHookMethod() {
        return hookMethodName != null && hooksClassName != null;
    }


    public String getPatchedMethodName() {
        return targetClassName + '#' + targetMethodName + targetMethodDescription;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AsmHook: ");

        sb.append(targetClassName).append('#').append(targetMethodName);
        sb.append(targetMethodDescription);
        sb.append(" -> ");
        sb.append(hooksClassName).append('#').append(hookMethodName);
        sb.append(hookMethodDescription);

        sb.append(", ReturnCondition=" + returnCondition);
        sb.append(", ReturnValue=" + returnValue);
        if (returnValue == ReturnValue.PRIMITIVE_CONSTANT) sb.append(", Constant=" + primitiveConstant);
        //sb.append(", InjectorFactory: " + getAnchorPoint().factory);
        sb.append(", CreateMethod = " + createMethod);

        return sb.toString();
    }

    @Override
    public int compareTo(AsmHook o) {
        if (getAnchorPoint().isPriorityInverted && o.getAnchorPoint().isPriorityInverted) {
            return priority.ordinal() > o.priority.ordinal() ? -1 : 1;
        } else if (!getAnchorPoint().isPriorityInverted && !o.getAnchorPoint().isPriorityInverted) {
            return priority.ordinal() > o.priority.ordinal() ? 1 : -1;
        } else {
            return getAnchorPoint().isPriorityInverted ? 1 : -1;
        }
    }

    public static Builder newBuilder() {
        return new AsmHook().new Builder();
    }

    public class Builder extends AsmHook {

        private Builder() {

        }

        public Builder setAnchorForInject(HashMap<String, Object> anchor) {
            AsmHook.this.anchor.ordinal = (Integer) anchor.getOrDefault("ordinal", -1);
            AsmHook.this.anchor.point = InjectionPoint.valueOf((String) anchor.get("point"));
            AsmHook.this.anchor.shift = Shift.valueOf((String) anchor.get("shift"));
            AsmHook.this.anchor.target = (String) anchor.get("anchorTarget");
            return this;
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
         * import static gloomyfolken.hooklib.asm.TypeHelper.*
         * //...
         * addTargetMethodParameter(Type.INT_TYPE)
         * Type worldType = getType("net.minecraft.world.World")
         * Type playerType = getType("net.minecraft.entity.player.EntityPlayer")
         * addTargetMethodParameter(worldType, playerType, playerType)
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
         * Изменяет тип, возвращаемый целевым методом.
         * Вовращаемый тип используется, чтобы составить описание целевого метода.
         * Чтобы однозначно определить целевой метод, недостаточно только его названия - нужно ещё и описание.
         * По умолчанию хук применяется ко всем методам, подходящим по названию и списку параметров.
         *
         * @param returnType Тип, возвращаемый целевым методом
         * @see TypeHelper
         */
        public Builder setTargetMethodReturnType(Type returnType) {
            AsmHook.this.targetMethodReturnType = returnType;
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
         * Например, в классе EntityLivingBase есть метод attackEntityFrom(DamageSource damageSource, float damage).
         * В нём будут использоваться такие номера параметров:
         * 1 - damageSource
         * 2 - damage
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
            if (!AsmHook.this.hasHookMethod()) {
                throw new IllegalStateException("Hook method is not specified, so can not append " +
                        "parameter to its parameters list.");
            }
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
            if (!AsmHook.this.hasHookMethod()) {
                throw new IllegalStateException("Hook method is not specified, so can not append " +
                        "parameter to its parameters list.");
            }
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
            if (!AsmHook.this.hasHookMethod()) {
                throw new IllegalStateException("Hook method is not specified, so can not append " +
                        "parameter to its parameters list.");
            }
            if (AsmHook.this.targetMethodReturnType == Type.VOID_TYPE) {
                throw new IllegalStateException("Target method's return type is void, it does not make sense to " +
                        "transmit its return value to hook method.");
            }
            AsmHook.this.hookMethodParameters.add(AsmHook.this.targetMethodReturnType);
            AsmHook.this.transmittableVariableIds.add(-1);
            AsmHook.this.hasReturnValueParameter = true;
            return this;
        }

        /**
         * Задает условие, при котором после вызова хук-метода вызывается return.
         * По умолчанию return не вызывается вообще.
         * Кроме того, этот метод изменяет тип возвращаемого значения хук-метода:
         * NEVER -> void
         * ALWAYS -> void
         * ON_SOLVE -> boolean
         * ON_NULL -> Object
         * ON_NOT_NULL -> Object
         *
         * @param condition Условие выхода после вызова хук-метода
         * @throws IllegalArgumentException если condition == ON_SOLVE, ON_NULL или ON_NOT_NULL, но не задан хук-метод.
         * @see ReturnCondition
         */
        public Builder setReturnCondition(ReturnCondition condition) {
            if (condition.requiresCondition && AsmHook.this.hookMethodName == null) {
                throw new IllegalArgumentException("Hook method is not specified, so can not use return " +
                        "condition that depends on hook method.");
            }

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
        public Builder setReturnValue(ReturnValue value) {
            if (AsmHook.this.returnCondition == ReturnCondition.NEVER) {
                throw new IllegalStateException("Current return condition is ReturnCondition.NEVER, so it does not " +
                        "make sense to specify the return value.");
            }
            Type returnType = AsmHook.this.targetMethodReturnType;
            if (value != ReturnValue.VOID && returnType == VOID_TYPE) {
                throw new IllegalArgumentException("Target method return value is void, so it does not make sense to " +
                        "return anything else.");
            }
            if (value == ReturnValue.VOID && returnType != VOID_TYPE) {
                throw new IllegalArgumentException("Target method return value is not void, so it is impossible " +
                        "to return VOID.");
            }
            if (value == ReturnValue.PRIMITIVE_CONSTANT && returnType != null && !isPrimitive(returnType)) {
                throw new IllegalArgumentException("Target method return value is not a primitive, so it is " +
                        "impossible to return PRIVITIVE_CONSTANT.");
            }
            if (value == ReturnValue.NULL && returnType != null && isPrimitive(returnType)) {
                throw new IllegalArgumentException("Target method return value is a primitive, so it is impossible " +
                        "to return NULL.");
            }
            if (value == ReturnValue.HOOK_RETURN_VALUE && !hasHookMethod()) {
                throw new IllegalArgumentException("Hook method is not specified, so can not use return " +
                        "value that depends on hook method.");
            }

            AsmHook.this.returnValue = value;
            if (value == ReturnValue.HOOK_RETURN_VALUE) {
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
         * --- ОБЯЗАТЕЛЬНО ВЫЗВАТЬ, ЕСЛИ ВОЗВРАЩАЕМОЕ ЗНАЧЕНИЕ УСТАНОВЛЕНО НА PRIMITIVE_CONSTANT ---
         * Следует вызывать после setReturnValue(ReturnValue.PRIMITIVE_CONSTANT)
         * Задает константу, которая будет возвращена при вызове return.
         * Класс заданного объекта должен соответствовать примитивному типу.
         * Например, если целевой метод возвращает int, то в этот метод должен быть передан объект класса Integer.
         *
         * @param constant Объект, класс которого соответствует примитиву, который следует возвращать.
         * @throws IllegalStateException    если возвращаемое значение не установлено на PRIMITIVE_CONSTANT
         * @throws IllegalArgumentException если класс объекта constant не является обёрткой
         *                                  для примитивного типа, который возвращает целевой метод.
         */
        public Builder setPrimitiveConstant(Object constant) {
            if (AsmHook.this.returnValue != ReturnValue.PRIMITIVE_CONSTANT) {
                throw new IllegalStateException("Return value is not PRIMITIVE_CONSTANT, so it does not make sence" +
                        "to specify that constant.");
            }
            Type returnType = AsmHook.this.targetMethodReturnType;
            if (returnType == BOOLEAN_TYPE && !(constant instanceof Boolean) ||
                    returnType == CHAR_TYPE && !(constant instanceof Character) ||
                    returnType == BYTE_TYPE && !(constant instanceof Byte) ||
                    returnType == SHORT_TYPE && !(constant instanceof Short) ||
                    returnType == INT_TYPE && !(constant instanceof Integer) ||
                    returnType == LONG_TYPE && !(constant instanceof Long) ||
                    returnType == FLOAT_TYPE && !(constant instanceof Float) ||
                    returnType == DOUBLE_TYPE && !(constant instanceof Double)) {
                throw new IllegalArgumentException("Given object class does not math anchorTarget method return type.");
            }

            AsmHook.this.primitiveConstant = constant;
            return this;
        }

        /**
         * --- ОБЯЗАТЕЛЬНО ВЫЗВАТЬ, ЕСЛИ ВОЗВРАЩАЕМОЕ ЗНАЧЕНИЕ УСТАНОВЛЕНО НА ANOTHER_METHOD_RETURN_VALUE ---
         * Следует вызывать после setReturnValue(ReturnValue.ANOTHER_METHOD_RETURN_VALUE)
         * Задает метод, результат вызова которого будет возвращён при вызове return.
         *
         * @param methodName название метода, результат вызова которого следует возвращать
         * @throws IllegalStateException если возвращаемое значение не установлено на ANOTHER_METHOD_RETURN_VALUE
         */
        public Builder setReturnMethod(String methodName) {
            if (AsmHook.this.returnValue != ReturnValue.ANOTHER_METHOD_RETURN_VALUE) {
                throw new IllegalStateException("Return value is not ANOTHER_METHOD_RETURN_VALUE, " +
                        "so it does not make sence to specify that method.");
            }

            AsmHook.this.returnMethodName = methodName;
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
            hook.targetMethodDescription = getMethodDesc(hook.targetMethodReturnType, hook.targetMethodParameters);

            if (hook.hasHookMethod()) {
                hook.hookMethodDescription = Type.getMethodDescriptor(hook.hookMethodReturnType,
                        hook.hookMethodParameters.toArray(new Type[0]));
            }
            if (hook.returnValue == ReturnValue.ANOTHER_METHOD_RETURN_VALUE) {
                hook.returnMethodDescription = getMethodDesc(hook.targetMethodReturnType, hook.hookMethodParameters);
            }

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

            if (hook.targetMethodName.equals("<init>") && hook.returnCondition != ReturnCondition.NEVER) {
                HookClassTransformer.logger.warning("Return from constructor before final fields initialized isn't recommended" +
                        "Don't use targetMethodName = <init> with InjectionPoint.HEAD and with not ReturnCondition.NEVER");
            }

            if (hook.returnValue == ReturnValue.PRIMITIVE_CONSTANT && hook.primitiveConstant == null) {
                throw new IllegalStateException("Return value is PRIMITIVE_CONSTANT, but the constant is not " +
                        "specified. Call setReturnValue() before build().");
            }

            if (hook.returnValue == ReturnValue.ANOTHER_METHOD_RETURN_VALUE && hook.returnMethodName == null) {
                throw new IllegalStateException("Return value is ANOTHER_METHOD_RETURN_VALUE, but the method is not " +
                        "specified. Call setReturnMethod() before build().");
            }

            if (!isReturnHook(hook) && hook.hasReturnValueParameter) {
                throw new IllegalStateException("Can not pass return value to hook method " +
                        "because hook location is not return insn.");
            }

            return hook;
        }

        private boolean isReturnHook(AsmHook hook) {
            return hook.getAnchorPoint() == InjectionPoint.RETURN;
        }

    }

}