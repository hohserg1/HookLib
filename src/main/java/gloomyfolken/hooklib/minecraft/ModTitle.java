package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.example.Test;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = "hooklib", name = "HookLib", version = "1.0", acceptedMinecraftVersions = "1.12")
public class ModTitle {
    Test test = new Test();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        System.out.println("lol " + test.lol());

        test.a=1;
        System.out.println("lol " + test.lol());

        test.a=2;
        System.out.println("lol " + test.lol());

    }
}
