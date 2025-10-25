package api.backup.longtime;

import api.backup.BackupApi;
import api.backup.BackupManager;
import com.aerospike.client.IAerospikeClient;
import org.junit.jupiter.api.BeforeAll;
import utils.*;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.adr.AerospikeDataUtils;

import java.util.ArrayList;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class LongTimeBackup {

    private static final PhaserWaitGroup waitGroup = PhaserWaitGroup.singleton("long-time");

    private final int secondsToSleepInBackupLoop = 6;
    private final IAerospikeClient srcClient = AerospikeDataUtils.getSourceClient();
    private final IAerospikeClient backupClient = AerospikeDataUtils.getBackupClient();

    protected String SOURCE_NAMESPACE = "source-ns1";
    protected String SOURCE_CLUSTER_NAME = "SourceClusterTest";
    protected String BACKUP_NAMESPACE = "adr-ns1";
    protected String BACKUP_NAME = "StressBackupTestBackupName";
    protected String POLICY_NAME = "StressBackupTestPolicy";
    protected final String DC_NAME = "LongTimeBackupDC";
    protected String SET_NAME = "setStressBackupTest";

    private String testDuration;
    private long initialThroughput;
    private final long waitForBackupLoopCount = 15;
    private static int loopCount;

    @BeforeAll
    public static void register() {
        waitGroup.register();
    }

    void setUpParent() {
        int minutesToWaitForAllLongTimeClassesToFinishSetup = 10;
        setPerformanceVariables();
        AerospikeLogger.info("Test duration is: " + testDuration);
        AerospikeLogger.info("Seconds to wait for backup: " + waitForBackupLoopCount * secondsToSleepInBackupLoop);
        AerospikeLogger.info("Minutes to wait For all long time classes to finish setup: "
                + minutesToWaitForAllLongTimeClassesToFinishSetup);

        if (ConfigParametersHandler.getParameter("recreate_adr_entities").equals("true")) {
            AerospikeLogger.info("Recreating ADR entities");
            BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
            AutoUtils.sleep(20_000);
            BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME,
                    DC_NAME, new ArrayList<>(Collections.singletonList(SET_NAME)));
        } else
            AerospikeLogger.info("Didn't recreate ADR entities");

        // If the test runs for the first time and recreate_adr_entities=false
        if (!BackupApi.isBackupExists(BACKUP_NAME)) {
            AerospikeLogger.info("Creating enabled backup for the first time");
            BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME,
                    DC_NAME, new ArrayList<>(Collections.singletonList(SET_NAME)));
        }

        waitGroup.wait(minutesToWaitForAllLongTimeClassesToFinishSetup); // wait for all tests to finish setup
    }

    void onGoingDataCreationParent() {
        ASBench.on(SOURCE_NAMESPACE, SET_NAME).duration(150 * loopCount).throughput(initialThroughput).keys(15_000_000).run();
        ASBench.on(SOURCE_NAMESPACE, SET_NAME).duration(150 * loopCount).throughput(initialThroughput * 2).keys(15_000_000).run();

        AutoUtils.sleep(5000);
        long numRecordsInSourceAfterAddingData = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);

        int backupDurationSeconds = 0;
        AerospikeLogger.info("Records count in source: " + numRecordsInSourceAfterAddingData);
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
        waitGroup.arrive();
        AerospikeLogger.info("Backup took " + backupDurationSeconds + " seconds");
        assertThat(AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME, BACKUP_NAMESPACE))
                .isEqualTo(numRecordsInSourceAfterAddingData);
    }

    void printProgressParent() {
        AerospikeLogger.info("Number of records in backup: "
                + AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME, BACKUP_NAMESPACE));
        AerospikeLogger.info("Number of records in source: "
                + AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE));
        AutoUtils.sleep(30000);
        while(waitGroup.numberOfArrivedParties.get() > 0){
            try {
                AutoUtils.sleep(300_000);
                AerospikeLogger.info("Number of records in backup: "
                        + AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME, BACKUP_NAMESPACE));
                AerospikeLogger.info("Number of records in source: "
                        + AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE));
            } catch (Exception e) {
                AerospikeLogger.info(e.getMessage());
            }
        }
    }

    private void setPerformanceVariables() {
        testDuration = ConfigParametersHandler.getParameter("test_duration");
        initialThroughput = Long.parseLong(ConfigParametersHandler.getParameter("on_going_initial_throughput"));
        if (testDuration.equals("short"))
            loopCount = 2;
        else
            loopCount = 300;
    }
}
