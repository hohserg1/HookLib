package gloomyfolken.hooklib.api;

/**
 * Use it with {@link Hook#priority}
 * <p>
 * Hooks with {@link #HIGHEST} priority  will be called earlier
 * <p>
 * Hooks with {@link #LOWEST} priority  will be called later
 */
public enum HookPriority {

    HIGHEST,
    HIGH,
    NORMAL,
    LOW,
    LOWEST

}
