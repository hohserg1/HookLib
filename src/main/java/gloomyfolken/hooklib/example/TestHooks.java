package gloomyfolken.hooklib.example;

import gloomyfolken.hooklib.api.Hook;
import gloomyfolken.hooklib.api.HookContainer;
import gloomyfolken.hooklib.api.ReturnValue;
import gloomyfolken.hooklib.asm.ReturnCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.ForgeHooks;

@HookContainer
public class TestHooks {
    @Hook()
    public static void resize(Minecraft mc, int x, int y) {
        System.out.println("Resize, x=" + x + ", y=" + y);
    }

    /**
     * Цель: уменьшить вдвое показатели брони у всех игроков.
     * P.S: фордж перехватывает получение показателя брони, ну а мы перехватим перехватчик :D
     */
    @Hook(injectOnExit = true, returnCondition = ReturnCondition.ALWAYS)
    public static int getTotalArmorValue(ForgeHooks fh, EntityPlayer player, @ReturnValue int returnValue) {
        return returnValue / 2;
    }
}