package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.asm.HookClassTransformer;
import gloomyfolken.hooklib.asm.HookInjectorClassVisitor;
import gloomyfolken.hooklib.asm.injections.AsmInjection;
import org.objectweb.asm.ClassVisitor;

import java.util.List;

public interface TransformingStage {
    HookInjectorClassVisitor createInjectorClassVisitor(HookClassTransformer transformer, ClassVisitor finalizeVisitor, List<AsmInjection> hooks);
}
