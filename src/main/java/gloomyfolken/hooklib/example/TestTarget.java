package gloomyfolken.hooklib.example;

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
}
