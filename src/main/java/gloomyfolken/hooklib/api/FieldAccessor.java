package gloomyfolken.hooklib.api;

/**
 * Use it to access to private field of some class
 *
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
