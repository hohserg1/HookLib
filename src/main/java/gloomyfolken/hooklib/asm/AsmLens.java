package gloomyfolken.hooklib.asm;

import org.objectweb.asm.Type;

import java.util.Objects;

public class AsmLens implements AsmInjection {
    private final String targetClassName;
    private final String targetFieldName;
    private final Type targetFieldType;
    private final boolean isMandatory;

    private final boolean createField;
    private final Object defaultValue;

    public AsmLens(String targetClassName, String targetFieldName, Type targetFieldType, boolean isMandatory, boolean createField, Object defaultValue) {
        this.targetClassName = targetClassName;
        this.targetFieldName = targetFieldName;
        this.targetFieldType = targetFieldType;
        this.isMandatory = isMandatory;
        this.createField = createField;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getTargetClassName() {
        return targetClassName;
    }

    public String getPatchedFieldName() {
        return targetClassName + '#' + targetFieldName + " " + targetFieldType.getDescriptor();
    }

    @Override
    public boolean isMandatory() {
        return isMandatory;
    }

    @Override
    public boolean needToCreate() {
        return createField;
    }

    @Override
    public void create(HookInjectorClassVisitor hookInjectorClassVisitor) {
        hookInjectorClassVisitor
                .visitField(0, targetFieldName, targetFieldType.getDescriptor(), null, defaultValue)
                .visitEnd();
    }

    @Override
    public int compareTo(AsmInjection o) {
        if (o instanceof AsmLens) {
            if (createField)
                return -1;
            else
                return ((AsmLens) o).createField ? 1 : 0;
        }
        return AsmInjection.super.compareTo(o);
    }

    public boolean isTargetField(String name, String desc) {
        return desc.equals(targetFieldType.getDescriptor()) && name.equals(targetFieldName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AsmLens lens = (AsmLens) o;
        return isMandatory == lens.isMandatory && createField == lens.createField && targetClassName.equals(lens.targetClassName) && targetFieldName.equals(lens.targetFieldName) && targetFieldType.equals(lens.targetFieldType) && Objects.equals(defaultValue, lens.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetClassName, targetFieldName, targetFieldType, isMandatory, createField, defaultValue);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AsmLens: ");

        sb.append(targetClassName).append('#').append(targetFieldName).append(": ");
        sb.append(targetFieldType);

        sb.append(", CreateField = " + createField);
        sb.append(", defaultValue = " + defaultValue);

        return sb.toString();
    }

}
