package gloomyfolken.hooklib.asm;

import gloomyfolken.hooklib.asm.Hook.LocalVariable;
import gloomyfolken.hooklib.asm.Hook.ReturnValue;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.objectweb.asm.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

public class HookContainerParser {

    private HookClassTransformer transformer;
    private String currentClassName;
    private String currentMethodName;
    private String currentMethodDesc;
    private boolean currentMethodPublicStatic;

    /*
    Ключ - название значения аннотации
     */
    private HashMap<String, Object> annotationValues = new HashMap<>();
    private HashMap<String, Object> sideOnlyValues = new HashMap<>();

    /*
    Ключ - номер параметра, значение - номер локальной переменной для перехвата
    или -1 для перехвата значения наверху стека.
     */
    private HashMap<Integer, Integer> parameterAnnotations = new HashMap<>();

    public static final String HOOK_DESC = Type.getDescriptor(Hook.class);
    public static final String SIDE_ONLY_DESC = Type.getDescriptor(SideOnly.class);
    public static final String LOCAL_DESC = Type.getDescriptor(LocalVariable.class);
    public static final String RETURN_DESC = Type.getDescriptor(ReturnValue.class);

    public HookContainerParser(HookClassTransformer transformer) {
        this.transformer = transformer;
    }

    protected void parseHooks(String className) {
        logHookParsing(() -> transformer.classMetadataReader.acceptVisitor(className, new HookClassVisitor()), className);
    }

    protected void parseHooks(byte[] classData) {
        logHookParsing(() -> transformer.classMetadataReader.acceptVisitor("", new HookClassVisitor()), "");
    }

    protected void parseHooks(String className, byte[] classData) {
        logHookParsing(() -> transformer.classMetadataReader.acceptVisitor(classData, new HookClassVisitor()), className);
    }

    private void logHookParsing(ThrowingRunnable<IOException> parse, String className) {
        transformer.logger.debug("Parsing hooks container " + className);
        try {
            parse.run();
        } catch (IOException e) {
            transformer.logger.severe("Can not parse hooks container " + className, e);
        }
    }

    private interface ThrowingRunnable<E extends Exception> {
        void run() throws E;
    }

    private void invalidHook(String message) {
        transformer.logger.warning("Found invalid hook " + currentClassName + "#" + currentMethodName);
        transformer.logger.warning(message);
    }

    private void createHook() {
        AsmHook.Builder builder = AsmHook.newBuilder();
        Type methodType = Type.getMethodType(currentMethodDesc);
        Type[] argumentTypes = methodType.getArgumentTypes();

        if (!currentMethodPublicStatic) {
            invalidHook("Hook method must be public and static.");
            return;
        }

        if (argumentTypes.length < 1) {
            invalidHook("Hook method has no parameters. First parameter of a " +
                    "hook method must belong the type of the target class.");
            return;
        }

        if (argumentTypes[0].getSort() != Type.OBJECT) {
            invalidHook("First parameter of the hook method is not an object. First parameter of a " +
                    "hook method must belong the type of the target class.");
            return;
        }

        builder.setTargetClass(argumentTypes[0].getClassName());

        builder.setTargetMethod((String) annotationValues.getOrDefault("targetMethod", currentMethodName));


        System.out.println(currentClassName);
        builder.setHookClass(currentClassName);
        builder.setHookMethod(currentMethodName);
        builder.addThisToHookMethodParameters();

        int currentParameterId = 1;
        for (int i = 1; i < argumentTypes.length; i++) {
            Type argType = argumentTypes[i];
            if (parameterAnnotations.containsKey(i)) {
                int localId = parameterAnnotations.get(i);
                if (localId == -1) {
                    builder.setTargetMethodReturnType(argType);
                    builder.addReturnValueToHookMethodParameters();
                } else {
                    builder.addHookMethodParameter(argType, localId);
                }
            } else {
                builder.addTargetMethodParameters(argType);
                builder.addHookMethodParameter(argType, currentParameterId);
                currentParameterId += argType == Type.LONG_TYPE || argType == Type.DOUBLE_TYPE ? 2 : 1;
            }
        }

        if (annotationValues.containsKey("at")) {
            builder.setAnchorForInject((HashMap<String, Object>) annotationValues.get("at"));
        }

        if (annotationValues.containsKey("returnType")) {
            builder.setTargetMethodReturnType((String) annotationValues.get("returnType"));
        }

        ReturnCondition returnCondition = ReturnCondition.NEVER;
        if (annotationValues.containsKey("returnCondition")) {
            returnCondition = ReturnCondition.valueOf((String) annotationValues.get("returnCondition"));
            builder.setReturnCondition(returnCondition);
        }

        if (returnCondition != ReturnCondition.NEVER) {
            Object primitiveConstant = getPrimitiveConstant();
            if (primitiveConstant != null) {
                builder.setReturnValue(gloomyfolken.hooklib.asm.ReturnValue.PRIMITIVE_CONSTANT);
                builder.setPrimitiveConstant(primitiveConstant);
            } else if (Boolean.TRUE.equals(annotationValues.get("returnNull"))) {
                builder.setReturnValue(gloomyfolken.hooklib.asm.ReturnValue.NULL);
            } else if (annotationValues.containsKey("returnAnotherMethod")) {
                builder.setReturnValue(gloomyfolken.hooklib.asm.ReturnValue.ANOTHER_METHOD_RETURN_VALUE);
                builder.setReturnMethod((String) annotationValues.get("returnAnotherMethod"));
            } else if (methodType.getReturnType() != Type.VOID_TYPE) {
                builder.setReturnValue(gloomyfolken.hooklib.asm.ReturnValue.HOOK_RETURN_VALUE);
            }
        }

        // setReturnCondition и setReturnValue сетают тип хук-метода, поэтому сетнуть его вручную можно только теперь
        builder.setHookMethodReturnType(methodType.getReturnType());

        if (returnCondition == ReturnCondition.ON_TRUE && methodType.getReturnType() != Type.BOOLEAN_TYPE) {
            invalidHook("Hook method must return boolean if returnCodition is ON_TRUE.");
            return;
        }
        if ((returnCondition == ReturnCondition.ON_NULL || returnCondition == ReturnCondition.ON_NOT_NULL) &&
                methodType.getReturnType().getSort() != Type.OBJECT &&
                methodType.getReturnType().getSort() != Type.ARRAY) {
            invalidHook("Hook method must return object if returnCodition is ON_NULL or ON_NOT_NULL.");
            return;
        }

        if (annotationValues.containsKey("priority")) {
            builder.setPriority(HookPriority.valueOf((String) annotationValues.get("priority")));
        }

        if (annotationValues.containsKey("createMethod")) {
            builder.setCreateMethod(Boolean.TRUE.equals(annotationValues.get("createMethod")));
        }
        if (annotationValues.containsKey("isMandatory")) {
            builder.setMandatory(Boolean.TRUE.equals(annotationValues.get("isMandatory")));
        }

        try {
            transformer.registerHook(builder.build());
        } catch (Exception e) {
            invalidHook(e.getMessage());
        }
    }

