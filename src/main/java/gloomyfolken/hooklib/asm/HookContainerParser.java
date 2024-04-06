package gloomyfolken.hooklib.asm;

import gloomyfolken.hooklib.api.*;
import gloomyfolken.hooklib.helper.Logger;
import gloomyfolken.hooklib.helper.annotation.AnnotationMap;
import gloomyfolken.hooklib.helper.annotation.AnnotationUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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
            try {
                AnnotationMap annotationMap = AnnotationUtils.annotationOf(methodNode);
                Hook hookAnnotation = annotationMap.get(Hook.class);
                if (hookAnnotation != null) {
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
                        AtomicReference<Type> targetMethodReturnType = new AtomicReference<>();
                        new SignatureReader(methodNode.signature).accept(new SignatureVisitor(Opcodes.ASM5) {
                            @Override
                            public SignatureVisitor visitReturnType() {
                                return new SignatureVisitor(Opcodes.ASM5) {
                                    @Override
                                    public SignatureVisitor visitTypeArgument(char wildcard) {
                                        return new SignatureVisitor(Opcodes.ASM5) {

                                            @Override
                                            public void visitClassType(String name) {
                                                targetMethodReturnType.set(Type.getObjectType(name));
                                            }

                                            @Override
                                            public SignatureVisitor visitTypeArgument(char wildcard) {
                                                return new SignatureVisitor(Opcodes.ASM5) {
                                                };
                                            }
                                        };
                                    }
                                };
                            }
                        });

                        if (targetMethodReturnType.get() == null)
                            return invalidHook("return type is raw ReturnSolve, should be parametrized", classNode, methodNode);

                        boolean isStrictlyPrimitive = false;
                        if (methodNode.invisibleTypeAnnotations != null)
                            for (TypeAnnotationNode a : methodNode.invisibleTypeAnnotations)
                                if (a.desc.equals(Type.getDescriptor(ReturnSolve.Primitive.class)))
                                    if (new TypeReference(a.typeRef).getSort() == TypeReference.METHOD_RETURN)
                                        if (a.typePath.getLength() == 1 && a.typePath.getStep(0) == TypePath.TYPE_ARGUMENT)
                                            if (a.typePath.getStepArgument(0) == 0)
                                                isStrictlyPrimitive = true;

                        if (isStrictlyPrimitive) {
                            Type maybePrimitive = AsmHook.objectToPrimitive.get(targetMethodReturnType.get());
                            if (maybePrimitive == null)
                                return invalidHook("@ReturnSolve.Primitive used at non-primitive type", classNode, methodNode);
                            targetMethodReturnType.set(maybePrimitive);
                        }

                        builder.setTargetMethodReturnType(targetMethodReturnType.get());
                        builder.setReturnCondition(ReturnCondition.ON_SOLVE);

                    } else {
                        builder.setTargetMethodReturnType(returnType);
                        builder.setReturnCondition(ReturnCondition.ALWAYS);
                    }

                    // setReturnCondition и setReturnValue сетают тип хук-метода, поэтому сетнуть его вручную можно только теперь
                    builder.setHookMethodReturnType(returnType);

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

            } catch (Throwable e) {
                throw new UnexpectedHookParsingError(classNode.name, methodNode.name, e);
            }

            return Stream.empty();
        });
    }

    public static class UnexpectedHookParsingError extends RuntimeException {
        public UnexpectedHookParsingError(String className, String methodName, Throwable cause) {
            super("while processing " + className + "#" + methodName + ". Plz report to https://github.com/hohserg1/HookLib/issues", cause);
        }
    }


}
