package gloomyfolken.hooklib.asm;

import com.google.common.collect.*;
import gloomyfolken.hooklib.api.*;
import gloomyfolken.hooklib.asm.AsmUtils.OpcodeDetails;
import gloomyfolken.hooklib.asm.SignatureExtractor.FlatTypeRepr;
import gloomyfolken.hooklib.asm.SignatureExtractor.ParametrizedTypeRepr;
import gloomyfolken.hooklib.asm.SignatureExtractor.TypeRepr;
import gloomyfolken.hooklib.asm.injections.*;
import gloomyfolken.hooklib.helper.Logger;
import gloomyfolken.hooklib.helper.SideOnlyUtils;
import gloomyfolken.hooklib.helper.annotation.AnnotationMap;
import gloomyfolken.hooklib.helper.annotation.AnnotationUtils;
import gloomyfolken.hooklib.minecraft.HookLoader;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.*;

public class HookContainerParser {
    final Map<String, String> classImageNameToPrivateClassName;

    public HookContainerParser(Collection<ClassNode> privateClassImages) {
        this.classImageNameToPrivateClassName = privateClassImages.stream().collect(Collectors.toMap(
            cn -> cn.name.replace('/', '.'),
            cn -> AnnotationUtils.annotationOf(cn).get(PrivateClass.class).value()
        ));
    }

    public ListMultimap<String, AsmInjection> makePrivateClassImageHooks() {
        return classImageNameToPrivateClassName.values().stream()
            .distinct()
            .collect(Multimaps.toMultimap(Function.identity(), AsmClassAccessFix::new, ArrayListMultimap::create));
    }


    private Stream<AsmInjection> invalidHook(String message, ClassNode classNode, MethodNode methodNode) {
        Logger.instance.warning("Found invalid hook " + classNode.name.replace('/', '.') + "#" + methodNode.name);
        Logger.instance.warning(message);
        return Stream.empty();
    }

    private Stream<AsmHook> invalidFieldLens(String message, ClassNode classNode, FieldNode fieldNode) {
        Logger.instance.warning("Found invalid hook lens " + classNode.name.replace('/', '.') + "#" + fieldNode.name);
        Logger.instance.warning(message);
        return Stream.empty();
    }

    private Stream<AsmInjection> invalidMethodLens(String message, ClassNode classNode, MethodNode methodNode) {
        Logger.instance.warning("Found invalid hook lens " + classNode.name.replace('/', '.') + "#" + methodNode.name);
        Logger.instance.warning(message);
        return Stream.empty();
    }

    private boolean checkRegularConditions(ClassNode classNode, MethodNode methodNode, Type[] argumentTypes) {
        if (!(AsmUtils.isPublic(methodNode) && AsmUtils.isStatic(methodNode))) {
            invalidHook("Hook method must be public and static.", classNode, methodNode);
            return false;
        }

        if (argumentTypes.length < 1) {
            invalidHook("Hook method has no parameters. First parameter of a hook method must belong the type of the target class.", classNode, methodNode);
            return false;
        }

        if (argumentTypes[0].getSort() != Type.OBJECT) {
            invalidHook("First parameter of the hook method is not an object. First parameter of a hook method must belong the type of the target class.", classNode, methodNode);
            return false;
        }

        return true;
    }

    private boolean checkRegularConditionsMethodLens(ClassNode classNode, MethodNode methodNode, Type[] argumentTypes) {
        if (!AsmUtils.isStatic(methodNode)) {
            invalidMethodLens("Hook lens must be static.", classNode, methodNode);
            return false;
        }

        if (argumentTypes.length < 1) {
            invalidMethodLens("Hook lens has no parameters. First parameter of a lens method must belong the type of the target class.", classNode, methodNode);
            return false;
        }

        if (argumentTypes[0].getSort() != Type.OBJECT) {
            invalidMethodLens("First parameter of the hook lens is not an object. First parameter of a lens method must belong the type of the target class.", classNode, methodNode);
            return false;
        }

        return true;
    }

