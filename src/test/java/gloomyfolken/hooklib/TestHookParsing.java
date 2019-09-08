package gloomyfolken.hooklib;

import com.google.common.collect.ImmutableList;
import gloomyfolken.hooklib.asm.*;
import gloomyfolken.hooklib.asm.model.method.hook.AsmHook;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.EnumFacing;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class TestHookParsing {
    HookClassTransformer transformer;
    public static final int hooksCount = 7;
    private List<AsmHook> testHooks;
    private List<AsmHook> handmadeHooks;

    @Before
    public void init() throws IOException {
        transformer = new HookClassTransformer();

        String className = "gloomyfolken.hooklib.example.TestHooks";
        testHooks = transformer.containerParser.parseHooks(className, transformer.classMetadataReader.getClassData(className)).collect(Collectors.toList());

        handmadeHooks = ImmutableList.of(
                AsmHook.builder()
                        .targetClassName("net.minecraft.item.Item$ToolMaterial")
                        .targetMethodName("<init>")

                        .hookClassInternalName("gloomyfolken/hooklib/example/TestHooks")
                        .hookMethodName("toolMaterial")

                        .startArgumentsFill()
                        .addTargetMethodParameter(Type.getType(String.class))
                        .addTargetMethodParameter(Type.INT_TYPE)
                        .addTargetMethodParameter(Type.INT_TYPE)
                        .addTargetMethodParameter(Type.INT_TYPE)
                        .addTargetMethodParameter(Type.FLOAT_TYPE)
                        .addTargetMethodParameter(Type.FLOAT_TYPE)
                        .addTargetMethodParameter(Type.INT_TYPE)

                        .addThisToHookMethodParameters()
                        .addHookMethodParameter(Type.getType(String.class), 1)
                        .addHookMethodParameter(Type.INT_TYPE, 2)
                        .addHookMethodParameter(Type.INT_TYPE, 3)
                        .addHookMethodParameter(Type.INT_TYPE, 4)
                        .addHookMethodParameter(Type.FLOAT_TYPE, 5)
                        .addHookMethodParameter(Type.FLOAT_TYPE, 6)
                        .addHookMethodParameter(Type.INT_TYPE, 7)

                        .hookMethodReturnType(Type.VOID_TYPE)

                        .finishArgumentsFill()
                        .build(),

                AsmHook.builder()
                        .targetClassName(className)
                        .targetMethodName("insertStack")

                        .hookClassInternalName("gloomyfolken/hooklib/example/TestHooks")
                        .hookMethodName("insertStack")

                        .startArgumentsFill()
                        .addTargetMethodParameter(Type.getType(TileEntityHopper.class))
                        .addTargetMethodParameter(Type.getType(IInventory.class))
                        .addTargetMethodParameter(Type.getType(IInventory.class))
                        .addTargetMethodParameter(Type.getType(ItemStack.class))
                        .addTargetMethodParameter(Type.INT_TYPE)
                        .addTargetMethodParameter(Type.getType(EnumFacing.class))

                        .addThisToHookMethodParameters()
                        .addHookMethodParameter(Type.getType(TileEntityHopper.class), 1)
                        .addHookMethodParameter(Type.getType(IInventory.class), 2)
                        .addHookMethodParameter(Type.getType(IInventory.class), 3)
                        .addHookMethodParameter(Type.getType(ItemStack.class), 4)
                        .addHookMethodParameter(Type.INT_TYPE, 5)
                        .addHookMethodParameter(Type.getType(EnumFacing.class), 6)

                        .hookMethodReturnType(Type.VOID_TYPE)

                        .finishArgumentsFill()
                        .build(),

                AsmHook.builder()
                        .targetClassName("net.minecraft.tileentity.TileEntityHopper")
                        .targetMethodName("insertStack")

                        .hookClassInternalName("gloomyfolken/hooklib/example/TestHooks")
                        .hookMethodName("insertStack")
                        .hookMethodReturnType(Type.VOID_TYPE)

                        .startArgumentsFill()
                        .addTargetMethodParameter(Type.getType(IInventory.class))
                        .addTargetMethodParameter(Type.getType(IInventory.class))
                        .addTargetMethodParameter(Type.getType(ItemStack.class))
                        .addTargetMethodParameter(Type.INT_TYPE)
                        .addTargetMethodParameter(Type.getType(EnumFacing.class))

                        .addThisToHookMethodParameters()
                        .addHookMethodParameter(Type.getType(IInventory.class), 1)
                        .addHookMethodParameter(Type.getType(IInventory.class), 2)
                        .addHookMethodParameter(Type.getType(ItemStack.class), 3)
                        .addHookMethodParameter(Type.INT_TYPE, 4)
                        .addHookMethodParameter(Type.getType(EnumFacing.class), 5)


                        .finishArgumentsFill()

                        .point(InjectionPoint.METHOD_CALL)
                        .shift(Shift.INSTEAD)
                        .anchorTarget("setInventorySlotContents")
                        .build(),

                AsmHook.builder()
                        .targetClassName("net.minecraft.client.Minecraft")
                        .targetMethodName("resize")

                        .hookClassInternalName("gloomyfolken/hooklib/example/TestHooks")
                        .hookMethodName("resize")
                        .hookMethodReturnType(Type.VOID_TYPE)

                        .startArgumentsFill()
                        .addTargetMethodParameter(Type.INT_TYPE)
                        .addTargetMethodParameter(Type.INT_TYPE)

                        .addThisToHookMethodParameters()
                        .addHookMethodParameter(Type.INT_TYPE, 1)
                        .addHookMethodParameter(Type.INT_TYPE, 2)


                        .finishArgumentsFill()
                        .build(),

                AsmHook.builder()
                        .targetClassName("net.minecraftforge.common.ForgeHooks")
                        .targetMethodName("getTotalArmorValue")
                        .targetMethodReturnType(Type.INT_TYPE)

                        .hookClassInternalName("gloomyfolken/hooklib/example/TestHooks")
                        .hookMethodName("getTotalArmorValue")
                        .hookMethodReturnType(Type.INT_TYPE)

                        .startArgumentsFill()
                        .addTargetMethodParameter(Type.getType(EntityPlayer.class))

                        .addThisToHookMethodParameters()
                        .addHookMethodParameter(Type.getType(EntityPlayer.class), 1)
                        .addReturnValueToHookMethodParameters()

                        .finishArgumentsFill()

                        .returnCondition(ReturnCondition.ALWAYS)
                        .point(InjectionPoint.RETURN)
                        .build(),

                AsmHook.builder()
                        .targetClassName("net.minecraft.entity.player.EntityPlayer")
                        .targetMethodName("getPortalCooldown")

                        .hookClassInternalName("gloomyfolken/hooklib/example/TestHooks")
                        .hookMethodName("getPortalCooldown")
                        .hookMethodReturnType(Type.getType(ResultSolve.class))

                        .startArgumentsFill()

                        .addThisToHookMethodParameters()

                        .finishArgumentsFill()

                        .returnCondition(ReturnCondition.ON_SOLVE)
                        .build(),

                AsmHook.builder()
                        .targetClassName("net.minecraft.entity.Entity")
                        .targetMethodName("getBrightnessForRender")

                        .hookClassInternalName("gloomyfolken/hooklib/example/TestHooks")
                        .hookMethodName("getBrightnessForRender")
                        .hookMethodReturnType(Type.getType(ResultSolve.class))

                        .startArgumentsFill()

                        .addThisToHookMethodParameters()

                        .finishArgumentsFill()

                        .returnCondition(ReturnCondition.ON_SOLVE)
                        .build()

        );

    }

    @After
    public void tearDown() {
    }

    @Test
    public void testAsmHookParsing() {
        for (int i = 0; i < testHooks.size(); i++) {
            AsmHook parsedHook = testHooks.get(i);
            AsmHook handmadeHook = handmadeHooks.get(i);
            assertEquals("hook number id " + i, handmadeHook, parsedHook);
        }

    }

}
