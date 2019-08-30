package gloomyfolken.hooklib.example;

import gloomyfolken.hooklib.asm.*;
import gloomyfolken.hooklib.asm.Hook.ReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.ForgeHooks;

@HookContainer
public class TestHooks {

    @HookLens
    public static Lens<TileEntityHopper, Integer> testField = null;

    @Hook(at = @At(point = InjectionPoint.HEAD), targetMethod = "<init>")
    public static void toolMaterial(Item.ToolMaterial toolMaterial, String name, int ordinal,
                                    int harvestLevel, int maxUses, float efficiency, float damageVsEntity, int enchantability) {
        System.out.println("toolMaterial " + name + " " + harvestLevel + " " + maxUses + " " + efficiency + " " + damageVsEntity + " " + enchantability);
    }

    //hook to hook
    @Hook(at = @At(point = InjectionPoint.HEAD))
    public static void insertStack(TestHooks testHooks, TileEntityHopper tile, IInventory source, IInventory destination, ItemStack stack, int index, EnumFacing direction) {
        System.out.println("test1");
    }


    @Hook(at = @At(point = InjectionPoint.METHOD_CALL, target = "setInventorySlotContents", shift = Shift.INSTEAD))
    public static void insertStack(TileEntityHopper tile, IInventory source, IInventory destination, ItemStack stack, int index, EnumFacing direction) {
        System.out.println("test");
    }

    /*
    @Hook(at = @At(point = InjectionPoint.METHOD_CALL, anchorTarget = "getSlotsForFace"), returnCondition = ReturnCondition.ON_SOLVE, booleanReturnConstant = true)
    public static boolean isInventoryFull(TileEntityHopper tile, IInventory inventoryIn, EnumFacing side) {
        return ((ISidedInventory) inventoryIn).getSlotsForFace(side) == null;
    }*/

    /**
     * Цель: при каждом ресайзе окна выводить в консоль новый размер, а также похерит ресайз:D
     * Чтобы починить нужно юзать InjectionPoint.RETURN или ReturnCondition.NEVER
     */
    @Hook(at = @At(point = InjectionPoint.HEAD))
    public static void resize(Minecraft mc, int x, int y) {
        System.out.println("Resize, x=" + x + ", y=" + y);
    }

    /**
     * Цель: уменьшить вдвое показатели брони у всех игроков.
     * P.S: фордж перехватывает получение показателя брони, ну а мы перехватим перехватчик :D
     */
    @Hook(at = @At(point = InjectionPoint.RETURN), returnCondition = ReturnCondition.ALWAYS)
    public static int getTotalArmorValue(ForgeHooks fh, EntityPlayer player, @ReturnValue int returnValue) {
        return returnValue / 2;
    }

    /**
     * Цель: запретить возможность телепортироваться в ад и обратно чаще, чем раз в пять секунд.
     */
    @Hook(returnCondition = ReturnCondition.ON_SOLVE, intReturnConstant = 100)
    public static ResultSolve<Integer> getPortalCooldown(EntityPlayer player) {
        return new ResultSolve<>(player.dimension == 0, 100);
    }

    /**
     * Цель: уменьшить вдвое яркость сущностей, которые выше полутора блоков.
     * Проверка на высоту в одном методе, пересчёт яркости - в другом.
     */
    @Hook(at = @At(point = InjectionPoint.HEAD), returnCondition = ReturnCondition.ON_SOLVE)
    public static ResultSolve<Integer> getBrightnessForRender(Entity entity) {
        return new ResultSolve<>(entity.height > 1.5f, getBrightness(entity));
    }

    public static int getBrightness(Entity entity) {
        int oldValue = 0;
        int j = ((oldValue >> 20) & 15) / 2;
        int k = ((oldValue >> 4) & 15) / 2;
        return j << 20 | k << 4;
    }
}