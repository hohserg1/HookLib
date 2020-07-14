package gloomyfolken.hooklib.api;

public abstract class Lens<TargetClassType, TargetFieldType> {

    public abstract void set(TargetClassType object, TargetFieldType value);

    public abstract TargetFieldType get(TargetClassType object);

}
