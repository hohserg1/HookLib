package gloomyfolken.hooklib.helper;

import gloomyfolken.hooklib.helper.annotation.AnnotationMap;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.SideOnly;

public class SideOnlyUtils {
    public static boolean isValidSide(AnnotationMap annotationMap) {
        return annotationMap.maybeGet(SideOnly.class).map(SideOnly::value).map(v -> v == FMLLaunchHandler.side()).orElse(true);
    }
}
