package gloomyfolken.hooklib.example;

import gloomyfolken.hooklib.api.HookContainer;
import gloomyfolken.hooklib.api.HookLens;
import gloomyfolken.hooklib.api.Lens;

@HookContainer(modid = "test-hooklib")
public class TestHooks {
    @HookLens
    public static Lens<Test, String> test = new Lens<Test, String>() {
    };

    public static String kek = "";

    public static class Test {
        public String test = "lol";
    }
}
