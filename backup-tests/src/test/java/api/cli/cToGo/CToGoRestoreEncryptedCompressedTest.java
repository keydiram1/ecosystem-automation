package api.cli.cToGo;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import utils.ASBench;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("CLI-BACKUP-C-TO-GO")
@Execution(ExecutionMode.SAME_THREAD)
class CToGoRestoreEncryptedCompressedTest extends CliBackupRunner {
    private static final String STRING_BIN = "RestoreTestBin";
    private static final String SET1 = "SetFullBackupTest1";
    private static Key KEY1;
    private static final String SOURCE_NAMESPACE = "source-ns1";

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void testEncryptionAndCompression128() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);
        ASBench.on(SOURCE_NAMESPACE, SET1).duration(1).run();
        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        String backupFilesPath =  CToGoUtils.runBackupByConfiguration(SOURCE_NAMESPACE, "zstd",
                20, "aes128");

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CToGoUtils.runRestoreByConfigurationWithEncryption(backupFilesPath, SOURCE_NAMESPACE, "zstd", "aes128");

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);
    }

    @Test
    void testEncryptionAndCompression256() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);
        ASBench.on(SOURCE_NAMESPACE, SET1).duration(1).run();
        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        String backupFilesPath =  CToGoUtils.runBackupByConfiguration(SOURCE_NAMESPACE, "zstd",
                20, "aes256");

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CToGoUtils.runRestoreByConfigurationWithEncryption(backupFilesPath, SOURCE_NAMESPACE, "zstd", "aes256");

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);
    }
}