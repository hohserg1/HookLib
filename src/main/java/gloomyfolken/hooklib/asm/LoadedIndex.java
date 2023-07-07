package gloomyfolken.hooklib.asm;

import java.util.ArrayList;
import java.util.List;

public enum LoadedIndex {
    instance;
    public boolean init = false;
    public List<String> index = new ArrayList<>(1000);
}
