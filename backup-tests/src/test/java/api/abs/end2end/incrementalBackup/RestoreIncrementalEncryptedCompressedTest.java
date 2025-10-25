package api.abs.end2end.incrementalBackup;

import api.abs.AbsBackupApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import api.abs.JobID;
import api.abs.generated.model.DtoCompressionPolicy;
import api.abs.generated.model.DtoEncryptionPolicy;
import api.abs.generated.model.DtoRestorePolicy;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import utils.ASBench;
import utils.AutoUtils;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import java.util.UUID;

import static api.abs.AbsBackupApi.waitForIncrementalBackup;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-E2E")
@DisabledIfSystemProperty(named = "qa_environment", matches = "GCP")
class RestoreIncrementalEncryptedCompressedTest extends AbsRunner {
    private static final String STRING_BIN = "RestoreIncBin";
    private static final String SET_NAME = "SetIncBackupTest";
    private static final String ROUTINE_NAME = "incrementalBackupEncryptedCompressed128";
    private static Key KEY1;
    private static String SOURCE_NAMESPACE;
    private final int recordSize = 500;
    private final int records = 100_000;

    @BeforeAll
    static void setUp() {
        SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);
        KEY1 = new Key(SOURCE_NAMESPACE, SET_NAME, "IT1");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void testEncryptionAndCompression128() {
        String firstValueCreate = "firstValueCreate" + UUID.randomUUID();

        AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        AutoUtils.waitUntilNextRoundSecond(10); // ensure that we are not generating data during incr backup.
        AutoUtils.sleep(2000); // ensure that incremental (empty) backup finished
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);
        ASBench.on(SOURCE_NAMESPACE, SET_NAME).keys(records).recordSize(recordSize).run();

        var backup = waitForIncrementalBackup(ROUTINE_NAME);

        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsBeforeTruncate).isEqualTo(records + 1);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        assertThat(backup.getByteCount())
                .as("compressed size should be less then uncompressed")
                .isLessThan(numberOfRecordsBeforeTruncate * recordSize);
        assertThat(backup.getRecordCount()).isEqualTo(numberOfRecordsBeforeTruncate);

        DtoEncryptionPolicy encryptionPolicy = new DtoEncryptionPolicy()
                .keyFile("/encryptionKey")
                .mode(DtoEncryptionPolicy.ModeEnum.AES128);
        DtoCompressionPolicy compressionPolicy = new DtoCompressionPolicy()
                .mode(DtoCompressionPolicy.ModeEnum.ZSTD);
        DtoRestorePolicy restorePolicy = new DtoRestorePolicy()
                .encryption(encryptionPolicy)
                .compression(compressionPolicy);

        AbsRestoreApi.restoreIncrementalSync(backup.getKey(), ROUTINE_NAME, restorePolicy);

        Record retrievedRecord = AerospikeDataUtils.get(KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);

        // should not work without decryption
        JobID jobFailed = AbsRestoreApi.restoreIncremental(backup.getKey(), ROUTINE_NAME, restorePolicy.encryption(null));
        AbsRestoreApi.waitForRestoreFail(jobFailed);
    }
}