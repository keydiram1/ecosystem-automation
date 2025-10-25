package utils;

import lombok.experimental.UtilityClass;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.awaitility.Awaitility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static utils.files.PropertiesHandler.QA_ENV_PROPERTY;

@UtilityClass
public class AutoUtils {

    public static void sleepOnCloud(long millis) {
        if (AutoUtils.isRunningOnGCP()) {
            sleep(millis);
        }
    }

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
            AerospikeLogger.info(getResultFromStringProcess(Runtime.getRuntime().exec("docker stats --no-stream")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String runBashCommand(String command, boolean printLog) {
        try {
            if (printLog)
                AerospikeLogger.info("Run bash command: " + command);
            String output = getResultFromStringProcess(Runtime.getRuntime().exec(command));
            if (printLog)
                AerospikeLogger.info(output);
            return output;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String runBashCommand(String[] command, boolean printLog) {
        return runBashCommand(command, printLog, false);
    }

    public static String runBashCommand(String[] command, boolean printLog, boolean shouldMaskSecrets) {
        try {
            AerospikeLogger.info("Run bash command: " + Arrays.toString(command), true);
            Process proc = Runtime.getRuntime().exec(command);
            String output = getResultFromArrayProcess(proc);
            if (printLog)
                AerospikeLogger.info(output, shouldMaskSecrets);
            return output;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String runBashCommand(String[] command) {
        return runBashCommand(command, true);
    }


    public static String runBashCommand(String command) {
        return runBashCommand(command, true);
    }

    private static String getResultFromArrayProcess(Process proc) {
        StringBuilder result = new StringBuilder();
        try (BufferedReader out = new BufferedReader(new InputStreamReader(proc.getInputStream()));
             BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
            String line;
            while ((line = out.readLine()) != null) result.append(line).append("\n");
            while ((line = err.readLine()) != null) result.append(line).append("\n"); // removed "ERROR: "
            int exitCode = proc.waitFor();
            result.append("Exit Code: ").append(exitCode).append("\n");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return result.toString();
    }

    private static String getResultFromStringProcess(Process proc) {
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

    public static boolean isRunningOnGCP() {
        AerospikeLogger.info("is running on gcp= " + ConfigParametersHandler.getParameter(QA_ENV_PROPERTY));
        return ConfigParametersHandler.getParameter(QA_ENV_PROPERTY) != null && ConfigParametersHandler.getParameter(QA_ENV_PROPERTY).equals("GCP");
    }

    public static boolean isRunningOnMacos() {
        return ConfigParametersHandler.getParameter("os.name").contains("Mac");
    }

    public static String getCurrentAbsolutePath(Class<?> myClass) {
        String packagePath = myClass.getPackage().getName().replace(".", "/");
        return Paths.get("src", "test", "java").toAbsolutePath().toString() + "/" + packagePath;
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

    public static String getFileNameFromDir(String directoryPath) {
        Path dir = Paths.get(directoryPath);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            return stream.iterator().next().getFileName().toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static long getSizeOfDir(String directoryPath) {
        try {
            return Files.walk(Paths.get(directoryPath))
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> p.toFile().length())
                    .sum();
        } catch (IOException e) {
            e.printStackTrace();
            return 0L;
        }
    }

    public static long countFilesInDir(String directoryPath) {
        try {
            return Files.walk(Paths.get(directoryPath))
                    .filter(Files::isRegularFile)
                    .count();
        } catch (IOException e) {
            e.printStackTrace();
            return 0L;
        }
    }

    public static String getCurrentFormattedTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }

    public static String getTextFromFile(String absolutePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(absolutePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            AerospikeLogger.info("Failed to read file: " + absolutePath);
            AerospikeLogger.info(e.getMessage());
        }
        return content.toString().trim();
    }

    public static void replaceFileContent(String filePath, String newContent) {
        Path path = Paths.get(filePath);
        try {
            Files.write(path, newContent.getBytes());
        } catch (IOException e) {
            AerospikeLogger.error("replaceFileContent failed with the following error: " + e.getMessage());
        }
    }

    public static String renameFile(String filePath, String newFileName) {
        Path oldPath = Paths.get(filePath);
        Path newPath = oldPath.getParent().resolve(newFileName); // Same directory, different name

        try {
            Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
            return newPath.toAbsolutePath().toString();
        } catch (IOException e) {
            AerospikeLogger.error("renameFileInSameDirectory failed with the following error: " + e.getMessage());
            return null;
        }
    }

    public static void waitUntilNextRoundSecond(int intervalSeconds) {
        long nowMillis = System.currentTimeMillis();
        long currentSecond = nowMillis / 1000;
        long nextRoundSecond = ((currentSecond / intervalSeconds) + 1) * intervalSeconds;
        long waitMillis = (nextRoundSecond * 1000) - nowMillis;

        AerospikeLogger.info("Wait until next round " + intervalSeconds + " second(s).");
        AutoUtils.sleep(waitMillis);
    }

    public static void restartPod(String namespace, String podName) {
        String deleteCommand = "kubectl delete pod " + podName + " -n " + namespace;
        String output = AutoUtils.runBashCommand(deleteCommand);
        AerospikeLogger.info("Delete pod output: " + output);
        assertThat(output).contains("deleted");

        // Wait until specific pod is Running and READY (1/1)
        Awaitility.await("Wait for pod to restart and become Ready")
                .pollInterval(Duration.ofSeconds(5))
                .atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> {
                    String podStatus = AutoUtils.runBashCommand("kubectl get pod " + podName + " -n " + namespace);
                    AerospikeLogger.info("Pod status: " + podStatus);
                    assertThat(podStatus).contains("1/1");
                    assertThat(podStatus).contains("Running");
                });

        AerospikeLogger.info("Restart complete!");
    }
}