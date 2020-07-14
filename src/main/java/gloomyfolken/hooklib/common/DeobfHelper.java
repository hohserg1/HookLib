package gloomyfolken.hooklib.common;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

public class DeobfHelper {
    public static String deobfClassName(String name) {
        return FMLDeobfuscatingRemapper.INSTANCE.map(name);
    }

    public static boolean isObfuscated() {
        return !(Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
    }
}
