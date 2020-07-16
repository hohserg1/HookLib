package gloomyfolken.hooklib.common.advanced.tree;

import gloomyfolken.hooklib.api.HookContainer;
import gloomyfolken.hooklib.api.HookLens;
import gloomyfolken.hooklib.common.AsmHelper;
import gloomyfolken.hooklib.common.ClassMetadataReader;
import org.apache.http.util.Asserts;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class AdvancedClassNodeTest {

    byte[] classData;
    String className = "gloomyfolken.hooklib.example.TestHooks";

    @Before
    public void init() throws IOException {
        classData = loadHookContainerBytes(className);
    }

    private byte[] loadHookContainerBytes(String className) throws IOException {
        return ClassMetadataReader.instance.getClassData(className);
    }

    @Test
    public void testAnnotations() {
        AdvancedClassNode advancedClassNode = AsmHelper.classNodeOf(classData, 0);

        Asserts.check(advancedClassNode.annotations.contains(HookContainer.class), "miss annotation on class");
        Asserts.check(advancedClassNode.annotations.get(HookContainer.class).<HookContainer>value().modid().equals("test-hooklib"), "not expected argument of annotation");

        advancedClassNode.fields.stream().filter(fn -> fn.name.equals("test")).findAny()
                .ifPresent(fn -> Asserts.check(fn.annotations.contains(HookLens.class), "miss annotation on field"));

        advancedClassNode.fields.stream().filter(fn -> fn.name.equals("kek")).findAny()
                .ifPresent(fn -> Asserts.check(fn.annotations.size() == 0, "unnecessary annotation on field"));


    }

}