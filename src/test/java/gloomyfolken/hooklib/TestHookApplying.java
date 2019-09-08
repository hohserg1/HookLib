package gloomyfolken.hooklib;

import gloomyfolken.hooklib.asm.HookApplier;
import gloomyfolken.hooklib.asm.model.method.hook.AsmHook;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.objectweb.asm.Opcodes.ASM5;

public class TestHookApplying {

    public HookApplier hookApplier;
    private ClassNode TestClassNode;
    private byte[] TestClass;
    private byte[] TestClassExpected;

    @Before
    public void init() throws IOException {
        //load hook container
        TestClass = loadClassToBytes("./clay/test/hooklib/TestClass.class");
        TestClassExpected = loadClassToBytes("./expected/test/hooklib/TestClass.class");
        byte[] TestHook = loadClassToBytes("./clay/test/hooklib/TestHook.class");


        ClassReader cr = new ClassReader(TestClass);

        TestClassNode = new ClassNode(ASM5);
        cr.accept(TestClassNode, 0);

        hookApplier = new HookApplier();

        ClassWriter classWriter = new ClassWriter(ASM5);
        TestClassNode.accept(classWriter);

        TestClass = classWriter.toByteArray();

    }

    private byte[] loadClassToBytes(String s) throws IOException {
        return IOUtils.toByteArray(new FileInputStream(new File(s)));
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testApplying() {
        AsmHook hookHead = AsmHook.builder()
                .targetClassName("test.hooklib.TestClass")
                .targetMethodName("firstMethod")
                .startArgumentsFill()
                .addTargetMethodParameter(Type.INT_TYPE)
                .hookClassInternalName("test.hooklib.TestHook")
                .hookMethodName("hookHead")
                .hookMethodReturnType(Type.VOID_TYPE)
                .addThisToHookMethodParameters()
                .addHookMethodParameter(Type.INT_TYPE, 1)
                .finishArgumentsFill()
                .build();

        MethodNode firstMethodNode = TestClassNode.methods.stream().filter(m -> m.name.equals(hookHead.getTargetMethodName())).findFirst().get();

        hookApplier.applyHook(hookHead, firstMethodNode);

        assertArrayEquals(TestClass, TestClassExpected);

    }
}
