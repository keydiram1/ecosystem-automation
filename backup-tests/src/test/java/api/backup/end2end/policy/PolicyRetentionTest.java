package api.backup.end2end.policy;

import api.backup.*;
import api.backup.dto.Policy;
import com.aerospike.client.Key;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import static java.util.stream.Collectors.groupingBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Tag("ADR-E2E")
class PolicyRetentionTest extends BackupRunner {
    private static final String SET_NAME = "setPolicyTest";
    private static final String SOURCE_NAMESPACE = "source-ns4";
    private static final String SOURCE_CLUSTER_NAME = "PolicyRetentionTestCluster";
    private static final String BACKUP_NAMESPACE = "adr-ns4";
    private static final String BACKUP_NAME = "PolicyRetentionTestBackup";
    private static final Key key = new Key(SOURCE_NAMESPACE, SET_NAME, "PolicyTestKey");
    private static final String POLICY_NAME = "RetentionTestPolicy";
    private static final String DC_NAME = "RetentionDC";

    @BeforeAll
    static void setUp() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @AfterAll
    static void tearDown() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @Test
    void retention() {
        int retention = 10; //in seconds
        int retentionMs = retention * 1000;

        prepareConfiguration(POLICY_NAME, 1, retention, 100000, 100000, 0, DC_NAME);
        HashSet<Long> createdBackups = new HashSet<>();
        long startGenerate = System.currentTimeMillis();
        do {
            AerospikeDataUtils.put(key, "value", System.currentTimeMillis());
            AutoUtils.sleep(retentionMs / 5); //we want roughly 5 elements in batch for simplicity

            addTimestamps(createdBackups);
        } while (System.currentTimeMillis() < startGenerate + 5L * retentionMs); //wait for 5 retention

        long lastCompaction = waitForCompaction();
        addTimestamps(createdBackups); //we need this because some backups might be created with delay during waitForCompaction
        AerospikeLogger.info("Generation finished, timestamps: " + createdBackups.stream().sorted().toList());
        AerospikeLogger.info("LastCompaction " + lastCompaction);
        Collection<Long> timestampsAfterCompaction = MetadataAPI.readBackupTimestampsForKey(BACKUP_NAME, 0, lastCompaction, key);

        // last interval, that's untouched
        List<Long> lastInterval = timestampsAfterCompaction.stream().filter(it -> it > lastCompaction - retentionMs).toList();
        List<Long> expected = createdBackups.stream().filter(it -> it > lastCompaction - retentionMs && it < lastCompaction).toList();
        assertThat(lastInterval).hasSameElementsAs(expected); //none of elements in last interval removed

        // everything older should be already compacted
        long lowerBound = ((lastCompaction / retentionMs) - 2) * retentionMs;
        List<Long> prevIntervals = timestampsAfterCompaction.stream().filter(it -> it < lowerBound).toList();
        assertHasOneElementInEachInterval(retention, prevIntervals);

        // now assert that after some time everything is compacted
        AutoUtils.sleep(3L * retentionMs);
        waitForCompaction();

        Collection<Long> allTimestampsAfterSleep = MetadataAPI.readBackupTimestampsForKey(BACKUP_NAME, 0, lastCompaction, key);

        assertHasOneElementInEachInterval(retention, allTimestampsAfterSleep);
    }

    private void assertHasOneElementInEachInterval(int retention, Collection<Long> prevIntervals) {
        assertThat(group(retention, prevIntervals)).allSatisfy(
                (group, timestamps) -> assertThat(timestamps).hasSize(1));
    }

    private static void addTimestamps(HashSet<Long> createdBackups) {
        createdBackups.addAll(MetadataAPI.readBackupTimestampsForKey(BACKUP_NAME, 0, Long.MAX_VALUE, key));
    }

    private long waitForCompaction() {
        long startWaitingForCompaction = BackupApi.getBackup(BACKUP_NAME).getLastCompaction();
        AerospikeLogger.info("Start waiting for compaction at: " + startWaitingForCompaction);
        return Awaitility.await()
                .pollInterval(Duration.ofSeconds(1))
                .atMost(Duration.ofMinutes(7))
                .until(() -> BackupApi.getBackup(BACKUP_NAME).getLastCompaction(),
                        lastCompacted -> lastCompacted > startWaitingForCompaction);
    }

