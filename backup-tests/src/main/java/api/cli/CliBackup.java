package api.cli;

import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.ConfigParametersHandler;
import utils.abs.TlsHandler;
import utils.cliBackup.CliBackupRunner;

public class CliBackup {
    private final String namespace;
    private String command;
    public String backupFilesDir = "/tmp/";
    public static final int DEFAULT_PARALLEL = 8;

    private CliBackup(String namespace) {
        this.namespace = namespace;
    }

    private CliBackup(String namespace, String backupFilesDir, int parallel, boolean overrideBackupDir, boolean useXdrBackup) {
        this.backupFilesDir = overrideBackupDir ? backupFilesDir : this.backupFilesDir + backupFilesDir;
        this.command = (useXdrBackup ? "./asbackup xdr " : "./asbackup ");
        this.command += "--namespace %s --user tester --password psw".formatted(namespace);
        if (!useXdrBackup) {
            this.command += " --parallel " + parallel;
        }
        if (backupFilesDir != null) {
            this.command += " --directory " + this.backupFilesDir;
        }
        if (ConfigParametersHandler.getParameter("IS_RUNNING_ON_LOCAL_3_NODES_ENV").equals("true"))
            command += " --host " + CliBackupRunner.AEROSPIKE_SOURCE_SERVER_IP + ":%s:%d ".formatted(TlsHandler.TLS_NAME, CliBackupRunner.AEROSPIKE_SOURCE_SERVER_PORT);
        this.namespace = namespace;
    }

    private CliBackup(String namespace, String backupFilesDir, int parallel, boolean overrideBackupDir) {
        this(namespace, backupFilesDir, parallel, overrideBackupDir, false);
    }

    public static CliBackup on(String namespace, String backupFilesDir) {
        if (AutoUtils.isRunningOnGCP())
            return onWithTls(namespace, backupFilesDir);
        return new CliBackup(namespace, backupFilesDir, DEFAULT_PARALLEL, false);
    }

    public static CliBackup onWithXdr(String namespace, String backupFilesDir) {
        return new CliBackup(namespace, backupFilesDir, DEFAULT_PARALLEL, false, true);
    }

    public static CliBackup onWithXdr(String namespace, String backupFilesDir, String dc, int localPort) {
        return new CliBackup(namespace, backupFilesDir, DEFAULT_PARALLEL, false, true)
                .setLocalAddress()
                .setDc(dc)
                .setLocalPort(localPort)
                .setRemoveFiles();
    }

    public static CliBackup onWithXdr(String namespace, String backupFilesDir, boolean overrideBackupDir) {
        return new CliBackup(namespace, backupFilesDir, DEFAULT_PARALLEL, overrideBackupDir, true);
    }

    public static CliBackup on(String namespace, String backupFilesDir, boolean overrideBackupDir) {
        if (AutoUtils.isRunningOnGCP())
            return onWithTls(namespace, backupFilesDir, overrideBackupDir);
        return new CliBackup(namespace, backupFilesDir, DEFAULT_PARALLEL, overrideBackupDir);
    }

    public static CliBackup on(String namespace, String backupFilesDir, int parallel) {
        if (AutoUtils.isRunningOnGCP())
            return onWithTls(namespace, backupFilesDir, parallel);
        return new CliBackup(namespace, backupFilesDir, parallel, false);
    }

    public static CliBackup on(String namespace) {
        if (AutoUtils.isRunningOnGCP())
            return onWithTls(namespace, null);
        return new CliBackup(namespace, null, DEFAULT_PARALLEL, false);
    }

    public BackupResult run(boolean cleanBackupDir, boolean createBackupResult, boolean ignoreErrors) {
        AerospikeLogger.info("Run asbackup on namespace " + namespace + " to directory " + backupFilesDir);
        if (cleanBackupDir)
            cleanDefaultBackupDirectory();
        String[] commands = {"/bin/sh", "-c", "cd ../devops/install/cli-backup/aerospike-backup-cli/cmd/asbackup; pwd; " + command};
        if (createBackupResult) {
            BackupResult backupResult = BackupResult.getInstance(AutoUtils.runBashCommand(commands, true, true), ignoreErrors);
            backupResult.setBackupDir(backupFilesDir);
            return backupResult;
        } else {
            AutoUtils.runBashCommand(commands, true, true);
            return null;
        }
    }

    public BackupResult run(boolean cleanBackupDir, boolean createBackupResult) {
        return run(cleanBackupDir, createBackupResult, false);
    }

    public BackupResult run() {
        return run(true, true, false);
    }

    public BackupResult run(boolean ignoreErrors) {
        return run(true, true, ignoreErrors);
    }

