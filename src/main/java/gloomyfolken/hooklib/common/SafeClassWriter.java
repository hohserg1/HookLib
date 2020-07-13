package gloomyfolken.hooklib.common;

import org.objectweb.asm.ClassWriter;

import java.util.ArrayList;

/**
 * ClassWriter с другой реализацией метода getCommonSuperClass: при его использовании не происходит загрузки классов.
 * Однако, сама по себе загрузка классов редко является проблемой, потому что инициализация класса (вызов статических
 * блоков) происходит не при загрузке класса. Проблемы появляются, когда хуки вставляются в зависимые друг от друга
 * классы, тогда стандартная реализация отваливается с ClassCircularityError.
 */
public class SafeClassWriter extends ClassWriter {

    public SafeClassWriter(int flags) {
        super(flags);
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        ArrayList<String> superClasses1 = ClassMetadataReader.instance.getSuperClasses(type1);
        ArrayList<String> superClasses2 = ClassMetadataReader.instance.getSuperClasses(type2);
        int size = Math.min(superClasses1.size(), superClasses2.size());
        int i;
        for (i = 0; i < size && superClasses1.get(i).equals(superClasses2.get(i)); i++) ;
        if (i == 0) {
            return "java/lang/Object";
        } else {
            return superClasses1.get(i - 1);
        }
    }


}