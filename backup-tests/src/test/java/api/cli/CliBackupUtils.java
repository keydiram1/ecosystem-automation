package api.cli;

import utils.AerospikeLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CliBackupUtils {

    public static void replaceRecordValueInBackupFile(String filePath, String stringToReplace, String newString) {
        Path path = Paths.get(filePath);
        try {
            List<String> lines = Files.readAllLines(path);

            int length = newString.length();

            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).contains(stringToReplace)) {
                    String replacement = String.format("- S restoreOptions %d %s", length, newString);
                    lines.set(i, replacement);  // Replace the line
                    break;
                }
            }

            Files.write(path, lines);
        } catch (Exception e) {
            AerospikeLogger.error("replaceValueWithNewString failed with the following error: " + e.getMessage());
        }
    }
}
