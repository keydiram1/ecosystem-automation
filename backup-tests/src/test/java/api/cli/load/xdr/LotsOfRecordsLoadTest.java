package api.cli.load.xdr;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import api.cli.RestoreResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("CLI-XDR-LOAD-TEST")
class LotsOfRecordsLoadTest extends CliBackupRunner {
    private static final String SET1 = "SetLoad";
    private static final String SOURCE_NAMESPACE = "source-ns6";

    @BeforeAll
    static void setUp() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        AerospikeDataUtils.createBigData(SOURCE_NAMESPACE, SET1, 10);
        AutoUtils.sleep(10_000);
    }


    @Test
    void lotsOfRecordsLoadTest() {
        int numberOfRecordsBefore = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        long dataSizeBeforeBackup = AerospikeDataUtils.getDataTotalBytes(SOURCE_NAMESPACE);
        AerospikeLogger.info("Data size before backup: " + dataSizeBeforeBackup);

        assertThat(numberOfRecordsBefore).isGreaterThan(8_000_000);
        long startTime = System.currentTimeMillis();
        BackupResult backupResult = CliBackup.onWithXdrTls(SOURCE_NAMESPACE, "LotsOfRecordsLoadTestDir", "lotsOfRecordsLoadTestDC", 8086)
                .setVerbose()
                .setMaxConnections(1000)
                .setMaxThroughput(1_000_000)
                .run();
        long duration = System.currentTimeMillis() - startTime;

        //   assertThat(backupResult.getRecordsRead()).isEqualTo(numberOfRecordsBefore);
        AerospikeLogger.info("Backup duration in seconds: " + duration / 1000);
        //  assertThat(duration / 1000).isLessThan(200);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        startTime = System.currentTimeMillis();
        RestoreResult restoreResult = CliRestore.onWithXdrTls(SOURCE_NAMESPACE, backupResult.getBackupDir())
                .setUnique()
                .setSocketTimeout(0)
                .run();
        long restoreDurationParallel8 = System.currentTimeMillis() - startTime;

        AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);

        AerospikeLogger.info("Restore duration in seconds with parallel=8: " + restoreDurationParallel8 / 1000);
        assertThat(restoreResult.getInsertedRecords()).isEqualTo(numberOfRecordsBefore);
        assertThat(restoreDurationParallel8 / 1000).isLessThan(200);
    }
}