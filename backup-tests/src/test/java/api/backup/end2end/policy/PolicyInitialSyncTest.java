package api.backup.end2end.policy;

import api.backup.*;
import com.aerospike.client.Key;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AutoUtils;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ADR-E2E")
class PolicyInitialSyncTest extends BackupRunner {
    private static final String SET_NAME = "setInitSyncTest";
    private static final String SOURCE_NAMESPACE = "source-ns2";
    private static final String SOURCE_CLUSTER_NAME = "PolicyInitialSyncCluster";
    private static final String BACKUP_NAMESPACE = "adr-ns2";
    private static final String INITIAL_VALUE = "initialValueInitSyncTest";
    private static final String BACKUP_NAME = "PolicyInitialSyncTestBackup";
    private static final String DC_NAME = "PolicyInitialSyncDC";
    private static final Key key = new Key(SOURCE_NAMESPACE, SET_NAME, "InitSyncTestKey");
    private static final Key key2 = new Key(SOURCE_NAMESPACE, SET_NAME, "InitSyncTestKey2");
    private static final String POLICY_NAME = "PolicyInitialSyncTestPolicy";

    @BeforeEach
    void setUp() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @AfterAll
    static void tearDown() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @Test
    void testInitialSync0() {
        int initialSync = 0; //sync all
        AerospikeDataUtils.put(key, "value", INITIAL_VALUE);
        AutoUtils.sleep(3000);
        prepareConfiguration(POLICY_NAME, 90000, 100000, 100000, initialSync, DC_NAME);
        BackupManager.waitForBackup(BACKUP_NAME, key, 1, 30);
    }

    @Test
    void testInitialSync1of2() {
        int initialSync = 5;
        AerospikeDataUtils.put(key, "value", INITIAL_VALUE);
        AutoUtils.sleep(10_000);
        AerospikeDataUtils.put(key2, "value", INITIAL_VALUE);
        prepareConfiguration(POLICY_NAME, 90000, 100000, 100000, initialSync, DC_NAME);

        // only backup for key2 exists for sure
        // key1 may be shipped, or may not, that looks like not constant behavior on XDR side.
        BackupManager.waitForBackup(BACKUP_NAME, key2, 1);
    }

    @Test
    void testInitialSync6() {
        int initialSync = 6;
        AerospikeDataUtils.put(key, "value", INITIAL_VALUE);
        AutoUtils.sleep(3000);
        prepareConfiguration(POLICY_NAME, 0, 0, 100000, initialSync, DC_NAME);
        BackupManager.waitForBackup(BACKUP_NAME, key, 1);
    }

    @SuppressWarnings("SameParameterValue")
    private void prepareConfiguration(String policyName, int retention, int keepFor,
                                      int maxThroughput, Integer initialSync, String dcName) {
        Response policyResponse = PolicyApi.createPolicy(policyName, 1, retention, keepFor, maxThroughput, initialSync);
        assertThat(policyResponse.getStatusCode()).isEqualTo(201);
        ClusterConnectionApi.createConnection(SOURCE_CLUSTER_NAME, dcName);
        Response backupResponse = BackupApi.createBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE,
                BACKUP_NAMESPACE, policyName, List.of(SET_NAME));

        assertThat(backupResponse.getStatusCode()).isEqualTo(201);
        assertThat(BackupApi.getBackup(BACKUP_NAME).getLastCompaction()).isZero();

        Response enableBackupResponse = BackupApi.enableBackup(BACKUP_NAME);
        assertThat(enableBackupResponse.getStatusCode()).isEqualTo(202);
    }
}
