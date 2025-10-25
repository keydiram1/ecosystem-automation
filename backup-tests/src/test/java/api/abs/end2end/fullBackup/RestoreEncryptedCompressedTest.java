package api.abs.end2end.fullBackup;

import api.abs.AbsBackupApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import api.abs.JobID;
import api.abs.generated.model.DtoBackupDetails;
import api.abs.generated.model.DtoCompressionPolicy;
import api.abs.generated.model.DtoEncryptionPolicy;
import api.abs.generated.model.DtoRestorePolicy;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import static api.abs.generated.model.DtoCompressionPolicy.ModeEnum.ZSTD;
import static api.abs.generated.model.DtoEncryptionPolicy.ModeEnum.AES128;
import static api.abs.generated.model.DtoEncryptionPolicy.ModeEnum.AES256;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-E2E")
@DisabledIfSystemProperty(named = "qa_environment", matches = "GCP")
class RestoreEncryptedCompressedTest extends AbsRunner {
    private static final String STRING_BIN = "RestoreTestBin";
    private static final String SET_NAME = "SetFullBackupTest";
    private static final String ROUTINE_NAME_128 = "fullBackupEncryptedCompressed128";
    private static final String ROUTINE_NAME_256 = "fullBackupEncryptedCompressed256";

    private final int keys = 1000;
    private final int recordSize = 100;

    private static final DtoRestorePolicy RESTORE_POLICY_128 = new DtoRestorePolicy()
            .encryption(new DtoEncryptionPolicy().keyFile("/encryptionKey").mode(AES128))
            .compression(new DtoCompressionPolicy().mode(ZSTD));

    private static final DtoRestorePolicy RESTORE_POLICY_256 = new DtoRestorePolicy()
            .encryption(new DtoEncryptionPolicy().keyEnv("BACKUP_ENCRYPTION_KEY").mode(AES256))
            .compression(new DtoCompressionPolicy().mode(ZSTD));

    private static final DtoRestorePolicy RESTORE_POLICY_FAIL = new DtoRestorePolicy().encryption(null);


    @Nested
    @DisplayName("Backup with AES-128 encryption")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class WhenAes128BackedUp {
        private DtoBackupDetails backup;
        private int numberOfRecordsBeforeTruncate;
        private String firstValueCreate;
        private Key key;
        private String namespace;

        @BeforeAll
        void setupAndRunBackup() {
            namespace = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME_128);
            key = new Key(namespace, SET_NAME, "IT1");
            firstValueCreate = "firstValueCreate" + System.currentTimeMillis();

            AerospikeDataUtils.truncateSourceNamespace(namespace);
            AerospikeDataUtils.put(key, STRING_BIN, firstValueCreate);
            ASBench.on(namespace, SET_NAME).keys(keys).recordSize(recordSize).run();
            numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, namespace);

            backup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME_128);
            assertThat(backup.getCompression()).isEqualTo(ZSTD.toString());
            assertThat(backup.getEncryption()).isEqualTo(AES128.toString());

            AerospikeDataUtils.truncateSourceNamespace(namespace);
        }

        @Test
        @DisplayName("Restore succeeds with the correct key")
        void restoreSucceeds() {
            JobID jobID = AbsRestoreApi.restoreFull(backup.getKey(), ROUTINE_NAME_128, RESTORE_POLICY_128);
            AbsRestoreApi.waitForRestore(jobID);

            Record retrievedRecord = AerospikeDataUtils.get(key);
            assertThat(retrievedRecord).isNotNull();
            assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
            assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, namespace)).isEqualTo(numberOfRecordsBeforeTruncate);
        }

        @Test
        @DisplayName("Restore fails without the decryption key")
        void restoreFailsWithoutKey() {
            JobID jobFailed = AbsRestoreApi.restoreFull(backup.getKey(), ROUTINE_NAME_128, RESTORE_POLICY_FAIL);
            AbsRestoreApi.waitForRestoreFail(jobFailed);
        }
    }

    @Nested
    @DisplayName("Backup with AES-256 encryption")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class WhenAes256BackedUp {
        private DtoBackupDetails backup;
        private int numberOfRecordsBeforeTruncate;
        private String firstValueCreate;
        private Key key;
        private String namespace;

        @BeforeAll
        void setupAndRunBackup() {
            namespace = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME_256);
            key = new Key(namespace, SET_NAME, "IT2");
            firstValueCreate = "firstValueCreate" + System.currentTimeMillis();

            AerospikeDataUtils.truncateSourceNamespace(namespace);
            AerospikeDataUtils.put(key, STRING_BIN, firstValueCreate);
            ASBench.on(namespace, SET_NAME).keys(keys).recordSize(recordSize).run();
            numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, namespace);

            backup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME_256);
            assertThat(backup.getCompression()).isEqualTo(ZSTD.toString());
            assertThat(backup.getEncryption()).isEqualTo(AES256.toString());

            AerospikeDataUtils.truncateSourceNamespace(namespace);
        }

        @Test
        @DisplayName("Restore succeeds with the correct key")
        void restoreSucceeds() {
            JobID jobID = AbsRestoreApi.restoreFull(backup.getKey(), ROUTINE_NAME_256, RESTORE_POLICY_256);
            AbsRestoreApi.waitForRestore(jobID);

            Record retrievedRecord = AerospikeDataUtils.get(key);
            assertThat(retrievedRecord).isNotNull();
            assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
            assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, namespace)).isEqualTo(numberOfRecordsBeforeTruncate);
        }

        @Test
        @DisplayName("Restore fails without the decryption key")
        void restoreFailsWithoutKey() {
            JobID jobFailed = AbsRestoreApi.restoreFull(backup.getKey(), ROUTINE_NAME_256, RESTORE_POLICY_FAIL);
            AbsRestoreApi.waitForRestoreFail(jobFailed);
        }
    }

    @Test
    @DisplayName("File limit with compression")
    void fileLimitWithCompression() {
        int fileLimitInMb = 1; // 1 MB the way it is configured in config.yml
        int fileLimitInBytes = fileLimitInMb * 1024 * 1024;
        int recordSize = 300;

        String namespace = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME_128);

        // Generate data
        ASBench.on(namespace, SET_NAME).keys(120_000).batchSize(100).recordSize(recordSize).run();
        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, namespace);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(10_000);

        var backupDetails = AbsBackupApi.startFullBackupSync(ROUTINE_NAME_128);

        AerospikeLogger.info("backup byteCount: " + backupDetails.getByteCount());
        AerospikeLogger.info("backup fileCount: " + backupDetails.getFileCount());

        long byteCount = backupDetails.getByteCount();
        long fileCount = backupDetails.getFileCount();

        long estimatedFileCountIfCompressionAfter = byteCount / fileLimitInBytes;
        AerospikeLogger.info("estimatedFileCountIfCompressionAfter: " + estimatedFileCountIfCompressionAfter);

        assertThat(fileCount).isGreaterThan(1); //!
        assertThat(fileCount).isGreaterThan(estimatedFileCountIfCompressionAfter - 2).isLessThan(estimatedFileCountIfCompressionAfter + 2);
    }
}