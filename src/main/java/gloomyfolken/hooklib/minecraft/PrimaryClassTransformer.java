package gloomyfolken.hooklib.minecraft;

import com.google.common.collect.ListMultimap;
import gloomyfolken.hooklib.asm.HookClassTransformer;
import gloomyfolken.hooklib.asm.HookInjectorClassVisitor;
import gloomyfolken.hooklib.asm.injections.AsmInjection;
import gloomyfolken.hooklib.asm.injections.AsmMethodInjection;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import java.util.List;

/**
 * This transformer uses for all classes which loaded before Minecraft classes.
 * kinda have no sense to separate it
 */
public class PrimaryClassTransformer extends HookClassTransformer implements IClassTransformer {

    //if some mod accessed to HookLib before it loaded
    static PrimaryClassTransformer instance = new PrimaryClassTransformer();
    boolean registeredSecondTransformer;

    public PrimaryClassTransformer() {
        classMetadataReader = HookLoader.getDeobfuscationMetadataReader();

        if (instance != null) {
            hooksMap.putAll(instance.getHooksMap());
            instance.getHooksMap().clear();
        } else {
            registerHookContainer("gloomyfolken.hooklib.minecraft.SecondaryTransformerHook");
        }
        instance = this;
    }

    @Override
    public byte[] transform(String oldName, String newName, byte[] bytecode) {
        return transform(newName, bytecode);
    }

    @Override
    protected HookInjectorClassVisitor createInjectorClassVisitor(ClassVisitor finalizeVisitor, List<AsmInjection> hooks) {
        return new HookInjectorClassVisitor(this, finalizeVisitor, hooks) {
            @Override
            protected boolean isTargetMethod(AsmMethodInjection hook, String name, String desc) {
                return super.isTargetMethod(hook, name, mapDesc(desc));
            }
        };
    }

    ListMultimap<String, AsmInjection> getHooksMap() {
        return hooksMap;
    }

    static String mapDesc(String desc) {
        if (!HookLibPlugin.getObfuscated()) return desc;

        Type methodType = Type.getMethodType(desc);
        Type mappedReturnType = map(methodType.getReturnType());
        Type[] argTypes = methodType.getArgumentTypes();
        Type[] mappedArgTypes = new Type[argTypes.length];
        for (int i = 0; i < mappedArgTypes.length; i++) {
            mappedArgTypes[i] = map(argTypes[i]);
        }
        return Type.getMethodDescriptor(mappedReturnType, mappedArgTypes);
    }

    static Type map(Type type) {
        if (!HookLibPlugin.getObfuscated()) return type;

        // void or primitive
        if (type.getSort() < 9) return type;

        //array
        if (type.getSort() == 9) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < type.getDimensions(); i++) {
                sb.append("[");
            }
            boolean isPrimitiveArray = type.getSort() < 9;
            if (!isPrimitiveArray) sb.append("L");
            sb.append(map(type.getElementType()).getInternalName());
            if (!isPrimitiveArray) sb.append(";");
            return Type.getType(sb.toString());
        } else if (type.getSort() == 10) {
            String unmappedName = FMLDeobfuscatingRemapper.INSTANCE.map(type.getInternalName());
            return Type.getType("L" + unmappedName + ";");
        } else {
            throw new IllegalArgumentException("Can not map method type!");
        }
    }
}
