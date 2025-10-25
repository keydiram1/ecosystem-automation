package api.cli.cToGo;

import com.aerospike.client.Info;
import com.aerospike.client.Key;
import com.aerospike.client.query.IndexType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("CLI-BACKUP-C-TO-GO")
@Execution(ExecutionMode.SAME_THREAD)
class CToGoRestoreUdfAndSecondaryIndexTest extends CliBackupRunner {

    private static final String STRING_BIN = "RestoreTestBin";
    private static final String SET1 = "SetFullBackupTest1";
    private static Key KEY1;
    private static final String SOURCE_NAMESPACE = "source-ns2";

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
    }

    @Test
    void restoreUdf() {
        String fileName = "myFullUdf.lua";
        AerospikeDataUtils.createUDF(fileName);
        assertThat(AerospikeDataUtils.isUdfExist(fileName)).isTrue();

        String backupFilesPath = CToGoUtils.runBackupByConfiguration("restoreUdf", SOURCE_NAMESPACE);

        AerospikeDataUtils.deleteUDF(fileName);
        assertThat(AerospikeDataUtils.isUdfExist(fileName)).isFalse();

        CToGoUtils.runRestoreByConfigurationWithEncryption(backupFilesPath, SOURCE_NAMESPACE);
        assertThat(AerospikeDataUtils.isUdfExist(fileName)).isTrue();
    }

    @Test
    void restoreSecondaryIndex() {
        String indexName = "testIndexFull";
        AerospikeDataUtils.put(KEY1, STRING_BIN, "someValue");
        srcClient.createIndex(null, SOURCE_NAMESPACE, SET1, indexName, STRING_BIN, IndexType.NUMERIC);
        assertThat(AerospikeDataUtils.isIndexExist(indexName)).isTrue();
        Info.request(srcClient.getInfoPolicyDefault(), srcClient.getCluster().getRandomNode(), "sindex");

        String backupFilesPath = CToGoUtils.runBackupByConfiguration("restoreSecondaryIndex", SOURCE_NAMESPACE);

        srcClient.dropIndex(null, SOURCE_NAMESPACE, SET1, indexName);
        assertThat(AerospikeDataUtils.isIndexExist(indexName)).isFalse();

        CToGoUtils.runRestoreByConfigurationWithEncryption(backupFilesPath, SOURCE_NAMESPACE);
        assertThat(AerospikeDataUtils.isIndexExist(indexName)).isTrue();
    }
}