    public void cleanDefaultBackupDirectory() {
        AutoUtils.runBashCommand("rm -rf " + backupFilesDir);
        AutoUtils.runBashCommand("mkdir -p " + backupFilesDir);
    }

    public CliBackup setSetList(String... sets) {
        command += " --set " + String.join(",", sets);
        return this;
    }

    public CliBackup setBinList(String... bins) {
        command += " --bin-list " + String.join(",", bins);
        return this;
    }

    public CliBackup setRecordsPerSecond(int recordsPerSecond) {
        command += " --records-per-second " + recordsPerSecond;
        return this;
    }

    public CliBackup setTotalTimeout(int totalTimeout) {
        command += " --total-timeout " + totalTimeout;
        return this;
    }

    public CliBackup setSocketTimeout(int socketTimeout) {
        command += " --socket-timeout " + socketTimeout;
        return this;
    }

    public CliBackup setNice(int bandwidth) {
        command += " --nice " + bandwidth;
        return this;
    }

    public CliBackup setBandwidth(int bandwidth) {
        command += " --bandwidth " + bandwidth;
        return this;
    }

    public CliBackup setFileLimit(int fileLimit) {
        command += " --file-limit " + fileLimit;
        return this;
    }

    public CliBackup setEncryptionMode(String encryptionMode) {
        command += " --encrypt " + encryptionMode;
        return this;
    }

    public CliBackup setEncryptionKeyFile() {
        command += " --encryption-key-file encryptionKey";
        return this;
    }

    public CliBackup setCompressMode(String compressMode) {
        command += " --compress " + compressMode;
        return this;
    }

    public CliBackup setCompressLevel(int compressLevel) {
        command += " --compression-level " + compressLevel;
        return this;
    }

    public CliBackup setNoBins() {
        command += " --no-bins";
        return this;
    }

    public CliBackup setNoTtlOnly() {
        command += " --no-ttl-only";
        return this;
    }

    public CliBackup setEstimate() {
        command += " --estimate";
        return this;
    }

    public CliBackup setEstimateSamples(int numberOfSamples) {
        command += " --estimate-samples " + numberOfSamples;
        return this;
    }

    public long estimate() {
        return estimate(true);
    }

    public long estimate(boolean removeFlag) {
        setEstimate();
        if (removeFlag)
            removeFlag("directory");
        AerospikeLogger.info("Run estimation on namespace " + namespace);
        String estimation = AutoUtils.runBashCommand(new String[]{"/bin/sh", "-c", "cd ../devops/install/cli-backup/aerospike-backup-cli/cmd/asbackup; " + command});

        if (estimation.contains("ERROR"))
            throw new EstimationProcessException("An error occurred during the estimation process: " + estimation);

        return Long.parseLong(estimation.split("File size \\(bytes\\):\\s+")[1].split("\\n")[0].trim());
    }

    public CliBackup setOutputFile(String fileName) {
        command = command.replaceAll("--directory\\s+(\\S+)", "--output-file $1/" + fileName + ".asb");
        return this;
    }

    public CliBackup setCompact() {
        command += " --compact";
        return this;
    }

    public CliBackup setRemoveFiles() {
        command += " --remove-files";
        return this;
    }

    public CliBackup setRemoveArtifacts() {
        command += " --remove-artifacts";
        return this;
    }

    public CliBackup setAfterDigest(String digest) {
        command += " --after-digest " + digest;
        return this;
    }

    public CliBackup setModifiedAfter(String timestamp) {
        command += " --modified-after " + timestamp;
        return this;
    }

    public CliBackup setModifiedBefore(String timestamp) {
        command += " --modified-before " + timestamp;
        return this;
    }

    public CliBackup setS3EndpointOverride(String endpoint) {
        command += " --s3-endpoint-override " + endpoint;
        return this;
    }

    public CliBackup setS3Region(String region) {
        command += " --s3-region " + region;
        return this;
    }

    public CliBackup setS3Profile(String profile) {
        command += " --s3-profile " + profile;
        return this;
    }

    public CliBackup setAzureAccountName(String accountName) {
        command += " --azure-account-name " + accountName;
        return this;
    }

    public CliBackup setAzureAccountKey(String accountKey) {
        command += " --azure-account-key " + accountKey;
        return this;
    }

    public CliBackup setAzureContainerName(String containerName) {
        command += " --azure-container-name " + containerName;
        return this;
    }

    public CliBackup setAzureEndpoint(String endpoint) {
        command += " --azure-endpoint " + endpoint;
        return this;
    }

    public CliBackup setAzureTenantId(String id) {
        command += " --azure-tenant-id " + id;
        return this;
    }

    public CliBackup setAzureClientSecret(String secret) {
        command += " --azure-client-secret " + secret;
        return this;
    }

    public CliBackup setAzureClientId(String id) {
        command += " --azure-client-id " + id;
        return this;
    }

