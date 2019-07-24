package gloomyfolken.hooklib.config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

public class Config {

    private static boolean loaded = false;

    private static boolean useClasspathCandidates;
    private static boolean useModsDirCandidates;

    public static boolean useClasspathCandidates() {
        if (!loaded)
            loadConfig();
        return useClasspathCandidates;

    }

    public static boolean useModsDirCandidates() {
        if (!loaded)
            loadConfig();
        return useModsDirCandidates;

    }

    private static File file = new File("./config/hooklib.cfg");

    private static void loadConfig() {
        try {
            file.getParentFile().mkdirs();
            if (!file.exists())
                writeDefaultConfig();

            Stream<String> stream = Files.lines(file.toPath());
            stream.forEach(line -> {
                if (line.contains("useClasspathCandidates"))
                    useClasspathCandidates = parseBoolean(line);
                else if (line.contains("useModsDirCandidates"))
                    useModsDirCandidates = parseBoolean(line);
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean parseBoolean(String line) {
        return Boolean.parseBoolean(line.substring(line.indexOf("=") + 1));
    }

    private static void writeDefaultConfig() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write("# Find hook-containers in classpath\n");
        writer.write("B:useClasspathCandidates=true\n\n");
        writer.write("# Find hook-containers in mods\n");
        writer.write("B:useModsDirCandidates=true\n");

        writer.close();
    }
}
