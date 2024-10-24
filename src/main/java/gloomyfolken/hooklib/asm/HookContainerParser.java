package gloomyfolken.hooklib.asm;

import gloomyfolken.hooklib.api.*;
import gloomyfolken.hooklib.asm.SignatureExtractor.FlatTypeRepr;
import gloomyfolken.hooklib.asm.SignatureExtractor.ParametrizedTypeRepr;
import gloomyfolken.hooklib.asm.SignatureExtractor.TypeRepr;
import gloomyfolken.hooklib.asm.injections.*;
import gloomyfolken.hooklib.helper.Logger;
import gloomyfolken.hooklib.helper.SideOnlyUtils;
import gloomyfolken.hooklib.helper.annotation.AnnotationMap;
import gloomyfolken.hooklib.helper.annotation.AnnotationUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class HookContainerParser {

    private static Stream<AsmHook> invalidHook(String message, ClassNode classNode, MethodNode methodNode) {
        Logger.instance.warning("Found invalid hook " + classNode.name.replace('/', '.') + "#" + methodNode.name);
        Logger.instance.warning(message);
        return Stream.empty();
    }

    private static Stream<AsmHook> invalidFieldLens(String message, ClassNode classNode, FieldNode fieldNode) {
        Logger.instance.warning("Found invalid hook lens " + classNode.name.replace('/', '.') + "#" + fieldNode.name);
        Logger.instance.warning(message);
        return Stream.empty();
    }

    private static Stream<AsmInjection> invalidMethodLens(String message, ClassNode classNode, MethodNode methodNode) {
        Logger.instance.warning("Found invalid hook lens " + classNode.name.replace('/', '.') + "#" + methodNode.name);
        Logger.instance.warning(message);
        return Stream.empty();
    }

    private static boolean checkRegularConditions(ClassNode classNode, MethodNode methodNode, Type[] argumentTypes) {
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

    private static boolean checkRegularConditionsMethodLens(ClassNode classNode, MethodNode methodNode, Type[] argumentTypes) {
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

    public static Stream<AsmInjection> parseHooks(ClassNode classNode) {
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
                        return parseFieldLens(classNode, fieldNode, lensAnnotation);
                    }

                    return Stream.empty();
                })
        );
    }

    private static Stream<? extends AsmInjection> parseFieldLens(ClassNode classNode, FieldNode fieldNode, FieldLens lensAnnotation) {
        if (Type.getType(fieldNode.desc).getClassName().equals(FieldAccessor.class.getCanonicalName())) {
            TypeRepr typeRepr = SignatureExtractor.fromField(fieldNode);

            if (typeRepr instanceof FlatTypeRepr)
                return invalidFieldLens("field lens type is raw FieldAccessor, should be parametrized", classNode, fieldNode);

            List<TypeRepr> parameters = ((ParametrizedTypeRepr) typeRepr).parameters;
            Type targetClassType = parameters.get(0).getRawType();
            String targetClassName = targetClassType.getClassName();
            Type targetFieldType = parameters.get(1).getRawType();

            String targetFieldName = !lensAnnotation.targetField().isEmpty() ? lensAnnotation.targetField() : fieldNode.name;

            String setterDesc = Type.getMethodDescriptor(Type.VOID_TYPE, targetClassType, targetFieldType);
            String getterDesc = Type.getMethodDescriptor(targetFieldType, targetClassType);

            return Stream.of(
                    new AsmFieldLensHook(classNode.name, fieldNode.name, targetClassName, targetFieldName, targetFieldType, lensAnnotation.isMandatory(), setterDesc, getterDesc),
                    new AsmFieldLens(targetClassName, targetFieldName, targetFieldType, lensAnnotation.isMandatory(), lensAnnotation.createField(), null, setterDesc, getterDesc)
            );
        } else
            return invalidFieldLens("field lens type should be FieldAccessor<TargetClass, TargetFieldType>", classNode, fieldNode);
    }

    private static Stream<AsmInjection> parseMethodLens(ClassNode classNode, MethodNode methodNode, AnnotationMap annotationMap, MethodLens methodLensAnnotation) {
        Type methodType = Type.getMethodType(methodNode.desc);
        Type[] argumentTypes = methodType.getArgumentTypes();
        Type returnType = methodType.getReturnType();

        if (!checkRegularConditionsMethodLens(classNode, methodNode, argumentTypes))
            return Stream.empty();

        String targetClassName = argumentTypes[0].getClassName();
        String targetMethodName = methodLensAnnotation.targetMethod().isEmpty() ? methodNode.name : methodLensAnnotation.targetMethod();
        String targetMethodDesc = Type.getMethodDescriptor(returnType, Arrays.copyOfRange(argumentTypes, 1, argumentTypes.length));

        AsmMethodLens targetClassInjection = new AsmMethodLens(
                targetClassName, targetMethodName, targetMethodDesc,
                methodNode.desc,
                methodLensAnnotation.isMandatory()
        );
        AsmMethodLensHook hookClassInjection = new AsmMethodLensHook(
                classNode.name, methodNode.name, methodNode.desc,
                targetClassName, targetMethodName, targetMethodDesc,
                methodLensAnnotation.isMandatory()
        );

        return Stream.of(
                targetClassInjection,
                hookClassInjection
        );
    }

    private static Stream<AsmHook> parseRegularHook(ClassNode classNode, MethodNode methodNode, AnnotationMap annotationMap, Hook hookAnnotation) {
        AsmHook.Builder builder = AsmHook.newBuilder();
        Type methodType = Type.getMethodType(methodNode.desc);
        Type[] argumentTypes = methodType.getArgumentTypes();
        Type returnType = methodType.getReturnType();

        if (!checkRegularConditions(classNode, methodNode, argumentTypes))
            return Stream.empty();

        builder.setTargetClass(argumentTypes[0].getClassName());

        if (!hookAnnotation.targetMethod().isEmpty())
            builder.setTargetMethod(hookAnnotation.targetMethod());
        else
            builder.setTargetMethod(methodNode.name);


        builder.setHookClass(classNode.name.replace('/', '.'));
        builder.setHookMethod(methodNode.name);
        builder.addThisToHookMethodParameters();

        int currentParameterId = 1;
        for (int i = 1; i < argumentTypes.length; i++) {
            Type argType = argumentTypes[i];
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

            boolean isStrictlyPrimitive = false;
            if (methodNode.invisibleTypeAnnotations != null)
                for (TypeAnnotationNode a : methodNode.invisibleTypeAnnotations)
                    if (a.desc.equals(Type.getDescriptor(ReturnSolve.Primitive.class)))
                        if (new TypeReference(a.typeRef).getSort() == TypeReference.METHOD_RETURN)
                            if (a.typePath.getLength() == 1 && a.typePath.getStep(0) == TypePath.TYPE_ARGUMENT)
                                if (a.typePath.getStepArgument(0) == 0)
                                    isStrictlyPrimitive = true;

            if (isStrictlyPrimitive) {
                Type maybePrimitive = AsmUtils.objectToPrimitive.get(targetMethodReturnType);
                if (maybePrimitive == null)
                    return invalidHook("@ReturnSolve.Primitive used at non-primitive type", classNode, methodNode);
                targetMethodReturnType = maybePrimitive;
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

        return Stream.of(builder.build());
    }

    public static class UnexpectedHookParsingError extends RuntimeException {
        public UnexpectedHookParsingError(String className, String methodName, Throwable cause) {
            super("while processing " + className + "#" + methodName + ". Plz report to https://github.com/hohserg1/HookLib/issues", cause);
        }
    }


}
