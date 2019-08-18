package gloomyfolken.hooklib.asm;

import gloomyfolken.hooklib.asm.Hook.LocalVariable;
import gloomyfolken.hooklib.asm.Hook.ReturnValue;
import gloomyfolken.hooklib.asm.model.AsmHook;
import gloomyfolken.hooklib.asm.model.MapUtils;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.HashMap;

import static org.objectweb.asm.Opcodes.ASM5;

public class HookContainerParser {

    private HookClassTransformer transformer;
    private String currentClassName;

    public static final String HOOK_DESC = Type.getDescriptor(Hook.class);
    public static final String HOOK_LENS_DESC = Type.getDescriptor(HookLens.class);
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

    private void invalidHook(String message, String currentMethodName) {
        transformer.logger.warning("Found invalid hook " + currentClassName + "#" + currentMethodName);
        transformer.logger.warning(message);
    }

    private void createHook(String currentMethodName, String currentMethodDesc, boolean currentMethodPublicStatic, String currentClassName,
                            HashMap<String, Object> annotationValues, HashMap<Integer, Integer> parameterAnnotations) {
        {
            Type methodType = Type.getMethodType(currentMethodDesc);
            Type[] argumentTypes = methodType.getArgumentTypes();

            if (!currentMethodPublicStatic) {
                invalidHook("Hook method must be public and static.", currentMethodName);
                return;
            }

            if (argumentTypes.length < 1) {
                invalidHook("Hook method has no parameters. First parameter of a " +
                        "hook method must belong the type of the anchorTarget class.", currentMethodName);
                return;
            }

            if (argumentTypes[0].getSort() != Type.OBJECT) {
                invalidHook("First parameter of the hook method is not an object. First parameter of a " +
                        "hook method must belong the type of the anchorTarget class.", currentMethodName);
                return;
            }

            AsmHook.AsmHookBuilder builder1 = AsmHook.builder();

            builder1.targetMethodName((String) annotationValues.getOrDefault("targetMethod", currentMethodName));
            builder1.targetClassName(argumentTypes[0].getClassName());

            builder1.hookMethodName(currentMethodName);
            builder1.hookClassName(currentClassName);

            builder1.startArgumentsFill();

            builder1.hookMethodReturnType(methodType.getReturnType());

            builder1.addThisToHookMethodParameters();

            int currentParameterId = 1;
            for (int i = 1; i < argumentTypes.length; i++) {
                Type argType = argumentTypes[i];
                if (parameterAnnotations.containsKey(i)) {
                    int localId = parameterAnnotations.get(i);
                    if (localId == -1) {
                        builder1.targetMethodReturnType(argType);
                        builder1.addReturnValueToHookMethodParameters();
                    } else {
                        builder1.addHookMethodParameter(argType, localId);
                    }
                } else {
                    builder1.addTargetMethodParameter(argType);
                    builder1.addHookMethodParameter(argType, currentParameterId);
                    currentParameterId += argType == Type.LONG_TYPE || argType == Type.DOUBLE_TYPE ? 2 : 1;
                }
            }

            builder1.finishArgumentsFill();

            if (annotationValues.containsKey("at"))
                builder1.setAnchorForInject((HashMap<String, Object>) annotationValues.get("at"));


            if (annotationValues.containsKey("returnType"))
                builder1.targetMethodReturnType(TypeHelper.getType((String) annotationValues.get("returnType")));


            ReturnCondition returnCondition = ReturnCondition.NEVER;
            if (annotationValues.containsKey("returnCondition"))
                returnCondition = ReturnCondition.valueOf((String) annotationValues.get("returnCondition"));

            builder1.returnCondition(returnCondition);


            MapUtils.<String>maybeOfMapValue(annotationValues, "priority").map(HookPriority::valueOf).ifPresent(builder1::priority);
            MapUtils.<Boolean>maybeOfMapValue(annotationValues, "createMethod").ifPresent(builder1::createMethod);
            MapUtils.<Boolean>maybeOfMapValue(annotationValues, "isMandatory").ifPresent(builder1::isMandatory);

            if (returnCondition == ReturnCondition.ON_SOLVE && methodType.getReturnType() != Type.getType(ResultSolve.class)) {
                invalidHook("Hook method must return ResultSolve if returnCodition is ON_SOLVE.", currentMethodName);
                return;
            }

            try {
                transformer.registerHook(builder1.build());
            } catch (Exception e) {
                invalidHook(e.getMessage(), currentMethodName);
            }

        }

    }


