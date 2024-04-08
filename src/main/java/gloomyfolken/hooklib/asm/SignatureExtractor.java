package gloomyfolken.hooklib.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class SignatureExtractor {
    public interface TypeRepr {
        Type getRawType();
    }

    public static class ParametrizedTypeRepr implements TypeRepr {
        public final Type rawType;
        public final List<TypeRepr> parameters;

        public ParametrizedTypeRepr(Type rawType, List<TypeRepr> parameters) {
            this.rawType = rawType;
            this.parameters = parameters;
        }

        @Override
        public String toString() {
            return "ParametrizedTypeRepr{rawType=" + rawType + ", parameters=" + parameters + '}';
        }

        @Override
        public Type getRawType() {
            return rawType;
        }
    }

    public static class FlatTypeRepr implements TypeRepr {
        public final Type type;

        public FlatTypeRepr(Type type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "FlatTypeRepr{type=" + type + '}';
        }

        @Override
        public Type getRawType() {
            return type;
        }
    }

    public static TypeRepr fromReturnType(MethodNode methodNode) {
        if (methodNode.signature == null)
            return new FlatTypeRepr(Type.getMethodType(methodNode.desc).getReturnType());

        ReturnTypeVisitor v = new ReturnTypeVisitor();
        new SignatureReader(methodNode.signature).accept(v);
        return v.result;
    }

    public static TypeRepr fromField(FieldNode fieldNode) {
        if (fieldNode.signature == null)
            return new FlatTypeRepr(Type.getType(fieldNode.desc));

        AtomicReference<TypeRepr> result = new AtomicReference<>();
        new SignatureReader(fieldNode.signature).acceptType(new ParametrizedVisitor(result::set));
        return result.get();
    }

    private static class ReturnTypeVisitor extends SignatureVisitor {
        public TypeRepr result;

        public ReturnTypeVisitor() {
            super(Opcodes.ASM5);
        }

        @Override
        public SignatureVisitor visitReturnType() {
            return new ParametrizedVisitor(t -> result = t);
        }
    }

    private static class ParametrizedVisitor extends SignatureVisitor {

        private final Consumer<TypeRepr> addParameter;

        Type rawType;
        List<TypeRepr> parameters = new ArrayList<>();

        public ParametrizedVisitor(Consumer<TypeRepr> addParameter) {
            super(Opcodes.ASM5);
            this.addParameter = addParameter;
        }

        @Override
        public void visitClassType(String name) {
            rawType = Type.getObjectType(name);
        }

        @Override
        public SignatureVisitor visitTypeArgument(char wildcard) {
            return new ParametrizedVisitor(parameters::add);
        }

        @Override
        public void visitEnd() {
            if (parameters.isEmpty())
                addParameter.accept(new FlatTypeRepr(rawType));
            else
                addParameter.accept(new ParametrizedTypeRepr(rawType, parameters));
        }
    }
}
