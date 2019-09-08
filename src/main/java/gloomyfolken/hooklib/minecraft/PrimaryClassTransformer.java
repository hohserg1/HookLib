package gloomyfolken.hooklib.minecraft;

import com.google.common.collect.Multimap;
import gloomyfolken.hooklib.asm.HookClassTransformer;
import gloomyfolken.hooklib.asm.model.method.hook.AsmHook;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.Type;

/**
 * Этим трансформером трансформятся все классы, которые грузятся раньше майновских.
 * В момент начала загрузки майна (точнее, чуть раньше - в Loader.injectData) все хуки отсюда переносятся в
 * MinecraftClassTransformer. Такой перенос нужен, чтобы трансформеры хуклибы применялись последними - в частности,
 * после деобфускации, которую делает фордж.
 */
public class PrimaryClassTransformer extends HookClassTransformer implements IClassTransformer {

    // костыль для случая, когда другой мод дергает хуклиб раньше, чем она запустилась
    static PrimaryClassTransformer instance = new PrimaryClassTransformer();
    boolean registeredSecondTransformer;

    public PrimaryClassTransformer() {
        classMetadataReader = HookLoader.getDeobfuscationMetadataReader();

        if (instance != null) {
            // переносим хуки, которые уже успели нарегистрировать
            hookMap.putAll(PrimaryClassTransformer.instance.getHooksMap());
            PrimaryClassTransformer.instance.getHooksMap().clear();
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
    protected boolean isTargetMethod(AsmHook ah, String name, String desc) {
        return super.isTargetMethod(ah, name, mapDesc(desc));
    }

    Multimap<String, AsmHook> getHooksMap() {
        return hookMap;
    }

    static String mapDesc(String desc) {
        if (!HookLibPlugin.isObfuscated()) return desc;

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
        if (!HookLibPlugin.isObfuscated()) return type;

        // void or primitive
        if (type.getSort() < 9) return type;

        //array
        if (type.getSort() == 9) {//check for equals
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < type.getDimensions(); i++) {
                sb.append("[");
            }
            boolean isPrimitiveArray = type.getSort() < 9;//check for leaser. may be error?
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
