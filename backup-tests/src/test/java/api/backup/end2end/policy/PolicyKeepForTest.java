package api.backup.end2end.policy;

import api.backup.*;
import api.backup.dto.RetrieveEntityRecord;
import com.aerospike.client.Key;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import utils.DockerManager;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ADR-E2E")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PolicyKeepForTest extends BackupRunner {
    static final int deleteOldRecordsIntervalSeconds = 5;

    private static final String SET_NAME = "setKeepForTest";
    private static final String SOURCE_NAMESPACE = "source-ns10";
    private static final String SOURCE_CLUSTER_NAME = "PolicyKeepForTestCluster";
    private static final String BACKUP_NAMESPACE = "adr-ns10";
    private static final String INITIAL_VALUE = "initialValueKeepForTest";
    private static final String BACKUP_NAME = "PolicyKeepForTestBackup";
    private static final Key key = new Key(SOURCE_NAMESPACE, SET_NAME, "KeepForTestKey");
    private static final String POLICY_NAME = "KeepForTestPolicy";
    private static final String DC_NAME = "KeepForDC";

    @BeforeAll
    static void setUp() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        prepareConfiguration(POLICY_NAME, 1, 1, 1, 100000, 0, DC_NAME);
    }

    @AfterAll
    static void tearDown() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    // keepFor: how many seconds do we keep the backup. It will actually be deleted
    // only when the compactor will work(deleteOldRecordsIntervalSeconds)
    @Test
    @Order(1)
    void keepForTest() {
        int keepFor = deleteOldRecordsIntervalSeconds + 1;

        //test that update works
        PolicyApi.updatePolicy(POLICY_NAME, 1, 10, keepFor, 100_000, 0);

        AerospikeDataUtils.put(key, "value", INITIAL_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, key, 1);
        final long afterFirstBackup = System.currentTimeMillis();

        List<RetrieveEntityRecord> actual = retrieveKey(afterFirstBackup);
        assertThat(actual).hasSize(1);
        long backedUpTimestamp = actual.get(0).getTimestamp();

        //delete should happen later
        Awaitility.await()
                .atMost(Duration.ofMinutes(3))
                .pollInterval(Duration.ofSeconds(5))
                .until(() -> retrieveKey(afterFirstBackup).isEmpty());

        long after = System.currentTimeMillis();
        assertThat(Duration.ofMillis(after - backedUpTimestamp))
                .isGreaterThan(Duration.ofSeconds(deleteOldRecordsIntervalSeconds));
    }

    @Test
    @Order(2)
    @DisabledIfSystemProperty(named = "qa_environment", matches = "GCP")
    void searchWarningsInAsBackupCluster() {
        String backupClusterLog = DockerManager.getLogFromContainer("aerospike-backup");
        final List<String> ERROR_MESSAGES = Arrays.asList("msg key size is 0");
        for (String errorMsg : ERROR_MESSAGES) {
            assertThat(backupClusterLog)
                    .overridingErrorMessage("The backup cluster log contained the string: %s", errorMsg)
                    .doesNotContain(errorMsg);
        }
    }

    private List<RetrieveEntityRecord> retrieveKey(long afterFirstBackup) {
        String digest = AerospikeDataUtils.getDigestFromKey(key);
        return RetrieveAPI.retrieve(RetrieveAPI.WhatToRetrieve.LATEST, afterFirstBackup, digest, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, SET_NAME);
    }

    @SuppressWarnings("SameParameterValue")
    private static void prepareConfiguration(String policyName, int duration, int retention, int keepFor,
                                             int maxThroughput, Integer initialSync, String dcName) {
        Response policyResponse = PolicyApi.createPolicy(policyName, duration, retention, keepFor, maxThroughput, initialSync);
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
