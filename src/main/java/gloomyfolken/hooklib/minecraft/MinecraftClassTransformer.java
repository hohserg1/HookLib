package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.asm.HookClassTransformer;
import gloomyfolken.hooklib.asm.HookInjectorClassVisitor;
import gloomyfolken.hooklib.asm.injections.AsmFieldLens;
import gloomyfolken.hooklib.asm.injections.AsmInjection;
import gloomyfolken.hooklib.asm.injections.AsmMethodInjection;
import org.objectweb.asm.ClassVisitor;

import java.util.List;

/**
 * This transformer uses after Minecraft classes have started loading
 * will be applied after all other transformers
 * kinda have no sense to separate it
 */
public class MinecraftClassTransformer implements TransformingStage {

    @Override
    public HookInjectorClassVisitor createInjectorClassVisitor(HookClassTransformer transformer, ClassVisitor finalizeVisitor, List<AsmInjection> hooks) {
        return new HookInjectorClassVisitor(transformer, finalizeVisitor, hooks) {
            @Override
            protected boolean isTargetMethod(AsmMethodInjection hook, String name, String desc) {
                return super.isTargetMethod(hook, Deobfuscation.instance.deobfMethod(name), desc);
            }

            @Override
            protected boolean isTargetField(AsmFieldLens lens, String name, String desc) {
                return super.isTargetField(lens, Deobfuscation.instance.deobfField(name), desc);
            }
        };
    }

}
