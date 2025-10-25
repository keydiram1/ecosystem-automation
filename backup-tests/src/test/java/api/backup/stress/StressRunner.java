package api.backup.stress;

import api.backup.BackupManager;
import com.aerospike.client.IAerospikeClient;
import org.junit.jupiter.api.BeforeAll;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.PhaserWaitGroup;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class StressRunner extends BackupRunner {

    protected static final PhaserWaitGroup waitGroup = new PhaserWaitGroup("stress");

    protected final IAerospikeClient srcClient = AerospikeDataUtils.getSourceClient();
    protected final IAerospikeClient backupClient = AerospikeDataUtils.getBackupClient();
    protected final int secondsToSleepInBackupLoop = 6;

    protected String SOURCE_NAMESPACE;
    protected String SOURCE_CLUSTER_NAME;
    protected String BACKUP_NAMESPACE;
    protected String BACKUP_NAME;
    protected String POLICY_NAME;
    protected String DC_NAME;
    protected String SET_NAME;

    protected long numRecordsInSourceAfterAddingData;
    protected int minutesToWaitForAllStressClassesToFinishSetup = 10;
    protected int asBenchDurationInSeconds;
    protected long waitForBackupLoopCount;

    @BeforeAll
    public static void register() {
        waitGroup.register();
    }

    protected void afterAllParent() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    protected void waitForBackup() {
        numRecordsInSourceAfterAddingData = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);
        int backupDurationSeconds = 0;
        for (int i = 0; i < waitForBackupLoopCount; i++) {
            backupDurationSeconds = i * secondsToSleepInBackupLoop;
            AutoUtils.sleep(secondsToSleepInBackupLoop * 1000L);
            AerospikeLogger.info("Waited " + backupDurationSeconds + " seconds");
            long objectCountInBackup = AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME, BACKUP_NAMESPACE);
            AerospikeLogger.info("Records count in backup: " + objectCountInBackup);
            AerospikeLogger.info("Records count in source after adding data: " + numRecordsInSourceAfterAddingData);
            if (objectCountInBackup == numRecordsInSourceAfterAddingData)
                break;
        }
        AerospikeLogger.info("Backup took " + backupDurationSeconds + " seconds");
        assertThat(AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME, BACKUP_NAMESPACE))
                .isEqualTo(numRecordsInSourceAfterAddingData);
    }
}
