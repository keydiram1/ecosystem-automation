package api.cli.load.scan;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import api.cli.RestoreResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AerospikeLogger;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("CLI-LOAD-TEST")
class LotsOfRecordsLoadTest extends CliBackupRunner {
    private static final String SET1 = "SetLoad";
    private static final String SOURCE_NAMESPACE = "source-ns6";

    @BeforeAll
    static void setUp() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, SET1, 10);
    }

    @Test
    void lotsOfRecordsLoadTest() {
        int numberOfRecordsBefore = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        long dataSizeBeforeBackup = AerospikeDataUtils.getDataTotalBytes(SOURCE_NAMESPACE);
        AerospikeLogger.info("Data size before backup: " + dataSizeBeforeBackup);

        assertThat(numberOfRecordsBefore).isGreaterThan(8_000_000);
        long startTime = System.currentTimeMillis();
        BackupResult backupResult = CliBackup.onWithTls(SOURCE_NAMESPACE, "LotsOfRecordsLoadTestDir")
                .setSocketTimeout(0)
                .setMaxRetries(100)
                .runWithTls();
        long duration = System.currentTimeMillis() - startTime;

        assertThat(backupResult.getRecordsRead()).isEqualTo(numberOfRecordsBefore);
        AerospikeLogger.info("Backup duration in seconds: " + duration / 1000);
        assertThat(duration / 1000).isLessThan(100);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        startTime = System.currentTimeMillis();
        RestoreResult restoreResult = CliRestore.onWithTls(SOURCE_NAMESPACE, backupResult.getBackupDir(), 1)
                .setSocketTimeout(0)
                .setUnique()
                .run();
        long restoreDurationParallel8 = System.currentTimeMillis() - startTime;

        assertThat(restoreResult.getInsertedRecords()).isEqualTo(numberOfRecordsBefore);
        AerospikeLogger.info("Restore duration in seconds with parallel=8: " + restoreDurationParallel8 / 1000);
        assertThat(restoreDurationParallel8 / 1000).isLessThan(200);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        startTime = System.currentTimeMillis();
        restoreResult = CliRestore.onWithTls(SOURCE_NAMESPACE, backupResult.getBackupDir(), 8)
                .setSocketTimeout(0)
                .setUnique()
                .setRetryMaxRetries(100)
                .run();
        long restoreDurationParallel1 = System.currentTimeMillis() - startTime;

        assertThat(restoreResult.getInsertedRecords()).isEqualTo(numberOfRecordsBefore);
        AerospikeLogger.info("Restore duration in seconds with parallel=1: " + restoreDurationParallel1 / 1000);
        assertThat(restoreDurationParallel1 / 1000).isLessThan(300);
        assertThat(restoreDurationParallel1).isGreaterThanOrEqualTo((long) (restoreDurationParallel8 * 1.2));

        long dataSizeAfterRestore = AerospikeDataUtils.getDataTotalBytes(SOURCE_NAMESPACE);
        AerospikeLogger.info("Data size after restore: " + dataSizeAfterRestore);

        assertThat(dataSizeAfterRestore)
                .as("Data size after restore should be equal to the size before taking a backup")
                .isEqualTo(dataSizeBeforeBackup);
    }
}