    private static TreeMap<Long, List<Long>> group(int retention, Collection<Long> timestampsBeforeCompaction) {
        return new TreeMap<>(timestampsBeforeCompaction
                .stream()
                .collect(groupingBy(it2 -> it2 / (retention * 1000L))));
    }

    @Test
    void testCrud() {
        PolicyApi.createPolicy("testPolicyCrud1", 1, 10, 100, 1000, 0);
        PolicyApi.createPolicy("testPolicyCrud2", 2, 20, 200, 2000, 1);
        PolicyApi.createPolicy("testPolicyCrud3", 3, 30, 300, 3000, 0);

        assertTrue(PolicyApi.isPolicyExists("testPolicyCrud1"));
        assertTrue(PolicyApi.isPolicyExists("testPolicyCrud2"));
        assertTrue(PolicyApi.isPolicyExists("testPolicyCrud3"));

        Policy testPolicyCrud1 = PolicyApi.getPolicy("testPolicyCrud1");
        Policy testPolicyCrud2 = PolicyApi.getPolicy("testPolicyCrud2");
        Policy testPolicyCrud3 = PolicyApi.getPolicy("testPolicyCrud3");

        assertEquals(1, testPolicyCrud1.getDuration());
        assertEquals(20, testPolicyCrud2.getRetention());
        assertEquals(300, testPolicyCrud3.getKeepFor());
        assertEquals(1000, testPolicyCrud1.getMaxThroughput());
        assertEquals(1, testPolicyCrud2.getInitialSync());

        PolicyApi.updatePolicy("testPolicyCrud1", 11, 110, 1100, 11000, 1);
        PolicyApi.updatePolicy("testPolicyCrud2", 22, 220, 2200, 22000, 0);
        PolicyApi.updatePolicy("testPolicyCrud3", 33, 330, 3300, 33000, 1);

        testPolicyCrud1 = PolicyApi.getPolicy("testPolicyCrud1");
        testPolicyCrud2 = PolicyApi.getPolicy("testPolicyCrud2");
        testPolicyCrud3 = PolicyApi.getPolicy("testPolicyCrud3");

        assertEquals(11, testPolicyCrud1.getDuration());
        assertEquals(220, testPolicyCrud2.getRetention());
        assertEquals(3300, testPolicyCrud3.getKeepFor());
        assertEquals(11000, testPolicyCrud1.getMaxThroughput());
        assertEquals(0, testPolicyCrud2.getInitialSync());

        PolicyApi.deletePolicy("testPolicyCrud1");
        PolicyApi.deletePolicy("testPolicyCrud2");
        PolicyApi.deletePolicy("testPolicyCrud3");

        assertFalse(PolicyApi.isPolicyExists("testPolicyCrud1"));
        assertFalse(PolicyApi.isPolicyExists("testPolicyCrud2"));
        assertFalse(PolicyApi.isPolicyExists("testPolicyCrud3"));
    }

    @SuppressWarnings("SameParameterValue")
    private void prepareConfiguration(String policyName, int duration, int retention, int keepFor,
                                      int maxThroughput, Integer initialSync, String dcName) {
        Response policyResponse = PolicyApi.createPolicy(policyName, duration, retention, keepFor, maxThroughput, initialSync);
        assertThat(policyResponse.getStatusCode()).isEqualTo(201);
        ClusterConnectionApi.createConnection(SOURCE_CLUSTER_NAME, dcName);
        Response backupResponse = BackupApi.createBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE,
                policyName, List.of(SET_NAME));

        assertThat(backupResponse.getStatusCode()).isEqualTo(201);
        assertThat(BackupApi.getBackup(BACKUP_NAME).getLastCompaction()).isZero();

        Response enableBackupResponse = BackupApi.enableBackup(BACKUP_NAME);
        assertThat(enableBackupResponse.getStatusCode()).isEqualTo(202);
    }
}
