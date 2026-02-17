package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.asm.AsmUtils;
import gloomyfolken.hooklib.asm.HookClassTransformer;
import gloomyfolken.hooklib.asm.HookInjectorClassVisitor;
import gloomyfolken.hooklib.asm.injections.AsmInjection;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import java.util.List;

/**
 * This transformer uses for all classes which loaded before Minecraft classes.
 * kinda have no sense to separate it
 */
public class PrimaryClassTransformer implements TransformingStage {

    @Override
    public HookInjectorClassVisitor createInjectorClassVisitor(HookClassTransformer transformer, ClassVisitor finalizeVisitor, List<AsmInjection> hooks) {
        return new HookInjectorClassVisitor(transformer, finalizeVisitor, hooks) {
            @Override
            protected String deobfDescription(String desc) {
                return mapDesc(desc);
            }
        };
    }

    static String mapDesc(String desc) {
        if (!HookLibPlugin.getObfuscated()) return desc;

        Type methodType = Type.getMethodType(desc);
        Type mappedReturnType = mapDeobf(methodType.getReturnType());
        Type[] argTypes = methodType.getArgumentTypes();
        Type[] mappedArgTypes = new Type[argTypes.length];
        for (int i = 0; i < mappedArgTypes.length; i++) {
            mappedArgTypes[i] = mapDeobf(argTypes[i]);
        }
        return Type.getMethodDescriptor(mappedReturnType, mappedArgTypes);
    }

    static Type mapDeobf(Type type) {
        return AsmUtils.mapBy(type, FMLDeobfuscatingRemapper.INSTANCE::map);
    }

}
