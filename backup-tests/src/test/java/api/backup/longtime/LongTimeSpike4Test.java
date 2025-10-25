package api.backup.longtime;

import api.backup.BackupApi;
import api.backup.BackupManager;
import com.aerospike.client.IAerospikeClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.*;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import java.util.ArrayList;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ADR-LONG-TIME-TEST")
public class LongTimeSpike4Test extends BackupRunner {

    private static final PhaserWaitGroup waitGroup = PhaserWaitGroup.singleton("long-time");

    private static final int secondsToSleepInBackupLoop = 6;
    private final IAerospikeClient srcClient = AerospikeDataUtils.getSourceClient();
    private final IAerospikeClient backupClient = AerospikeDataUtils.getBackupClient();

    private static final String SOURCE_NAMESPACE = "source-ns4";
    private static final String SOURCE_CLUSTER_NAME = "SourceClusterLongTimeSpike4Test";
    private static final String BACKUP_NAMESPACE = "adr-ns4";
    private static final String BACKUP_NAME = "StressBackupLongTimeSpike4Test";
    private static final String POLICY_NAME = "LongTimeSpike4TestPolicy";
    private static final String DC_NAME = "LongTimeSpike4TestDC";
    private static final String SET_NAME = "setLongTimeSpike4Test";

    private static String testDuration;
    private static int spikeDuration;
    private static final long waitForBackupLoopCount = 15;

    @BeforeAll
    public static void setUp() {
        waitGroup.register();
        int minutesToWaitForAllLongTimeClassesToFinishSetup = 10;
        setVariables();
        AerospikeLogger.info("Test duration is: " + testDuration);
        AerospikeLogger.info("Seconds to wait for backup: " + waitForBackupLoopCount * secondsToSleepInBackupLoop);
        AerospikeLogger.info("Minutes to wait For all long time classes to finish setup: "
                + minutesToWaitForAllLongTimeClassesToFinishSetup);

        if (!BackupApi.isBackupExists(BACKUP_NAME)) {
            AerospikeLogger.info("Creating enabled backup for the first time");
            BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME,
                    DC_NAME, new ArrayList<>(Collections.singletonList(SET_NAME)));
        }

        if (ConfigParametersHandler.getParameter("recreate_adr_entities").equals("true")) {
            AerospikeLogger.info("Recreating ADR entities");
            BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
            AutoUtils.sleep(20_000);
            BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME,
                    DC_NAME, new ArrayList<>(Collections.singletonList(SET_NAME)));
        } else
            AerospikeLogger.info("Didn't recreate ADR entities");

        waitGroup.wait(minutesToWaitForAllLongTimeClassesToFinishSetup); // wait for all tests to finish setup
    }

    @Test
    void spikeDataCreation() {
        int loopCount = testDuration.equals("short") ? 2 : 300;
        for (int i = 0; i < loopCount; i++) {
            try {
                ASBench.on(SOURCE_NAMESPACE, SET_NAME).duration(spikeDuration).keys(15_000_000).run();
                AutoUtils.sleep(300_000);//sleep 5 minutes
                if (testDuration.equals("long"))
                    AutoUtils.sleep(3_300_000);//sleep 55 minutes
                AerospikeLogger.info("Number of records in backup: "
                        + AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME, BACKUP_NAMESPACE));
                AerospikeLogger.info("Number of records in source: "
                        + AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE));
            } catch (Exception e) {
                AerospikeLogger.info(e.getMessage());
            }
        }
        waitGroup.arrive();
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
        AerospikeLogger.info("Backup took " + backupDurationSeconds + " seconds");
        assertThat(AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME, BACKUP_NAMESPACE))
                .isEqualTo(numRecordsInSourceAfterAddingData);
    }

    private static void setVariables() {
        testDuration = ConfigParametersHandler.getParameter("test_duration");
        spikeDuration = Integer.parseInt(ConfigParametersHandler.getParameter("data_spikes_duration"));
    }
}
