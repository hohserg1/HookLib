package gloomyfolken.hooklib.helper;

import org.apache.commons.io.FileUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generate methods.bin from methods.csv
 */
public class DictionaryGenerator {

    public static void main(String[] args) throws Exception {
        prepareNames("methods.csv", "methods.bin");
        prepareNames("fields.csv", "fields.bin");

    }

    private static void prepareNames(String sourceFileName, String outputFileName) throws IOException {
        List<String> lines = FileUtils.readLines(new File(sourceFileName));
        lines.remove(0);
        HashMap<Integer, String> map = new HashMap<Integer, String>();
        for (String str : lines) {
            String[] splitted = str.split(",");
            int first = splitted[0].indexOf('_');
            int second = splitted[0].indexOf('_', first + 1);
            int id = Integer.parseInt(splitted[0].substring(first + 1, second));
            map.put(id, splitted[1]);
        }

        DataOutputStream out = new DataOutputStream(new FileOutputStream(outputFileName));
        out.writeInt(map.size());

        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            out.writeInt(entry.getKey());
            out.writeUTF(entry.getValue());
        }

        out.close();
    }
}
