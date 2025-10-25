package api.cli.tlsEnv.scan;

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
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("BACKUP-TLS-ENV")
class ScanTlsRestoreTest extends CliBackupRunner {
    private static final String STRING_BIN = "fastRestoreBin";
    private static final String SET1 = "SetFastRestore";
    private static Key KEY1;
    private static final String SOURCE_NAMESPACE = "source-ns3";
    private static final String BACKUP_DIR = "FirstXdrRestoreTestDir";

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void fastRestore() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        BackupResult fastRestoreBackup = CliBackup.onWithTls(SOURCE_NAMESPACE, BACKUP_DIR).runWithTls();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.onWithXdrTls(SOURCE_NAMESPACE, fastRestoreBackup.getBackupDir()).run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
    }

    @Test
    void asbenchBackupRestore() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        ASBench.on(SOURCE_NAMESPACE, SET1).duration(1).run();
        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(1000);

        BackupResult fastRestoreBackup = CliBackup.onWithTls(SOURCE_NAMESPACE, BACKUP_DIR).runWithTls();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.onWithXdrTls(SOURCE_NAMESPACE, fastRestoreBackup.getBackupDir()).run();

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);
    }
}