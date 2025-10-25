package utils;

public class ASRestore {
    private final String namespace;
    private String compressionMode;
    private String directory;
    private String encryptionMode;
    private String encryptionKeyFile;


    private ASRestore(String namespace, String directory) {
        this.namespace = namespace;
        this.directory = directory;
    }

    public static ASRestore on(String namespace, String directory) {
        return new ASRestore(namespace, directory);
    }

    public String getDirectory() {
        return this.directory;
    }

    public ASRestore setCompressionMode(String compressionMode) {
        this.compressionMode = compressionMode;
        return this;
    }

    public ASRestore encryptionKeyFile(String encryptionKeyFile) {
        this.encryptionKeyFile = encryptionKeyFile;
        return this;
    }

    public ASRestore encryptionMode(String encryptionMode) {
        this.encryptionMode = encryptionMode;
        return this;
    }

    public void run() {
        String command = ("asrestore " +
                "--namespace %s " +
                "--directory %s " +
                "--user tester --password psw")
                .formatted(namespace, directory);

        command += (compressionMode != null) ? " --compress " + compressionMode : "";
        command += (encryptionMode != null) ? " --encrypt " + encryptionMode : "";
        command += (encryptionKeyFile != null) ? " --encryption-key-file " + encryptionKeyFile : "";

        AerospikeLogger.info("Run asrestore on namespace " + namespace + " from directory " + directory);
        String[] commands = {"/bin/sh", "-c", "cd ../devops/install/abs; pwd; " + command};
        AutoUtils.runBashCommand(commands);
    }

    public static void copyBackupFromContainer(String containerFilePath, String namespace) {
        String commandCopyFromContainer = String.format("docker cp backup-service:%s %s",
                "/" + containerFilePath, ASBackup.asbackupLocalDefaultDirectory);
        AutoUtils.runBashCommand(commandCopyFromContainer);
        String[] commandCopyToBackupDedfaultDir = {
                "bash", "-c", "mv " + ASBackup.asbackupLocalDefaultDirectory + "/" + namespace + "/* " + ASBackup.asbackupLocalDefaultDirectory
        };
        AutoUtils.runBashCommand(commandCopyToBackupDedfaultDir);
    }

    public static void runRestore(String backupFilesPath, String namespace, String compressionMode, String encryptionMode, String encryptionKeyFile) {
        if (ConfigParametersHandler.getParameter("BACKUP_METHOD").equals("backup_service")) {
            copyBackupFromContainer(backupFilesPath, namespace);
        }
        ASRestore asBackup = ASRestore.on(namespace, ASBackup.asbackupLocalDefaultDirectory);
        if (compressionMode != null)
            asBackup.setCompressionMode(compressionMode);
        if (encryptionMode != null)
            asBackup.encryptionMode(encryptionMode);
        if (encryptionKeyFile != null)
            asBackup.encryptionKeyFile(encryptionKeyFile);
        asBackup.run();
        AutoUtils.sleep(2000);
    }
}