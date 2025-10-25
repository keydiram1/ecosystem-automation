package api.backup;

import api.backup.dto.RestoreSetRequest;
import com.aerospike.client.IAerospikeClient;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AdrLogHandler;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("ADR-TWO-SOURCE-CLUSTERS")
@Execution(ExecutionMode.CONCURRENT)
public class TwoSourcesTest extends BackupRunner {
    // We use the same namespace name in two different Aerospike clusters (same for DC name).
    private static final String SOURCE_NAMESPACE = "source-ns1";
    private static final String SOURCE2_NAMESPACE = "source-ns16";
    private static final String SOURCE_CLUSTER_NAME = "TwoSourcesTestClusterName";
    private static final String SOURCE_CLUSTER_NAME2 = "TwoSourcesTestClusterName2";
    private static final String BACKUP_NAMESPACE = "adr-ns1";
    private static final String BACKUP_NAMESPACE2 = "adr-ns16";
    private static final String BACKUP_NAME = "TwoSourcesTestBackupName";
    private static final String BACKUP_NAME2 = "TwoSourcesTestBackupName2";
    private static final String POLICY_NAME = "TwoSourcesTestPolicy";
    private static final String POLICY_NAME2 = "TwoSourcesTestPolicy2";
    private static final String DC_NAME = "TwoSourcesDC";
    private static final String SET_NAME = "setTwoSourcesTest";
    private static final String SET_NAME2 = "setTwoSourcesTest2";
    private static final IAerospikeClient srcClient = AerospikeDataUtils.getSourceClient();
    private static final IAerospikeClient srcClient2 = AerospikeDataUtils.createSourceClient(3008);
    private static final IAerospikeClient backupClient = AerospikeDataUtils.getBackupClient();
    private static long numRecordsInSourceAfterAddingData;
    private static long numRecordsInSourceAfterAddingData2;

    @BeforeAll
    public static void setUp() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        BackupManager.cleanUp(BACKUP_NAMESPACE2, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME2, BACKUP_NAME2, POLICY_NAME2);

        ASBench.on(SOURCE_NAMESPACE, SET_NAME).port(3000).run();
        ASBench.on(SOURCE2_NAMESPACE, SET_NAME2).port(3008).run();

