package api.backup.end2end;

import api.backup.BackupApi;
import api.backup.BackupManager;
import api.backup.dto.ContinuousBackup;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Tag("ADR-E2E")
class ContinuousBackupTest extends BackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns3";
    private static final String SOURCE_CLUSTER_NAME = "clusterNameContinuousBackupTest";
    private static final String BACKUP_NAMESPACE = "adr-ns3";
    private static final String INITIAL_VALUE = "initialValueContinuousBackupTest";
    private static final String BACKUP_NAME = "ContinuousBackupTestBackup1";
    private static final String POLICY_NAME = "ContinuousBackupPolicy1";
    private static final String DC_NAME = "ContinuousBackupDC";
    private static final List<String> sets = List.of("set1", "set2", "set3");
    private static final Key key = new Key(SOURCE_NAMESPACE, sets.get(0), "ContinuousBackup2TestKey");
    private static final IAerospikeClient srcClient = AerospikeDataUtils.getSourceClient();
    private static final IAerospikeClient backupClient = AerospikeDataUtils.getBackupClient();

    @AfterAll
    static void afterAll() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @BeforeEach
    public void setUp() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, sets, 86400, DC_NAME);
    }

    @Test
    void updateBackup() {
        ContinuousBackup backup = BackupApi.getBackup(BACKUP_NAME);
        assertEquals("auto description", backup.getDescription());
        BackupApi.updateBackup(BACKUP_NAME, "updated auto description");
        ContinuousBackup backupUpdated = BackupApi.getBackup(BACKUP_NAME);
        assertEquals("updated auto description", backupUpdated.getDescription());
    }

    @Test
    @EnabledIfSystemProperty(named = "STATIC_CONFIGURATION", matches = "false")
    void disableEnableBackup() {
        AerospikeDataUtils.put(key, "value", INITIAL_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, key, 1);

        // In static mode disable won't work because XDR config still exists
        BackupApi.disableBackup(BACKUP_NAME);

        AerospikeDataUtils.put(key, "updated", INITIAL_VALUE);
        Assertions.assertThatThrownBy(() -> BackupManager.waitForBackup(BACKUP_NAME, key, 2, 30))
                .isInstanceOf(ConditionTimeoutException.class);

        BackupApi.enableBackup(BACKUP_NAME);
        AerospikeDataUtils.put(key, "updated", INITIAL_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, key, 2);
    }

    @Test
    void deleteCreateBackup() {
        AerospikeDataUtils.put(key, "value", INITIAL_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, key, 1);

        BackupApi.deleteBackup(BACKUP_NAME);

        AerospikeDataUtils.put(key, "value", INITIAL_VALUE);

        BackupApi.createBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME);
        AutoUtils.sleep(5000);
        BackupApi.enableBackup(BACKUP_NAME);
        BackupManager.waitForBackup(BACKUP_NAME, key, 2);
    }

    @Test
    @EnabledIfSystemProperty(named = "STATIC_CONFIGURATION", matches = "false")
    void createDeletedBackupWithDifferentTargetNS() {
        BackupApi.deleteBackup(BACKUP_NAME);
        AerospikeDataUtils.put(key, "value", INITIAL_VALUE);
        BackupApi.createBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, "adr-ns30", POLICY_NAME);
        AutoUtils.sleep(5000);
        BackupApi.enableBackup(BACKUP_NAME);
        BackupManager.waitForBackup(BACKUP_NAME, key, 1);
    }

    @Test
    void getBackup() {
        ContinuousBackup backup = BackupApi.getBackup(BACKUP_NAME);
        assertEquals("auto description", backup.getDescription());
        assertEquals(BACKUP_NAMESPACE, backup.getBackupNS());
        assertEquals("ContinuousBackupTestBackup1", backup.getName());
        assertEquals("ContinuousBackupPolicy1", backup.getPolicy());
        assertEquals("clusterNameContinuousBackupTest", backup.getSrcClusterName());
        assertEquals(SOURCE_NAMESPACE, backup.getSrcNS());
    }

    @Test
    void getAllBackups() {
        // This test has been surrounded with try catch block since it needs 5 seconds
        // sleep in the end for it to not fail tests that run after him.
        try {
            assertTrue(BackupApi.isBackupExists(BACKUP_NAME));
            BackupApi.deleteBackup(BACKUP_NAME);
            assertFalse(BackupApi.isBackupExists(BACKUP_NAME));

            BackupApi.createBackup(BACKUP_NAME + "2", SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME);
            assertTrue(BackupApi.isBackupExists(BACKUP_NAME + "2"));
            BackupApi.deleteBackup(BACKUP_NAME + "2");
            assertFalse(BackupApi.isBackupExists(BACKUP_NAME + "2"));
        } catch (Exception e) {
            AutoUtils.sleep(5000);
            e.printStackTrace();
            fail(e.getMessage());
        }
        AutoUtils.sleep(5000);
    }

    @Test
    void writeToUnknownSetBackupNotCreated() {
        Key keySet2 = new Key(SOURCE_NAMESPACE, "set2", "ContinuousBackupTestKey2");
        AerospikeDataUtils.put(keySet2, "value", INITIAL_VALUE);
        Key keySet3 = new Key(SOURCE_NAMESPACE, "set3", "ContinuousBackupTestKey3");
        AerospikeDataUtils.put(keySet3, "value", INITIAL_VALUE);
        Key noBackupKey = new Key(SOURCE_NAMESPACE, "unknownSet", "ContinuousBackupUnknownSet");
        AerospikeDataUtils.put(noBackupKey, "value", INITIAL_VALUE);

        BackupManager.waitForBackup(BACKUP_NAME, keySet2, 1);
        BackupManager.waitForBackup(BACKUP_NAME, keySet3, 1);

        Assertions.assertThatThrownBy(() -> BackupManager.waitForBackup(BACKUP_NAME, noBackupKey, 1, 30))
                .isInstanceOf(ConditionTimeoutException.class);
    }

    /*
     *  In dynamic getBackupSets fetched from DTO (configured Continuous Backup).
     *  In static getBackupSets is fetched using get config of shipped-only-specified-sets.
     *
     *  In case not configured it will fetch all known sets (for that namespace).
     */
    @Test
    void getBackupSets() {
        Response backupSetsList = BackupApi.getBackupSets(BACKUP_NAME);
        List<String> backupSets = backupSetsList.body().as(new TypeRef<>() {});
        assertThat(backupSets).containsAll(sets);
    }

    @Test
    void backupWithAsbench() {
        String set = sets.get(0);
        ASBench.on(SOURCE_NAMESPACE, set).duration(1).run();
        AutoUtils.sleep(5_000);
        long numRecordsInSourceAfterAddingData = AerospikeCountUtils.getSetObjectCount(srcClient, sets.get(0), SOURCE_NAMESPACE);
        AerospikeLogger.info("Number of records after generation: " + numRecordsInSourceAfterAddingData);
        Awaitility.await("Backup of " + numRecordsInSourceAfterAddingData + " keys")
                .pollInterval(5, TimeUnit.SECONDS)
                .atMost(2, TimeUnit.MINUTES)
                .until(() -> {
                    long actual = AerospikeCountUtils.getSetObjectCount(backupClient, sets.get(0), BACKUP_NAMESPACE);
                    return actual == numRecordsInSourceAfterAddingData;
                });
    }
}
