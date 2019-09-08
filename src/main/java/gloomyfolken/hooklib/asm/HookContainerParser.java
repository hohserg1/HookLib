package gloomyfolken.hooklib.asm;

import gloomyfolken.hooklib.asm.Hook.LocalVariable;
import gloomyfolken.hooklib.asm.Hook.ReturnValue;
import gloomyfolken.hooklib.asm.model.method.hook.AsmHook;
import gloomyfolken.hooklib.experimental.utils.annotation.AnnotationMap;
import gloomyfolken.hooklib.experimental.utils.annotation.AnnotationUtils;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;

import static gloomyfolken.hooklib.experimental.utils.SideOnlyUtils.isValidSide;
import static org.objectweb.asm.Opcodes.ASM5;

public class HookContainerParser {

    private HookClassTransformer transformer;

    public HookContainerParser(HookClassTransformer transformer) {
        this.transformer = transformer;
    }

    //MainHookLoader
    public Stream<AsmHook> parseHooks(String className, byte[] classData) {
        transformer.logger.debug("Parsing hooks container " + className);
        ClassNode classNode = new ClassNode(ASM5);
        transformer.classMetadataReader.acceptVisitor(classData, classNode);
        return classNode.methods.stream().flatMap(methodNode -> parseOneHook(classNode, methodNode));
    }

    private Stream<AsmHook> parseOneHook(ClassNode classNode, MethodNode methodNode) {
        AnnotationMap methodAnnotations = AnnotationUtils.annotationOf(methodNode);
        return methodAnnotations.maybeGet(Hook.class)
                .filter(__ -> isValidSide(methodAnnotations))
                .flatMap(hookAnnotation -> {
                    System.out.println("Parsing hook " + methodNode.name);

                    HashMap<Integer, Integer> parametersAnnotations = new HashMap<>();

                    int parametersCount = Type.getMethodType(methodNode.desc).getArgumentTypes().length;
                    for (int parameter = 0; parameter < parametersCount; parameter++) {
                        AnnotationMap annotationOfParameter = AnnotationUtils.annotationOfParameter(methodNode, parameter);

                        if (annotationOfParameter.contains(ReturnValue.class)) {
                            parametersAnnotations.put(parameter, -1);
                        }

                        final int finalParameter = parameter;
                        annotationOfParameter.maybeGet(LocalVariable.class)
                                .ifPresent(localVariable ->
                                        parametersAnnotations.put(finalParameter, localVariable.value()));
                    }

                    return createHook(
                            methodNode.name,
                            methodNode.desc,
                            (methodNode.access & Opcodes.ACC_PUBLIC) != 0 && (methodNode.access & Opcodes.ACC_STATIC) != 0,
                            classNode.name,
                            hookAnnotation,
                            parametersAnnotations);
                })
                .map(Stream::of)
                .orElse(Stream.empty());

    }

    private void invalidHook(String message, String currentMethodName) {
        transformer.logger.warning("Found invalid hook " + currentClassName + "#" + currentMethodName);
        transformer.logger.warning(message);
    }

    private Optional<AsmHook> createHook(String currentMethodName, String currentMethodDesc, boolean currentMethodPublicStatic, String currentClassName,
                                         Hook annotationValues, HashMap<Integer, Integer> parameterAnnotations) {
        Type methodType = Type.getMethodType(currentMethodDesc);
        Type[] argumentTypes = methodType.getArgumentTypes();

        if (!currentMethodPublicStatic) {
            invalidHook("Hook method must be public and static.", currentMethodName);
            return Optional.empty();
        }

        if (argumentTypes.length < 1) {
            invalidHook("Hook method has no parameters. First parameter of a " +
                    "hook method must belong the type of the anchorTarget class.", currentMethodName);
            return Optional.empty();
        }

        if (argumentTypes[0].getSort() != Type.OBJECT) {
            invalidHook("First parameter of the hook method is not an object. First parameter of a " +
                    "hook method must belong the type of the anchorTarget class.", currentMethodName);
            return Optional.empty();
        }

        AsmHook.AsmHookBuilder builder1 = AsmHook.builder();

        builder1.targetMethodName(ifDefinedOrElse(annotationValues.targetMethod(), currentMethodName));
        builder1.targetClassName(argumentTypes[0].getClassName());

        builder1.hookMethodName(currentMethodName);
        builder1.hookClassInternalName(currentClassName);

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

        builder1.setAnchorForInject(annotationValues.at());

        if (annotationValues.returnType().length() > 0)
            builder1.targetMethodReturnType(TypeHelper.getType(annotationValues.returnType()));


        ReturnCondition returnCondition = annotationValues.returnCondition();

        builder1.returnCondition(returnCondition);

        builder1.priority(annotationValues.priority());
        builder1.createMethod(annotationValues.createMethod());
        builder1.isMandatory(annotationValues.isMandatory());


        if (returnCondition == ReturnCondition.ON_SOLVE && !methodType.getReturnType().equals(Type.getType(ResultSolve.class))) {
            invalidHook("Hook method must return ResultSolve if returnCodition is ON_SOLVE.", currentMethodName);
            return Optional.empty();
        }

        try {
            return Optional.of(builder1.build());
        } catch (Exception e) {
            invalidHook(e.getMessage(), currentMethodName);
            return Optional.empty();
        }
    }

    private String ifDefinedOrElse(String value, String defaultValue) {
        return Optional.ofNullable(value).filter(n -> n.length() > 0).orElse(defaultValue);
    }


}
