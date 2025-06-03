package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.asm.HookClassTransformer;
import gloomyfolken.hooklib.asm.HookInjectorClassVisitor;
import gloomyfolken.hooklib.asm.injections.AsmInjection;
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
            protected String deobfMethod(String name) {
                return Deobfuscation.instance.deobfMethod(name);
            }

            @Override
            protected String deobfField(String name) {
                return Deobfuscation.instance.deobfField(name);
            }
        };
    }

}
