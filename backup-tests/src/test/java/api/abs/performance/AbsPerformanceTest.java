package api.abs.performance;

import api.abs.AbsBackupApi;
import api.abs.AbsPolicyApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import api.abs.generated.ApiResponse;
import api.abs.generated.model.DtoBackupDetails;
import api.abs.generated.model.DtoBackupPolicy;
import api.abs.generated.model.DtoRestorePolicy;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.*;
import utils.AerospikeLogger;
import utils.ConfigParametersHandler;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.constants.AsDataTypes;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-PERFORMANCE-TEST")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AbsPerformanceTest extends AbsRunner {
    private static int numberOfRecordsBeforeTruncate;
    private static DtoBackupDetails backup;
    private static final String ROUTINE_NAME = "performanceTest";
    private static final String SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);
    private static long dataSizeBeforeTruncate;
    private static boolean createData;
    private static int numberOfRecordsInMillions;
    private static String dataType;
    private static boolean truncateData;
    private static int backupParallel;
    private static int backupRecordsPerSecond;
    private static int backupBandwidth;
    private static int backupSocketTimeout;
    private static int backupTotalTimeout;
    private static int backupFileLimit;

    private static int restoreParallel;
    private static int restoreMaxAsyncBatches;
    private static int restoreBatchSize;
    private static int restoreSocketTimeout;
    private static int restoreTotalTimeout;
    private static long maxBackupDuration;
    private static long maxRestoreDuration;

    // V3.2 baseline (max durations from two runs, before tolerance)
    private static final Map<String, List<Long>> V3_BASELINE_DURATIONS = Map.of(
            AsDataTypes.SCALAR_1KB, List.of(92L, 539L),
            AsDataTypes.COMPLEX_1KB, List.of(352L, 1157L),
            AsDataTypes.MIXED_1KB, List.of(264L, 1115L),
            AsDataTypes.SCALAR_3KB, List.of(286L, 1102L),
            AsDataTypes.COMPLEX_3KB, List.of(381L, 611L),
            AsDataTypes.MIXED_3KB, List.of(319L, 594L),
            AsDataTypes.SCALAR_100KB, List.of(404L, 357L),
            AsDataTypes.COMPLEX_100KB, List.of(589L, 461L),
            AsDataTypes.MIXED_100KB, List.of(364L, 291L)
    );
    private static final double TOLERANCE_FACTOR = 1.25; // Max allowed duration for backup and restore = baseline × tolerance (25% margin)

    @BeforeAll
    static void setUp() {
        createData = ConfigParametersHandler.getParameter("CREATE_DATA").equalsIgnoreCase("y");
        numberOfRecordsInMillions = Integer.parseInt(ConfigParametersHandler.getParameter("NUMBER_OF_RECORDS_IN_MILLIONS"));
        dataType = ConfigParametersHandler.getParameter("DATA_TYPE");
        truncateData = ConfigParametersHandler.getParameter("TRUNCATE_DATA").equalsIgnoreCase("y");

        backupParallel = Integer.parseInt(ConfigParametersHandler.getParameter("BACKUP_PARALLEL"));
        backupRecordsPerSecond = Integer.parseInt(ConfigParametersHandler.getParameter("BACKUP_RECORDS_PER_SECOND"));
        backupBandwidth = Integer.parseInt(ConfigParametersHandler.getParameter("BACKUP_BANDWIDTH"));
        backupSocketTimeout = Integer.parseInt(ConfigParametersHandler.getParameter("BACKUP_SOCKET_TIMEOUT"));
        backupTotalTimeout = Integer.parseInt(ConfigParametersHandler.getParameter("BACKUP_TOTAL_TIMEOUT"));
        backupFileLimit = Integer.parseInt(ConfigParametersHandler.getParameter("BACKUP_FILE_LIMIT"));

        restoreParallel = Integer.parseInt(ConfigParametersHandler.getParameter("RESTORE_PARALLEL"));
        restoreMaxAsyncBatches = Integer.parseInt(ConfigParametersHandler.getParameter("RESTORE_MAX_ASYNC_BATCHES"));
        restoreBatchSize = Integer.parseInt(ConfigParametersHandler.getParameter("RESTORE_BATCH_SIZE"));
        restoreSocketTimeout = Integer.parseInt(ConfigParametersHandler.getParameter("RESTORE_SOCKET_TIMEOUT"));
        restoreTotalTimeout = Integer.parseInt(ConfigParametersHandler.getParameter("RESTORE_TOTAL_TIMEOUT"));

        if (truncateData) {
            AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        }

        if (createData) {
            createPerformanceTestData();
        }

        numberOfRecordsBeforeTruncate = AerospikeCountUtils.getNamespaceObjectCount(srcClient, SOURCE_NAMESPACE);
        dataSizeBeforeTruncate = AerospikeDataUtils.getDataTotalBytes(SOURCE_NAMESPACE);
        AerospikeLogger.info("Data size before truncate: " + dataSizeBeforeTruncate);
        AerospikeLogger.info("Records in DB: " + numberOfRecordsBeforeTruncate);
    }

    @Test
    @Order(1)
    void runBackup() {
        updateBackupPolicy();

        Instant startTime = Instant.now();
        backup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME, Duration.ofMinutes(500));
        Duration backupDuration = Duration.between(startTime, Instant.now());
        long durationInSeconds = backupDuration.toSeconds();

        long dataSize = AerospikeDataUtils.getDataTotalBytes(SOURCE_NAMESPACE);

        AerospikeLogger.info("Backup of " + numberOfRecordsBeforeTruncate + " records with record size=" + dataType +
                " with total data size of " + dataSize +
                " with parameters: parallel=" + backupParallel +
                ", recordsPerSecond=" + backupRecordsPerSecond +
                ", bandwidth=" + backupBandwidth +
                ", socketTimeout=" + backupSocketTimeout +
                ", totalTimeout=" + backupTotalTimeout +
                ", backupFileLimit=" + backupFileLimit +
                " finished in " + durationInSeconds + " seconds");

        AerospikeLogger.info("Max backup duration: " + maxBackupDuration + "s, actual: " + durationInSeconds + "s (" + (maxBackupDuration - durationInSeconds) + "s under)");

        assertThat(backup.getRecordCount()).isEqualTo(numberOfRecordsBeforeTruncate);
        assertThat(durationInSeconds)
                .withFailMessage("Backup exceeded max expected duration of %d seconds", maxBackupDuration)
                .isLessThanOrEqualTo(maxBackupDuration);

    }

    @Test
    @Order(2)
    void runRestore() {
        AerospikeLogger.info("The number of records before truncate: " + numberOfRecordsBeforeTruncate);

        DtoRestorePolicy restorePolicy = new DtoRestorePolicy()
                .noGeneration(true)
                .batchSize(restoreBatchSize)
                .maxAsyncBatches(restoreMaxAsyncBatches)
                .parallel(restoreParallel)
                .socketTimeout(restoreSocketTimeout)
                .totalTimeout(restoreTotalTimeout);

        Instant startTime = Instant.now();
        AbsRestoreApi.restoreFullSync(backup.getKey(), ROUTINE_NAME, restorePolicy, Duration.ofHours(10));
        Duration restoreDuration = Duration.between(startTime, Instant.now());
        long durationInSeconds = restoreDuration.toSeconds();

        long dataSize = AerospikeDataUtils.getDataTotalBytes(SOURCE_NAMESPACE);

        AerospikeLogger.info("Restore of " + numberOfRecordsBeforeTruncate + " records with record size=" + dataType +
                " with total data size of " + dataSize +
                " with parameters: parallel=" + restoreParallel +
                ", maxAsyncBatches=" + restoreMaxAsyncBatches +
                ", batchSize=" + restoreBatchSize +
                ", socketTimeout=" + restoreSocketTimeout +
                ", totalTimeout=" + restoreTotalTimeout +
                " finished in " + durationInSeconds + " seconds");

        long dataSizeAfterRestore = AerospikeDataUtils.getDataTotalBytes(SOURCE_NAMESPACE);
        AerospikeLogger.info("Data size after restore: " + dataSizeAfterRestore);
        assertThat(dataSizeAfterRestore)
                .as("Data size after restore should be equal to the size before taking a backup")
                .isEqualTo(dataSizeBeforeTruncate);

        AerospikeLogger.info("Max restore duration: " + maxRestoreDuration + "s, actual: " + durationInSeconds + "s (" + (maxRestoreDuration - durationInSeconds) + "s under)");

        assertThat(AerospikeCountUtils.getNamespaceObjectCount(srcClient, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);
        assertThat(durationInSeconds)
                .withFailMessage("Restore exceeded max expected duration of %d seconds", maxRestoreDuration)
                .isLessThanOrEqualTo(maxRestoreDuration);
    }

    static void updateBackupPolicy() {
        DtoBackupPolicy performancePolicy = new DtoBackupPolicy()
                .parallel(backupParallel)
                .recordsPerSecond(backupRecordsPerSecond == 0 ? null : backupRecordsPerSecond)
                .bandwidth(backupBandwidth == 0 ? null : backupBandwidth)
                .socketTimeout(backupSocketTimeout)
                .totalTimeout(backupTotalTimeout)
                .sealed(true)
                .fileLimit(backupFileLimit);

        ApiResponse<Void> response = AbsPolicyApi.updatePolicy("defaultPolicy", performancePolicy);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
    }

    private static void createPerformanceTestData() {
        // Apply baseline with tolerance factor
        if (V3_BASELINE_DURATIONS.containsKey(dataType)) {
            List<Long> baseline = V3_BASELINE_DURATIONS.get(dataType);
            maxBackupDuration = (long) (baseline.get(0) * TOLERANCE_FACTOR);
            maxRestoreDuration = (long) (baseline.get(1) * TOLERANCE_FACTOR);
            AerospikeLogger.info("Max backup duration for " + dataType + ": " + maxBackupDuration + "s");
            AerospikeLogger.info("Max restore duration for " + dataType + ": " + maxRestoreDuration + "s");
        } else {
            AerospikeLogger.info("No baseline duration found for data type: " + dataType);
            maxBackupDuration = Long.MAX_VALUE;
            maxRestoreDuration = Long.MAX_VALUE;
        }

        double recordsPerType;

        switch (dataType) {
            case AsDataTypes.SCALAR_1KB:
                recordsPerType = numberOfRecordsInMillions / 3.0;
                AerospikeLogger.info("Creating records for " + dataType + " with type: I8");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "I8");

                AerospikeLogger.info("Creating records for " + dataType + " with type: S1024");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "S1024");

                AerospikeLogger.info("Creating records for " + dataType + " with type: D");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "D");
                break;

            case AsDataTypes.COMPLEX_1KB:
                recordsPerType = numberOfRecordsInMillions / 3.0;
                AerospikeLogger.info("Creating records for " + dataType + " with type: B1024");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "B1024");

                AerospikeLogger.info("Creating records for " + dataType + " with type: [256*S2]");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "[256*S2]");

                AerospikeLogger.info("Creating records for " + dataType + " with type: {70*S10:I4}");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "{70*S10:I4}");
                break;

            case AsDataTypes.MIXED_1KB:
                recordsPerType = numberOfRecordsInMillions / 6.0;
                AerospikeLogger.info("Creating records for " + dataType + " with type: I8");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "I8");

                AerospikeLogger.info("Creating records for " + dataType + " with type: S1024");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "S1024");

                AerospikeLogger.info("Creating records for " + dataType + " with type: D");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "D");

                AerospikeLogger.info("Creating records for " + dataType + " with type: B1024");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "B1024");

                AerospikeLogger.info("Creating records for " + dataType + " with type: [256*S2]");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "[256*S2]");

                AerospikeLogger.info("Creating records for " + dataType + " with type: {70*S10:I4}");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "{70*S10:I4}");
                break;

            case AsDataTypes.SCALAR_3KB:
                recordsPerType = numberOfRecordsInMillions / 3.0;
                AerospikeLogger.info("Creating records for " + dataType + " with type: I8");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "I8");

                AerospikeLogger.info("Creating records for " + dataType + " with type: S3072");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "S3072");

                AerospikeLogger.info("Creating records for " + dataType + " with type: D");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "D");
                break;

            case AsDataTypes.COMPLEX_3KB:
                recordsPerType = numberOfRecordsInMillions / 3.0;
                AerospikeLogger.info("Creating records for " + dataType + " with type: B3072");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "B3072");

                AerospikeLogger.info("Creating records for " + dataType + " with type: [768*S2]");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "[768*S2]");

                AerospikeLogger.info("Creating records for " + dataType + " with type: {210*S10:I4}");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "{210*S10:I4}");
                break;

            case AsDataTypes.MIXED_3KB:
                recordsPerType = numberOfRecordsInMillions / 6.0;
                AerospikeLogger.info("Creating records for " + dataType + " with type: I8");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "I8");

                AerospikeLogger.info("Creating records for " + dataType + " with type: S3072");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "S3072");

                AerospikeLogger.info("Creating records for " + dataType + " with type: D");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "D");

                AerospikeLogger.info("Creating records for " + dataType + " with type: B3072");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "B3072");

                AerospikeLogger.info("Creating records for " + dataType + " with type: [768*S2]");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "[768*S2]");

                AerospikeLogger.info("Creating records for " + dataType + " with type: {210*S10:I4}");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "{210*S10:I4}");
                break;

            case AsDataTypes.SCALAR_100KB:
                recordsPerType = numberOfRecordsInMillions / 3.0;
                AerospikeLogger.info("Creating records for " + dataType + " with type: I8");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "I8");

                AerospikeLogger.info("Creating records for " + dataType + " with type: S102400");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "S102400");

                AerospikeLogger.info("Creating records for " + dataType + " with type: D");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "D");
                break;

            case AsDataTypes.COMPLEX_100KB:
                recordsPerType = numberOfRecordsInMillions / 3.0;
                AerospikeLogger.info("Creating records for " + dataType + " with type: B102400");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "B102400");

                AerospikeLogger.info("Creating records for " + dataType + " with type: [25600*S2]");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "[25600*S2]");

                AerospikeLogger.info("Creating records for " + dataType + " with type: {7000*S10:I4}");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "{7000*S10:I4}");
                break;

            case AsDataTypes.MIXED_100KB:
                recordsPerType = numberOfRecordsInMillions / 6.0;
                AerospikeLogger.info("Creating records for " + dataType + " with type: I8");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "I8");

                AerospikeLogger.info("Creating records for " + dataType + " with type: S102400");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "S102400");

                AerospikeLogger.info("Creating records for " + dataType + " with type: D");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "D");

                AerospikeLogger.info("Creating records for " + dataType + " with type: B102400");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "B102400");

                AerospikeLogger.info("Creating records for " + dataType + " with type: [25600*S2]");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "[25600*S2]");

                AerospikeLogger.info("Creating records for " + dataType + " with type: {7000*S10:I4}");
                AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, dataType, recordsPerType, "{7000*S10:I4}");
                break;

            default:
                throw new IllegalArgumentException("Unsupported DATA_TYPE: " + dataType);
        }
    }
}