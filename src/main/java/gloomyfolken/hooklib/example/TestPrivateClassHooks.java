package gloomyfolken.hooklib.example;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import gloomyfolken.hooklib.api.*;
import net.minecraft.server.management.PlayerProfileCache;

import java.io.File;
import java.net.Proxy;

@HookContainer
public class TestPrivateClassHooks {

    @FieldLens(targetField = "privateTypeField")
    public static FieldAccessor<TestTarget, @PrivateClass("gloomyfolken.hooklib.example.TestTarget$InnerPrivateClass") Object> privateTypeField_Annotation;

    @FieldLens(targetField = "privateTypeField")
    public static FieldAccessor<TestTarget, InnerPrivateClassImage> privateTypeField_Image;


    @Hook(targetMethod = Constants.CONSTRUCTOR_NAME)
    @OnBegin
    public static void init(@PrivateClass("net.minecraft.server.MinecraftServer") Object server,
                            File anvilFileIn, Proxy proxyIn, @PrivateClass("net.minecraft.util.datafix.DataFixer") Object dataFixerIn, YggdrasilAuthenticationService authServiceIn, MinecraftSessionService sessionServiceIn, GameProfileRepository profileRepoIn, PlayerProfileCache profileCacheIn) {
        TestTarget.triggerInnerClass();
        System.out.println(TestTarget.triggetInnerAnonymousClass());
        System.out.println("privateTypeField_Annotation before " + privateTypeField_Annotation.get(null));
        System.out.println("privateTypeField_Image before " + privateTypeField_Image.get(null));
        privateTypeField_Annotation.set(null, null);
        System.out.println("privateTypeField_Annotation after " + privateTypeField_Annotation.get(null));
        System.out.println("privateTypeField_Image after " + privateTypeField_Image.get(null));
    }
}
