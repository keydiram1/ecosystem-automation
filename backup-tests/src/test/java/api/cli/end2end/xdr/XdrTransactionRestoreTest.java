package api.cli.end2end.xdr;

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
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.ConfigParametersHandler;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("XDR-CLI-BACKUP")
class XdrTransactionRestoreTest extends CliBackupRunner {
    private static final String STRING_BIN = "trxRestoreBin";
    private static final String SET1 = "SetTrxRestore";
    private static Key KEY1;
    private static final String SOURCE_NAMESPACE = "source-ns15";
    private static final String DC = "trxRestoreDC";
    private static final String BACKUP_DIR = "XdrTransactionRestoreTest";
    private static final int LOCAL_PORT = 8091;

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void xdrTransactionRestore() {
        int numberOfRecords = 10;
        String firstValueCreate = "firstValueCreate";

        List<Key> keys = AerospikeDataUtils.putTransactions(KEY1, STRING_BIN, firstValueCreate, numberOfRecords);

        AutoUtils.sleep(3000);

        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalAddress()
                .setDc(DC)
                .setLocalPort(LOCAL_PORT)
                .run();
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

    //@Test
    void xdrBackupInTheMiddleOfUncommittedTransaction() {
        int numberOfRecords = 20;
        String firstValueCreate = "firstValueCreate";

        new Thread(() -> {
            AerospikeDataUtils.putTransactions(KEY1, STRING_BIN, firstValueCreate, numberOfRecords, 1);
        }).start();

        BackupResult backupResult = null;
        for (int i = 0; i < numberOfRecords; i++) {
            AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
            backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                    .setLocalAddress()
                    .setDc(DC)
                    .setLocalPort(LOCAL_PORT)
                    .run();
            if (backupResult.getRecordsRead() > 0)
                break;
        }

        assertThat(backupResult.getRecordsRead()).isGreaterThan(0);
        assertThat(backupResult.getFilesWritten()).isEqualTo(0);
    }

    @Test
    void xdrBackupInTheMiddleOfCommitTransaction() {
        int numberOfRecords = 3;
        String firstValueCreate = "firstValueCreate";

        new Thread(() -> {
            AerospikeDataUtils.putTransactions(KEY1, STRING_BIN, firstValueCreate, numberOfRecords, 1);
        }).start();

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isLessThan(numberOfRecords);

        long start = System.nanoTime();

        CliBackup cliBackup = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalAddress()
                .setDc(DC)
                .setLocalPort(LOCAL_PORT);
        if (ConfigParametersHandler.getParameter("IS_RUNNING_ON_LOCAL_3_NODES_ENV").equals("true"))
            cliBackup.setMaxConnections(15);
        else
            cliBackup.setMaxConnections(5);

        BackupResult backupResult = cliBackup.run();

        AerospikeLogger.info("Backup duration: " + (System.nanoTime() - start) / 1_000_000_000.0 + "s");

        assertThat(backupResult.getRecordsRead()).isLessThan(numberOfRecords);
        assertThat(backupResult.getFilesWritten()).isGreaterThan(0);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(0);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(numberOfRecords);
    }

    @Test
    void xdrTransactionRestoreUpdatedValues() {
        int numberOfRecords = 10;
        String firstValueCreate = "firstValueCreate";

        AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);

        List<Key> keys = AerospikeDataUtils.putAndUpdateTransactions(KEY1, STRING_BIN, firstValueCreate, numberOfRecords);

        AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);

        AutoUtils.sleep(5000);

        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalAddress()
                .setDc(DC)
                .setLocalPort(LOCAL_PORT)
                .run();
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
            assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate + (i + 1) + 10); // Expect updated value
        }
    }
}