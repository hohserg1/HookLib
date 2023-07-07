package gloomyfolken.hooklib.helper;

public enum Logger {
    instance;

    public void debug(String message) {
        System.out.println("[HookLib][DEBUG] " + message);
    }

    public void warning(String message) {
        System.out.println("[HookLib][WARNING] " + message);
    }

    public void error(String message) {
        System.out.println("[HookLib][ERROR] " + message);
    }

    public void error(String message, Throwable cause) {
        error(message);
        cause.printStackTrace();
    }

    public void info(String message) {
        System.out.println("[HookLib][INFO] " + message);
    }
}
