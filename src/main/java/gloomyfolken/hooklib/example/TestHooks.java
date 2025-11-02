package gloomyfolken.hooklib.example;

import com.mojang.authlib.*;
import com.mojang.authlib.minecraft.*;
import com.mojang.authlib.yggdrasil.*;
import gloomyfolken.hooklib.api.*;
import io.netty.buffer.*;
import net.minecraft.block.*;
import net.minecraft.block.state.*;
import net.minecraft.client.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.network.*;
import net.minecraft.server.management.*;
import net.minecraft.util.*;
import net.minecraft.util.datafix.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.world.chunk.*;
import net.minecraftforge.common.*;
import net.minecraftforge.fml.relauncher.*;
import net.minecraftforge.items.*;

import javax.annotation.*;
import java.io.*;
import java.net.*;
import java.util.*;

@HookContainer
public class TestHooks {

    @FieldLens(createField = true)
    public static FieldAccessor<InnerPrivateClassImage, @Primitive Integer> testFieldAddition1 = FieldAccessor.defaultValue(1);

    @FieldLens(createField = true)
    public static FieldAccessor<TestTarget, Integer> testFieldAddition2 = FieldAccessor.defaultValue(1);

    @FieldLens(createField = true)
    public static FieldAccessor<TestTarget, String> testFieldAddition3 = FieldAccessor.defaultValue("test");

    @FieldLens(createField = true)
    public static FieldAccessor<TestTarget, Test> testFieldAddition4 = FieldAccessor.defaultValue(new Test());

    @FieldLens(createField = true)
    public static FieldAccessor<TestTarget, @Primitive Double> testFieldAddition5 = FieldAccessor.defaultValue(2d);

    @SideOnly(Side.CLIENT)
    @Hook
    @OnMethodCall(value = "readBytes", shift = Shift.INSTEAD, ordinal = {0, 1})
    public static ByteBuf read(Chunk chunk, PacketBuffer buf, int availableSections, boolean groundUpContinuous) {
        return null;
    }

    @Hook
    @OnMethodCall(value = "println", ordinal = {1, 2})
    public static void targetMethodFewCalls(TestTarget testTarget) {
    }

    @FieldLens
    public static FieldAccessor<Minecraft, Boolean> actionKeyF3;

    @FieldLens
    public static FieldAccessor<Minecraft, Long> debugUpdateTime;

    @MethodLens
    public static void staticTargetMethodVoid(InnerPrivateClassImage testTarget, int a, String b) {
    }

    @MethodLens
    public static String staticTargetMethodObject(TestTarget testTarget, int a, String b) {
        return "";
    }

    @MethodLens
    public static int staticTargetMethodPrimitive(TestTarget testTarget, int a, String b) {
        return 0;
    }

    @MethodLens
    public static void targetMethodVoid(InnerPrivateClassImage testTarget, int a, String b) {
    }

    @MethodLens
    public static String targetMethodObject(TestTarget testTarget, int a, String b) {
        return "";
    }

    @MethodLens
    public static int targetMethodPrimitive(TestTarget testTarget, int a, String b) {
        return 0;
    }

    @SideOnly(Side.CLIENT)
    @Hook(targetMethod = "randomDisplayTick")
    @OnExpression(expressionPattern = "randomDisplayTickPattern", shift = Shift.INSTEAD)
    public static EnumParticleTypes randomDisplayTick(BlockTorch torch, IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        return EnumParticleTypes.FLAME;
    }

    @SideOnly(Side.CLIENT)
    public static EnumParticleTypes randomDisplayTickPattern() {
        return EnumParticleTypes.FLAME;
    }

    public static void testDoubleArgumentPop2() {
        new TestHooks().kek(EnumParticleTypes.EXPLOSION_LARGE, 0, 0, 0, 0, 0, 0);
    }

    public void kek(EnumParticleTypes particleType, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
    }

    @FieldLens(createField = true)
    public static FieldAccessor<Minecraft, Integer> prevX;

    @Hook
    @OnBegin
    public static void resize(Minecraft mc, int x, int y) {
        System.out.println("prevX=" + prevX.get(mc));
        prevX.set(mc, x);
        System.out.println("Resize, x=" + x + ", y=" + y);
        System.out.println(staticTargetMethodObject(null, 1, "1"));
        System.out.println(staticTargetMethodPrimitive(null, 1, "1"));
        System.out.println(targetMethodObject(new TestTarget(), 1, "1"));
        System.out.println(targetMethodPrimitive(new TestTarget(), 1, "1"));
    }


    @Hook
    @OnMethodCall(value = "resize", shift = Shift.AFTER)
    public static void checkWindowResize(Minecraft mc) {
        System.out.println("init resize, x=" + mc.displayWidth + ", y=" + mc.displayHeight);
        System.out.println(SlotItemHandler.class);
    }

    @Hook
    @OnReturn
    public static ReturnSolve<Integer> getTotalArmorValue(ForgeHooks fh, EntityPlayer player) {
        return ReturnSolve.yes(1);
    }

    @Hook
    @OnMethodCall(value = "trigger", shift = Shift.BEFORE)
    public static ReturnSolve<@Primitive Boolean> attemptDamageItem(ItemStack stack, int amount, Random rand, @Nullable EntityPlayerMP damager) {
        if (amount > 0)
            return ReturnSolve.yes(false);

        return ReturnSolve.no();
    }

    @Hook(targetMethod = Constants.CONSTRUCTOR_NAME)
    @OnBegin
    public static void init(InnerPrivateClassImage self) {

    }

    @Hook
    @OnBegin
    public static String toString(@PrivateClass("gloomyfolken.hooklib.example.TestTarget$1") Test self) {
        return "hmmmmmm";
    }

    @Hook(targetMethod = Constants.CONSTRUCTOR_NAME)
    @OnBegin
    public static void init(@PrivateClass("net.minecraft.server.MinecraftServer") Object server,
                            File anvilFileIn, Proxy proxyIn, DataFixer dataFixerIn, YggdrasilAuthenticationService authServiceIn, MinecraftSessionService sessionServiceIn, GameProfileRepository profileRepoIn, PlayerProfileCache profileCacheIn) {
        TestTarget.triggerInnerClass();
        System.out.println(TestTarget.triggetInnerAnonymousClass());
    }
}