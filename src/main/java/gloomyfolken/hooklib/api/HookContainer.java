package gloomyfolken.hooklib.api;

import gloomyfolken.hooklib.minecraft.ModTitle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface HookContainer {
    String modid();

    String mappings() default ModTitle.default_mappings;
}
