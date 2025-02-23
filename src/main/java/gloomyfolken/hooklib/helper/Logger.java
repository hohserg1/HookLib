package gloomyfolken.hooklib.helper;

import java.util.logging.FileHandler;

public enum Logger {
    instance;

    private java.util.logging.Logger toFileLogger;
    private boolean canLogToFile;

    Logger() {
        try {
            toFileLogger = java.util.logging.Logger.getLogger("hooklib");
            FileHandler fh = new FileHandler("./logs/hooklib.log", true);
            toFileLogger.addHandler(fh);
            fh.setFormatter(new LogFormat());
            canLogToFile = true;
        } catch (Throwable e) {
            toFileLogger = null;
            canLogToFile = false;
            error("unable to init HookLib logger", e);
        }
    }

    public void debug(String message) {
        System.out.println("[HookLib][DEBUG] " + message);
        if (canLogToFile)
            toFileLogger.fine(message);
    }

    public void warning(String message) {
        System.out.println("[HookLib][WARNING] " + message);
        if (canLogToFile)
            toFileLogger.warning(message);
    }

    public void error(String message) {
        System.out.println("[HookLib][ERROR] " + message);
        if (canLogToFile)
            toFileLogger.severe(message);
    }

    public void error(String message, Throwable cause) {
        error(message);
        cause.printStackTrace();
        if (canLogToFile)
            toFileLogger.throwing("", "", cause);
    }

    public void info(String message) {
        System.out.println("[HookLib][INFO] " + message);
        if (canLogToFile)
            toFileLogger.info(message);
    }
}
