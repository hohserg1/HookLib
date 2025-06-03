package gloomyfolken.hooklib.example;

import gloomyfolken.hooklib.api.*;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;

//@HookContainer
public class DisableDistantEntities {

    private static double disabledEntityRenderDistance = 20;

    @Hook(targetMethod = "renderEntities", priority = HookPriority.NORMAL)
    @OnMethodCall(value = "shouldRenderInPass", shift = Shift.INSTEAD, ordinal = 1)
    public static boolean skipEntity(RenderGlobal renderGlobal,
                                     Entity renderViewEntity, ICamera camera, float partialTicks,
                                     @LocalVariable(id = 4) int pass, @LocalVariable(id = 27) Entity entity2) {
        if (entity2.shouldRenderInPass(pass)) {
            return entity2.getDistanceSq(renderViewEntity) < disabledEntityRenderDistance * disabledEntityRenderDistance;
        } else {
            return false;
        }
    }

    @Hook(targetMethod = "renderEntities", priority = HookPriority.HIGH)
    @OnMethodCall(value = "shouldRenderInPass", shift = Shift.INSTEAD, ordinal = 1)
    public static boolean skipTile1(RenderGlobal renderGlobal,
                                    Entity renderViewEntity, ICamera camera, float partialTicks,
                                    @LocalVariable(id = 4) int pass, @LocalVariable(id = 26) net.minecraft.tileentity.TileEntity tileentity2) {
        if (tileentity2.shouldRenderInPass(pass)) {
            return renderViewEntity.getDistanceSq(tileentity2.getPos()) < disabledEntityRenderDistance * disabledEntityRenderDistance;
        } else {
            return false;
        }
    }

    @Hook(targetMethod = "renderEntities", priority = HookPriority.HIGHEST)
    @OnMethodCall(value = "shouldRenderInPass", shift = Shift.INSTEAD, ordinal = 1)
    public static boolean skipTile2(RenderGlobal renderGlobal,
                                    Entity renderViewEntity, ICamera camera, float partialTicks,
                                    @LocalVariable(id = 4) int pass, @LocalVariable(id = 24) net.minecraft.tileentity.TileEntity tileentity) {
        if (tileentity.shouldRenderInPass(pass)) {
            return renderViewEntity.getDistanceSq(tileentity.getPos()) < disabledEntityRenderDistance * disabledEntityRenderDistance;
        } else {
            return false;
        }
    }

}
