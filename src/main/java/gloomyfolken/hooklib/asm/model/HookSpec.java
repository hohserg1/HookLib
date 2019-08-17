package gloomyfolken.hooklib.asm.model;

import com.google.common.collect.ImmutableList;
import lombok.Data;
import org.objectweb.asm.Type;

public @Data
class HookSpec {
    public final String name;
    public final String classContainer;
    public final ImmutableList<Type> parameters;
    public final ImmutableList<Integer> localCaptureIds;
    public final Type returnType;


    public String description() {
        return Type.getMethodType(getReturnType(), getParameters().toArray(new Type[0])).getDescriptor();
    }

    public boolean hasReturnCapture() {
        return getLocalCaptureIds().stream().anyMatch(i -> i == -1);
    }
}
