package api.cli.performance;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import org.junit.jupiter.api.*;
import utils.AerospikeLogger;
import utils.ConfigParametersHandler;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("CLI-PERFORMANCE-TEST")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CliPerformanceTest extends CliBackupRunner {
    private static int numberOfRecordsBeforeTruncate;
    private static BackupResult backup;
    private static final String SET = "SetPerformanceTest";
    private static final String SOURCE_NAMESPACE = "source-ns1";
    private static long dataSizeBeforeTruncate;
    private static boolean createData;
    private static int numberOfRecordsInMillions;
    private static String recordType;
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


    @BeforeAll
    static void setUp() {
        createData = ConfigParametersHandler.getParameter("CREATE_DATA").equalsIgnoreCase("y");
        numberOfRecordsInMillions = Integer.parseInt(ConfigParametersHandler.getParameter("NUMBER_OF_RECORDS_IN_MILLIONS"));
        recordType = ConfigParametersHandler.getParameter("RECORD_SIZE_IN_BYTES");
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
            AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, SET, numberOfRecordsInMillions, "B1024");
        }

        numberOfRecordsBeforeTruncate = AerospikeCountUtils.getNamespaceObjectCount(srcClient, SOURCE_NAMESPACE);
        dataSizeBeforeTruncate = AerospikeDataUtils.getDataTotalBytes(SOURCE_NAMESPACE);
        AerospikeLogger.info("Data size before truncate: " + dataSizeBeforeTruncate);
        AerospikeLogger.info("Records in DB: " + numberOfRecordsBeforeTruncate);
    }

    @Test
    @Order(1)
    void runBackup() {
        Instant startTime = Instant.now();
        backup = CliBackup.on(SOURCE_NAMESPACE, "fastRestoreBackup", backupParallel)
                .setRecordsPerSecond(backupRecordsPerSecond)
                .setBandwidth(backupBandwidth)
                .setSocketTimeout(backupSocketTimeout)
                .setTotalTimeout(backupTotalTimeout)
                .run();

        Duration backupDuration = Duration.between(startTime, Instant.now());
        long durationInSeconds = backupDuration.toSeconds();

        long dataSize = AerospikeDataUtils.getDataTotalBytes(SOURCE_NAMESPACE);

        AerospikeLogger.info("Backup of " + numberOfRecordsBeforeTruncate + " records with record type=" + recordType +
                " with total data size of " + dataSize +
                " with parameters: parallel=" + backupParallel +
                ", recordsPerSecond=" + backupRecordsPerSecond +
                ", bandwidth=" + backupBandwidth +
                ", socketTimeout=" + backupSocketTimeout +
                ", totalTimeout=" + backupTotalTimeout +
                ", backupFileLimit=" + backupFileLimit +
                " finished in " + durationInSeconds + " seconds");

        assertThat(backup.getRecordsRead()).isEqualTo(numberOfRecordsBeforeTruncate);
    }

    @Test
    @Order(2)
    void runRestore() {
        AerospikeLogger.info("The number of records before truncate: " + numberOfRecordsBeforeTruncate);

        Instant startTime = Instant.now();
        CliRestore.on(SOURCE_NAMESPACE, backup.getBackupDir())
                .setNoGeneration()
                .setBatchSize(restoreBatchSize)
                .setMaxAsyncBatches(restoreMaxAsyncBatches)
                .setParallel(restoreParallel)
                .setSocketTimeout(restoreSocketTimeout)
                .setTotalTimeout(restoreTotalTimeout)
                .run();


        Duration restoreDuration = Duration.between(startTime, Instant.now());
        long durationInSeconds = restoreDuration.toSeconds();

        long dataSize = AerospikeDataUtils.getDataTotalBytes(SOURCE_NAMESPACE);

        AerospikeLogger.info("Restore of " + numberOfRecordsBeforeTruncate + " records with record type=" + recordType +
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

        assertThat(AerospikeCountUtils.getNamespaceObjectCount(srcClient, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);
    }
}