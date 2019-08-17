package gloomyfolken.hooklib.asm.model;

import com.google.common.collect.ImmutableList;
import lombok.Data;
import org.objectweb.asm.Type;

import java.util.stream.Collectors;

public @Data
class TargetMethodSpec {
    public final String name;
    public final String classContainer;
    public final ImmutableList<Type> parameters;
    public final Type returnType;

    public String toString() {
        return classContainer + "#" + name + "(" + parameters.stream().map(Type::getDescriptor).collect(Collectors.joining(",")) + ")" + returnType.getDescriptor();
    }
}
