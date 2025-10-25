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
import utils.AutoUtils;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("XDR-CLI-BACKUP")
class XdrFastRestoreTest extends CliBackupRunner {
    private static final String STRING_BIN = "fastRestoreBin";
    private static final String SET1 = "SetFirstRestore";
    private static Key KEY1;
    private static final String SOURCE_NAMESPACE = "source-ns1";
    private static final String DC = "DcFirstXdrRestoreTest";
    private static final String BACKUP_DIR = "FirstXdrRestoreTestDir";
    private static final int LOCAL_PORT = 8083;

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void fastXdrRestore() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalAddress()
                .setDc(DC)
                .setLocalPort(LOCAL_PORT)
                .run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();
        AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
    }

    @Test
    void fastXdrTransactionRestore() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();

        AerospikeDataUtils.putTransaction(KEY1, STRING_BIN, firstValueCreate);

        AutoUtils.sleep(3000);
        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalAddress()
                .setDc(DC)
                .setLocalPort(LOCAL_PORT)
                .run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();
        AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
    }
}