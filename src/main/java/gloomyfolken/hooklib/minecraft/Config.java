package gloomyfolken.hooklib.minecraft;

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

public enum Config {
    instance;

    public boolean useClasspathCandidates = false;

    private final File file = new File("./config/hooklib.cfg");

    Config() {
        loadConfig();
    }

    public void loadConfig() {
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
                        field.set(this, parseLine(line));
                }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Object parseLine(String line) {
        String type = line.substring(0, line.indexOf(':'));
        switch (type) {
            case "B":
                return parseBoolean(line);
            default:
                return null;
        }
    }

    private boolean parseBoolean(String line) {
        return Boolean.parseBoolean(line.substring(line.indexOf("=") + 1));
    }

    private void writeDefaultConfig() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("# Find hook-containers in classpath\n");
            writer.write("B:useClasspathCandidates=" + useClasspathCandidates + "\n\n");
        }
    }
}
