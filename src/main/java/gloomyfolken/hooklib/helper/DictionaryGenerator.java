package gloomyfolken.hooklib.helper;

import com.google.common.collect.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Generate methods.bin from methods.csv
 */
public class DictionaryGenerator {

    public static void main(String[] args) throws Exception {
        File sourceDirectory = new File(args[0]);
        prepareNames("methods.bin2", sourceDirectory, "methods.csv", "mcp_snapshot/20171003-1.12", "mcp_stable/39-1.12");
        prepareNames("fields.bin2", sourceDirectory, "fields.csv", "mcp_snapshot/20171003-1.12", "mcp_stable/39-1.12");
    }

    private static void prepareNames(String outputFileName, File sourceDirectory, String sourceFileName, String... sourceMappings) throws IOException {
        SetMultimap<String, String> mcpToSrg = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);
        for (String mappings : sourceMappings) {
            String mappingsPath = mappings + "/" + mappings.replace('/', '-') + ".zip";
            try (ZipFile zipFile = new ZipFile(new File(sourceDirectory, mappingsPath))) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.getName().equals(sourceFileName)) {
                        try (InputStream inputStream = zipFile.getInputStream(entry);
                             Scanner scanner = new Scanner(inputStream)) {
                            if (scanner.hasNextLine())
                                scanner.nextLine();
                            while (scanner.hasNextLine()) {
                                String line = scanner.nextLine();
                                String[] splitted = line.split(",");
                                mcpToSrg.put(splitted[1], splitted[0]);
                            }
                        }
                        break;
                    }
                }
            }
        }

        DataOutputStream out = new DataOutputStream(new FileOutputStream(outputFileName));
        out.writeInt(mcpToSrg.size());

        for (Map.Entry<String, String> entry : mcpToSrg.entries()) {
            out.writeUTF(entry.getKey());
            out.writeUTF(entry.getValue());
        }

        out.close();
    }
}
