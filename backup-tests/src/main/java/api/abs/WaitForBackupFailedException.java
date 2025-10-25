package api.abs;

import utils.AerospikeLogger;

public class WaitForBackupFailedException extends RuntimeException {
    public WaitForBackupFailedException(Exception cause) {
        super("Failed", cause);
        if (cause != null) {
            AerospikeLogger.info("Wait for backup failed due to: " + cause.getMessage());
        } else {
            AerospikeLogger.info("Wait for backup failed due to an unknown reason.");
        }
    }
}

