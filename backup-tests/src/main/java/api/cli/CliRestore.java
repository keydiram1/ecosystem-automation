package api.cli;

import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.ConfigParametersHandler;
import utils.abs.TlsHandler;
import utils.cliBackup.CliBackupRunner;

public class CliRestore {
    private final String namespace;
    private String command;
    public static final int DEFAULT_PARALLEL = 8;

    private CliRestore(String namespace) {
        this.namespace = namespace;
    }

    private CliRestore(String namespace, String backupDirPath, int parallel) {
        this.command = ("./asrestore " +
                "--namespace %s " +
                "--parallel " + parallel +
                " --user tester --password psw")
                .formatted(namespace);
        if (!backupDirPath.equals(""))
            command += " --directory " + backupDirPath;
        if (ConfigParametersHandler.getParameter("IS_RUNNING_ON_LOCAL_3_NODES_ENV").equals("true"))
            command += " --host " + CliBackupRunner.AEROSPIKE_SOURCE_SERVER_IP + ":%s:%d ".formatted(TlsHandler.TLS_NAME, CliBackupRunner.AEROSPIKE_SOURCE_SERVER_PORT);

        this.namespace = namespace;
    }

    private CliRestore(String namespace, String backupDirPath) {
        this(namespace, backupDirPath, DEFAULT_PARALLEL);
    }

    public static CliRestore on(String namespace, String backupDirName) {
        if (AutoUtils.isRunningOnGCP())
            return onWithTls(namespace, backupDirName);
        return new CliRestore(namespace, backupDirName);
    }

    public static CliRestore on() {
        return new CliRestore("", "");
    }

    public static CliRestore on(String namespace) {
        if (AutoUtils.isRunningOnGCP())
            return onWithTls(namespace, "");
        return new CliRestore(namespace, "");
    }

    public static CliRestore on(String namespace, String backupDirName, int parallel) {
        if (AutoUtils.isRunningOnGCP())
            return onWithTls(namespace, backupDirName, parallel);
        return new CliRestore(namespace, backupDirName, parallel);
    }

    public static CliRestore on(String sourceNamespace, String destinationNamespace, String backupDirName) {
        if (AutoUtils.isRunningOnGCP())
            return onWithTls(sourceNamespace + "," + destinationNamespace, backupDirName);
        return new CliRestore(sourceNamespace + "," + destinationNamespace, backupDirName);
    }

    public RestoreResult run(boolean ignoreErrors) {
        AerospikeLogger.info("Run asrestore on namespace " + namespace);
        String[] commands = {"/bin/sh", "-c", "cd ../devops/install/cli-backup/aerospike-backup-cli/cmd/asrestore; pwd; " + command};
        return RestoreResult.getInstance(AutoUtils.runBashCommand(commands, true, true), ignoreErrors);
    }

    public RestoreResult run() {
        return run(false);
    }

    public CliRestore setNoRecords() {
        command += " --no-records";
        return this;
    }

    public CliRestore setNoUdf() {
        command += " --no-udfs";
        return this;
    }

    public CliRestore setNoSecondaryIndexes() {
        command += " --no-indexes";
        return this;
    }

    public CliRestore setNoGeneration() {
        command += " --no-generation";
        return this;
    }

    public CliRestore setUnique() {
        command += " --unique";
        return this;
    }

    public CliRestore setReplace() {
        command += " --replace";
        return this;
    }

    public CliRestore setExtraTtl(int extraTtl) {
        command += " --extra-ttl " + extraTtl;
        return this;
    }

    public CliRestore setSetList(String... sets) {
        command += " --set " + String.join(",", sets);
        return this;
    }

    public CliRestore setBinList(String... bins) {
        command += " --bin-list " + String.join(",", bins);
        return this;
    }

    public CliRestore setNice(int bandwidth) {
        command += " --nice " + bandwidth;
        return this;
    }

    public CliRestore setBandwidth(int bandwidth) {
        command += " --bandwidth " + bandwidth;
        return this;
    }

    public CliRestore disableBatchWrites() {
        command += " --disable-batch-writes";
        return this;
    }

    public CliRestore setMaxAsyncBatches(int maxAsyncBatches) {
        command += " --max-async-batches " + maxAsyncBatches;
        return this;
    }

    public CliRestore setBatchSize(int batchSize) {
        command += " --batch-size " + batchSize;
        return this;
    }

