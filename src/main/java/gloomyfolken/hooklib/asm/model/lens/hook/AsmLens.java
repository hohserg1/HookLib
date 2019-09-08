package gloomyfolken.hooklib.asm.model.lens.hook;

import lombok.Builder;
import lombok.Value;
import org.objectweb.asm.Type;

@Builder
@Value
public class AsmLens implements Comparable<AsmLens>{
    String targetClassName;
    String targetFieldName;
    Type targetFieldType;


    String hookClassInternalName;
    String lensFieldName;

    @Override
    public int compareTo(AsmLens o) {
        return 0;
    }
}
