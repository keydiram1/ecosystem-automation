package api.abs.end2end;

import api.abs.AbsBackupApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import api.abs.generated.model.DtoBackupDetails;
import api.abs.generated.model.DtoRestoreJobStatus;
import api.abs.generated.model.DtoRestorePolicy;
import com.aerospike.client.Info;
import com.aerospike.client.Key;
import com.aerospike.client.query.IndexType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import utils.abs.AbsRunner;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.ConfigParametersHandler;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-SEQUENTIAL-TESTS-2")
@Execution(ExecutionMode.SAME_THREAD)
class RestoreUdfAndSecondaryIndexTest extends AbsRunner {

    private static final String STRING_BIN = "RestoreTestBin";
    private static final String SET1 = "SetFullBackupTest1";
    private static final String SET2 = "SetFullBackupTest2";
    private static final String ROUTINE_NAME = "fullBackup1";
    static String fileName = "myFullUdf.lua";
    static String fileName2 = "myFullUdf2.lua";
    private static Key KEY1;
    private static Key KEY2;
    private static String SOURCE_NAMESPACE;

    @BeforeAll
    static void setUp() {
        String sourceNamespace = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);
        KEY1 = new Key(sourceNamespace, SET1, "IT1");
        KEY2 = new Key(sourceNamespace, SET2, "IT2");
        SOURCE_NAMESPACE = sourceNamespace;
    }


    @Test
    void restoreUdf() {
        AerospikeDataUtils.createUDF(fileName);
        assertThat(AerospikeDataUtils.isUdfExist(fileName)).isTrue();

        AerospikeDataUtils.deleteUDF(fileName2);
        assertThat(AerospikeDataUtils.isUdfExist(fileName2)).isFalse();

        DtoBackupDetails backup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        assertThat(backup.getUdfCount()).isEqualTo(1);
        String backupKey = backup.getKey();

        AerospikeDataUtils.deleteUDF(fileName);
        assertThat(AerospikeDataUtils.isUdfExist(fileName)).isFalse();

        DtoRestorePolicy policy = new DtoRestorePolicy().parallel(Integer.valueOf(ConfigParametersHandler.getParameter("CONFIG_RESTORE_PARALLEL"))).noUdfs(true);
        AbsRestoreApi.restoreFullSync(backupKey, ROUTINE_NAME, policy);
        assertThat(AerospikeDataUtils.isUdfExist(fileName)).isFalse();

        DtoRestoreJobStatus restoreStatus = AbsRestoreApi.restoreFullSync(backupKey, ROUTINE_NAME);
        assertThat(AerospikeDataUtils.isUdfExist(fileName)).isTrue();
        assertThat(restoreStatus.getUdfCount()).isEqualTo(1);

        AerospikeDataUtils.createUDF(fileName2);
        backup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        restoreStatus = AbsRestoreApi.restoreFullSync(backup.getKey(), ROUTINE_NAME);
        assertThat(restoreStatus.getUdfCount()).isEqualTo(2);
    }

    @AfterAll
    static void afterAll() {
        AerospikeDataUtils.deleteUDF(fileName);
        AerospikeDataUtils.deleteUDF(fileName2);
    }

    @Test
    void restoreSecondaryIndex() {
        String indexName = "testIndexFull";
        String indexName2 = "testIndexFull2";
        srcClient.dropIndex(null, SOURCE_NAMESPACE, SET1, indexName);
        srcClient.dropIndex(null, SOURCE_NAMESPACE, SET1, indexName2);
        assertThat(AerospikeDataUtils.isIndexExist(indexName)).isFalse();
        assertThat(AerospikeDataUtils.isIndexExist(indexName2)).isFalse();

        AerospikeDataUtils.put(KEY1, STRING_BIN, "someValue");
        srcClient.createIndex(null, SOURCE_NAMESPACE, SET1, indexName, STRING_BIN, IndexType.NUMERIC);
        assertThat(AerospikeDataUtils.isIndexExist(indexName)).isTrue();
        Info.request(srcClient.getInfoPolicyDefault(), srcClient.getCluster().getRandomNode(), "sindex");

        DtoBackupDetails backup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        assertThat(backup.getSecondaryIndexCount()).isEqualTo(1);
        String keyCreateFirstValue = backup.getKey();

        srcClient.dropIndex(null, SOURCE_NAMESPACE, SET1, indexName);
        assertThat(AerospikeDataUtils.isIndexExist(indexName)).isFalse();

        var policyNoIndex = new DtoRestorePolicy().parallel(Integer.valueOf(ConfigParametersHandler.getParameter("CONFIG_RESTORE_PARALLEL"))).noIndexes(true);
        AbsRestoreApi.restoreFullSync(keyCreateFirstValue, ROUTINE_NAME, policyNoIndex);
        assertThat(AerospikeDataUtils.isIndexExist(indexName)).isFalse();

        var policyWithIndex = new DtoRestorePolicy().parallel(Integer.valueOf(ConfigParametersHandler.getParameter("CONFIG_RESTORE_PARALLEL"))).noIndexes(false);
        DtoRestoreJobStatus restoreStatus = AbsRestoreApi.restoreFullSync(keyCreateFirstValue, ROUTINE_NAME, policyWithIndex);
        assertThat(restoreStatus.getIndexCount()).isEqualTo(1);
        assertThat(AerospikeDataUtils.isIndexExist(indexName)).isTrue();

        AerospikeDataUtils.put(KEY2, STRING_BIN, "someValue");
        srcClient.createIndex(null, SOURCE_NAMESPACE, SET2, indexName2, STRING_BIN, IndexType.NUMERIC);
        backup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        restoreStatus = AbsRestoreApi.restoreFullSync(backup.getKey(), ROUTINE_NAME, policyWithIndex);
        assertThat(restoreStatus.getIndexCount()).isEqualTo(2);
    }
}