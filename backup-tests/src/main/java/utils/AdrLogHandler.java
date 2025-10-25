package utils;

import org.apache.commons.lang3.StringUtils;

public class AdrLogHandler {

    private String restBackendInitialLog;
    private String xdrTransformerInitialLog;
    private String authenticatorInitialLog;
    private String storageProviderInitialLog;
    private String queueHandlerInitialLog;
    private String smdHandlerInitialLog;
    private String compactorInitialLog;
    private String xdrSchedulerInitialLog;

    public AdrLogHandler() {
        if (!AutoUtils.isRunningOnMacos() && !AutoUtils.isRunningOnGCP()) {
            AerospikeLogger.info("Initializing all AdrLogHandler logs");
            initRestBackendLog();
            initCompactorLog();
            initAuthenticatorLog();
            initSmdHandlerLog();
            initQueueHandlerLog();
            initXdrSchedulerLog();
            initXdrTransformerLog();
            initStorageProviderLog();
        }
    }

    public void printAllLogs() {
        if (!AutoUtils.isRunningOnMacos() && !AutoUtils.isRunningOnGCP()) {
            AerospikeLogger.info("Print only the logs that occurred during this test suite");
            String log = getRestBackendLog();
            if (!log.equals(""))
                AerospikeLogger.info("Rest Backend Log: \n" + log);
            log = getCompactorLog();
            if (!log.equals(""))
                AerospikeLogger.info("Compactor Log: \n" + log);
            log = getAuthenticatorLog();
            if (!log.equals(""))
                AerospikeLogger.info("Authenticator Log: \n" + log);
            log = getSmdHandlerLog();
            if (!log.equals(""))
                AerospikeLogger.info("Smd Handler Log: \n" + log);
            log = getQueueHandlerLog();
            if (!log.equals(""))
                AerospikeLogger.info("Queue Handler Log: \n" + log);
            log = getXdrSchedulerLog();
            if (!log.equals(""))
                AerospikeLogger.info("XDR Scheduler Log: \n" + log);
            log = getXdrTransformerLog();
            if (!log.equals(""))
                AerospikeLogger.info("XDR Transformer Log: \n" + log);
            log = getStorageProviderLog();
            if (!log.equals(""))
                AerospikeLogger.info("Storage Provider Log: \n" + log);
        }
    }

    public String getRestBackendLog() {
        String currentLog = AutoUtils.runBashCommand("docker logs adr-rest-backend", false);
        return StringUtils.substringAfter(currentLog, restBackendInitialLog);
    }

    public String getXdrTransformerLog() {
        String currentLog = AutoUtils.runBashCommand("docker logs adr-xdr-transformer", false);
        return StringUtils.substringAfter(currentLog, xdrTransformerInitialLog);
    }

    public String getAuthenticatorLog() {
        String currentLog = AutoUtils.runBashCommand("docker logs adr-authenticator", false);
        return StringUtils.substringAfter(currentLog, authenticatorInitialLog);
    }

    public String getStorageProviderLog() {
        String currentLog = AutoUtils.runBashCommand("docker logs adr-storage-provider", false);
        return StringUtils.substringAfter(currentLog, storageProviderInitialLog);
    }

    public String getQueueHandlerLog() {
        String currentLog = AutoUtils.runBashCommand("docker logs adr-queue-handler", false);
        return StringUtils.substringAfter(currentLog, queueHandlerInitialLog);
    }

    public String getSmdHandlerLog() {
        String currentLog = AutoUtils.runBashCommand("docker logs adr-smd-handler", false);
        return StringUtils.substringAfter(currentLog, smdHandlerInitialLog);
    }

    public String getCompactorLog() {
        String currentLog = AutoUtils.runBashCommand("docker logs adr-compactor", false);
        return StringUtils.substringAfter(currentLog, compactorInitialLog);
    }

    public String getXdrSchedulerLog() {
        String currentLog = AutoUtils.runBashCommand("docker logs adr-xdr-scheduler", false);
        return StringUtils.substringAfter(currentLog, xdrSchedulerInitialLog);
    }

    public void initRestBackendLog() {
        restBackendInitialLog = AutoUtils.runBashCommand("docker logs --tail 30 adr-rest-backend", false);
    }

    public void initXdrTransformerLog() {
        xdrTransformerInitialLog = AutoUtils.runBashCommand("docker logs --tail 30 adr-xdr-transformer", false);
    }

    public void initAuthenticatorLog() {
        authenticatorInitialLog = AutoUtils.runBashCommand("docker logs --tail 30 adr-authenticator", false);
    }

    public void initStorageProviderLog() {
        storageProviderInitialLog = AutoUtils.runBashCommand("docker logs --tail 30 adr-storage-provider", false);
    }

    public void initQueueHandlerLog() {
        queueHandlerInitialLog = AutoUtils.runBashCommand("docker logs --tail 30 adr-queue-handler", false);
    }

    public void initSmdHandlerLog() {
        smdHandlerInitialLog = AutoUtils.runBashCommand("docker logs --tail 30 adr-smd-handler", false);
    }

    public void initCompactorLog() {
        compactorInitialLog = AutoUtils.runBashCommand("docker logs --tail 30 adr-compactor", false);
    }

    public void initXdrSchedulerLog() {
        xdrSchedulerInitialLog = AutoUtils.runBashCommand("docker logs --tail 30 adr-xdr-scheduler", false);
    }
}