    public CliRestore setEncryptionMode(String encryptionMode) {
        command += " --encrypt " + encryptionMode;
        return this;
    }

    public CliRestore setEncryptionKeyFile() {
        command += " --encryption-key-file encryptionKey";
        return this;
    }

    public CliRestore setCompressMode(String compressMode) {
        command += " --compress " + compressMode;
        return this;
    }

    public CliRestore setRecordsPerSecond(int recordsPerSecond) {
        command += " --records-per-second " + recordsPerSecond;
        return this;
    }

    public CliRestore setTotalTimeout(int totalTimeout) {
        command += " --total-timeout " + totalTimeout;
        return this;
    }

    public CliRestore setSocketTimeout(int socketTimeout) {
        command += " --socket-timeout " + socketTimeout;
        return this;
    }

    public CliRestore setIgnoreRecordError() {
        command += " --ignore-record-error";
        return this;
    }

    public CliRestore setS3EndpointOverride(String endpoint) {
        command += " --s3-endpoint-override " + endpoint;
        return this;
    }

    public CliRestore setS3Region(String region) {
        command += " --s3-region " + region;
        return this;
    }

    public CliRestore setS3Profile(String profile) {
        command += " --s3-profile " + profile;
        return this;
    }

    public CliRestore setAzureAccountName(String accountName) {
        command += " --azure-account-name " + accountName;
        return this;
    }

    public CliRestore setAzureAccountKey(String accountKey) {
        command += " --azure-account-key " + accountKey;
        return this;
    }

    public CliRestore setAzureContainerName(String containerName) {
        command += " --azure-container-name " + containerName;
        return this;
    }

    public CliRestore setAzureEndpoint(String endpoint) {
        command += " --azure-endpoint " + endpoint;
        return this;
    }

    public CliRestore setAzureTenantId(String id) {
        command += " --azure-tenant-id " + id;
        return this;
    }

    public CliRestore setAzureClientSecret(String secret) {
        command += " --azure-client-secret " + secret;
        return this;
    }

    public CliRestore setAzureClientId(String id) {
        command += " --azure-client-id " + id;
        return this;
    }

    public CliRestore setGcpKeyPath() {
        command += " --gcp-key-path " + ConfigParametersHandler.getParameter("GCP_SA_KEY_FILE");
        return this;
    }

    public CliRestore setGcpBucketName(String bucketName) {
        command += " --gcp-bucket-name " + bucketName;
        return this;
    }

    public CliRestore setGcpEndpointOverride(String endpoint) {
        command += " --gcp-endpoint-override " + endpoint;
        return this;
    }

    public CliRestore setParallel(int parallel) {
        command += " --parallel " + parallel;
        return this;
    }

    public static CliRestore onWithXdrTls(String namespace, String backupDir) {
        CliRestore restoreWithTls = new CliRestore(namespace);

        String command = "./asrestore";
        command += " --user tester --password psw";
        command += " --directory " + backupDir;
        command += " --namespace " + namespace;
        command += " --host " + CliBackupRunner.AEROSPIKE_SOURCE_SERVER_IP + ":%s:%d ".formatted(TlsHandler.TLS_NAME, CliBackupRunner.AEROSPIKE_SOURCE_SERVER_PORT);
        command += " --tls-cafile=" + ConfigParametersHandler.getParameter("CA_AEROSPIKE_COM_PEM_PATH");
        command += " --tls-name=%s ".formatted(TlsHandler.TLS_NAME);
        command += " --tls-enable";

        restoreWithTls.command = command;

        return restoreWithTls;
    }

    public CliRestore setSecretAgent() {
        command += " --sa-port " + ConfigParametersHandler.getParameter("SECRET_AGENT_PORT");
        command += " --sa-connection-type tcp";
        command += " --sa-address " + ConfigParametersHandler.getParameter("SECRET_AGENT_IP").replaceFirst("^http://", "");
        command += " --sa-is-base64 true";
        return this;
    }

    public CliRestore setEncryptionKeySecret(String encryptionKeySecret) {
        command += " --encryption-key-secret " + encryptionKeySecret;
        return this;
    }

    public CliRestore setCustomFlag(String flag, String value) {
        command += " --" + flag + " " + value;
        return this;
    }