    public Stream<AsmInjection> parseHooks(ClassNode classNode) {
        Optional<MethodNode> maybeClinit = classNode.methods.stream().filter(mn -> mn.name.equals(Constants.STATIC_INITIALIZER_NAME)).findFirst();
        return Stream.concat(
            classNode.methods.stream().flatMap(methodNode -> {
                try {
                    AnnotationMap annotationMap = AnnotationUtils.annotationOf(methodNode);

                    if (!SideOnlyUtils.isValidSide(annotationMap))
                        return Stream.empty();

                    Hook hookAnnotation = annotationMap.get(Hook.class);
                    MethodLens methodLensAnnotation = annotationMap.get(MethodLens.class);

                    if (hookAnnotation != null)
                        return parseRegularHook(classNode, methodNode, annotationMap, hookAnnotation);

                    if (methodLensAnnotation != null)
                        return parseMethodLens(classNode, methodNode, annotationMap, methodLensAnnotation);


                } catch (Throwable e) {
                    throw new UnexpectedHookParsingError(classNode.name, methodNode.name, e);
                }

                return Stream.empty();
            }),
            classNode.fields.stream().flatMap(fieldNode -> {
                AnnotationMap annotationMap = AnnotationUtils.annotationOf(fieldNode);
                FieldLens lensAnnotation = annotationMap.get(FieldLens.class);
                if (lensAnnotation != null) {
                    return parseFieldLens(classNode, fieldNode, lensAnnotation, maybeClinit);
                }

                return Stream.empty();
            })
        );
    }

    private Stream<? extends AsmInjection> parseFieldLens(ClassNode classNode, FieldNode fieldNode, FieldLens lensAnnotation, Optional<MethodNode> maybeClinit) {
        if (Type.getType(fieldNode.desc).getClassName().equals(FieldAccessor.class.getCanonicalName())) {
            TypeRepr typeRepr = SignatureExtractor.fromField(fieldNode);

            if (typeRepr instanceof FlatTypeRepr)
                return invalidFieldLens("field lens type is raw FieldAccessor, should be parametrized", classNode, fieldNode);

            List<TypeRepr> parameters = ((ParametrizedTypeRepr) typeRepr).parameters;
            Type targetClassType = parameters.get(0).getRawType();
            Type targetFieldType = parameters.get(1).getRawType();

            if (fieldNode.invisibleTypeAnnotations != null)
                for (TypeAnnotationNode a : fieldNode.invisibleTypeAnnotations) {
                    if (new TypeReference(a.typeRef).getSort() == TypeReference.FIELD)
                        if (a.typePath.getLength() == 1 && a.typePath.getStep(0) == TypePath.TYPE_ARGUMENT)
                            if (a.desc.equals(Type.getDescriptor(Primitive.class))) {
                                if (a.typePath.getStepArgument(0) == 1) {
                                    Type maybePrimitive = AsmUtils.objectToPrimitive.get(targetFieldType);
                                    if (maybePrimitive == null)
                                        return invalidFieldLens("@Primitive used at non-primitive type", classNode, fieldNode);
                                    targetFieldType = maybePrimitive;
                                    break;
                                }
                            } else if (a.desc.equals(Type.getDescriptor(PrivateClass.class))) {
                                Type typeFromAnno = Type.getObjectType(AnnotationUtils.<PrivateClass>annotation(a).value().replace('.', '/'));
                                int parameterIndex = a.typePath.getStepArgument(0);
                                if (parameterIndex == 0) {
                                    targetClassType = typeFromAnno;
                                } else if (parameterIndex == 1) {
                                    targetFieldType = typeFromAnno;
                                }
                            }
                }

            String targetClassName = targetClassType.getClassName();
            targetClassName = classImageNameToPrivateClassName.getOrDefault(targetClassName, targetClassName);
            targetClassType = Type.getObjectType(targetClassName.replace('.', '/'));

            if (!AsmUtils.isPrimitive(targetFieldType)) {
                targetFieldType = AsmUtils.mapBy(targetFieldType, typeName -> classImageNameToPrivateClassName.getOrDefault(typeName.replace('/', '.'), typeName).replace('.', '/'));
            }

            String targetFieldName = !lensAnnotation.targetField().isEmpty() ? lensAnnotation.targetField() : fieldNode.name;

            String setterDesc = Type.getMethodDescriptor(Type.VOID_TYPE, targetClassType, targetFieldType);
            String getterDesc = Type.getMethodDescriptor(targetFieldType, targetClassType);

            InsnList defaultValue;
            if (lensAnnotation.createField() && maybeClinit.isPresent()) {
                defaultValue = findDefaultValue(maybeClinit.get().instructions, classNode, fieldNode.name, targetFieldType);
            } else
                defaultValue = null;

            return Stream.concat(
                Stream.of(
                    new AsmFieldLensHook(classNode.name, fieldNode.name, targetClassName, targetFieldName, targetFieldType, lensAnnotation.isMandatory(), setterDesc, getterDesc),
                    new AsmFieldLens(targetClassName, targetFieldName, targetFieldType, lensAnnotation.isMandatory(), lensAnnotation.createField(), setterDesc, getterDesc)
                ),
                lensAnnotation.createField() && defaultValue != null ?
                    Stream.of(new AsmFieldLensInit(targetClassName, targetFieldName, targetFieldType, lensAnnotation.isMandatory(), defaultValue)) :
                    Stream.empty()
            );
        } else
            return invalidFieldLens("field lens type should be FieldAccessor<TargetClass, TargetFieldType>", classNode, fieldNode);
    }

