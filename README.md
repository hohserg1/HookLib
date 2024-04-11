[![](https://jitpack.io/v/hohserg1/HookLib.svg)](https://jitpack.io/#hohserg1/HookLib)

# HookLib

This is framework for coremods at MinecraftForge.

Goal of this project is to provide an easy way to modify existing logic without directly changing code of it, but by writing your additional code.

Doesn't require knowledge of jvm bytecode.

*Welp, kinda I'm late with publishing it project, bc of Mixins took over the world, but I found out HookLib more handy, and I still have unimplemented
good ideas, which will make it even better. So maybe it makes sense*

## Getting started

### Understanding of hooks

When exists some code which you wanna to change, but its part of Minecraft or other mod, you can write something like:

```java

@HookContainer
public class MyHooks {
    @Hook
    @OnBegin
    public static void resize(Minecraft mc, int x, int y) {
        System.out.println("Resize, x=" + x + ", y=" + y);
    }
}
```

HookLib will find method with name `resize` with two int's arguments in class `Minecraft` and will insert call of MyHooks#resize to begin of found
target method.

Then you will see in console message when window resized.

### Adding to project

1. Add dependency to build.gradle:

```groovy
repositories {
    maven {
        url "https://cursemaven.com"
    }
}
dependencies {
    implementation "curse.maven:hooklib:12345"
}
```

2. Add VM option `-Dfml.coreMods.load=gloomyfolken.hooklib.minecraft.MainHookLoader`

It's possible by gradle too:

```groovy
minecraft {
    mappings channel: 'snapshot', version: '20171003-1.12'

    runs {
        client {
            workingDirectory project.file('run')
            property "fml.coreMods.load", "gloomyfolken.hooklib.minecraft.MainHookLoader" //here
        }

        server {
            workingDirectory project.file('run')
            property "fml.coreMods.load", "gloomyfolken.hooklib.minecraft.MainHookLoader" //here
        }
    }
}
```

3. Perform Gradle Refresh in your ide

### Next learning

Check our wiki for api details and advanced techniques
