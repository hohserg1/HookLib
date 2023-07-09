package gloomyfolken.hooklib.example;

import gloomyfolken.hooklib.api.*;
import gloomyfolken.hooklib.asm.ReturnCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.ForgeHooks;

@HookContainer
public class TestHooks {
    @Hook
    @OnBegin
    public static void resize(Minecraft mc, int x, int y) {
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