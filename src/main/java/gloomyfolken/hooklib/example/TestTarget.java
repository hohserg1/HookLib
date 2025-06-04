package gloomyfolken.hooklib.example;

import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class TestTarget {

    private static void staticTargetMethodVoid(int a, String b) {

    }

    private static String staticTargetMethodObject(int a, String b) {
        return "staticTargetMethodObject result";
    }

    private static int staticTargetMethodPrimitive(int a, String b) {
        return 1;
    }

    private void targetMethodVoid(int a, String b) {

    }

    private String targetMethodObject(int a, String b) {
        return "targetMethodObject result";
    }

    private int targetMethodPrimitive(int a, String b) {
        return 2;
    }

    private void targetMethodFewCalls() {
        System.out.println("bruh");
        System.out.println("kek");
        System.out.println("lol");
        System.out.println("foo");
        System.out.println("bar");
    }

    public static void triggerInnerClass() {
        System.out.println(InnerPrivateClass.class);
    }

    private static class InnerPrivateClass {

        private static void staticTargetMethodVoid(int a, String b) {

        }

        private void targetMethodVoid(int a, String b) {

        }

    }

    public static Test triggetInnerAnonymousClass() {
        return new Test() {
            @Override
            public String toString() {
                return "hm";
            }
        };
    }
}
