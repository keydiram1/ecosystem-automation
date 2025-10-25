package api.cli.end2end.scan;

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

import static org.assertj.core.api.Assertions.assertThat;

@Tag("CLI-BACKUP")
class RestoreEncryptedCompressedTest extends CliBackupRunner {
    private static final String STRING_BIN = "RestoreTestBin";
    private static final String SET_NAME = "SetFullBackupTest";
    private static Key KEY1;
    private static String SOURCE_NAMESPACE = "source-ns7";

    private final int keys = 10_000;
    private final int recordSize = 500;

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

        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, "RestoreEncryptedCompressedTestBackup").
                setEncryptionKeyFile().setEncryptionMode("aes128").setCompressMode("ZSTD").setCompressLevel(10).run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        assertThat(backupResult.getBytesWritten())
                .as("compressed size should be less then uncompressed")
                .isLessThan(keys * recordSize);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).setEncryptionKeyFile().setEncryptionMode("aes128").setCompressMode("ZSTD").run();

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

        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, "RestoreEncryptedCompressedTestBackup").
                setEncryptionKeyFile().setEncryptionMode("aes256").setCompressMode("ZSTD").setCompressLevel(5).run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        assertThat(backupResult.getBytesWritten())
                .as("compressed size should be less then uncompressed")
                .isLessThan(keys * recordSize);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).setEncryptionKeyFile().setEncryptionMode("aes256").setCompressMode("ZSTD").run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);
    }

    @Test
    void fileLimitWithCompression() {
        int fileLimitInMb = 1;
        int fileLimitInBytes = fileLimitInMb * 1_000_000;
        int recordSize = 300;
        int keys = 100_000;

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        ASBench.on(SOURCE_NAMESPACE, SET_NAME).keys(keys).recordSize(recordSize).run();
        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(10_000);

        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, "BackupFileLimitCompressionCli", 1)
                .setCompressMode("zstd")
                .setCompressLevel(10)
                .setFileLimit(fileLimitInBytes)
                .run();

        AerospikeLogger.info("backup byteCount: " + backupResult.getBytesWritten());
        AerospikeLogger.info("backup fileCount: " + backupResult.getFilesWritten());

        long byteCount = backupResult.getBytesWritten();
        long fileCount = backupResult.getFilesWritten();

        long estimatedFileCountIfCompressionAfter = byteCount / fileLimitInBytes;
        AerospikeLogger.info("estimatedFileCountIfCompressionAfter: " + estimatedFileCountIfCompressionAfter);

        assertThat(fileCount)
                .as("File count should be close to what we expect based on compressed size and file-limit.")
                .isGreaterThan(estimatedFileCountIfCompressionAfter - 2)
                .isLessThan(estimatedFileCountIfCompressionAfter + 2);
    }
}