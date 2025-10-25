package api.backup.end2end;

import api.backup.BackupManager;
import api.backup.RestoreApi;
import api.backup.dto.RestoreSetRequest;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AutoUtils;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("ADR-E2E")
class RestoreDeletedEntitiesTest extends BackupRunner {
    private static final String SET_NAME = "RestoreDeletedEntitiesSet";
    private static final String SET_NAME2 = "RestoreDeletedEntitiesSet2";
    private static final String SOURCE_NAMESPACE = "source-ns5";
    private static final String SOURCE_CLUSTER_NAME = "RestoreDeletedEntitiesSrcCluster";
    private static final String BACKUP_NAMESPACE = "adr-ns5";
    private static final String INITIAL_VALUE = "RestoreDeletedEntitiesInitialValue";
    private static final String BACKUP_NAME = "RestoreDeletedEntitiesContinuousBackup";
    private static final String DC_NAME = "RestoreDeletedEntitiesDC";
    private static final String POLICY_NAME = "RestoreDeletedEntitiesPolicy";
    private static long afterBackupForDeletedEntity;
    private static final Key RESTORE_DELETED_ENTITIES_KEY = new Key(SOURCE_NAMESPACE, SET_NAME, "IT");
    private static final Key RESTORE_DELETED_ENTITIES_KEY_SAME_SET = new Key(SOURCE_NAMESPACE, SET_NAME, "IT2");
    private static final Key RESTORE_DELETED_ENTITIES_KEY_DIFFERENT_SET = new Key(SOURCE_NAMESPACE, SET_NAME2, "IT3");
    private static final IAerospikeClient srcClient = AerospikeDataUtils.getSourceClient();

    @BeforeEach
    public void setUp() {
        // In restore related tests we have to wait before truncate and validate because the same records will
        // be restored to the source cluster and backed up to ADR again.
        AutoUtils.sleep(5000);
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);
        prepareData();
    }

    @AfterAll
    public static void afterAll() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @Test
    void restoreRecord() {
        AerospikeDataUtils.put(RESTORE_DELETED_ENTITIES_KEY, "value", "toBeDeleted");
        AerospikeDataUtils.put(RESTORE_DELETED_ENTITIES_KEY_SAME_SET, "value", "wontBeDeletedSameSetDifferentKey");
        AerospikeDataUtils.put(RESTORE_DELETED_ENTITIES_KEY_DIFFERENT_SET, "value", "wontBeDeletedDifferentSet");

        String digest = AerospikeDataUtils.getDigestFromKey(RESTORE_DELETED_ENTITIES_KEY);
        int restored = RestoreApi.restoreRecord(afterBackupForDeletedEntity, digest, SET_NAME, SOURCE_CLUSTER_NAME,
                SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE,
                SOURCE_NAMESPACE);
        assertThat(restored).isEqualTo(1);

        Record record = srcClient.get(null, RESTORE_DELETED_ENTITIES_KEY_DIFFERENT_SET);
        String value = record.getString("value");
        assertEquals("wontBeDeletedDifferentSet", value);

        record = srcClient.get(null, RESTORE_DELETED_ENTITIES_KEY_SAME_SET);
        value = record.getString("value");
        assertEquals("wontBeDeletedSameSetDifferentKey", value);

        record = srcClient.get(null, RESTORE_DELETED_ENTITIES_KEY);
        assertNull(record);
    }

    @Test
    void restoreSet() {
        AerospikeDataUtils.put(RESTORE_DELETED_ENTITIES_KEY, "value", "toBeDeleted");
        AerospikeDataUtils.put(RESTORE_DELETED_ENTITIES_KEY_SAME_SET, "value", "toBeDeletedSameSet");
        AerospikeDataUtils.put(RESTORE_DELETED_ENTITIES_KEY_DIFFERENT_SET, "value", "wontBeDeletedDifferentSet");

        long restored = RestoreApi.restoreSet(RestoreSetRequest.builder()
                .fromTime(0L)
                .toTime(afterBackupForDeletedEntity)
                .srcClusterName(SOURCE_CLUSTER_NAME)
                .trgClusterName(SOURCE_CLUSTER_NAME)
                .srcNS(SOURCE_NAMESPACE)
                .trgNS(SOURCE_NAMESPACE)
                .set(SET_NAME)
                .build()).getProcessed();
        assertThat(restored).isEqualTo(2);

        Record record = srcClient.get(null, RESTORE_DELETED_ENTITIES_KEY_DIFFERENT_SET);
        String value = record.getString("value");
        assertEquals("wontBeDeletedDifferentSet", value);

        record = srcClient.get(null, RESTORE_DELETED_ENTITIES_KEY);
        assertNull(record);

        record = srcClient.get(null, RESTORE_DELETED_ENTITIES_KEY_SAME_SET);
        assertNull(record);
    }

    @Test
    void restoreNamespace() {
        AerospikeDataUtils.put(RESTORE_DELETED_ENTITIES_KEY, "value", "toBeDeleted");
        AerospikeDataUtils.put(RESTORE_DELETED_ENTITIES_KEY_SAME_SET, "value", "toBeDeletedSameSetSameNS");
        AerospikeDataUtils.put(RESTORE_DELETED_ENTITIES_KEY_DIFFERENT_SET, "value", "toBeDeletedDifferentSetSameNS");

        long restored = RestoreApi.restoreNamespace(afterBackupForDeletedEntity, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE).getProcessed();
        assertThat(restored).isEqualTo(3);

        Record record = srcClient.get(null, RESTORE_DELETED_ENTITIES_KEY_DIFFERENT_SET);
        assertNull(record);

        record = srcClient.get(null, RESTORE_DELETED_ENTITIES_KEY);
        assertNull(record);

        record = srcClient.get(null, RESTORE_DELETED_ENTITIES_KEY_SAME_SET);
        assertNull(record);
    }

    private void prepareData() {
        AerospikeDataUtils.put(RESTORE_DELETED_ENTITIES_KEY, "value", INITIAL_VALUE);
        AerospikeDataUtils.put(RESTORE_DELETED_ENTITIES_KEY_SAME_SET, "value", INITIAL_VALUE);
        AerospikeDataUtils.put(RESTORE_DELETED_ENTITIES_KEY_DIFFERENT_SET, "value", "wontBeDeleted");

        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_DELETED_ENTITIES_KEY, 1);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_DELETED_ENTITIES_KEY_SAME_SET, 1);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_DELETED_ENTITIES_KEY_DIFFERENT_SET, 1);

        srcClient.delete(null, RESTORE_DELETED_ENTITIES_KEY);
        srcClient.delete(null, RESTORE_DELETED_ENTITIES_KEY_SAME_SET);
        srcClient.delete(null, RESTORE_DELETED_ENTITIES_KEY_DIFFERENT_SET);

        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_DELETED_ENTITIES_KEY, 2);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_DELETED_ENTITIES_KEY_SAME_SET, 2);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_DELETED_ENTITIES_KEY_DIFFERENT_SET, 2);

        afterBackupForDeletedEntity = System.currentTimeMillis();
    }
}
