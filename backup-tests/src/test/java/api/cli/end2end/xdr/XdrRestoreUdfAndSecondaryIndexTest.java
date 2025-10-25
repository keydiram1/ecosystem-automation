package api.cli.end2end.xdr;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import api.cli.RestoreResult;
import com.aerospike.client.Info;
import com.aerospike.client.Key;
import com.aerospike.client.query.IndexType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.ASBench;
import utils.AutoUtils;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("CLI-BACKUP-SEQUENTIAL")
@Disabled
class XdrRestoreUdfAndSecondaryIndexTest extends CliBackupRunner {

    private static final String STRING_BIN = "RestoreTestBin";
    private static final String SET1 = "SetFullBackupTest1";
    private static final String SET2 = "SetFullBackupTest2";
    private static Key KEY1;
    private static Key KEY2;
    private static final String SOURCE_NAMESPACE = "source-ns4";
    private static final String BACKUP_DIR = "XdrRestoreUdfAndSecondaryIndexTest";
    private static final int LOCAL_PORT = 8088;
    private static final String DC = "DcFirstXdrRestoreTest";

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

        BackupResult backup = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR, DC, LOCAL_PORT).run();
        assertThat(backup.getUdfsRead()).isEqualTo(1);
        String backupKey = backup.getBackupDir();

        AerospikeDataUtils.deleteUDF(fileName);
        assertThat(AerospikeDataUtils.isUdfExist(fileName)).isFalse();

        RestoreResult restoreResult = CliRestore.on(SOURCE_NAMESPACE, backupKey).run();
        assertThat(AerospikeDataUtils.isUdfExist(fileName)).isTrue();
        assertThat(restoreResult.getUdfsRead()).isEqualTo(1);

        AerospikeDataUtils.createUDF(fileName2);
        backup = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR, DC, LOCAL_PORT).run();
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

        BackupResult backup = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR, DC, LOCAL_PORT).run();
        assertThat(backup.getSIndexRead()).isEqualTo(1);
        String keyCreateFirstValue = backup.getBackupDir();

        srcClient.dropIndex(null, SOURCE_NAMESPACE, SET1, indexName);
        assertThat(AerospikeDataUtils.isIndexExist(indexName)).isFalse();

        RestoreResult restoreResult = CliRestore.on(SOURCE_NAMESPACE, keyCreateFirstValue).run();
        assertThat(restoreResult.getSIndexRead()).isEqualTo(1);
        assertThat(AerospikeDataUtils.isIndexExist(indexName)).isTrue();

        AerospikeDataUtils.put(KEY2, STRING_BIN, "someValue");
        srcClient.createIndex(null, SOURCE_NAMESPACE, SET2, indexName2, STRING_BIN, IndexType.NUMERIC);

        CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR, DC, LOCAL_PORT).run();
        restoreResult = CliRestore.on(SOURCE_NAMESPACE, keyCreateFirstValue).run();
        assertThat(restoreResult.getSIndexRead()).isEqualTo(2);
    }

    @Test
    void backupAndRestoreAll() {
        String udfFile1 = "myFullUdf.lua";
        String udfFile2 = "myFullUdf2.lua";
        String indexName1 = "testIndexFull";
        String indexName2 = "testIndexFull2";

        AerospikeDataUtils.createUDF(udfFile1);
        AerospikeDataUtils.createUDF(udfFile2);
        assertThat(AerospikeDataUtils.isUdfExist(udfFile1)).isTrue();
        assertThat(AerospikeDataUtils.isUdfExist(udfFile2)).isTrue();

        srcClient.dropIndex(null, SOURCE_NAMESPACE, SET1, indexName1);
        srcClient.dropIndex(null, SOURCE_NAMESPACE, SET2, indexName2);
        assertThat(AerospikeDataUtils.isIndexExist(indexName1)).isFalse();
        assertThat(AerospikeDataUtils.isIndexExist(indexName2)).isFalse();

        AerospikeDataUtils.put(KEY1, STRING_BIN, "someValue");
        srcClient.createIndex(null, SOURCE_NAMESPACE, SET1, indexName1, STRING_BIN, IndexType.NUMERIC);

        AerospikeDataUtils.put(KEY2, STRING_BIN, "anotherValue");
        srcClient.createIndex(null, SOURCE_NAMESPACE, SET2, indexName2, STRING_BIN, IndexType.NUMERIC);

        assertThat(AerospikeDataUtils.isIndexExist(indexName1)).isTrue();
        assertThat(AerospikeDataUtils.isIndexExist(indexName2)).isTrue();

        ASBench.on(SOURCE_NAMESPACE, SET1).duration(1).run();

        int recordsBeforeBackup = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        assertThat(recordsBeforeBackup).isGreaterThan(100);

        AutoUtils.sleep(3000);

        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR, DC, LOCAL_PORT).run();
        assertThat(backupResult.getUdfsRead()).isEqualTo(2);
        assertThat(backupResult.getSIndexRead()).isEqualTo(2);

        AerospikeDataUtils.deleteUDF(udfFile1);
        AerospikeDataUtils.deleteUDF(udfFile2);
        assertThat(AerospikeDataUtils.isUdfExist(udfFile1)).isFalse();
        assertThat(AerospikeDataUtils.isUdfExist(udfFile2)).isFalse();

        srcClient.dropIndex(null, SOURCE_NAMESPACE, SET1, indexName1);
        srcClient.dropIndex(null, SOURCE_NAMESPACE, SET2, indexName2);
        assertThat(AerospikeDataUtils.isIndexExist(indexName1)).isFalse();
        assertThat(AerospikeDataUtils.isIndexExist(indexName2)).isFalse();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(0);

        RestoreResult restoreResult = CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        assertThat(AerospikeDataUtils.isUdfExist(udfFile1)).isTrue();
        assertThat(AerospikeDataUtils.isUdfExist(udfFile2)).isTrue();
        assertThat(AerospikeDataUtils.isIndexExist(indexName1)).isTrue();
        assertThat(AerospikeDataUtils.isIndexExist(indexName2)).isTrue();
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(recordsBeforeBackup);

        assertThat(restoreResult.getInsertedRecords()).isEqualTo(recordsBeforeBackup + 1);
        assertThat(restoreResult.getUdfsRead()).isEqualTo(2);
        assertThat(restoreResult.getSIndexRead()).isEqualTo(2);
    }

}