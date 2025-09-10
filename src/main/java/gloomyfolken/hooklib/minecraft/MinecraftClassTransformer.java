package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.asm.*;
import gloomyfolken.hooklib.asm.injections.*;
import org.objectweb.asm.*;

import java.util.*;

/**
 * This transformer uses after Minecraft classes have started loading
 * will be applied after all other transformers
 * kinda have no sense to separate it
 */
public class MinecraftClassTransformer implements TransformingStage {

    @Override
    public HookInjectorClassVisitor createInjectorClassVisitor(HookClassTransformer transformer, ClassVisitor finalizeVisitor, List<AsmInjection> hooks) {
        return new HookInjectorClassVisitor(transformer, finalizeVisitor, hooks);
    }

}
