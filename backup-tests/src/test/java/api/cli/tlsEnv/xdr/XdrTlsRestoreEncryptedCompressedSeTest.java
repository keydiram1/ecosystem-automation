package api.cli.tlsEnv.xdr;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.*;
import utils.ASBench;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("BACKUP-TLS-ENV")
@Disabled
class XdrTlsRestoreEncryptedCompressedSeTest extends CliBackupRunner {
    private static final String STRING_BIN = "RestoreTestBin";
    private static final String SET_NAME = "SetFullBackupTest";
    private static Key KEY1;
    private static final String SOURCE_NAMESPACE = "source-ns4";

    private final int keys = 10_000;
    private final int recordSize = 500;
    private static final String DC = "EncryptedDC";
    private static final String BACKUP_DIR = "XdrTlsRestoreEncryptedCompressedSeTest";
    private static final int LOCAL_PORT = 8086;

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET_NAME, "IT1");
    }


    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void testEncryptionAndCompression128() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);
        ASBench.on(SOURCE_NAMESPACE, SET_NAME).keys(keys).recordSize(recordSize).run();
        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        BackupResult backupResult = CliBackup.onWithXdrTls(SOURCE_NAMESPACE, BACKUP_DIR,DC, LOCAL_PORT)
                .setSecretAgent()
                .setEncryptionKeySecret("secrets:encKey:encryption-key-pem").setEncryptionMode("aes128").setCompressMode("ZSTD").setCompressLevel(5)
                .runWithTls();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        assertThat(backupResult.getBytesWritten())
                .as("compressed size should be less then uncompressed")
                .isLessThan(keys * recordSize);

        CliRestore.onWithTls(SOURCE_NAMESPACE, backupResult.getBackupDir()).setSecretAgent()
                .setEncryptionKeySecret("secrets:encKey:encryption-key-pem").setEncryptionMode("aes128").setCompressMode("ZSTD")
                .run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);
    }

    @Test
    void testEncryptionAndCompression256() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);
        ASBench.on(SOURCE_NAMESPACE, SET_NAME).keys(keys).recordSize(recordSize).run();
        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        BackupResult backupResult = CliBackup.onWithXdrTls(SOURCE_NAMESPACE, BACKUP_DIR,DC, LOCAL_PORT)
                .setSecretAgent()
                .setEncryptionKeySecret("secrets:encKey:encryption-key-pem").setEncryptionMode("aes256").setCompressMode("ZSTD").setCompressLevel(10)
                .runWithTls();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        assertThat(backupResult.getBytesWritten())
                .as("compressed size should be less then uncompressed")
                .isLessThan(keys * recordSize);

        CliRestore.onWithTls(SOURCE_NAMESPACE, backupResult.getBackupDir()).setSecretAgent()
                .setEncryptionKeySecret("secrets:encKey:encryption-key-pem").setEncryptionMode("aes256").setCompressMode("ZSTD")
                .run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);
    }
}