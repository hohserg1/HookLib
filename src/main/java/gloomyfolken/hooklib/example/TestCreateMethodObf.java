package gloomyfolken.hooklib.example;

import gloomyfolken.hooklib.api.*;
import net.minecraftforge.items.*;

@HookContainer
public class TestCreateMethodObf {

    @Hook(createMethod = true)
    @OnBegin
    public static void onSlotChanged(SlotItemHandler slotItemHandler) {
        System.out.println("bruh");
    }
}
