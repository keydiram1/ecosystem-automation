package api.abs;

import java.util.Map;

public record Metrics(long backupServiceDurationMillis,
                      long backupServiceFailureTotal,
                      long incrementalBackupDurationMillis,
                      long incrementalBackupFailureTotal,
                      long incrementalBackupRunsTotal,
                      long incrementalBackupSkipTotal,
                      long backupRunsTotal,
                      long backupSkipTotal,
                      long goGoroutines,
                      long malloc,
                      long goThreads,
                      long promhttpMetricHandlerRequestsInFlight,
                      long promhttpMetricHandlerRequestsTotal,
                      Map<String, BackupProgress> backupProgress,
                      long restoreInProgress
) {
}

enum BackupType {
    FULL, INCREMENTAL;

    public static BackupType fromString(String type) {
        return switch (type.toLowerCase()) {
            case "full" -> FULL;
            case "incremental" -> INCREMENTAL;
            default -> throw new IllegalArgumentException("Unknown backup type: " + type);
        };
    }
}