        numRecordsInSourceAfterAddingData = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);
        numRecordsInSourceAfterAddingData2 = AerospikeCountUtils.getSetObjectCount(srcClient2, SET_NAME2, SOURCE2_NAMESPACE);
        assertThat(numRecordsInSourceAfterAddingData).isPositive();
        assertThat(numRecordsInSourceAfterAddingData2).isPositive();
    }

    @Test
    void runBackup1() {
        AdrLogHandler adrLogHandler = new AdrLogHandler();
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);

        AerospikeLogger.info("Records count in source1: " + numRecordsInSourceAfterAddingData);

        Awaitility.waitAtMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(5)).await().untilAsserted(() -> {
                    long objectCountInBackup = AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME, BACKUP_NAMESPACE);
                    AerospikeLogger.info("Records count in backup: " + objectCountInBackup);
                    AerospikeLogger.info("Records count in source after adding data: " + numRecordsInSourceAfterAddingData);
                    assertThat(objectCountInBackup).isEqualTo(numRecordsInSourceAfterAddingData);
                });

        // Start the restore process
        AerospikeDataUtils.truncateSourceSet(SOURCE_NAMESPACE, SET_NAME);
        long numRecordsInSourceAfterTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);
        assertThat(numRecordsInSourceAfterTruncate).isZero();

        long restored = RestoreApi.restoreSet(RestoreSetRequest.builder().fromTime(0L).toTime(System.currentTimeMillis())
                .srcClusterName(SOURCE_CLUSTER_NAME).trgClusterName(SOURCE_CLUSTER_NAME).srcNS(SOURCE_NAMESPACE)
                .trgNS(SOURCE_NAMESPACE).set(SET_NAME).build()).getProcessed();

        AerospikeLogger.info("Restored records: " + restored);
        long numRecordsInSourceAfterRestore = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);
        assertThat(numRecordsInSourceAfterRestore).isEqualTo(numRecordsInSourceAfterAddingData);
        Assertions.assertThat(adrLogHandler.getRestBackendLog()).doesNotContain("ERROR");
        Assertions.assertThat(adrLogHandler.getQueueHandlerLog()).doesNotContain("ERROR");
    }

    @Test
    void runBackup2() {
        createSecondEnabledBackup(BACKUP_NAME2, SOURCE_CLUSTER_NAME2, SOURCE2_NAMESPACE, BACKUP_NAMESPACE2, POLICY_NAME2, DC_NAME);

        AerospikeLogger.info("Records count in source2: " + numRecordsInSourceAfterAddingData2);

        AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME2, BACKUP_NAMESPACE2);

        Awaitility.waitAtMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(5)).await().untilAsserted(() -> {
                    long objectCountInBackup2 = AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME2, BACKUP_NAMESPACE2);
                    AerospikeLogger.info("Records count in backup: " + objectCountInBackup2);
                    AerospikeLogger.info("Records count in source2 after adding data: " + numRecordsInSourceAfterAddingData2);
                    assertThat(objectCountInBackup2).isEqualTo(numRecordsInSourceAfterAddingData2);
                });

        // Start the restore process
        AerospikeDataUtils.truncateSet(srcClient2, SOURCE2_NAMESPACE, SET_NAME2);
        long numRecordsInSourceAfterTruncate = AerospikeCountUtils.getSetObjectCount(srcClient2, SET_NAME2, SOURCE2_NAMESPACE);
        assertThat(numRecordsInSourceAfterTruncate).isZero();

        long restored = RestoreApi.restoreSet(RestoreSetRequest.builder().fromTime(0L).toTime(System.currentTimeMillis())
                .srcClusterName(SOURCE_CLUSTER_NAME2).trgClusterName(SOURCE_CLUSTER_NAME2).srcNS(SOURCE2_NAMESPACE)
                .trgNS(SOURCE2_NAMESPACE).set(SET_NAME2).build()).getProcessed();

        AerospikeLogger.info("Restored records: " + restored);
        long numRecordsInSourceAfterRestore = AerospikeCountUtils.getSetObjectCount(srcClient2, SET_NAME2, SOURCE2_NAMESPACE);
        assertThat(numRecordsInSourceAfterRestore).isEqualTo(numRecordsInSourceAfterAddingData2);
    }

    @SuppressWarnings("SameParameterValue")
    private static void createSecondEnabledBackup(String backupName, String sourceClusterName, String sourceNS, String backupNS,
                                                 String policyName, String dcName) {
        Response policyResponse = PolicyApi.createPolicy(policyName, 1);
        assertEquals(201, policyResponse.getStatusCode(), () -> policyResponse.getBody().asPrettyString());

        Response connectionResponse = ClusterConnectionApi.createConnection(sourceClusterName, dcName, 86400, 3008);
        assertEquals(201, connectionResponse.getStatusCode(), () -> connectionResponse.getBody().asPrettyString());

        Response backupResponse = createBackup(backupName, sourceClusterName, sourceNS, backupNS, policyName, null);
        assertEquals(201, backupResponse.getStatusCode(), () -> backupResponse.getBody().asPrettyString());

        Response enableBackupResponse = BackupApi.enableBackup(backupName);
        assertEquals(202, enableBackupResponse.getStatusCode(), () -> enableBackupResponse.getBody().asPrettyString());
    }

    @SuppressWarnings("SameParameterValue")
    private static Response createBackup(String backupName, String sourceClusterName, String sourceNS, String backupNS,
                                         String policyName, List<String> sets) {
        if (sets != null) {
            return BackupApi.createBackup(backupName, sourceClusterName, sourceNS, backupNS, policyName, sets);
        }
        return BackupApi.createBackup(backupName, sourceClusterName, sourceNS, backupNS, policyName);
    }
}
