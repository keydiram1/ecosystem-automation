package api.cli.end2end.scan;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import api.cli.RestoreResult;
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

@Tag("CLI-BACKUP-SEQUENTIAL")
@Execution(ExecutionMode.SAME_THREAD)
class RestoreUdfAndSecondaryIndexTest extends CliBackupRunner {

    private static final String STRING_BIN = "RestoreTestBin";
    private static final String SET1 = "SetFullBackupTest1";
    private static final String SET2 = "SetFullBackupTest2";
    private static Key KEY1;
    private static Key KEY2;
    private static final String SOURCE_NAMESPACE = "source-ns4";

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
        KEY2 = new Key(SOURCE_NAMESPACE, SET2, "IT2");
    }

    @Test
    void restoreUdf() {
        String fileName = "myFullUdf.lua";
        String fileName2 = "myFullUdf2.lua";
        AerospikeDataUtils.createUDF(fileName);
        assertThat(AerospikeDataUtils.isUdfExist(fileName)).isTrue();

        AerospikeDataUtils.deleteUDF(fileName2);
        assertThat(AerospikeDataUtils.isUdfExist(fileName2)).isFalse();

        BackupResult backup = CliBackup.on(SOURCE_NAMESPACE, "restoreUdfBackup").run();
        assertThat(backup.getUdfsRead()).isEqualTo(1);
        String backupKey = backup.getBackupDir();

        AerospikeDataUtils.deleteUDF(fileName);
        assertThat(AerospikeDataUtils.isUdfExist(fileName)).isFalse();

        CliRestore.on(SOURCE_NAMESPACE, backupKey).setNoUdf().run();
        assertThat(AerospikeDataUtils.isUdfExist(fileName)).isFalse();

        RestoreResult restoreResult = CliRestore.on(SOURCE_NAMESPACE, backupKey).run();
        assertThat(AerospikeDataUtils.isUdfExist(fileName)).isTrue();
        assertThat(restoreResult.getUdfsRead()).isEqualTo(1);

        AerospikeDataUtils.createUDF(fileName2);
        backup = CliBackup.on(SOURCE_NAMESPACE, "restoreUdfBackup").run();
        restoreResult = CliRestore.on(SOURCE_NAMESPACE, backup.getBackupDir()).run();
        assertThat(restoreResult.getUdfsRead()).isEqualTo(2);
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

        BackupResult backup = CliBackup.on(SOURCE_NAMESPACE, "restoreSecondaryIndexBackup").run();
        assertThat(backup.getSIndexRead()).isEqualTo(1);
        String keyCreateFirstValue = backup.getBackupDir();

        srcClient.dropIndex(null, SOURCE_NAMESPACE, SET1, indexName);
        assertThat(AerospikeDataUtils.isIndexExist(indexName)).isFalse();

        CliRestore.on(SOURCE_NAMESPACE, keyCreateFirstValue).setNoSecondaryIndexes().run();
        assertThat(AerospikeDataUtils.isIndexExist(indexName)).isFalse();

        RestoreResult restoreResult = CliRestore.on(SOURCE_NAMESPACE, keyCreateFirstValue).run();
        assertThat(restoreResult.getSIndexRead()).isEqualTo(1);
        assertThat(AerospikeDataUtils.isIndexExist(indexName)).isTrue();

        AerospikeDataUtils.put(KEY2, STRING_BIN, "someValue");
        srcClient.createIndex(null, SOURCE_NAMESPACE, SET2, indexName2, STRING_BIN, IndexType.NUMERIC);

        CliBackup.on(SOURCE_NAMESPACE, "restoreSecondaryIndexBackup").run();
        restoreResult = CliRestore.on(SOURCE_NAMESPACE, keyCreateFirstValue).run();
        assertThat(restoreResult.getSIndexRead()).isEqualTo(2);
    }
}