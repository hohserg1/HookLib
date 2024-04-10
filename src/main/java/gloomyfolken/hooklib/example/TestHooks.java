package gloomyfolken.hooklib.example;

import gloomyfolken.hooklib.api.*;
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
    public static FieldAccessor<Minecraft, Integer> prevX;

    @Hook
    @OnBegin
    public static void resize(Minecraft mc, int x, int y) {
        System.out.println("prevX=" + prevX.get(mc));
        prevX.set(mc, x);
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
    @Hook
    @OnReturn
    public static ReturnSolve<Integer> getTotalArmorValue(ForgeHooks fh, EntityPlayer player) {
        return ReturnSolve.yes(1);
    }

    @Hook
    @OnMethodCall(value = "trigger", shift = Shift.BEFORE)
    public static ReturnSolve<@ReturnSolve.Primitive Boolean> attemptDamageItem(ItemStack stack, int amount, Random rand, @Nullable EntityPlayerMP damager) {
        if (amount > 0)
            return ReturnSolve.yes(false);

        return ReturnSolve.no();
    }
}