    public CliBackup setGcpKeyPath() {
        command += " --gcp-key-path " + ConfigParametersHandler.getParameter("GCP_SA_KEY_FILE");
        return this;
    }

    public CliBackup setGcpBucketName(String bucketName) {
        command += " --gcp-bucket-name " + bucketName;
        return this;
    }

    public CliBackup setGcpEndpointOverride(String endpoint) {
        command += " --gcp-endpoint-override " + endpoint;
        return this;
    }

    public BackupResult runWithTls() {
        String[] commands = {"/bin/sh", "-c", "cd ../devops/install/cli-backup/aerospike-backup-cli/cmd/asbackup; pwd; " + command};
        BackupResult backupResult = BackupResult.getInstance(AutoUtils.runBashCommand(commands, true, true));
        backupResult.setBackupDir(this.backupFilesDir);
        return backupResult;
    }

    public CliBackup setSecretAgent() {
        command += " --sa-port " + ConfigParametersHandler.getParameter("SECRET_AGENT_PORT");
        command += " --sa-connection-type tcp";
        command += " --sa-address " + ConfigParametersHandler.getParameter("SECRET_AGENT_IP").replaceFirst("^http://", "");
        command += " --sa-is-base64";
        return this;
    }

    public CliBackup setEncryptionKeySecret(String encryptionKeySecret) {
        command += " --encryption-key-secret " + encryptionKeySecret;
        return this;
    }

    public CliBackup setCustomFlag(String flag, String value) {
        command += " --" + flag + " " + value;
        return this;
    }

    public CliBackup setEnableTls() {
        command += " --tls-enable";
        return this;
    }

    public CliBackup setPort(int port) {
        command += " --port " + port;
        return this;
    }

    public CliBackup setHost(String host) {
        command += " --host " + host;
        return this;
    }

    public CliBackup setTlsCafile(String tlsCafile) {
        command += " --tls-cafile " + tlsCafile;
        return this;
    }

    public CliBackup setNamespace(String namespace) {
        command += " --namespace " + namespace;
        return this;
    }

    public CliBackup setUser(String user) {
        command += " --user " + user;
        return this;
    }

    public CliBackup setPassword(String password) {
        command += " --password " + password;
        return this;
    }

    public CliBackup setPartitionList(String partitionList) {
        command += " --partition-list " + partitionList;
        return this;
    }

    public CliBackup setFilterExpression(String filterExpression) {
        command += " --filter-exp " + filterExpression;
        return this;
    }

    public CliBackup setS3BucketName(String bucket) {
        command += " --s3-bucket-name " + bucket;
        return this;
    }

    public CliBackup setS3AccessKeyId(String accessKeyId) {
        command += " --s3-access-key-id " + accessKeyId;
        return this;
    }

    public CliBackup setS3SecretAccessKey(String secretAccessKey) {
        command += " --s3-secret-access-key " + secretAccessKey;
        return this;
    }

    public CliBackup setParallelWrite(int parallelWrite) {
        command += " --parallel-write " + parallelWrite;
        return this;
    }

    public CliBackup setDc(String dc) {
        command += " --dc " + dc;
        return this;
    }

    public CliBackup setLocalAddress(String localAddress) {
        command += " --local-address " + localAddress;
        return this;
    }

    public CliBackup setDirectory(String directory) {
        command += " --directory " + directory;
        return this;
    }

    public CliBackup setNodeList(String nodeList) {
        command += " --node-list " + nodeList;
        return this;
    }

    public CliBackup setLocalAddress() {
        if (ConfigParametersHandler.getParameter("IS_RUNNING_ON_LOCAL_3_NODES_ENV").equals("true"))
            command += " --local-address " + ConfigParametersHandler.getParameter("LOCAL_SLAVE_GATEWAY");
        else
            command += " --local-address host.docker.internal";
        return this;
    }

    public CliBackup setLocalPort(int localPort) {
        command += " --local-port " + localPort;
        return this;
    }

    public CliBackup setRewind(int seconds) {
        command += " --rewind " + seconds;
        return this;
    }

    public CliBackup setReadTimeout(int readTimeout) {
        command += " --read-timeout " + readTimeout;
        return this;
    }

    public CliBackup setWriteTimeout(int writeTimeout) {
        command += " --write-timeout " + writeTimeout;
        return this;
    }

    public CliBackup setStartTimeout(int timeout) {
        command += " --start-timeout " + timeout;
        return this;
    }

    public CliBackup setMaxThroughput(int throughput) {
        command += " --max-throughput " + throughput;
        return this;
    }

    public CliBackup setResultsQueueSize(int resultsQueueSize) {
        command += " --results-queue-size " + resultsQueueSize;
        return this;
    }

