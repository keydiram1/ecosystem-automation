package api.cli.end2end.xdr;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.AerospikeScanner;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("XDR-CLI-BACKUP")
class XdrRestoreOptionsTest extends CliBackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns10";
    private static final String SET1 = "setXdrOptions";
    private static Key KEY1;
    private static final String BACKUP_DIR = "XdrRestoreOptionsTest";
    private static int numberOfRecordsToBackup;
    private static final int LOCAL_PORT = 8090;
    private static final String DC = "DcXdrRestoreOptionsTest";
    private static Key KEY2;
    private static final String SET2 = "Set2";
    private static final String STRING_BIN = "xdrRestore";
    private static Key KEY3;
    private static final String SET3 = "SetFullBackupTest3";

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
        KEY2 = new Key(SOURCE_NAMESPACE, SET2, "IT2");
        KEY3 = new Key(SOURCE_NAMESPACE, SET3, "IT3");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void recordsPerSecond() {
        ASBench.on(SOURCE_NAMESPACE, SET1).keys(10).recordSize(1_000_000).run();

        numberOfRecordsToBackup = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsToBackup).isEqualTo(10);

        AutoUtils.sleep(3000);
        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR, DC, LOCAL_PORT).run();

        long startTime = System.currentTimeMillis();
        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).setRecordsPerSecond(1).run();
        long duration = System.currentTimeMillis() - startTime;

        AerospikeLogger.info("Restore for " + numberOfRecordsToBackup + " records took " + duration + " milliseconds");
        AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);

        assertThat(duration / 1000).isGreaterThan(8).isLessThan(12);
    }

    @Test
    void xdrRestoreDoublePrecisionTest() {
        AerospikeDataUtils.put(KEY1, STRING_BIN, 2.779745911202054e-161);
        AerospikeDataUtils.put(KEY2, STRING_BIN, 97.47637592329345);
        AerospikeDataUtils.put(KEY3, STRING_BIN, 0.05972567867873778);

        String backupKey = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR, DC, LOCAL_PORT).run().getBackupDir();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupKey).run();

        assertThat(srcClient.get(null, KEY1).getDouble(STRING_BIN)).isEqualTo(2.779745911202054e-161);
        assertThat(srcClient.get(null, KEY2).getDouble(STRING_BIN)).isEqualTo(97.47637592329345);
        assertThat(srcClient.get(null, KEY3).getDouble(STRING_BIN)).isEqualTo(0.05972567867873778);
    }

    @Test
    void restoreUserKey() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        WritePolicy writePolicy = new WritePolicy();
        writePolicy.setSendKey(true);
        AerospikeDataUtils.put(writePolicy, KEY1, STRING_BIN, firstValueCreate);
        AerospikeScanner scanner = new AerospikeScanner();
        scanner.scanKeys(srcClient, SOURCE_NAMESPACE, SET1);
        assertThat(scanner.getAllKeys().size()).isEqualTo(1);

        String backupKey = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR, DC, LOCAL_PORT).run().getBackupDir();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        scanner.scanKeys(srcClient, SOURCE_NAMESPACE, SET1);
        assertThat(scanner.getAllKeys().size()).isEqualTo(0);

        CliRestore.on(SOURCE_NAMESPACE, backupKey).run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);

        scanner.scanKeys(srcClient, SOURCE_NAMESPACE, SET1);
        assertThat(scanner.getAllKeys().size()).isEqualTo(1);
    }
}