package gloomyfolken.hooklib.minecraft;

import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.relauncher.CoreModManager;

import java.lang.reflect.Field;

public class HookLibPlugin {

    private static boolean obf;
    private static boolean checked;

    public static boolean isObfuscated() {
        if (!checked) {
            try {
                Field deobfField = CoreModManager.class.getDeclaredField("deobfuscatedEnvironment");
                deobfField.setAccessible(true);
                obf = !deobfField.getBoolean(null);
                FMLLog.log.info(" Obfuscated: " + obf);
            } catch (Exception e) {
                e.printStackTrace();
            }
            checked = true;
        }
        return obf;
    }
}
