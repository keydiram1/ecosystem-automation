package api.abs.end2end.cToGo;

import api.abs.AbsRoutineApi;
import api.abs.generated.model.DtoCompressionPolicy;
import api.abs.generated.model.DtoEncryptionPolicy;
import api.abs.generated.model.DtoRestorePolicy;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-C-TO-GO")
@Execution(ExecutionMode.SAME_THREAD)
class CToGoRestoreEncryptedCompressedTest extends AbsRunner {
    private static final String STRING_BIN = "RestoreTestBin";
    private static final String SET1 = "SetFullBackupTest1";
    private static final String ROUTINE_NAME = "fullBackupEncryptedCompressed128";
    private static final String ROUTINE_NAME2 = "fullBackupEncryptedCompressed256";
    private static Key KEY1;
    private static Key KEY2;
    private static String SOURCE_NAMESPACE;
    private static String SOURCE_NAMESPACE2;

    @BeforeAll
    static void setUp() {
        SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");

        SOURCE_NAMESPACE2 = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME2);
        KEY2 = new Key(SOURCE_NAMESPACE2, SET1, "IT1");
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

        String backupFilesPath = CToGoUtils.runBackupByConfiguration(ROUTINE_NAME, SOURCE_NAMESPACE, "zstd",
                20, "aes128", "encryptionKey");

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        DtoEncryptionPolicy encryptionPolicy = new DtoEncryptionPolicy()
                .keyFile("/encryptionKey")
                .mode(DtoEncryptionPolicy.ModeEnum.AES128);
        DtoCompressionPolicy compressionPolicy = new DtoCompressionPolicy()
                .mode(DtoCompressionPolicy.ModeEnum.ZSTD);
        DtoRestorePolicy restorePolicy = new DtoRestorePolicy()
                .encryption(encryptionPolicy)
                .compression(compressionPolicy);

        CToGoUtils.runRestoreByConfiguration(restorePolicy, backupFilesPath, ROUTINE_NAME, SOURCE_NAMESPACE, "zstd", "aes128", "encryptionKey");

        Record retrievedRecord = AerospikeDataUtils.get( KEY1);
        if (retrievedRecord == null)
            AerospikeLogger.logFileByPath(backupFilesPath);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);
    }

    @Test
    void testEncryptionAndCompression256() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY2, STRING_BIN, firstValueCreate);
        ASBench.on(SOURCE_NAMESPACE2, SET1).duration(1).run();
        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE2);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        String backupFilesPath = CToGoUtils.runBackupByConfiguration(ROUTINE_NAME2, SOURCE_NAMESPACE2, "zstd",
                10, "aes256", "encryptionKey");

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE2);

        DtoEncryptionPolicy encryptionPolicy = new DtoEncryptionPolicy()
                .keyFile("/encryptionKey")
                .mode(DtoEncryptionPolicy.ModeEnum.AES256);
        DtoCompressionPolicy compressionPolicy = new DtoCompressionPolicy()
                .mode(DtoCompressionPolicy.ModeEnum.ZSTD);
        DtoRestorePolicy restorePolicy = new DtoRestorePolicy()
                .encryption(encryptionPolicy)
                .compression(compressionPolicy);
        CToGoUtils.runRestoreByConfiguration(restorePolicy, backupFilesPath, ROUTINE_NAME2, SOURCE_NAMESPACE2, "zstd", "aes256", "encryptionKey");

        Record retrievedRecord = AerospikeDataUtils.get( KEY2);
        if (retrievedRecord == null)
            AerospikeLogger.logFileByPath(backupFilesPath);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE2)).isEqualTo(numberOfRecordsBeforeTruncate);
    }
}