    private class HookClassVisitor extends ClassNode {
        public HookClassVisitor() {
            super(ASM5);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            currentClassName = name.replace('/', '.');
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            return new HookMethodVisitor(name, desc, (access & Opcodes.ACC_PUBLIC) != 0 && (access & Opcodes.ACC_STATIC) != 0, currentClassName,
                    HookContainerParser.this);
        }


        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            return new HookLensFieldVisitor(access, name, desc, signature, currentClassName,HookContainerParser.this);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
        }
    }

    private static class HookLensFieldVisitor extends FieldVisitor {
        private final String name;
        private final String desc;
        private final String signature;
        private final String currentClassName;
        private final HookContainerParser hookContainerParser;
        private HashMap<String, Object> lensAnnotationValues = new HashMap<>();

        public HookLensFieldVisitor(int access, String name, String desc, String signature, String currentClassName, HookContainerParser hookContainerParser) {
            super(ASM5);
            this.name = name;
            this.desc = desc;
            this.signature = signature;
            this.currentClassName = currentClassName;
            this.hookContainerParser = hookContainerParser;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (HOOK_LENS_DESC.equals(desc))
                return new HookAnnotationVisitor(lensAnnotationValues);

            return super.visitAnnotation(desc, visible);
        }

        @Override
        public void visitEnd() {
            boolean isLens = desc.equals(Type.getDescriptor(Lens.class));
            if (isLens) {
                String fieldName = (String) lensAnnotationValues.getOrDefault("name", name);

                String[] substring = signature.substring(signature.indexOf('<') + 1, signature.indexOf('>')).split(";");
                String owner = Type.getType(substring[0]+";").getClassName();
                String value = Type.getType(substring[1]+";").getClassName();

                AsmHook.AsmHookBuilder builder = AsmHook.builder();

                builder.targetClassName(currentClassName);

                //hookContainerParser.transformer.registerHook(builder.build());



                //todo: create hook to <cinit> which inset to field right value instead of aconst_null

                System.out.println(owner + ", " + value);
            }
        }
    }

    private static class HookMethodVisitor extends MethodVisitor {
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

        private final String currentMethodName;
        private final String currentMethodDesc;
        private final boolean currentMethodPublicStatic;
        private final String currentClassName;
        private final HookContainerParser hookContainerParser;

        public HookMethodVisitor(String currentMethodName, String currentMethodDesc, boolean currentMethodPublicStatic, String currentClassName, HookContainerParser hookContainerParser) {
            super(ASM5);
            this.currentMethodName = currentMethodName;
            this.currentMethodDesc = currentMethodDesc;
            this.currentMethodPublicStatic = currentMethodPublicStatic;
            this.currentClassName = currentClassName;
            this.hookContainerParser = hookContainerParser;
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
                return new AnnotationVisitor(ASM5) {
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
                hookContainerParser.createHook(currentMethodName, currentMethodDesc, currentMethodPublicStatic, currentClassName, annotationValues, parameterAnnotations);
        }
    }

    public static class HookAnnotationVisitor extends AnnotationVisitor {

        private final HashMap<String, Object> map;

        public HookAnnotationVisitor(HashMap<String, Object> map) {
            super(ASM5);
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
            return new AnnotationVisitor(ASM5) {
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
