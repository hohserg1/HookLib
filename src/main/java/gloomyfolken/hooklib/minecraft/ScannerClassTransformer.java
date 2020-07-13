package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.common.SafeClassWriter;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.ASM5;

public class ScannerClassTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        System.out.println("transform " + name.equals(transformedName) + " name " + name + ", transformedName " + transformedName);

        ClassReader classReader = new ClassReader(basicClass);


        ClassNode classNode = new ClassNode(ASM5);
        classReader.accept(classNode, ClassReader.SKIP_FRAMES);

        System.out.println("methods " + classNode.methods.stream().map(m -> m.name).collect(Collectors.toList()));


        ClassWriter classWriter = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
}
