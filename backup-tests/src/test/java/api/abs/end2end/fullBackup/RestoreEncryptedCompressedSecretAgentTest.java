package api.abs.end2end.fullBackup;

import api.abs.AbsBackupApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import api.abs.JobID;
import api.abs.generated.model.DtoCompressionPolicy;
import api.abs.generated.model.DtoEncryptionPolicy;
import api.abs.generated.model.DtoRestorePolicy;
import api.abs.generated.model.DtoSecretAgent;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import utils.ASBench;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-E2E")
@EnabledIfSystemProperty(named = "qa_environment", matches = "GCP")
class RestoreEncryptedCompressedSecretAgentTest extends AbsRunner {
    private static final String STRING_BIN = "RestoreTestBin";
    private static final String SET_NAME = "SetFullBackupTest";
    private static final String ROUTINE_NAME = "fullBackupEncryptedCompressedSecretAgent";
    private static Key KEY2;
    private static String SOURCE_NAMESPACE;

    @BeforeAll
    static void setUp() {
        SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);
        KEY2 = new Key(SOURCE_NAMESPACE, SET_NAME, "IT1");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void testEncryptionAndCompressionWithSecretAgent() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY2, STRING_BIN, firstValueCreate);
        ASBench.on(SOURCE_NAMESPACE, SET_NAME).duration(1).run();
        long dataTotalBytes = AerospikeDataUtils.getDataTotalBytes(SOURCE_NAMESPACE);
        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        var backupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        assertThat(backupKey.getByteCount()).isLessThan(dataTotalBytes);

        DtoEncryptionPolicy encryptionPolicy = new DtoEncryptionPolicy()
                .keySecret("secrets:encKey:encryption-key-pem")
                .mode(DtoEncryptionPolicy.ModeEnum.AES256);
        DtoCompressionPolicy compressionPolicy = new DtoCompressionPolicy()
                .mode(DtoCompressionPolicy.ModeEnum.ZSTD);
        DtoRestorePolicy restorePolicy = new DtoRestorePolicy()
                .encryption(encryptionPolicy)
                .compression(compressionPolicy);
        DtoSecretAgent secretAgent = new DtoSecretAgent()
                .address(AbsRunner.SECRET_AGENT_K8S_DNS_NAME)
                .port(3005)
                .isBase64(true)
                .connectionType(DtoSecretAgent.ConnectionTypeEnum.TCP);
        JobID jobID = AbsRestoreApi.restoreFull(backupKey.getKey(), ROUTINE_NAME, restorePolicy, secretAgent);
        AbsRestoreApi.waitForRestore(jobID);

        Record retrievedRecord = AerospikeDataUtils.get( KEY2);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);
    }
}