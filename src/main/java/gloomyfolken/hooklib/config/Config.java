package gloomyfolken.hooklib.config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Config {

    public static boolean useClasspathCandidates;
    public static boolean useModsDirCandidates;
    public static boolean enableTestHooks;

    private static File file = new File("./config/hooklib.cfg");

    public static void loadConfig() {
        try {
            file.getParentFile().mkdirs();
            if (!file.exists())
                writeDefaultConfig();

            Map<String, Field> fields = Arrays.stream(Config.class.getFields()).collect(Collectors.toMap(Field::getName, Function.identity()));
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines)
                if (line.contains("=")) {
                    Field field = fields.get(line.substring(line.indexOf(':') + 1, line.indexOf('=')));
                    if (field != null)
                        field.set(null, parseLine(line));
                }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Object parseLine(String line) {
        String type = line.substring(0, line.indexOf(':'));
        switch (type) {
            case "B":
                return parseBoolean(line);
            default:
                return null;
        }
    }

    private static boolean parseBoolean(String line) {
        return Boolean.parseBoolean(line.substring(line.indexOf("=") + 1));
    }

    private static void writeDefaultConfig() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("# Find hook-containers in classpath\n");
            writer.write("B:useClasspathCandidates=true\n\n");
            writer.write("# Find hook-containers in mods\n");
            writer.write("B:useModsDirCandidates=true\n\n");
            writer.write("# Enable hook-container gloomyfolken.hooklib.example.TestHooks\n");
            writer.write("B:enableTestHooks=false\n\n");
        }
    }
}
