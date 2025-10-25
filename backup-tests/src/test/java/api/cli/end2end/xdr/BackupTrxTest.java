package api.cli.end2end.xdr;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("XDR-CLI-BACKUP")
class BackupTrxTest extends CliBackupRunner {
    private static final String STRING_BIN = "trxRestoreBin";
    private static final String SET1 = "SetTrxRestore";
    private static Key KEY1;
    private static final String SOURCE_NAMESPACE = "source-ns12";
    private static final String BACKUP_DIR = "BackupTrxTest";
    private static final int NUMBER_OF_RECORDS_IN_TRANSACTION = 4096;
    private static final String DC = "DcBackupTrxTest";
    private static final int LOCAL_PORT = 8092;

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void xdrParallelTransactionBackupRestore() throws ExecutionException, InterruptedException {
        ASBench.on(SOURCE_NAMESPACE, SET1).duration(60).keys(2_000_000_000).run();
        int numberOfRecordsInDbAfterAsbench = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);

        CompletableFuture<Void> putTransactionFuture = CompletableFuture.runAsync(() -> {
            String firstValueCreate = "firstValueCreate";
            long start = System.nanoTime();
            AerospikeLogger.info("Starting Transaction");
            AerospikeDataUtils.putTransactionsInUniquePartition(KEY1, STRING_BIN, firstValueCreate, NUMBER_OF_RECORDS_IN_TRANSACTION, 0);
            double backupDurationSeconds = (System.nanoTime() - start) / 1_000_000_000.0;
            AerospikeLogger.info("Transaction ended. Duration: " + backupDurationSeconds + "s");
            assertThat(backupDurationSeconds).isLessThan(10.0);
            AerospikeLogger.info("Transactions inserted successfully.");
        });

        CompletableFuture<BackupResult> backupFuture = CompletableFuture.supplyAsync(() -> {
            long start = System.nanoTime();
            AerospikeLogger.info("Starting Backup");
            BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                    .setLocalAddress()
                    .setDc(DC)
                    .setLocalPort(LOCAL_PORT)
                    .setParallelWrite(1)
                    .run();
            double backupDurationSeconds = (System.nanoTime() - start) / 1_000_000_000.0;
            AerospikeLogger.info("Backup ended. Duration: " + backupDurationSeconds + "s");
            assertThat(backupDurationSeconds).isGreaterThan(10.0);
            return backupResult;
        });

        putTransactionFuture.get();
        BackupResult backupResult = backupFuture.get();

        assertThat(backupResult.getRecordsRead()).isGreaterThan(numberOfRecordsInDbAfterAsbench);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(0);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE))
                .isEqualTo(numberOfRecordsInDbAfterAsbench + NUMBER_OF_RECORDS_IN_TRANSACTION);
    }

    @Test
    void scanParallelTransactionBackupRestore() throws ExecutionException, InterruptedException {
        ASBench.on(SOURCE_NAMESPACE, SET1).keys(25_000).recordSize(1000).run();
        int numberOfRecordsInDbAfterAsbench = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);

        CompletableFuture<Void> putTransactionFuture = CompletableFuture.runAsync(() -> {
            String firstValueCreate = "firstValueCreate";
            long start = System.nanoTime();
            AerospikeLogger.info("Starting Transaction");
            AerospikeDataUtils.putTransactionsInUniquePartition(KEY1, STRING_BIN, firstValueCreate, NUMBER_OF_RECORDS_IN_TRANSACTION, 0);
            double backupDurationSeconds = (System.nanoTime() - start) / 1_000_000_000.0;
            AerospikeLogger.info("Transaction ended. Duration: " + backupDurationSeconds + "s");
            assertThat(backupDurationSeconds).isLessThan(7.0);
            AerospikeLogger.info("Transactions inserted successfully.");
        });

        CompletableFuture<BackupResult> backupFuture = CompletableFuture.supplyAsync(() -> {
            long start = System.nanoTime();
            AerospikeLogger.info("Starting Backup");
            BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setBandwidth(1).run();
            double backupDurationSeconds = (System.nanoTime() - start) / 1_000_000_000.0;
            AerospikeLogger.info("Backup ended. Duration: " + backupDurationSeconds + "s");
            assertThat(backupDurationSeconds).isGreaterThan(7.0);
            return backupResult;
        });

        putTransactionFuture.get();
        BackupResult backupResult = backupFuture.get();

        assertThat(backupResult.getRecordsRead()).isGreaterThan(numberOfRecordsInDbAfterAsbench);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(0);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        AerospikeLogger.info("numberOfRecordsInDB=" + numberOfRecordsInDbAfterAsbench);
        AerospikeLogger.info("NUMBER_OF_RECORDS_IN_TRANSACTION=" + NUMBER_OF_RECORDS_IN_TRANSACTION);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE))
                .isEqualTo(numberOfRecordsInDbAfterAsbench);
    }

    @Test
    void largeTransactionBackupRestore() throws ExecutionException, InterruptedException {
        String largeValue = "X".repeat(1024 * 1024); // 1MB record

        Key bigRecordKey = AerospikeDataUtils.putTransactionsInUniquePartition(KEY1, STRING_BIN, largeValue, 1, 0).get(0);
        largeValue = largeValue + "0";

        int numberOfRecordsBeforeBackup = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);

        assertThat(numberOfRecordsBeforeBackup).isEqualTo(1);
        Record retrievedRecord = srcClient.get(null, bigRecordKey);
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(largeValue);

        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(0);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        retrievedRecord = srcClient.get(null, bigRecordKey);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(1);
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(largeValue);
    }

}