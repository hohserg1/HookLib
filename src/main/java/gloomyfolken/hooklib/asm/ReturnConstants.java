package gloomyfolken.hooklib.asm;

import gloomyfolken.hooklib.api.Hook;
import lombok.Value;

@Value
public class ReturnConstants {
    public boolean booleanReturnConstant;
    public byte byteReturnConstant;
    public short shortReturnConstant;
    public int intReturnConstant;
    public long longReturnConstant;
    public float floatReturnConstant;
    public double doubleReturnConstant;
    public char charReturnConstant;
    public String stringReturnConstant;

    public static ReturnConstants of(Hook hook) {
        return new ReturnConstants(hook.booleanReturnConstant(),
                hook.byteReturnConstant(),
                hook.shortReturnConstant(),
                hook.intReturnConstant(),
                hook.longReturnConstant(),
                hook.floatReturnConstant(),
                hook.doubleReturnConstant(),
                hook.charReturnConstant(),
                hook.stringReturnConstant());
    }
}