    private InsnList findDefaultValue(InsnList clinitInstructions, ClassNode classNode, String lensFieldName, Type targetFieldType) {
        ListIterator<AbstractInsnNode> it = clinitInstructions.iterator();
        while (it.hasNext()) {
            AbstractInsnNode current = it.next();
            if (current instanceof FieldInsnNode) {
                FieldInsnNode fieldInit = (FieldInsnNode) current;
                if (fieldInit.getOpcode() == PUTSTATIC && fieldInit.name.equals(lensFieldName) && fieldInit.desc.equals(Type.getDescriptor(FieldAccessor.class))) {
                    boolean needReport = true;
                    it.previous();
                    if (it.hasPrevious()) {
                        AbstractInsnNode prev = it.previous();
                        if (prev instanceof MethodInsnNode) {
                            MethodInsnNode wrapperCall = (MethodInsnNode) prev;
                            if (wrapperCall.getOpcode() == INVOKESTATIC && wrapperCall.name.equals("defaultValue") && wrapperCall.owner.equals(Type.getInternalName(FieldAccessor.class))) {
                                try {
                                    InsnList valueConstruction = collectValueConstruction(it, targetFieldType);
                                    if (normalizeResultType(classNode, lensFieldName, targetFieldType, valueConstruction)) {
                                        return valueConstruction;
                                    }
                                } catch (IllegalArgumentException e) {
                                    needReport = false;
                                    Logger.instance.debug("Known issue with specific opcode in hook lens default value of " + classNode.name.replace('/', '.') + "#" + lensFieldName);
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    Logger.instance.warning("Found complicated hook lens default value of " + classNode.name.replace('/', '.') + "#" + lensFieldName);
                    Logger.instance.warning("Try to change such way: ");
                    Logger.instance.warning("    public static FieldAccessor<A, B> bruh = FieldAccessor.defaultValue(bruhDefaultValueFactory());");
                    Logger.instance.warning("    public static B bruhDefaultValueFactory(){");
                    Logger.instance.warning("        return someB;");
                    Logger.instance.warning("    }");
                    if (needReport)
                        Logger.instance.warning("Plz, report about it to https://github.com/hohserg1/HookLib/issues");
                    return null;
                }
            }
        }
        return null;
    }

    private boolean normalizeResultType(ClassNode classNode, String lensFieldName, Type targetFieldType, InsnList valueConstruction) {
        AbstractInsnNode last = valueConstruction.getLast();
        Type resultType = getInstructionType(last);
        if (Type.VOID_TYPE.equals(resultType) && last instanceof MethodInsnNode && ((MethodInsnNode) last).name.equals(Constants.CONSTRUCTOR_NAME) && last.getPrevious().getOpcode() == DUP) {
            last = last.getPrevious().getPrevious();
            resultType = getInstructionType(last);
        }
        if (targetFieldType.equals(resultType) || isSubtype(targetFieldType, resultType)) {
            return true;
        } else {
            if (targetFieldType.equals(AsmUtils.objectToPrimitive.get(resultType))) {
                if (last instanceof MethodInsnNode && ((MethodInsnNode) last).name.equals("valueOf")) {
                    valueConstruction.remove(last);
                } else {
                    valueConstruction.add(new MethodInsnNode(
                        INVOKEVIRTUAL,
                        resultType.getInternalName(),
                        AsmUtils.primitiveToUnboxingMethod.get(targetFieldType),
                        Type.getMethodDescriptor(targetFieldType), false
                    ));
                }
                return true;
            } else {
                Logger.instance.warning("Wrong type of hook lens default value of " + classNode.name.replace('/', '.') + "#" + lensFieldName);
                Logger.instance.warning("Required " + targetFieldType + " but got " + resultType);
                return false;
            }
        }
    }

    private boolean isSubtype(Type parent, Type some) {
        if ((AsmUtils.allPrimitives.contains(parent) || AsmUtils.allPrimitives.contains(some)) && !parent.equals(some))
            return false;
        return HookLoader.getDeobfuscationMetadataReader().checkSuperType(some.getInternalName(), parent.getInternalName());
    }

    private Type getInstructionType(AbstractInsnNode i) {
        if (i instanceof MethodInsnNode) {
            Type methodType = Type.getMethodType(((MethodInsnNode) i).desc);
            return methodType.getReturnType();
        } else {
            OpcodeDetails details = AsmUtils.getOpcodeDetails(i.getOpcode());
            return details.resultType.apply(i);
        }
    }

    private InsnList collectValueConstruction(ListIterator<AbstractInsnNode> it, Type targetFieldType) {
        LinkedList<AbstractInsnNode> collector = new LinkedList<>();
        int stackRequired = -1;
        while (it.hasPrevious() && stackRequired != 0) {
            AbstractInsnNode prev = it.previous();
            collector.addFirst(prev.clone(ImmutableMap.of()));
            stackRequired += getStackAffection(prev);
        }
        InsnList r = new InsnList();
        collector.forEach(r::add);
        return r;
    }

    private Set<Integer> forbiddenDefaultValueOpcodes = ImmutableSet.<Integer>builder()
        .add(NOP)
        .add(GOTO)
        .add(JSR)
        .add(RET)
        .add(TABLESWITCH)
        .add(LOOKUPSWITCH)
        .add(LOOKUPSWITCH)
        .add(IRETURN)
        .add(LRETURN)
        .add(FRETURN)
        .add(DRETURN)
        .add(ARETURN)
        .add(RETURN)
        .add(PUTSTATIC)
        .add(PUTFIELD)
        .add(IFEQ)
        .add(IFNE)
        .add(IFLT)
        .add(IFGE)
        .add(IFGT)
        .add(IFLE)
        .add(IF_ICMPEQ)
        .add(IF_ICMPNE)
        .add(IF_ICMPLT)
        .add(IF_ICMPGE)
        .add(IF_ICMPGT)
        .add(IF_ICMPLE)
        .add(IF_ACMPEQ)
        .add(IF_ACMPNE)
        .add(ATHROW)
        .add(MONITORENTER)
        .add(MONITOREXIT)
        .add(IFNULL)
        .add(IFNONNULL)
        .add(IINC)
        .build();

    private int getStackAffection(AbstractInsnNode prev) {
        if (prev instanceof MethodInsnNode) {
            Type methodType = Type.getMethodType(((MethodInsnNode) prev).desc);
            int addition = methodType.getReturnType() != Type.VOID_TYPE ? 1 : 0;
            int consumption = methodType.getArgumentTypes().length + (prev.getOpcode() == INVOKESTATIC ? 0 : 1);
            return addition - consumption;

        } else if (prev instanceof MultiANewArrayInsnNode) {
            return 1 - ((MultiANewArrayInsnNode) prev).dims;

        } else if (forbiddenDefaultValueOpcodes.contains(prev.getOpcode())) {
            throw new IllegalArgumentException("opcode " + prev.getOpcode() + " not supported for hook lens default value");

        } else {
            OpcodeDetails details = AsmUtils.getOpcodeDetails(prev.getOpcode());
            return details.putToStack - details.consumeFromStack;
        }
    }

    private Stream<AsmInjection> parseMethodLens(ClassNode classNode, MethodNode methodNode, AnnotationMap annotationMap, MethodLens methodLensAnnotation) {
        Type methodType = Type.getMethodType(methodNode.desc);
        Type[] argumentTypes = methodType.getArgumentTypes();
        Type returnType = methodType.getReturnType();

        if (!checkRegularConditionsMethodLens(classNode, methodNode, argumentTypes))
            return Stream.empty();

        List<Pair<Integer, Type>> hookArgsToReplace = new ArrayList<>();
        Type targetClassType = getPrivateClassType(methodNode, 0, argumentTypes[0]);
        if (!targetClassType.equals(argumentTypes[0])) {
            hookArgsToReplace.add(Pair.of(0, targetClassType));
        }
        Type[] targetMethodArgumentTypes = Arrays.copyOfRange(argumentTypes, 1, argumentTypes.length);
        for (int targetMethodArgIndex = 0; targetMethodArgIndex < targetMethodArgumentTypes.length; targetMethodArgIndex++) {
            int lensArgIndex = targetMethodArgIndex + 1;
            Type actualArgType = targetMethodArgumentTypes[targetMethodArgIndex];
            Type fixedArgType = getPrivateClassType(methodNode, lensArgIndex, actualArgType);
            if (!fixedArgType.equals(actualArgType)) {
                targetMethodArgumentTypes[targetMethodArgIndex] = fixedArgType;
                hookArgsToReplace.add(Pair.of(lensArgIndex, fixedArgType));
            }
        }

        String targetClassName = targetClassType.getClassName();
        String targetMethodName = methodLensAnnotation.targetMethod().isEmpty() ? methodNode.name : methodLensAnnotation.targetMethod();
        String targetMethodDesc = Type.getMethodDescriptor(returnType, targetMethodArgumentTypes);

        Type[] fixedArgs = argumentTypes.clone();
        fixedArgs[0] = Type.getObjectType(targetClassName.replace('.', '/'));
        String hookMethodLensDescription = Type.getMethodDescriptor(returnType, fixedArgs);

        AsmMethodLens targetClassInjection = new AsmMethodLens(
            targetClassName, targetMethodName, targetMethodDesc,
            hookMethodLensDescription,
            methodLensAnnotation.isMandatory()
        );
        AsmMethodLensHook hookClassInjection = new AsmMethodLensHook(
            classNode.name, methodNode.name, hookMethodLensDescription,
            targetClassName, targetMethodName, targetMethodDesc,
            methodLensAnnotation.isMandatory()
        );


        if (!hookArgsToReplace.isEmpty())
            return Stream.of(
                targetClassInjection,
                hookClassInjection,
                new AsmFixPrivateClassArguments(
                    classNode.name.replace('/', '.'),
                    methodNode.name,
                    methodNode.desc,
                    hookArgsToReplace,
                    hookClassInjection.isMandatory()
                ));
        else
            return Stream.of(
                targetClassInjection,
                hookClassInjection
            );
    }

    private Stream<AsmInjection> parseRegularHook(ClassNode classNode, MethodNode methodNode, AnnotationMap annotationMap, Hook hookAnnotation) {
        AsmHook.Builder builder = AsmHook.newBuilder();
        Type methodType = Type.getMethodType(methodNode.desc);
        Type[] argumentTypes = methodType.getArgumentTypes();
        Type returnType = methodType.getReturnType();

        if (!checkRegularConditions(classNode, methodNode, argumentTypes))
            return Stream.empty();

        List<Pair<Integer, Type>> hookArgsToReplace = new ArrayList<>();
        Type targetClassType = getPrivateClassType(methodNode, 0, argumentTypes[0]);
        builder.setTargetClass(targetClassType.getClassName());
        if (!targetClassType.equals(argumentTypes[0])) {
            hookArgsToReplace.add(Pair.of(0, targetClassType));
        }

        if (!hookAnnotation.targetMethod().isEmpty())
            builder.setTargetMethod(hookAnnotation.targetMethod());
        else
            builder.setTargetMethod(methodNode.name);


        builder.setHookClass(classNode.name.replace('/', '.'));
        builder.setHookMethod(methodNode.name);
        builder.addThisToHookMethodParameters();

        int currentParameterId = 1;
        for (int i = 1; i < argumentTypes.length; i++) {
            Type argType = getPrivateClassType(methodNode, i, argumentTypes[i]);
            if (!argType.equals(argumentTypes[i])) {
                hookArgsToReplace.add(Pair.of(i, argType));
            }
            AnnotationMap parameterAnnotations = AnnotationUtils.annotationOfParameter(methodNode, i);
            ReturnValue returnValue = parameterAnnotations.get(ReturnValue.class);
            LocalVariable localVariable = parameterAnnotations.get(LocalVariable.class);
            if (returnValue != null) {
                builder.setTargetMethodReturnType(argType);
                builder.addReturnValueToHookMethodParameters();
            } else if (localVariable != null)
                builder.addHookMethodParameter(argType, localVariable.id());
            else {
                builder.addTargetMethodParameters(argType);
                builder.addHookMethodParameter(argType, currentParameterId);
                currentParameterId += argType == Type.LONG_TYPE || argType == Type.DOUBLE_TYPE ? 2 : 1;
            }
        }

        OnBegin onBegin = annotationMap.get(OnBegin.class);
        OnReturn onReturn = annotationMap.get(OnReturn.class);
        OnMethodCall onMethodCall = annotationMap.get(OnMethodCall.class);
        OnExpression onExpression = annotationMap.get(OnExpression.class);

        if (onBegin != null)
            builder.setInjectorFactory(HookInjectorFactory.BeginFactory.INSTANCE);

        else if (onReturn != null)
            builder.setInjectorFactory(new HookInjectorFactory.ReturnFactory(onReturn.ordinal()));

        else if (onMethodCall != null)
            builder.setInjectorFactory(new HookInjectorFactory.MethodCallFactory(
                onMethodCall.value(), onMethodCall.desc(), onMethodCall.ordinal(), onMethodCall.shift()
            ));

        else if (onExpression != null) {
            String expressionPatternMethodName = onExpression.expressionPattern();

            Optional<MethodNode> maybeExpressionPatternMethod =
                classNode.methods.stream().filter(mn -> mn.name.equals(expressionPatternMethodName)).findAny();

            if (!maybeExpressionPatternMethod.isPresent())
                return invalidHook("Expression pattern \"" + expressionPatternMethodName + "\" not found", classNode, methodNode);

            MethodNode expressionPatternMethod = maybeExpressionPatternMethod.get();
            List<AbstractInsnNode> pattern = new ArrayList<>();
            for (AbstractInsnNode i : expressionPatternMethod.instructions.toArray()) {
                if (AsmUtils.isReturn(i))
                    break;
                if (AsmUtils.isPatternSensitive(i))
                    pattern.add(i);
            }

            builder.setInjectorFactory(new HookInjectorFactory.ExpressionFactory(pattern, onExpression.shift(), onExpression.ordinal(), Type.getMethodType(expressionPatternMethod.desc)));

        } else
            return invalidHook("Injection point doesnt described. Use one of @OnBegin,@OnReturn or @OnMethodCall", classNode, methodNode);

        if (returnType == Type.VOID_TYPE || onExpression != null && onExpression.shift() == Shift.INSTEAD || onMethodCall != null && onMethodCall.shift() == Shift.INSTEAD) {
            builder.setReturnCondition(ReturnCondition.NEVER);

        } else if (returnType.getClassName().equals(ReturnSolve.class.getCanonicalName())) {
            TypeRepr typeRepr = SignatureExtractor.fromReturnType(methodNode);

            if (typeRepr instanceof FlatTypeRepr)
                return invalidHook("return type is raw ReturnSolve, should be parametrized", classNode, methodNode);

            Type targetMethodReturnType = ((ParametrizedTypeRepr) typeRepr).parameters.get(0).getRawType();

            if (methodNode.invisibleTypeAnnotations != null)
                for (TypeAnnotationNode a : methodNode.invisibleTypeAnnotations)
                    if (a.desc.equals(Type.getDescriptor(ReturnSolve.Primitive.class)) || a.desc.equals(Type.getDescriptor(Primitive.class)))
                        if (new TypeReference(a.typeRef).getSort() == TypeReference.METHOD_RETURN)
                            if (a.typePath.getLength() == 1 && a.typePath.getStep(0) == TypePath.TYPE_ARGUMENT)
                                if (a.typePath.getStepArgument(0) == 0) {
                                    Type maybePrimitive = AsmUtils.objectToPrimitive.get(targetMethodReturnType);
                                    if (maybePrimitive == null)
                                        return invalidHook("@Primitive used at non-primitive type", classNode, methodNode);
                                    targetMethodReturnType = maybePrimitive;
                                    break;
                                }

            builder.setTargetMethodReturnType(targetMethodReturnType);
            builder.setReturnCondition(ReturnCondition.ON_SOLVE);

        } else {
            builder.setTargetMethodReturnType(returnType);
            builder.setReturnCondition(ReturnCondition.ALWAYS);
        }

        builder.setHookMethodReturnType(returnType);

        builder.setPriority(hookAnnotation.priority());

        builder.setCreateMethod(hookAnnotation.createMethod());

        builder.setMandatory(hookAnnotation.isMandatory());

        builder.setRequiredPrintLocalVariables(annotationMap.get(PrintLocalVariables.class) != null);

        AsmHook hook = builder.build();
        if (!hookArgsToReplace.isEmpty())
            return Stream.of(hook, new AsmFixPrivateClassArguments(
                hook.getHookClassName(),
                methodNode.name,
                methodNode.desc,
                hookArgsToReplace,
                hook.isMandatory()
            ));
        return Stream.of(hook);
    }

    private Type getPrivateClassType(MethodNode methodNode, int argumentIndex, Type argumentType) {
        AnnotationMap argumentAnnotation = AnnotationUtils.annotationOfParameter(methodNode, argumentIndex);
        return AsmUtils.mapBy(argumentType, typeName -> getPrivateClassName(argumentAnnotation, typeName.replace('/', '.')).replace('.', '/'));
    }

    private String getPrivateClassName(AnnotationMap argumentAnnotation, String argumentClassName) {
        if (argumentAnnotation.contains(PrivateClass.class))
            return argumentAnnotation.get(PrivateClass.class).value();

        else if (classImageNameToPrivateClassName.containsKey(argumentClassName))
            return classImageNameToPrivateClassName.get(argumentClassName);

        return argumentClassName;
    }

    public class UnexpectedHookParsingError extends RuntimeException {
        public UnexpectedHookParsingError(String className, String methodName, Throwable cause) {
            super("while processing " + className + "#" + methodName + ". Plz report to https://github.com/hohserg1/HookLib/issues", cause);
        }
    }


}
