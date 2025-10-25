package api.abs.end2end;

import api.abs.*;
import api.abs.generated.model.*;
import org.assertj.core.data.Percentage;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static api.abs.PrometheusClient.prometheusClient;
import static api.abs.end2end.SlowBackupTest.assertMajorityWithinTolerance;
import static api.abs.end2end.SlowBackupTest.assertSummaryWithTolerance;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

@Tag("ABS-SEQUENTIAL-TESTS-2")
class RateLimitTest extends AbsRunner {
    private static final String SET = "SetRateLimitTest";
    private static final String ROUTINE_NAME = "fullBackup2";
    private static final String SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);
    private static final Map<String, DtoBackupRoutine> routines = AbsRoutineApi.getAllRoutines();

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void testRestoreWithLowBandwidthAndCurrentJobStatus() {
        int keys = 50_000;
        int recordSize = 2_000;
        ASBench.on(SOURCE_NAMESPACE, SET).keys(keys).recordSize(recordSize).run();
        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsBeforeTruncate).isEqualTo(keys);
        DtoBackupDetails backupDetails = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        assertThat(backupDetails.getByteCount()).isGreaterThan(keys * recordSize);
        String backupKey = backupDetails.getKey();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        int bandwidth = 10; // bandwidth in MiB/S
        DtoRestorePolicy policy = new DtoRestorePolicy().bandwidth(bandwidth);

        JobID jobID = AbsRestoreApi.restoreFull(backupKey, ROUTINE_NAME, policy);

        var initialRestoreStatus = Awaitility.await()
                .atMost(20, TimeUnit.SECONDS)
                .until(() -> AbsRestoreApi.getRestoreStatus(jobID).getCurrentJob(), job ->
                        job != null &&
                                job.getPercentageDone() != null &&
                                job.getPercentageDone() > 1
                );

        assertThat(prometheusClient.fetch().restoreInProgress())
                .as("At least one restore job is running")
                .isPositive();

        var previousPercentageDone = new AtomicReference<>(initialRestoreStatus.getPercentageDone());
        var previousDoneRecords = new AtomicReference<>(initialRestoreStatus.getDoneRecords());
        var previousEstimatedEndTime = new AtomicReference<>(Instant.parse(initialRestoreStatus.getEstimatedEndTime()));

        assertThat(numberOfRecordsBeforeTruncate)
                .isCloseTo(initialRestoreStatus.getTotalRecords(), Percentage.withPercentage(5)); // total records are calculated approximately

        Duration expectedDuration = Duration.ofMillis(1000 * backupDetails.getByteCount() / bandwidth / 1024 / 1024);

        List<DtoMetrics> metrics = new ArrayList<>();
        metrics.add(initialRestoreStatus.getMetrics());
        var restoreDoneStatus = Awaitility.await()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(2, TimeUnit.MINUTES)
                .until(() -> AbsRestoreApi.getRestoreStatus(jobID), currentRestoreStatus -> {
                    int currentPercentageDone = currentRestoreStatus.getCurrentJob().getPercentageDone();
                    int currentDoneRecords = currentRestoreStatus.getCurrentJob().getDoneRecords();
                    Instant currentEstimatedEndTime = Instant.parse(currentRestoreStatus.getCurrentJob().getEstimatedEndTime());

                    metrics.add(currentRestoreStatus.getCurrentJob().getMetrics());

                    // Assert that the percentage done and done records are increasing or stable
                    assertThat(currentPercentageDone).isGreaterThanOrEqualTo(previousPercentageDone.get());
                    assertThat(currentDoneRecords).isGreaterThanOrEqualTo(previousDoneRecords.get());

                    // Update previous values
                    previousPercentageDone.set(currentPercentageDone);
                    previousDoneRecords.set(currentDoneRecords);
                    previousEstimatedEndTime.set(currentEstimatedEndTime);

                    return currentRestoreStatus.getStatus() == DtoJobStatus.JobStatusDone;
                });

        // In ASB files we have overhead for headers and service info, like key, type, namespace, etc.
        // so record size in an ASB file will be more than `recordSize` in database.
        // So records size will be (backup bytes written / record count).
        long estimatedRPS = bandwidth * 1024 * 1024 / (backupDetails.getByteCount() / keys);

        assertMajorityWithinTolerance(metrics, DtoMetrics::getRecordsPerSecond, (int) estimatedRPS, "rps");
        assertSummaryWithTolerance(metrics, DtoMetrics::getKilobytesPerSecond, (int) (restoreDoneStatus.getTotalBytes() / 1024), "rps");


        Instant finishTime = Instant.parse(restoreDoneStatus.getCurrentJob().getFinishTime());
        Instant startTime = Instant.parse(restoreDoneStatus.getCurrentJob().getStartTime());
        Duration restoreDuration = Duration.between(startTime, finishTime);

        AerospikeLogger.info("Restore took " + restoreDuration.get(ChronoUnit.SECONDS) + " seconds");

        assertThat(restoreDuration).isCloseTo(expectedDuration, Duration.ofSeconds(10));
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);
    }

    @Test
    void testRestoreWithLowTPS() {
        final int TPC = 50;
        final int KEYS = 1500;
        ASBench.on(SOURCE_NAMESPACE, SET).batchSize(100).keys(KEYS).threads(1).run();
        AutoUtils.sleep(1000);
        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsBeforeTruncate).isEqualTo(KEYS);

        String backupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        DtoRestorePolicy policy = new DtoRestorePolicy()
                .noGeneration(true)
                .tps(TPC)
                .bandwidth(10000000);

        Instant startTime = Instant.now();
        AbsRestoreApi.restoreFullSync(backupKey, ROUTINE_NAME, policy);
        Duration restoreDuration = Duration.between(startTime, Instant.now());
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE))
                .as("All data restored")
                .isEqualTo(numberOfRecordsBeforeTruncate);

        AerospikeLogger.info("Restore took " + restoreDuration);
        int expectedDuration = KEYS / TPC;
        // For testing with multiple storage providers we need 70% tolerance factor
        assertThat(restoreDuration.toSeconds()).isCloseTo(expectedDuration, withinPercentage(70));
    }

    @Test
    void testRestoreWithAndWithoutLimiter() {
        var KEYS = 10_000;
        ASBench.on(SOURCE_NAMESPACE, SET).batchSize(100).keys(KEYS).threads(10).run();
        AutoUtils.sleep(1000);

        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsBeforeTruncate).isEqualTo(KEYS);

        DtoBackupDetails backup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);

        assertThat(backup.getRecordCount()).isEqualTo(numberOfRecordsBeforeTruncate);
        assertThat(backup.getNamespace()).isEqualTo(SOURCE_NAMESPACE);
        assertThat(backup.getByteCount()).isGreaterThan(1000);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        var startTime = Instant.now();
        DtoRestoreJobStatus restoreNoLimit = AbsRestoreApi.restoreFullSync(backup.getKey(), ROUTINE_NAME);
        Duration noLimitDuration = Duration.between(startTime, Instant.now());

        assertThat(restoreNoLimit.getReadRecords()).isEqualTo(KEYS);
        assertThat(restoreNoLimit.getTotalBytes()).isGreaterThan(1000);

        AerospikeLogger.info("Restore took " + noLimitDuration.toSeconds() + " seconds");
        assertThat(noLimitDuration.toSeconds()).isLessThan(30);
        Awaitility.await("Number of records after restore").untilAsserted(
                () -> assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE)).isEqualTo(KEYS)
        );

        DtoRestorePolicy singleThreadRestore = new DtoRestorePolicy().maxAsyncBatches(1).batchSize(1);
        startTime = Instant.now();
        DtoRestoreJobStatus restoreSingleThread = AbsRestoreApi.restoreFullSync(backup.getKey(), ROUTINE_NAME, singleThreadRestore);
        Duration singleThreadDuration = Duration.between(startTime, Instant.now());

        assertThat(singleThreadDuration).isGreaterThan(noLimitDuration);
        assertThat(restoreSingleThread.getTotalBytes()).isEqualTo(restoreNoLimit.getTotalBytes());
        assertThat(restoreSingleThread.getReadRecords()).isEqualTo(restoreNoLimit.getReadRecords());
    }

    @Test
    @DisabledIfSystemProperty(named = "qa_environment", matches = "GCP")
    void testFileLimit() {
        ASBench.on(SOURCE_NAMESPACE, SET).duration(10).run();
        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        DtoBackupDetails backupDetails = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        AbsRestoreApi.restoreFullSync(backupDetails.getKey(), ROUTINE_NAME);

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);

        int fileLimit = AbsPolicyApi.getPolicy(routines.get(ROUTINE_NAME).getBackupPolicy()).getFileLimit();
        AerospikeLogger.info("file limit is " + fileLimit + " mb");

        long byteCount = backupDetails.getByteCount();

        AerospikeLogger.info("backup byteCount: " + byteCount);
        AerospikeLogger.info("backup FileCount: " + backupDetails.getFileCount());

        assertThat(fileLimit).isEqualTo(1);
        assertThat(backupDetails.getByteCount()).isGreaterThan(2_000_000);
        long estimatedFileCount = byteCount / (fileLimit * 1_048_576L);
        AerospikeLogger.info("estimatedFileCount: " + estimatedFileCount);
        assertThat(backupDetails.getFileCount()).isBetween(estimatedFileCount, estimatedFileCount + 5);
    }

    @Test
    void disableBatchWrites() {
        int keys = 100_000;

        // Step 1: Generate data
        ASBench.on(SOURCE_NAMESPACE, SET)
                .keys(keys)
                .run();

        int recordsBefore = AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE);
        assertThat(recordsBefore).isEqualTo(keys);

        // Step 2: Backup
        String backupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();

        // Step 3: Restore with batch writes ENABLED
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        long batchedMillis = measureRestoreDurationWithRetries(backupKey, ROUTINE_NAME, new DtoRestorePolicy().parallel(1));

        int afterBatchedRestore = AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE);
        assertThat(afterBatchedRestore).isEqualTo(keys);

        // Step 4: Restore with batch writes DISABLED
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        DtoRestorePolicy noBatchPolicy = new DtoRestorePolicy().disableBatchWrites(true);

        // Restore of 100_000 records without batch write can cause SERVER_MEM_ERROR. We need to use measureRestoreDurationWithRetries.
        long noBatchMillis = measureRestoreDurationWithRetries(backupKey, ROUTINE_NAME, noBatchPolicy.parallel(1));

        int afterNoBatchRestore = AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE);
        assertThat(afterNoBatchRestore).isEqualTo(keys);

        // Step 5: Log and assert
        AerospikeLogger.info("Restore with batch writes: " + batchedMillis + " ms");
        AerospikeLogger.info("Restore with DISABLED batch writes: " + noBatchMillis + " ms");

        assertThat(noBatchMillis)
                .as("Restore without batch writes should be at least 1.3× slower")
                .isGreaterThan((long) (batchedMillis * 1.3));
    }

    private long measureRestoreDurationWithRetries(String backupKey, String routineName, DtoRestorePolicy policy) {
        final int maxAttempts = 3;
        int attempt = 1;
        while (true) {
            try {
                attempt++;
                JobID jobID = AbsRestoreApi.restoreFull(backupKey, routineName, policy);
                DtoRestoreJobStatus restore = AbsRestoreApi.waitForRestore(jobID);
                long started = AbsBackupApi.parseDate(restore.getCurrentJob().getStartTime());
                long finished = AbsBackupApi.parseDate(restore.getCurrentJob().getFinishTime());
                return finished - started;
            } catch (RuntimeException e) {
                AerospikeLogger.info("Restore attempt " + attempt + " failed: " + e.getMessage());
                if (attempt == maxAttempts) {
                    throw new RuntimeException("Restore failed after " + maxAttempts + " attempts", e);
                }
                AerospikeLogger.info("Retrying in 3 seconds...");
                AutoUtils.sleep(3000);
            }
        }
    }
}