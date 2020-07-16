package gloomyfolken.hooklib.common.model.field.lens;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@ToString
@EqualsAndHashCode
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
public class AsmLens implements Comparable {

    public String hookClassName;
    public String hookFieldName;
    public String targetClassName;
    public String targetFieldName;
    public String targetFieldTypeName;

    public String hookLensAnonymousClassName;

    boolean boxing;
    boolean createField;

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
