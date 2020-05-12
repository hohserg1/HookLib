package gloomyfolken.hooklib.asm.model.lens.hook;

import lombok.Builder;
import lombok.Value;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;

@Builder
@Value
public class AsmLens implements Comparable<AsmLens> {
    String targetClassInternalName;
    String targetFieldName;
    Type targetFieldType;


    String hookClassInternalName;
    String lensFieldName;

    @Builder.Default
    boolean boxing = false;
    @Builder.Default
    boolean createField = false;

    @Override
    public int compareTo(AsmLens o) {
        return 0;
    }

    public boolean isTargetField(FieldNode fieldNode) {
        return fieldNode.name.equals(targetFieldName) && Type.getType(fieldNode.desc).equals(targetFieldType);
    }
}
