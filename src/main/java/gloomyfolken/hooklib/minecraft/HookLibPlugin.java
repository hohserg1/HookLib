package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.helper.Logger;
import net.minecraftforge.fml.relauncher.CoreModManager;

import java.lang.reflect.Field;

public class HookLibPlugin {

    private static boolean obf;
    private static boolean checked;

    public static boolean getObfuscated() {
        if (!checked) {
            try {
                Field deobfField = CoreModManager.class.getDeclaredField("deobfuscatedEnvironment");
                deobfField.setAccessible(true);
                obf = !deobfField.getBoolean(null);
                Logger.instance.info("Obfuscated: " + obf);
            } catch (Exception e) {
                e.printStackTrace();
            }
            checked = true;
        }
        return obf;
    }
}
