package gloomyfolken.hooklib.example;

import gloomyfolken.hooklib.api.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

@HookContainer
public class TestHooks {

    public static void renderRainSnow(float partialTicks) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        blockpos$mutableblockpos.setPos(0, 0, 0);
        Biome biome = Minecraft.getMinecraft().world.getBiome(blockpos$mutableblockpos);

        if (biome.canRain() || biome.getEnableSnow()) {
            System.out.println("bruh");
            System.out.println("test");
            System.out.println("lol");
        }
    }


    @Hook(targetMethod = "renderRainSnow")
    @OnExpression(expressionPattern = "originalRainCondition", shift = Shift.INSTEAD)
    public static boolean preventRainInDesert(TestHooks entityRenderer, float partialTicks,
                                              @LocalVariable(1) BlockPos.MutableBlockPos pos, @LocalVariable(2) Biome biome) {
        //@LocalVariable(21) BlockPos.MutableBlockPos pos, @LocalVariable(29) Biome biome) {
        return false;
    }

    private static void originalRainCondition(Biome biome) {
        if (biome.canRain() || biome.getEnableSnow()) ;
    }

    @Hook
    @OnBegin
    public static void resize(Minecraft mc, int x, int y) {
        System.out.println("Resize, x=" + x + ", y=" + y);
    }
/*
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
    @OnMethodCall(value = "resize", shift = Shift.AFTER)
    public static void checkWindowResize(Minecraft mc) {
        System.out.println("init resize, x=" + mc.displayWidth + ", y=" + mc.displayHeight);
    }

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
    */
}