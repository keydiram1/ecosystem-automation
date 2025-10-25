package utils;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;

import java.io.File;

@UtilityClass
public class FileBackupRestore {

    @SneakyThrows
    public static void createFileBackup(String filename) {
        File originalFile = new File(filename);
        File backupFile = new File(getBackupFilename(filename));

        FileUtils.copyFile(originalFile, backupFile);
    }

    @SneakyThrows
    public static void restoreFileFromBackup(String filename) {
        File backupFile = new File(getBackupFilename(filename));
        File originalFile = new File(filename);

        if (backupFile.exists()) {
            FileUtils.copyFile(backupFile, originalFile);
            FileUtils.deleteQuietly(backupFile);
        }
    }

    @SneakyThrows
    public static void replaceSubstringInFile(String filename, String substring, String replacement) {
        String fileContent = FileUtils.readFileToString(new File(filename), "UTF-8");
        String modifiedContent = fileContent.replace(substring, replacement);
        FileUtils.writeStringToFile(new File(filename), modifiedContent, "UTF-8");
    }

    private static String getBackupFilename(String filename) {
        File file = new File(filename);
        String parentDirectory = file.getParent();
        String name = file.getName();
        return parentDirectory + File.separator + "~" + name;
    }
}
