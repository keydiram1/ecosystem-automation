package utils;

import lombok.experimental.UtilityClass;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Arrays;

@UtilityClass
public class AutoUtils {

    public static void sleep(long millis) {
        AerospikeLogger.info("Starting to sleep for " + millis + " milliseconds");
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String removeSpaces(String stringToRmoveSpacesFrom) {
        return stringToRmoveSpacesFrom.replace("\n", "").replaceAll("\\s+", "");
    }

    public static void printDockerStats() {
        try {
            AerospikeLogger.info(getResultFromProcess(Runtime.getRuntime().exec("docker stats --no-stream")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String runBashCommand(String command, boolean printLog) {
        try {
            if (printLog)
                AerospikeLogger.info("Run bash command: " + command);
            String output = getResultFromProcess(Runtime.getRuntime().exec(command));
            if (printLog)
                AerospikeLogger.info(output);
            return output;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String runBashCommand(String[] command) {
        try {
            AerospikeLogger.info("Run bash command: " + Arrays.toString(command));
            String output = getResultFromProcess(Runtime.getRuntime().exec(command));
            return output;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String runBashCommand(String command) {
        return runBashCommand(command, true);
    }

    private static String getResultFromProcess(Process proc) {
        StringBuilder result = new StringBuilder();
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String reader;
        try {
            while ((reader = stdInput.readLine()) != null) {
                result.append(reader);
                result.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public static boolean isRunningOnMacos() {
        return System.getProperty("os.name").contains("Mac");
    }

    public static String getCurrentAbsolutePath(Class<?> myClass) {
        String packagePath = myClass.getPackage().getName().replace(".", "/");
        return Paths.get("src", "test", "java").toAbsolutePath().toString() +"/"+ packagePath;
    }

    public static void unzipFile(String source, String destination) {
        try {
            ZipFile zipFile = new ZipFile(source);
            zipFile.extractAll(destination);
        } catch (ZipException e) {
            AerospikeLogger.info("Unzip file failed");
            AerospikeLogger.info(e.getMessage());
        }
    }
}
