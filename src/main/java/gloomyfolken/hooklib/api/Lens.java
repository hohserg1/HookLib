package gloomyfolken.hooklib.api;

public abstract class Lens<TargetClassType, TargetFieldType> {

    public void set(TargetClassType object, TargetFieldType value) {
    }

    public TargetFieldType get(TargetClassType object) {
        return null;
    }

}
