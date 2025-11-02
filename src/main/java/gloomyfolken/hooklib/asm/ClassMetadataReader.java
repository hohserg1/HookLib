package gloomyfolken.hooklib.asm;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import lombok.Value;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Allow to fing methods inside still not loaded classes and mutual super-classes.
 * Will find class in classpath, or by reflection.
 *
 * @see gloomyfolken.hooklib.minecraft.DeobfuscationMetadataReader
 */
public class ClassMetadataReader {
    private static Method findLoadedClass;

    static {
        try {
            findLoadedClass = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            findLoadedClass.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public byte[] getClassData(String className) throws IOException {
        String classResourceName = '/' + className.replace('.', '/') + ".class";
        return IOUtils.toByteArray(ClassMetadataReader.class.getResourceAsStream(classResourceName));
    }

    public void acceptVisitor(byte[] classData, ClassVisitor visitor) {
        new ClassReader(classData).accept(visitor, 0);
    }

    public void acceptVisitor(String className, ClassVisitor visitor) throws IOException {
        acceptVisitor(getClassData(className), visitor);
    }

    public MethodReference findVirtualMethod(String owner, String name, String desc) {
        ArrayList<String> superClasses = getSuperClasses(owner);
        for (int i = superClasses.size() - 1; i > 0; i--) {
            String className = superClasses.get(i);
            MethodReference methodReference = getMethodReference(className, name, desc);
            if (methodReference != null) {
                return methodReference;
            }
        }
        return null;
    }

    private MethodReference getMethodReference(String type, String methodName, String desc) {
        try {
            return getMethodReferenceASM(type, methodName, desc, false);
        } catch (Exception e) {
            return getMethodReferenceReflect(type, methodName, desc);
        }
    }

    protected MethodReference getMethodReferenceASM(String type, String methodName, String desc, boolean privateToo) throws IOException {
        FindMethodClassVisitor cv = new FindMethodClassVisitor(methodName, desc, privateToo);
        acceptVisitor(type, cv);
        if (cv.found) {
            return new MethodReference(type, cv.targetAccess, cv.targetName, cv.targetDesc);
        }
        return null;
    }

    protected MethodReference getMethodReferenceReflect(String type, String methodName, String desc) {
        Class loadedClass = getLoadedClass(type);
        if (loadedClass != null) {
            for (Method m : loadedClass.getDeclaredMethods()) {
                if (checkSameMethod(methodName, desc, m.getName(), Type.getMethodDescriptor(m))) {
                    return new MethodReference(type, m.getModifiers(), m.getName(), Type.getMethodDescriptor(m));
                }
            }
        }
        return null;
    }

    protected boolean checkSameMethod(String sourceName, String sourceDesc, String targetName, String targetDesc) {
        return sourceName.equals(targetName) && sourceDesc.equals(targetDesc);
    }

    /**
     * @return super-classes in order from java/lang/Object to `type` argument
     */
    public ArrayList<String> getSuperClasses(String type) {
        ArrayList<String> superclasses = new ArrayList<String>(1);
        superclasses.add(type);
        while ((type = getSuperClass(type)) != null) {
            superclasses.add(type);
        }
        Collections.reverse(superclasses);
        return superclasses;
    }

    /**
     * @return super-classes and interfaces of `type` argument
     */
    public Set<String> getSuperTypes(String type) {
        Set<String> r = new HashSet<>();
        r.add(type);
        for (String name : getSuperTypesStep(type)) {
            r.addAll(getSuperTypes(name));
        }
        return r;
    }

    @Value
    private static class ClassHierarchyKey {
        public String checkedType, requiredParent;
    }

    private LoadingCache<ClassHierarchyKey, Boolean> checkSuperTypeCache = CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.MINUTES)
        .build(new CacheLoader<ClassHierarchyKey, Boolean>() {
            @Override
            public Boolean load(ClassHierarchyKey key) throws Exception {
                return checkSuperTypeInternal(key.checkedType, key.requiredParent);
            }
        });

    public boolean checkSuperType(String type, String requiredParent) {
        if (type.equals(requiredParent))
            return true;
        return checkSuperTypeCache.getUnchecked(new ClassHierarchyKey(type, requiredParent));
    }

    private boolean checkSuperTypeInternal(String currentType, String requiredParent) {
        if (currentType.equals(requiredParent))
            return true;
        for (String name : getSuperTypesStep(currentType)) {
            if (name.equals(requiredParent))
                return true;
            if (checkSuperTypeCache.getUnchecked(new ClassHierarchyKey(name, requiredParent)))
                return true;
        }
        return false;
    }

    private Class getLoadedClass(String type) {
        if (findLoadedClass != null) {
            try {
                ClassLoader classLoader = ClassMetadataReader.class.getClassLoader();
                return (Class) findLoadedClass.invoke(classLoader, type.replace('/', '.'));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public Set<String> getSuperTypesStep(String type) {
        try {
            return getSuperTypesASM(type);
        } catch (Exception e) {
            return getSuperTypesReflect(type);
        }
    }

    public String getSuperClass(String type) {
        try {
            return getSuperClassASM(type);
        } catch (Exception e) {
            return getSuperClassReflect(type);
        }
    }

    protected Set<String> getSuperTypesASM(String type) throws IOException {
        CheckSuperTypesVisitor cv = new CheckSuperTypesVisitor();
        acceptVisitor(type, cv);
        return cv.superTypes;
    }

    protected String getSuperClassASM(String type) throws IOException {
        CheckSuperClassVisitor cv = new CheckSuperClassVisitor();
        acceptVisitor(type, cv);
        return cv.superClassName;
    }

    protected Set<String> getSuperTypesReflect(String type) {
        Class loadedClass = getLoadedClass(type);
        if (loadedClass != null) {
            Set<String> r = new HashSet<>();
            if (loadedClass.getSuperclass() != null)
                r.add(loadedClass.getSuperclass().getName().replace('.', '/'));
            for (Class i : loadedClass.getInterfaces()) {
                r.add(i.getName().replace('.', '/'));
            }
            return r;
        }
        return Sets.newHashSet("java/lang/Object");
    }

    protected String getSuperClassReflect(String type) {
        Class loadedClass = getLoadedClass(type);
        if (loadedClass != null) {
            if (loadedClass.getSuperclass() == null) return null;
            return loadedClass.getSuperclass().getName().replace('.', '/');
        }
        return "java/lang/Object";
    }

    private class CheckSuperClassVisitor extends ClassVisitor {

        String superClassName;

        public CheckSuperClassVisitor() {
            super(Opcodes.ASM5);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            this.superClassName = superName;
        }
    }

    private class CheckSuperTypesVisitor extends ClassVisitor {

        Set<String> superTypes = new HashSet<>();

        public CheckSuperTypesVisitor() {
            super(Opcodes.ASM5);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            if (superName != null)
                superTypes.add(superName);
            Collections.addAll(superTypes, interfaces);
        }
    }

    protected class FindMethodClassVisitor extends ClassVisitor {

        public final boolean privateToo;

        public int targetAccess;
        public String targetName;
        public String targetDesc;
        public boolean found;

        public FindMethodClassVisitor(String name, String desc, boolean privateToo) {
            super(Opcodes.ASM5);
            this.targetName = name;
            this.targetDesc = desc;
            this.privateToo = privateToo;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if ((privateToo || (access & Opcodes.ACC_PRIVATE) == 0) && checkSameMethod(name, desc, targetName, targetDesc)) {
                found = true;
                targetAccess = access;
                targetName = name;
                targetDesc = desc;
            }
            return null;
        }
    }

    public static class MethodReference {

        public final String owner;
        public final int access;
        public final String name;
        public final String desc;

        public MethodReference(String owner, int access, String name, String desc) {
            this.owner = owner;
            this.access = access;
            this.name = name;
            this.desc = desc;
        }

        public Type getType() {
            return Type.getMethodType(desc);
        }

        @Override
        public String toString() {
            return "MethodReference{" +
                "owner='" + owner + '\'' +
                ", access='" + access + '\'' +
                ", name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                '}';
        }
    }

}
