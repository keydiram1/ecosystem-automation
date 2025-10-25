package utils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ASBackup {
    private final String namespace;
    private String compressionMode;
    private int compressionLevel = 0;
    private String encryptionMode;
    private String encryptionKeyFile;
    public static String asbackupLocalDefaultDirectory = ConfigParametersHandler.getParameter("user.home") + "/ASBackupDir";
    public static String asbackupServiceContainerDefaultDirectory = "/tmp";

    private ASBackup(String namespace) {
        this.namespace = namespace;
    }

    public static ASBackup on(String namespace) {
        return new ASBackup(namespace);
    }

    public ASBackup setCompressionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
        return this;
    }

    public ASBackup setCompressionMode(String compressionMode) {
        this.compressionMode = compressionMode;
        return this;
    }

    public ASBackup encryptionKeyFile(String encryptionKeyFile) {
        this.encryptionKeyFile = encryptionKeyFile;
        return this;
    }

    public ASBackup encryptionMode(String encryptionMode) {
        this.encryptionMode = encryptionMode;
        return this;
    }

    public void run() {
        String command = ("asbackup " +
                "--namespace %s " +
                "--directory %s " +
                "--user tester --password psw -r")
                .formatted(namespace, asbackupLocalDefaultDirectory);

        command += (compressionMode != null) ? " --compress " + compressionMode : "";
        command += (compressionLevel != 0) ? " --compression-level " + compressionLevel : "";
        command += (encryptionMode != null) ? " --encrypt " + encryptionMode : "";
        command += (encryptionKeyFile != null) ? " --encryption-key-file " + encryptionKeyFile : "";

        AerospikeLogger.info("Run asbackup on namespace " + namespace + " to directory " + asbackupLocalDefaultDirectory);
        cleanDefaultBackupDirectory();
        String[] commands = {"/bin/sh", "-c", "cd ../devops/install/abs; pwd; " + command};
        AutoUtils.runBashCommand(commands);
    }

    public static void cleanDefaultBackupDirectory() {
        AutoUtils.runBashCommand("rm -rf " + asbackupLocalDefaultDirectory);
        AutoUtils.runBashCommand("mkdir -p " + asbackupLocalDefaultDirectory);
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

    public static String runBackup(String namespace) {
        return runBackup(namespace, null, 0, null, null);
    }

    public static String runBackup(String namespace, String compressionMode, int compressionLevel, String encryptionMode, String encryptionKeyFile) {
        ASBackup asBackup = ASBackup.on(namespace);
        if (compressionMode != null)
            asBackup.setCompressionMode(compressionMode);
        if (compressionLevel != 0)
            asBackup.setCompressionLevel(compressionLevel);
        if (encryptionMode != null)
            asBackup.encryptionMode(encryptionMode);
        if (encryptionKeyFile != null)
            asBackup.encryptionKeyFile(encryptionKeyFile);
        asBackup.run();
        String source = asbackupLocalDefaultDirectory + "/" + getFileNameFromDir(asbackupLocalDefaultDirectory);
        AutoUtils.runBashCommand("mkdir -p ../devops/install/abs/mnt" + asbackupServiceContainerDefaultDirectory);
        AutoUtils.runBashCommand("cp " + source + " ../devops/install/abs/mnt/" + asbackupServiceContainerDefaultDirectory);
        var actual = "/data" + asbackupServiceContainerDefaultDirectory + "/";
        cleanBackupsDirectoryInsideBackupContainer(actual);
        asBackup.copyBackupIntoContainer(actual);
        return asbackupServiceContainerDefaultDirectory;
    }

    public void copyBackupIntoContainer(String destinationPath) {
        String command = String.format("docker cp %s backup-service:%s", asbackupLocalDefaultDirectory + "/" + getFileNameFromDir(asbackupLocalDefaultDirectory), destinationPath);
        AutoUtils.runBashCommand(command);
    }

    public static void cleanBackupsDirectoryInsideBackupContainer(String directory) {
        String[] commands = {"/bin/sh", "-c", "docker exec backup-service sh -c 'rm -rf " + directory + "/*'"};
        AutoUtils.runBashCommand(commands);
    }
}