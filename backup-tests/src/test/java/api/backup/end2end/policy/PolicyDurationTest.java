package api.backup.end2end.policy;

import api.backup.BackupApi;
import api.backup.ClusterConnectionApi;
import api.backup.BackupManager;
import api.backup.PolicyApi;
import api.backup.dto.ClusterConnection;
import com.aerospike.client.Key;
import io.restassured.response.Response;
import org.assertj.core.data.Offset;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import utils.AerospikeLogger;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import java.time.Duration;
import java.util.List;

import static java.util.concurrent.TimeUnit.*;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("ADR-E2E")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PolicyDurationTest extends BackupRunner {
    private static final String SET_NAME = "setPolicyDuration";
    private static final String SOURCE_NAMESPACE = "source-ns9";
    private static final String SOURCE_CLUSTER_NAME = "PolicyDurationTestCluster";
    private static final String BACKUP_NAMESPACE = "adr-ns9";
    private static final String INITIAL_VALUE = "initialValuePolicyDuration";
    private static final String UPDATED_VALUE = "updatedValuePolicyDuration";
    private static final String BACKUP_NAME = "PolicyDurationTestBackup";
    private static final String DC_NAME = "PolicyDurationDC";

    private static final String POLICY_NAME = "TestPolicyDuration";
    private String backupDCName;

    @BeforeEach
    public void setUp() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        prepareConfiguration(POLICY_NAME, 1, 0, 0, 100000, 0);
    }

    @AfterAll
    static void tearDown() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @Test
    @Order(1)
    void testPolicyDurationShort() {
        PolicyApi.updatePolicy(POLICY_NAME, 1);
        waitForThroughput(true);
        waitForThroughput(false);
        waitForThroughput(true);

        Key key = new Key(SOURCE_NAMESPACE, SET_NAME, "PolicyDurationKey1");
        AerospikeDataUtils.put(key, "value", INITIAL_VALUE);
        Duration backupDuration = BackupManager.waitForBackup(BACKUP_NAME, key, 1);
        assertThat(backupDuration.getSeconds()).isLessThan(10);
    }

    @Test
    @Order(2)
    void testPolicyDurationLong() {
        int duration = 30;
        PolicyApi.updatePolicy(POLICY_NAME, duration);

        Awaitility.await().pollInterval(Duration.ofSeconds(1))
                .until(() -> PolicyApi.getPolicy(POLICY_NAME).getDuration() == duration);

        Key key = new Key(SOURCE_NAMESPACE, SET_NAME, "PolicyDurationKey2");
        AerospikeDataUtils.put(key, "value", INITIAL_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, key, 1);
        waitForThroughput(true);
        waitForThroughput(false);
        waitForThroughput(true);
        AerospikeDataUtils.put(key, "value", UPDATED_VALUE);

        Duration backupDuration = BackupManager.waitForBackup(BACKUP_NAME, key, 2);
        assertThat((int)backupDuration.getSeconds())
                .isCloseTo(duration, Offset.offset(10));
    }

    @Test
    void testMaxThroughputUpdated() {
        int maxThroughput = 20_000;
        PolicyApi.updatePolicy(POLICY_NAME, 1, 0, 0, maxThroughput, 0);
        Awaitility.await()
                .atMost(30, SECONDS)
                .pollInterval(200, MILLISECONDS)
                .until(() -> {
                    AerospikeLogger.info(AerospikeDataUtils.getXdrConfig(backupDCName, SOURCE_NAMESPACE));
                    String xdrConfigurationInSource = AerospikeDataUtils.getXdrConfig(backupDCName, SOURCE_NAMESPACE);
                    return xdrConfigurationInSource.contains("max-throughput=" + maxThroughput);
                });
    }

    private long waitForThroughput(Boolean needZero) {
        long start = System.currentTimeMillis();
        Awaitility.await()
                .atMost(2, MINUTES)
                .pollInterval(100, MILLISECONDS)
                .until(() -> {
                    String xdrConfigurationInSource = AerospikeDataUtils.getXdrConfig(backupDCName, SOURCE_NAMESPACE);
                    boolean isZero = xdrConfigurationInSource.contains("max-throughput=0");
                    return needZero == isZero;
                });
        return System.currentTimeMillis() - start;
    }

    @SuppressWarnings("SameParameterValue")
    private void prepareConfiguration(String policyName, int duration, int retention,
                                             int keepFor, int maxThroughput, Integer initialSync) {
        Response policyResponse = PolicyApi.createPolicy(policyName, duration, retention, keepFor, maxThroughput, initialSync);
        assertThat(policyResponse.getStatusCode()).isEqualTo(201);
        ClusterConnectionApi.createConnection(SOURCE_CLUSTER_NAME, DC_NAME);
        ClusterConnection clusterConnection = ClusterConnectionApi.getClusterConnection(SOURCE_CLUSTER_NAME);
        assertThat(clusterConnection).isNotNull();
        backupDCName = clusterConnection.getBackupDCName();
        Response backupResponse = BackupApi.createBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE,
                policyName, List.of(SET_NAME));

        assertThat(backupResponse.getStatusCode()).isEqualTo(201);

        Response enableBackupResponse = BackupApi.enableBackup(BACKUP_NAME);
        assertThat(enableBackupResponse.getStatusCode()).isEqualTo(202);
    }
}
