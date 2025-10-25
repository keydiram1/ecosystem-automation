package api.abs.end2end.cToGo;

import api.abs.AbsRoutineApi;
import com.aerospike.client.Info;
import com.aerospike.client.Key;
import com.aerospike.client.query.IndexType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import utils.abs.AbsRunner;
import utils.aerospike.abs.AerospikeDataUtils;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-C-TO-GO")
@Execution(ExecutionMode.SAME_THREAD)
class CToGoRestoreUdfAndSecondaryIndexTest extends AbsRunner {

    private static final String STRING_BIN = "RestoreTestBin";
    private static final String SET1 = "SetFullBackupTest1";
    private static final String ROUTINE_NAME = "fullBackup1";
    private static Key KEY1;
    private static String SOURCE_NAMESPACE;

    @BeforeAll
    static void setUp() {
        String sourceNamespace = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);
        KEY1 = new Key(sourceNamespace, SET1, "IT1");
        SOURCE_NAMESPACE = sourceNamespace;
    }

    @Test
    void restoreUdf() {
        String fileName = "myFullUdf.lua";
        AerospikeDataUtils.createUDF(fileName);
        assertThat(AerospikeDataUtils.isUdfExist(fileName)).isTrue();

        String backupFilesPath = CToGoUtils.runBackupByConfiguration(ROUTINE_NAME, SOURCE_NAMESPACE);

        AerospikeDataUtils.deleteUDF(fileName);
        assertThat(AerospikeDataUtils.isUdfExist(fileName)).isFalse();

        CToGoUtils.runRestoreByConfiguration(backupFilesPath, ROUTINE_NAME, SOURCE_NAMESPACE);
        assertThat(AerospikeDataUtils.isUdfExist(fileName)).isTrue();
    }

    @Test
    void restoreSecondaryIndex() {
        String indexName = "testIndexFull";
        AerospikeDataUtils.put(KEY1, STRING_BIN, "someValue");
        srcClient.createIndex(null, SOURCE_NAMESPACE, SET1, indexName, STRING_BIN, IndexType.NUMERIC);
        assertThat(AerospikeDataUtils.isIndexExist(indexName)).isTrue();
        Info.request(srcClient.getInfoPolicyDefault(), srcClient.getCluster().getRandomNode(), "sindex");

        String backupFilesPath = CToGoUtils.runBackupByConfiguration(ROUTINE_NAME, SOURCE_NAMESPACE);

        srcClient.dropIndex(null, SOURCE_NAMESPACE, SET1, indexName);
        assertThat(AerospikeDataUtils.isIndexExist(indexName)).isFalse();

        CToGoUtils.runRestoreByConfiguration(backupFilesPath, ROUTINE_NAME, SOURCE_NAMESPACE);
        assertThat(AerospikeDataUtils.isIndexExist(indexName)).isTrue();
    }
}