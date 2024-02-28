package gloomyfolken.hooklib.asm;

import gloomyfolken.hooklib.api.*;
import gloomyfolken.hooklib.helper.Logger;
import gloomyfolken.hooklib.helper.annotation.AnnotationMap;
import gloomyfolken.hooklib.helper.annotation.AnnotationUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class HookContainerParser {

    private static Stream<AsmHook> invalidHook(String message, ClassNode classNode, MethodNode methodNode) {
        Logger.instance.warning("Found invalid hook " + classNode.name.replace('/', '.') + "#" + methodNode.name);
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

    public static Stream<AsmInjection> parseHooks(ClassNode classNode) {
        return classNode.methods.stream().flatMap(methodNode -> {
            AnnotationMap annotationMap = AnnotationUtils.annotationOf(methodNode);
            Hook hookAnnotation = annotationMap.get(Hook.class);
            if (hookAnnotation != null) {
                AsmHook.Builder builder = AsmHook.newBuilder();
                Type methodType = Type.getMethodType(methodNode.desc);
                Type[] argumentTypes = methodType.getArgumentTypes();

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

                if (!hookAnnotation.returnType().isEmpty())
                    builder.setTargetMethodReturnType(hookAnnotation.returnType());

                ReturnCondition returnCondition = hookAnnotation.returnCondition();
                if (hookAnnotation.returnCondition() != ReturnCondition.NEVER) {
                    builder.setReturnCondition(returnCondition);
                }

                try {
                    if (returnCondition != ReturnCondition.NEVER) {
                        if (hookAnnotation.returnConstant().length != 0) {
                            builder.setReturnValue(ReturnSort.PRIMITIVE_CONSTANT);
                            builder.setPrimitiveConstant(hookAnnotation.returnConstant()[0]);
                        } else if (hookAnnotation.returnNull()) {
                            builder.setReturnValue(ReturnSort.NULL);
                        } else if (!hookAnnotation.returnAnotherMethod().isEmpty()) {
                            builder.setReturnValue(ReturnSort.ANOTHER_METHOD_RETURN_VALUE);
                            builder.setReturnMethod(hookAnnotation.returnAnotherMethod());
                        } else if (methodType.getReturnType() != Type.VOID_TYPE) {
                            builder.setReturnValue(ReturnSort.HOOK_RETURN_VALUE);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    invalidHook(e.getMessage(), classNode, methodNode);
                    return Stream.empty();
                }

                // setReturnCondition и setReturnValue сетают тип хук-метода, поэтому сетнуть его вручную можно только теперь
                builder.setHookMethodReturnType(methodType.getReturnType());

                if (returnCondition == ReturnCondition.ON_TRUE && methodType.getReturnType() != Type.BOOLEAN_TYPE) {
                    invalidHook("Hook method must return boolean if returnCodition is ON_TRUE.", classNode, methodNode);
                    return Stream.empty();
                }
                if ((returnCondition == ReturnCondition.ON_NULL || returnCondition == ReturnCondition.ON_NOT_NULL) &&
                        methodType.getReturnType().getSort() != Type.OBJECT &&
                        methodType.getReturnType().getSort() != Type.ARRAY) {
                    invalidHook("Hook method must return object if returnCodition is ON_NULL or ON_NOT_NULL.", classNode, methodNode);
                    return Stream.empty();
                }

                builder.setPriority(hookAnnotation.priority());

                builder.setCreateMethod(hookAnnotation.createMethod());

                builder.setMandatory(hookAnnotation.isMandatory());

                builder.setRequiredPrintLocalVariables(annotationMap.get(PrintLocalVariables.class) != null);

                return Stream.of(builder.build());
            }


            FieldLens lensAnnotation = annotationMap.get(FieldLens.class);
            if (lensAnnotation != null) {
                Type methodType = Type.getMethodType(methodNode.desc);
                Type[] argumentTypes = methodType.getArgumentTypes();

                if (!checkRegularConditions(classNode, methodNode, argumentTypes)) {
                    return Stream.empty();
                }

                boolean isGetter = argumentTypes.length == 1;

                String targetClassName = argumentTypes[0].getClassName();
                String targetFieldName = !lensAnnotation.targetField().isEmpty() ? lensAnnotation.targetField() : methodNode.name;
                Type targetFieldType = isGetter ? methodType.getReturnType() : argumentTypes[1];

                return Stream.of(
                        new AsmLensHook(classNode.name, methodNode.name, methodNode.desc, targetClassName, targetFieldName, targetFieldType, isGetter, lensAnnotation.isMandatory()),
                        new AsmLens(targetClassName, targetFieldName, targetFieldType, lensAnnotation.isMandatory(), lensAnnotation.createField(), null)
                );
            }

            return Stream.empty();
        });
    }


}
