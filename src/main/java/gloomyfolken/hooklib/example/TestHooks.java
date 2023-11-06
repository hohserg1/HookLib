package gloomyfolken.hooklib.example;

import gloomyfolken.hooklib.api.*;
import gloomyfolken.hooklib.asm.ReturnCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nullable;
import java.util.Random;

@HookContainer
public class TestHooks {

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

    /**
     * Цель: уменьшить вдвое показатели брони у всех игроков.
     * P.S: фордж перехватывает получение показателя брони, ну а мы перехватим перехватчик :D
     */
    @Hook(returnCondition = ReturnCondition.ALWAYS, returnConstant = @ReturnConstant(intValue = 1))
    @OnReturn
    public static void getTotalArmorValue(ForgeHooks fh, EntityPlayer player) {
        //return returnValue / 2;
    }
}