    private Object getPrimitiveConstant() {
        for (Entry<String, Object> entry : annotationValues.entrySet()) {
            if (entry.getKey().endsWith("Constant")) {
                return entry.getValue();
            }
        }
        return null;
    }


    private class HookClassVisitor extends ClassVisitor {
        public HookClassVisitor() {
            super(Opcodes.ASM5);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            currentClassName = name.replace('/', '.');
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            currentMethodName = name;
            currentMethodDesc = desc;
            currentMethodPublicStatic = (access & Opcodes.ACC_PUBLIC) != 0 && (access & Opcodes.ACC_STATIC) != 0;
            return new HookMethodVisitor();
        }
    }

    private class HookMethodVisitor extends MethodVisitor {

        public HookMethodVisitor() {
            super(Opcodes.ASM5);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (HOOK_DESC.equals(desc))
                return new HookAnnotationVisitor(annotationValues);

            if (SIDE_ONLY_DESC.equals(desc))
                return new HookAnnotationVisitor(sideOnlyValues);

            return super.visitAnnotation(desc, visible);
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
            if (RETURN_DESC.equals(desc)) {
                parameterAnnotations.put(parameter, -1);
            }
            if (LOCAL_DESC.equals(desc)) {
                return new AnnotationVisitor(Opcodes.ASM5) {
                    @Override
                    public void visit(String name, Object value) {
                        parameterAnnotations.put(parameter, (Integer) value);
                    }
                };
            }
            return null;
        }

        @Override
        public void visitEnd() {
            String currentSide = FMLLaunchHandler.side().toString();
            if (!annotationValues.isEmpty() && sideOnlyValues.getOrDefault("value", currentSide).equals(currentSide))
                createHook();

            annotationValues.clear();
            sideOnlyValues.clear();
            parameterAnnotations.clear();
            currentMethodName = currentMethodDesc = null;
            currentMethodPublicStatic = false;
        }
    }

    public static class HookAnnotationVisitor extends AnnotationVisitor {

        private final HashMap<String, Object> map;

        public HookAnnotationVisitor(HashMap<String, Object> map) {
            super(Opcodes.ASM5);
            this.map = map;
        }

        @Override
        public void visit(String name, Object value) {
            map.put(name, value);
        }

        /**
         * Вложенные аннотации
         */

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            map.put(name, new HashMap<String, Object>());
            return new AnnotationVisitor(Opcodes.ASM5) {
                @Override
                public void visit(String name1, Object value) {
                    ((HashMap<String, Object>) map.get(name)).put(name1, value);
                }

                @Override
                public void visitEnum(String name1, String desc, String value) {
                    ((HashMap<String, Object>) map.get(name)).put(name1, value);
                }
            };
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            visit(name, value);
        }
    }
}
