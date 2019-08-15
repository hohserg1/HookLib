package gloomyfolken.hooklib.asm;

import lombok.Data;
import org.objectweb.asm.tree.AbstractInsnNode;

@Data
public class ConcretePoint {
    public final AbstractInsnNode from;
    public final int size;
}
