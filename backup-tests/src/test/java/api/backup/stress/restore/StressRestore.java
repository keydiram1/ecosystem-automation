package api.backup.stress.restore;

import api.backup.BackupManager;
import api.backup.RestoreApi;
import api.backup.dto.RestoreSetRequest;
import api.backup.stress.StressRunner;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.awaitility.Awaitility;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.ConfigParametersHandler;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.aerospike.AerospikeScanner;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class StressRestore extends StressRunner {

    private long minutesToWaitForRestore = 10;
    private Key randomKey;
    private Record randomRecordBeforeRestore;

    void setUpParent() {
        DC_NAME = "AdrDC";
        setPerformanceVariables();
        AerospikeLogger.info("asbench duration in seconds: " + asBenchDurationInSeconds);
        AerospikeLogger.info("Seconds to wait for backup: " + waitForBackupLoopCount * secondsToSleepInBackupLoop);
        AerospikeLogger.info("Minutes to wait for restore: " + minutesToWaitForRestore);
        AerospikeLogger.info("Minutes to wait for all stress classes to finish setup: "
                + minutesToWaitForAllStressClassesToFinishSetup);

        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        AutoUtils.sleep(20_000);
        ASBench.on(SOURCE_NAMESPACE, SET_NAME).keys(100000000).duration(asBenchDurationInSeconds).run();
        AerospikeScanner aerospikeScanner = new AerospikeScanner();
        aerospikeScanner.scan(srcClient, SOURCE_NAMESPACE, SET_NAME);
        randomKey = aerospikeScanner.getRandomKey();
        randomRecordBeforeRestore = srcClient.get(null, randomKey);
    }

    void createBackupWithInitialSync0Parent() {
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME,
                new ArrayList<>(Collections.singletonList(SET_NAME)));

        waitForBackup();

        waitGroup.wait(minutesToWaitForAllStressClassesToFinishSetup); // wait for all tests to finish setup
    }

    void restoreSetParent() {
        AutoUtils.sleep(20_000);
        long afterBackup = System.currentTimeMillis();

        AerospikeDataUtils.truncateSourceSet(SOURCE_NAMESPACE, SET_NAME);
        Awaitility.waitAtMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(5)).until(() ->
                        AerospikeCountUtils.isSetEmpty(srcClient, SOURCE_NAMESPACE, SET_NAME));

        long restored = RestoreApi.restoreSet(RestoreSetRequest.builder().fromTime(0).toTime(afterBackup)
                .srcClusterName(SOURCE_CLUSTER_NAME).trgClusterName(SOURCE_CLUSTER_NAME).srcNS(SOURCE_NAMESPACE)
                .trgNS(SOURCE_NAMESPACE).set(SET_NAME).build(), minutesToWaitForRestore).getProcessed();

        AerospikeLogger.info("Restored records: " + restored);
        AutoUtils.sleep(10_000);
        long numRecordsInSourceAfterRestore = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);
        AerospikeLogger.info("The number of records after restore " + numRecordsInSourceAfterRestore);
        assertThat(numRecordsInSourceAfterRestore).isEqualTo(numRecordsInSourceAfterAddingData);

        Record recordAfterRestore = srcClient.get(null, randomKey);
        AerospikeLogger.info("Record before restore: " + randomRecordBeforeRestore.getLong("testbin"));
        AerospikeLogger.info("Record after restore: " + recordAfterRestore.getLong("testbin"));
        assertThat(randomRecordBeforeRestore.getLong("testbin")).isEqualTo(recordAfterRestore.getLong("testbin"));
    }

    private void setPerformanceVariables() {
        asBenchDurationInSeconds = 40;
        waitForBackupLoopCount = 50;
        if (ConfigParametersHandler.getParameter("asbench_duration_seconds") != null) {
            if (ConfigParametersHandler.getParameter("asbench_duration_seconds").equals("200")) {
                asBenchDurationInSeconds = 200;
                waitForBackupLoopCount = 300;
                minutesToWaitForRestore = 100;
                minutesToWaitForAllStressClassesToFinishSetup = 30;
            }
        }
    }
}
