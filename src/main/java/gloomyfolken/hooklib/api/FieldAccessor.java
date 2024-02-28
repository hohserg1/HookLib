package gloomyfolken.hooklib.api;

/**
 * Use it for access to private field of some class
 *
 * @see FieldLens
 */
public interface FieldAccessor<TargetClass, TargetFieldType> {
    TargetFieldType get(TargetClass instance);

    void set(TargetClass instance, TargetFieldType newValue);
}
