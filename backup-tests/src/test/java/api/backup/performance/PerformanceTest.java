package api.backup.performance;

import api.backup.BackupManager;
import api.backup.RestoreApi;
import api.backup.dto.BackgroundJob;
import api.backup.dto.BackgroundJobPolicy;
import api.backup.dto.RestoreSetRequest;
import com.aerospike.client.IAerospikeClient;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ADR-PERFORMANCE")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PerformanceTest extends BackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns1";
    private static final String SOURCE_CLUSTER_NAME = "PerformanceSourceCluster";
    private static final String BACKUP_NAMESPACE = "adr-ns1";
    private static final String BACKUP_NAME = "PerformanceTestBackupName";
    private static final String POLICY_NAME = "PerformanceTestPolicy";
    private static final String DC_NAME = "PerformanceDC";

    private static final String SET_NAME = "setPerformanceTest";

    private static long beforeBackup;
    private static long afterBackup;
    private static long numRecordsInSourceAfterAddingData;
    private static final IAerospikeClient srcClient = AerospikeDataUtils.getSourceClient();
    private static final IAerospikeClient backupClient = AerospikeDataUtils.getBackupClient();
    private static int asBenchDurationInSeconds = 10;
    private static long waitForBackupLoopCount = 30;
    private static long minutesToWaitForRestore = 15;
    private static final int secondsToSleepInBackupLoop = 6;

    @AfterAll
    public static void afterAll() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @BeforeAll
    static void setUp() {
        setPerformanceVariables();
        AerospikeLogger.info("Seconds to wait for backup: " + waitForBackupLoopCount * secondsToSleepInBackupLoop);
        AerospikeLogger.info("Minutes to wait for restore: " + minutesToWaitForRestore);

        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        AutoUtils.sleep(10000);
        beforeBackup = System.currentTimeMillis();
        ASBench.on(SOURCE_NAMESPACE, SET_NAME).duration(asBenchDurationInSeconds).run();
        numRecordsInSourceAfterAddingData = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);
    }

    @Test
    @Order(1)
    void createBackupWithInitialSync0() {
        int backupDurationSeconds = 0;
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);

        AerospikeLogger.info("Records count in source: " + numRecordsInSourceAfterAddingData);
        for (int i = 0; i < waitForBackupLoopCount; i++) {
            backupDurationSeconds = i;
            AutoUtils.sleep(secondsToSleepInBackupLoop * 1000L);
            AerospikeLogger.info("Waited " + backupDurationSeconds * secondsToSleepInBackupLoop + " seconds");
            long objectCountInBackup = AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME, BACKUP_NAMESPACE);
            AerospikeLogger.info("Records count in backup: " + objectCountInBackup);
            AerospikeLogger.info("Records count in source after adding data: " + numRecordsInSourceAfterAddingData);
            if (objectCountInBackup == numRecordsInSourceAfterAddingData)
                break;
        }
        afterBackup = System.currentTimeMillis();
        AerospikeLogger.info("Backup took " + backupDurationSeconds + " seconds");
        assertThat(AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME, BACKUP_NAMESPACE))
                .isEqualTo(numRecordsInSourceAfterAddingData);
    }

    @Test
    @Order(2)
    void restoreSet() {
        AerospikeDataUtils.truncateSourceSet(SOURCE_NAMESPACE, SET_NAME);
        long numRecordsInSourceAfterTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);
        assertThat(numRecordsInSourceAfterTruncate).isZero();

        long restored = RestoreApi.restoreSet(RestoreSetRequest.builder().fromTime(beforeBackup).toTime(afterBackup)
                .srcClusterName(SOURCE_CLUSTER_NAME).trgClusterName(SOURCE_CLUSTER_NAME).srcNS(SOURCE_NAMESPACE)
                .trgNS(SOURCE_NAMESPACE).set(SET_NAME).build(), minutesToWaitForRestore).getProcessed();

        AerospikeLogger.info("Restored records: " + restored);
        long numRecordsInSourceAfterRestore = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);
        assertThat(numRecordsInSourceAfterRestore).isEqualTo(numRecordsInSourceAfterAddingData);
    }

    private static void setPerformanceVariables() {
        if (System.getProperty("asbench_duration_seconds") != null) {
            if (System.getProperty("asbench_duration_seconds").equals("400")) {
                asBenchDurationInSeconds = 400;
                waitForBackupLoopCount = 2400;
                minutesToWaitForRestore = 120;
            }
        }
    }

    @Test
    @Order(3)
    @Disabled
    void restoreSetPerformanceTuning() {

        List<Pair<BackgroundJobPolicy, Duration>> results = new ArrayList<>();

        for (Integer batch : List.of(10, 50, 100, 200)) {
            for (Integer partition : List.of(64, 1024, 4096)) {
                for (Integer parallelism : List.of(1, 2, 4, 8)) {
                    BackgroundJobPolicy policy = new BackgroundJobPolicy(batch, partition, parallelism);
                    BackgroundJob restoreJob = RestoreApi.restoreSet(RestoreSetRequest.builder().fromTime(beforeBackup).toTime(afterBackup)
                            .srcClusterName(SOURCE_CLUSTER_NAME).trgClusterName(SOURCE_CLUSTER_NAME).srcNS(SOURCE_NAMESPACE)
                            .trgNS(SOURCE_NAMESPACE).set(SET_NAME).build(), minutesToWaitForRestore);
                    Duration duration = Duration.between(Instant.ofEpochMilli(restoreJob.getCreated()),
                                Instant.ofEpochMilli(restoreJob.getProcessed()));
                    AerospikeLogger.info("Test finished for " + policy + " in " + duration);
                    results.add(Pair.of(policy, duration));
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Restore results:\nduration,batch,partition,parallelism\n");
        results.stream().sorted(java.util.Map.Entry.comparingByValue())
                .forEach(r -> sb.append("%s,%d,%d,%d\n".formatted(r.getValue(),
                        r.getKey().getRecordsBatchSize(),
                        r.getKey().getPartitionFilter(),
                        r.getKey().getParallelism())));

        AerospikeLogger.info(sb);
    }

}
