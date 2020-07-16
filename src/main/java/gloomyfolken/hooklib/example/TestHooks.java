package gloomyfolken.hooklib.example;

import gloomyfolken.hooklib.api.Hook;
import gloomyfolken.hooklib.api.HookContainer;
import gloomyfolken.hooklib.api.HookLens;
import gloomyfolken.hooklib.api.Lens;
import net.minecraft.tileentity.TileEntity;

@HookContainer(modid = "test-hooklib")
public class TestHooks {

    @Hook
    public static void test(TileEntity tileEntity, int a, @Hook.LocalVariable(2) int b, int c) {

    }


    @HookLens
    public static Lens<Test, String> test = new Lens<Test, String>() {
    };

    public static String kek = "";

    public static class Test {
        public String test = "lol";
    }
}
