package gloomyfolken.hooklib.example;

import gloomyfolken.hooklib.api.*;
import gloomyfolken.hooklib.asm.ReturnCondition;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nullable;
import java.util.Random;

@HookContainer
public class TestHooks {


    @Hook(targetMethod = "randomDisplayTick")
    @OnExpression(expressionPattern = "randomDisplayTickPattern", shift = Shift.INSTEAD)
    public static EnumParticleTypes randomDisplayTick(BlockTorch torch, IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        return EnumParticleTypes.FLAME;
    }

    public static EnumParticleTypes randomDisplayTickPattern() {
        return EnumParticleTypes.FLAME;
    }

    @Hook
    @OnMethodCall(value = "kek", shift = Shift.INSTEAD, ordinal = -1)
    public static void testDoubleArgumentPop2(TestHooks self) {

    }

    public static void testDoubleArgumentPop2() {
        new TestHooks().kek(EnumParticleTypes.EXPLOSION_LARGE, 0, 0, 0, 0, 0, 0);
    }

    public void kek(EnumParticleTypes particleType, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
    }

    @FieldLens(createField = true)
    public static void prevX(Minecraft mc, int x) {
    }

    @FieldLens(createField = true)
    public static int prevX(Minecraft mc) {
        return 0;
    }

    @Hook
    @OnBegin
    public static void resize(Minecraft mc, int x, int y) {
        System.out.println("prevX=" + prevX(mc));
        prevX(mc, x);
        System.out.println("Resize, x=" + x + ", y=" + y);
    }


    @Hook
    @OnMethodCall(value = "resize", shift = Shift.AFTER)
    public static void checkWindowResize(Minecraft mc) {
        System.out.println("init resize, x=" + mc.displayWidth + ", y=" + mc.displayHeight);
    }

    /**
     * Цель: уменьшить вдвое показатели брони у всех игроков.
     * P.S: фордж перехватывает получение показателя брони, ну а мы перехватим перехватчик :D
     */
    @Hook(returnCondition = ReturnCondition.ALWAYS, returnConstant = @ReturnConstant(intValue = 1))
    @OnReturn
    public static void getTotalArmorValue(ForgeHooks fh, EntityPlayer player) {
        //return returnValue / 2;
    }

    @Hook(returnCondition = ReturnCondition.ON_TRUE, returnConstant = @ReturnConstant(booleanValue = false))
    @OnMethodCall(value = "trigger", shift = Shift.BEFORE)
    public static boolean attemptDamageItem(ItemStack stack, int amount, Random rand, @Nullable EntityPlayerMP damager) {
        if (amount > 0) {
            return rand.nextFloat() > 0.5;
        }
        return false;
    }
}