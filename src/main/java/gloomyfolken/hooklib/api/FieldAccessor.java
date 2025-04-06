package gloomyfolken.hooklib.api;

/**
 * Use it to access to private field of some class
 *
 * @param <TargetFieldType> is a type of target field, can be marked by {@link gloomyfolken.hooklib.api.Primitive} annotation for strictly primitive type
 * @see FieldLens
 */
public interface FieldAccessor<TargetClass, TargetFieldType> {
    TargetFieldType get(TargetClass instance);

    void set(TargetClass instance, TargetFieldType newValue);

    /**
     * Use it to set default value of created field
     *
     * @see FieldLens#createField
     */
    static <TargetClass, TargetFieldType> FieldAccessor<TargetClass, TargetFieldType> defaultValue(TargetFieldType v) {
        return null;
    }
}
