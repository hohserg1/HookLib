package gloomyfolken.hooklib.common.model.method.hook.transformation;

import gloomyfolken.hooklib.common.ClassMetadataReader;
import gloomyfolken.hooklib.common.model.Transformation;
import gloomyfolken.hooklib.common.model.method.hook.AsmHook;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ListIterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.INVOKESPECIAL;

@ToString
@EqualsAndHashCode
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
public class InsertHookCall {
    AsmHook base;
}
