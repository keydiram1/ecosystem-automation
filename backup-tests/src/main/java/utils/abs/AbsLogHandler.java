package utils.abs;

import org.apache.commons.lang3.StringUtils;
import utils.AerospikeLogger;
import utils.AutoUtils;

import java.util.Arrays;

public class AbsLogHandler {

    private String backupServiceInitialLog;

    public AbsLogHandler() {
        AerospikeLogger.info("Initializing Backup Service logs");
        initBackupServiceLog();
    }

    public void printAllLogs() {
        if (!AutoUtils.isRunningOnMacos()) {
            AerospikeLogger.info("Print only the logs that occurred during this test suite");
            String log = getBackupServiceLog();
            if (!log.isEmpty()) {
                AerospikeLogger.info("Backup Service Log: \n" + log);
            }
        }
    }

    public String getBackupServiceLog() {
        String currentLog = AutoUtils.runBashCommand("docker logs backup-service", false);
        return StringUtils.substringAfter(currentLog, backupServiceInitialLog);
    }

    public void initBackupServiceLog() {
        String logs = AutoUtils.runBashCommand("docker logs --tail 30 backup-service", false);
        String[] lines = logs.split("\n");
        backupServiceInitialLog = (lines.length > 0 && "Exit Code: 0".equals(lines[lines.length - 1]))
                ? String.join("\n", Arrays.copyOf(lines, lines.length - 1))
                : logs;
    }
}