    public CliBackup setAckQueueSize(int ackQueueSize) {
        command += " --ack-queue-size " + ackQueueSize;
        return this;
    }

    public CliBackup setMaxConnections(int maxConnections) {
        command += " --max-connections " + maxConnections;
        return this;
    }

    public CliBackup setMaxRetries(int maxRetries) {
        command += " --max-retries " + maxRetries;
        return this;
    }

    public CliBackup setInfoPollingPeriod(int pollingPeriod) {
        command += " --info-poling-period " + pollingPeriod;
        return this;
    }

    public CliBackup setStopXdr() {
        command += " --stop-xdr";
        return this;
    }

    public CliBackup setVerbose() {
        command += " --verbose";
        return this;
    }

    public String stopXdr(String dcName) {
        setStopXdr();
        setDc(dcName);
        setLocalAddress();
        AerospikeLogger.info("Stopping xdr for dc " + dcName);
        return AutoUtils.runBashCommand(new String[]{"/bin/sh", "-c", "cd ../devops/install/cli-backup/aerospike-backup-cli/cmd/asbackup; " + command});
    }

    public static CliBackup onWithXdrTls(String namespace, String backupDir, String dc, int localPort) {
        CliBackup backupWithTls = new CliBackup(namespace, backupDir, DEFAULT_PARALLEL, false, true)
                .setLocalAddress(ConfigParametersHandler.getParameter("LOCAL_SLAVE_GATEWAY"))
                .setDc(dc)
                .setLocalPort(localPort)
                .setRemoveFiles();

        backupWithTls.command += " --host " + CliBackupRunner.AEROSPIKE_SOURCE_SERVER_IP + ":%s:%d ".formatted(TlsHandler.TLS_NAME, CliBackupRunner.AEROSPIKE_SOURCE_SERVER_PORT);
        backupWithTls.command += " --tls-cafile=" + ConfigParametersHandler.getParameter("CA_AEROSPIKE_COM_PEM_PATH");
        backupWithTls.command += " --tls-enable";

        backupWithTls.backupFilesDir = "/tmp/" + backupDir;

        return backupWithTls;
    }

    public CliBackup setUnblockMrt() {
        command += " --unblock-mrt";
        return this;
    }

    public String unblockMrtWrites(String namespace) {
        setUnblockMrt();
        setLocalAddress();
        AerospikeLogger.info("Unblocking mrt for namespace " + namespace);
        return AutoUtils.runBashCommand(new String[]{"/bin/sh", "-c", "cd ../devops/install/cli-backup/aerospike-backup-cli/cmd/asbackup; " + command});
    }

    public static CliBackup onWithTls(String namespace, String backupDir) {
        CliBackup backupWithTls = new CliBackup(namespace);
        backupWithTls.backupFilesDir = "/tmp/" + backupDir;
        backupWithTls.command = "./asbackup";
        applyTlsSettings(backupWithTls, namespace);
        return backupWithTls;
    }

    public static CliBackup onWithTls(String namespace, String backupDir, int parallel) {
        CliBackup backupWithTls = new CliBackup(namespace, backupDir, parallel, false);
        applyTlsSettings(backupWithTls, namespace);
        return backupWithTls;
    }

    public static CliBackup onWithTls(String namespace, String backupDir, boolean overrideBackupDir) {
        CliBackup backupWithTls = new CliBackup(namespace, backupDir, DEFAULT_PARALLEL, overrideBackupDir);
        applyTlsSettings(backupWithTls, namespace);
        return backupWithTls;
    }

    private static void applyTlsSettings(CliBackup backupWithTls, String namespace) {
        backupWithTls.command += " --user tester --password psw";
        backupWithTls.command += " --directory " + backupWithTls.backupFilesDir;
        backupWithTls.command += " --namespace " + namespace;
        backupWithTls.command += " --remove-files";
        backupWithTls.command += " --host " + CliBackupRunner.AEROSPIKE_SOURCE_SERVER_IP + ":%s:%d ".formatted(
                TlsHandler.TLS_NAME, CliBackupRunner.AEROSPIKE_SOURCE_SERVER_PORT);
        backupWithTls.command += " --tls-cafile=" + ConfigParametersHandler.getParameter("CA_AEROSPIKE_COM_PEM_PATH");
        backupWithTls.command += " --tls-enable";
    }

    public CliBackup removeFlag(String flag) {
        command = command
                .replaceAll("(^|\\s)--" + flag + "\\s+\\S+", "")  // match start or space before --flag <value>
                .replaceAll("(^|\\s)--" + flag + "=[^\\s]+", "")  // match start or space before --flag=<value>
                .replaceAll("\\s{2,}", " ")                       // remove double spaces
                .trim();
        return this;
    }
}