package api.cli.end2end.scan;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import api.cli.RestoreResult;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

//@Tag("CLI-BACKUP")
class TransactionRestoreTest extends CliBackupRunner {
    private static final String STRING_BIN = "trxRestoreBin";
    private static final String SET1 = "SetTrxRestore";
    private static Key KEY1;
    private static final String SOURCE_NAMESPACE = "source-ns16";
    private static final String BACKUP_DIR = "TransactionRestoreTest";

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void transactionRestore() {
        int numberOfRecords = 4096;
        String firstValueCreate = "firstValueCreate";

        AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        List<Key> keys = AerospikeDataUtils.putTransactions(KEY1, STRING_BIN, firstValueCreate, numberOfRecords);
        AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);

        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).run();
        assertThat(backupResult.getRecordsRead()).isEqualTo(numberOfRecords);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(0);
        RestoreResult restoreResult = CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();
        assertThat(restoreResult.getInsertedRecords()).isEqualTo(numberOfRecords);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(numberOfRecords);


        for (int i = 0; i < numberOfRecords; i++) {
            Key key = keys.get(i);
            Record retrievedRecord = srcClient.get(null, key);

            assertThat(retrievedRecord).isNotNull();
            assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate + (i + 1));
        }
    }

   // @Test
    void backupInTheMiddleOfUncommittedTransaction() {
        int numberOfRecords = 6;
        String firstValueCreate = "firstValueCreate";

        new Thread(() -> {
            AerospikeDataUtils.putTransactions(KEY1, STRING_BIN, firstValueCreate, numberOfRecords, 1);
        }).start();

        BackupResult backupResult = null;
        for (int i = 0; i < numberOfRecords; i++) {
            AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
            backupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).run();
            if (backupResult.getRecordsRead() > 0)
                break;
        }

        assertThat(backupResult.getRecordsRead()).isBetween(0, numberOfRecords);
        assertThat(backupResult.getFilesWritten()).isEqualTo(0);
    }

    @Test
    void backupInTheMiddleOfCommitTransaction() {
        ASBench.on(SOURCE_NAMESPACE, SET1).duration(3).run();
        int numberOfRecordsWithoutTransaction = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        int numberOfRecordsInTransaction = 3;
        int numberOfTotalRecords = numberOfRecordsWithoutTransaction + numberOfRecordsInTransaction;
        String firstValueCreate = "firstValueCreate";

        new Thread(() -> {
            AerospikeDataUtils.putTransactions(KEY1, STRING_BIN, firstValueCreate, numberOfRecordsInTransaction, 1);
        }).start();
        AutoUtils.sleep(1100);
        int numberOfRecordsBeforeStartingBackup = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsBeforeStartingBackup)
                .isLessThan(numberOfTotalRecords)
                .isGreaterThan(numberOfTotalRecords - numberOfRecordsInTransaction);

        long start = System.nanoTime();
        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setBandwidth(1).run();
        double backupDurationSeconds = (System.nanoTime() - start) / 1_000_000_000.0;
        AerospikeLogger.info("Backup duration: " + backupDurationSeconds + "s");

        assertThat(backupDurationSeconds).isGreaterThan(3.0);

        assertThat(backupResult.getRecordsRead()).isLessThan(numberOfTotalRecords);
        assertThat(backupResult.getFilesWritten()).isGreaterThan(0);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(0);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        AerospikeLogger.info("numberOfRecordsInTransaction=" + numberOfRecordsInTransaction);
        AerospikeLogger.info("numberOfTotalRecords=" + numberOfTotalRecords);
        AerospikeLogger.info("numberOfRecordsBeforeStartingBackup=" + numberOfRecordsBeforeStartingBackup);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsWithoutTransaction);
    }

    @Test
    void restoreInTheMiddleOfUncommittedTransaction() {
        ASBench.on(SOURCE_NAMESPACE, SET1).duration(1).run();
        AutoUtils.sleep(3000);
        int numberOfRecordsWithoutTransaction = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        int numberOfRecords = 10;
        String firstValueCreate = "firstValueCreate";

        new Thread(() -> {
            AerospikeDataUtils.putTransactions(KEY1, STRING_BIN, firstValueCreate, numberOfRecords, 1);
        }).start();

        AutoUtils.sleep(1500);
        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).run();

        int backupTotalRecords = backupResult.getRecordsRead();
        assertThat(backupTotalRecords > numberOfRecordsWithoutTransaction && backupTotalRecords < numberOfRecordsWithoutTransaction + numberOfRecords);

        // Need to check why we need this sleep. Without sleep the test gets stuck.
        AutoUtils.sleep(11_000);
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        RestoreResult restoreResult = CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();
        int numberOfRecordsInDbAfterRestore = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);

        assertThat(restoreResult.getInsertedRecords()).isLessThan(backupTotalRecords);
        AerospikeLogger.info("backupTotalRecords=" + backupTotalRecords);
        AerospikeLogger.info("numberOfRecordsInDbAfterRestore=" + numberOfRecordsInDbAfterRestore);
        AerospikeLogger.info("numberOfRecordsWithoutTransaction=" + numberOfRecordsWithoutTransaction);
        assertThat(numberOfRecordsInDbAfterRestore).isEqualTo(numberOfRecordsWithoutTransaction);
    }
}