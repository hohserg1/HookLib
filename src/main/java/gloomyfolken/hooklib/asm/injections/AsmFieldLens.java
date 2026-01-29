package gloomyfolken.hooklib.asm.injections;

import gloomyfolken.hooklib.asm.AsmUtils;
import gloomyfolken.hooklib.asm.HookInjectorClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.*;

public class AsmFieldLens implements AsmInjection {
    static final String setterSuffix = "$hook$lens$set";
    static final String getterSuffix = "$hook$lens$get";

    private final String targetClassName;
    private final String targetFieldName;
    private final Set<String> expectedTargetFieldTypeDescriptors;
    private final boolean isMandatory;

    private final Type boxedType;

    private final boolean createField;

    private final String setterDesc;
    private final String getterDesc;

    private boolean found = false;
    private boolean isStaticField = false;
    private String actualFieldName;
    private Type actualFieldType;

    public AsmFieldLens(String targetClassName, String targetFieldName, Type expectedTargetFieldType,
                        boolean isMandatory, boolean createField,
                        String setterDesc, String getterDesc) {
        this.targetClassName = targetClassName;
        this.targetFieldName = targetFieldName;
        this.expectedTargetFieldTypeDescriptors =
            Stream.of(expectedTargetFieldType, AsmUtils.objectToPrimitive.get(expectedTargetFieldType))
                .filter(Objects::nonNull)
                .map(Type::getDescriptor)
                .collect(Collectors.toSet());

        boxedType = expectedTargetFieldType;

        this.isMandatory = isMandatory;
        this.createField = createField;
        actualFieldName = targetFieldName;
        actualFieldType = expectedTargetFieldType;
        this.setterDesc = setterDesc;
        this.getterDesc = getterDesc;
    }

    @Override
    public String getTargetClassName() {
        return targetClassName;
    }

    public String getTargetFieldName() {
        return targetFieldName;
    }

    public boolean checkDescription(String desc) {
        return expectedTargetFieldTypeDescriptors.contains(desc);
    }

    public String getPatchedFieldName() {
        return targetClassName + '#' + targetFieldName + " " + actualFieldType.getDescriptor() + " (actually named " + actualFieldName + ")";
    }

    @Override
    public boolean isMandatory() {
        return isMandatory;
    }

    @Override
    public boolean needToCreate() {
        return true;
    }

    public void foundExistedField(String name, int access, String desc) {
        found = true;
        isStaticField = AsmUtils.isStatic(access);
        actualFieldName = name;
        actualFieldType = Type.getType(desc);
    }

    @Override
    public void create(HookInjectorClassVisitor hookInjectorClassVisitor) {
        if (!found) {
            if (createField) {
                hookInjectorClassVisitor
                    .visitField(0, actualFieldName, actualFieldType.getDescriptor(), null, null)
                    .visitEnd();
            } else {
                return;
            }
        }
        clearStateForMixinTwiceProcessing();

        {
            MethodVisitor mv = hookInjectorClassVisitor.visitMethod(ACC_STATIC | ACC_PUBLIC, targetFieldName + setterSuffix, setterDesc, null, null);
            mv.visitCode();
            mv.visitLabel(new Label());

            if (!isStaticField)
                mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(boxedType.getOpcode(ILOAD), 1);
            if (!boxedType.equals(actualFieldType))
                mv.visitMethodInsn(INVOKEVIRTUAL, boxedType.getInternalName(), AsmUtils.primitiveToUnboxingMethod.get(actualFieldType), Type.getMethodDescriptor(actualFieldType), false);
            mv.visitFieldInsn(isStaticField ? PUTSTATIC : PUTFIELD, getTargetClassInternalName(), actualFieldName, actualFieldType.getDescriptor());

            mv.visitInsn(RETURN);

            mv.visitLabel(new Label());
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = hookInjectorClassVisitor.visitMethod(ACC_STATIC | ACC_PUBLIC, targetFieldName + getterSuffix, getterDesc, null, null);
            mv.visitCode();
            mv.visitLabel(new Label());

            if (!isStaticField)
                mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(isStaticField ? GETSTATIC : GETFIELD, getTargetClassInternalName(), actualFieldName, actualFieldType.getDescriptor());
            if (!boxedType.equals(actualFieldType))
                mv.visitMethodInsn(INVOKESTATIC, boxedType.getInternalName(), "valueOf", Type.getMethodDescriptor(boxedType, actualFieldType), false);

            mv.visitInsn(boxedType.getOpcode(IRETURN));


            mv.visitLabel(new Label());
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        hookInjectorClassVisitor.markInjected(this);
    }

    private void clearStateForMixinTwiceProcessing() {
        found = false;
    }

    @Override
    public int compareTo(AsmInjection o) {
        if (o instanceof AsmFieldLens) {
            AsmFieldLens other = (AsmFieldLens) o;
            if (createField || isMandatory)
                return -1;
            else {
                return other.createField || other.isMandatory ? 1 : 0;
            }
        }
        return AsmInjection.super.compareTo(o);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AsmFieldLens lens = (AsmFieldLens) o;
        return targetClassName.equals(lens.targetClassName) && targetFieldName.equals(lens.targetFieldName) && boxedType.equals(lens.boxedType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetClassName, targetFieldName, boxedType);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AsmLens: ");

        sb.append(targetClassName).append('#').append(targetFieldName).append(": ");
        sb.append(boxedType);

        sb.append(", CreateField = " + createField);

        return sb.toString();
    }

}