    public CliRestore setEnableTls() {
        command += " --tls-enable";
        return this;
    }

    public CliRestore setPort(int port) {
        command += " --port " + port;
        return this;
    }

    public CliRestore setHost(String host) {
        command += " --host " + host;
        return this;
    }

    public CliRestore setTlsCafile(String tlsCafile) {
        command += " --tls-cafile " + tlsCafile;
        return this;
    }

    public CliRestore setNamespace(String namespace) {
        command += " --namespace " + namespace;
        return this;
    }

    public CliRestore setUser(String user) {
        command += " --user " + user;
        return this;
    }

    public CliRestore setPassword(String password) {
        command += " --password " + password;
        return this;
    }

    public CliRestore setS3BucketName(String bucket) {
        command += " --s3-bucket-name " + bucket;
        return this;
    }

    public CliRestore setDirectoryList(String... dirList) {
        return setDirectoryList(false, dirList);
    }

    public CliRestore setDirectoryList(boolean removeDirectory, String... dirList) {
        command += " --directory-list " + String.join(",", dirList);
        if (removeDirectory)
            removeFlag("directory");
        return this;
    }

    public CliRestore setParentDirectory(String parentDir) {
        command += " --parent-directory " + parentDir;
        return this;
    }

    public CliRestore setS3AccessKeyId(String accessKeyId) {
        command += " --s3-access-key-id " + accessKeyId;
        return this;
    }

    public CliRestore setS3SecretAccessKey(String secretAccessKey) {
        command += " --s3-secret-access-key " + secretAccessKey;
        return this;
    }

    public CliRestore setRetryMaxRetries(int maxRetries) {
        command += " --retry-max-retries " + maxRetries;
        return this;
    }

    public CliRestore setMaxRetries(int maxRetries) {
        command += " --max-retries " + maxRetries;
        return this;
    }

    public static CliRestore onWithTls(String namespace, String backupDir) {
        CliRestore restore = new CliRestore(namespace);
        restore.command = "./asrestore";
        applyTlsSettings(restore, namespace, backupDir);
        return restore;
    }

    public static CliRestore onWithTls(String namespace, String backupDir, int parallel) {
        CliRestore restore = new CliRestore(namespace, backupDir, parallel);
        applyTlsSettings(restore, namespace, backupDir);
        return restore;
    }

    private static void applyTlsSettings(CliRestore restore, String namespace, String backupDir) {
        restore.command += " --user tester --password psw";
        restore.command += " --directory " + backupDir;
        restore.command += " --namespace " + namespace;
        restore.command += " --host " + CliBackupRunner.AEROSPIKE_SOURCE_SERVER_IP + ":%s:%d ".formatted(
                TlsHandler.TLS_NAME, CliBackupRunner.AEROSPIKE_SOURCE_SERVER_PORT);
        restore.command += " --tls-cafile=" + ConfigParametersHandler.getParameter("CA_AEROSPIKE_COM_PEM_PATH");
        restore.command += " --tls-name=%s ".formatted(TlsHandler.TLS_NAME);
        restore.command += " --tls-enable";
    }

    public CliRestore removeFlag(String flag) {
        String[] parts = command.split("\\s+");
        StringBuilder result = new StringBuilder();

        AerospikeLogger.info("command before removeFlag: " + command);

        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("--" + flag)) {
                // Only skip the next part if it's not another flag
                if (i + 1 < parts.length && !parts[i + 1].startsWith("--")) {
                    i++; // Skip the value
                }
            } else {
                result.append(parts[i]).append(" ");
            }
        }

        command = result.toString().trim();
        return this;
    }

    public CliRestore setValidateFile(String filePath) {
        command = "./asrestore --input-file " + filePath + " --validate";
        return this;
    }

    public CliRestore setValidateDirectory(String directoryPath) {
        command = "./asrestore --directory " + directoryPath + " --validate";
        return this;
    }

    public CliRestore setValidateDirectoryList(String... directories) {
        String joinedDirs = String.join(",", directories);
        command = "./asrestore --directory-list " + joinedDirs + " --validate";
        return this;
    }

    public String validate() {
        String[] commands = {"/bin/sh", "-c", "cd ../devops/install/cli-backup/aerospike-backup-cli/cmd/asrestore; pwd; " + command};
        return AutoUtils.runBashCommand(commands, true, true);
    }
}