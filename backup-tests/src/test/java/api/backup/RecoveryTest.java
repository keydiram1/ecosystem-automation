package api.backup;

import api.backup.dto.RestoreSetRequest;
import com.aerospike.client.IAerospikeClient;
import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.*;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Tag("ADR-RECOVERY-TEST")
class RecoveryTest extends BackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns1";
    private static final String SOURCE_CLUSTER_NAME = "RecoveryClusterName";
    private static final String BACKUP_NAMESPACE = "adr-ns1";
    private static final String BACKUP_NAME = "RecoveryBackupName";
    private static final String POLICY_NAME = "RecoveryPolicy";
    private static final String DC_NAME = "RecoveryDC";
    private static final String SET_NAME = "setRecovery";
    private static long numRecordsInSourceAfterAddingData;
    private final IAerospikeClient srcClient = AerospikeDataUtils.getSourceClient();
    private final IAerospikeClient backupClient = AerospikeDataUtils.getBackupClient();

    @BeforeEach
    public void setUp() {
        DockerManager.startAndWaitForRestBackend();
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        ASBench.on(SOURCE_NAMESPACE, SET_NAME).duration(2).run();
        numRecordsInSourceAfterAddingData = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);
        AerospikeLogger.info("Number of records after adding data: " + numRecordsInSourceAfterAddingData);
    }

    @Test
    void restartRestBackendWhileBackup() {
        AerospikeLogger.info("Number of records after adding data: " + numRecordsInSourceAfterAddingData);
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);

        Awaitility.waitAtMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1)).await().untilAsserted(() -> {
                    long objectCountInBackup = AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME, BACKUP_NAMESPACE);
                    AerospikeLogger.info("Records count in backup: " + objectCountInBackup);
                    assertThat(objectCountInBackup).isPositive();
                });

        AutoUtils.runBashCommand("docker stop adr-rest-backend");

        long objectCountInBackup = AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME, BACKUP_NAMESPACE);
        assertThat(objectCountInBackup)
                .isPositive()
                .isLessThan(numRecordsInSourceAfterAddingData);

        DockerManager.startAndWaitForRestBackend();

        Awaitility.waitAtMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    long objectCountInBackup2 = AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME, BACKUP_NAMESPACE);
                    AerospikeLogger.info("Records count in backup: " + objectCountInBackup2);
                    AerospikeLogger.info("Records count in source after adding data: " + numRecordsInSourceAfterAddingData);
                    assertThat(objectCountInBackup2).isEqualTo(numRecordsInSourceAfterAddingData);
                });
    }

    @Test
    void restartRestBackendWhileRestore() {
        AerospikeLogger.info("Number of records after adding data: " + numRecordsInSourceAfterAddingData);
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);

        Awaitility.waitAtMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    long objectCountInBackup2 = AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME, BACKUP_NAMESPACE);
                    AerospikeLogger.info("Records count in backup: " + objectCountInBackup2);
                    AerospikeLogger.info("Records count in source after adding data: " + numRecordsInSourceAfterAddingData);
                    assertThat(objectCountInBackup2).isEqualTo(numRecordsInSourceAfterAddingData);
                });

        AerospikeDataUtils.truncateSourceSet(SOURCE_NAMESPACE, SET_NAME);
        long numRecordsInSourceAfterTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);
        assertThat(numRecordsInSourceAfterTruncate).isZero();

        Response restoreResponse = RestoreApi.restore("/v1/restore/set").body(RestoreSetRequest.builder()
                .fromTime(0).toTime(System.currentTimeMillis()).srcClusterName(SOURCE_CLUSTER_NAME)
                .trgClusterName(SOURCE_CLUSTER_NAME).srcNS(SOURCE_NAMESPACE).trgNS(SOURCE_NAMESPACE)
                .set(SET_NAME).build()).post();
        String restoreId = restoreResponse.body().asString();

        Awaitility.waitAtMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    long objectCountInBackup = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);
                    assertThat(objectCountInBackup).isPositive();
                });
        AutoUtils.runBashCommand("docker stop adr-rest-backend");

        long objectCountInSource = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);
        assertThat(objectCountInSource)
                .isPositive()
                .isLessThan(numRecordsInSourceAfterAddingData);

        DockerManager.startAndWaitForRestBackend();

        JobAPI.resumeJob(restoreId, 3);

        long numRecordsInSourceAfterRestore = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);
        AerospikeLogger.info("The number of records(replication-factor 2) that was in the source cluster after the data was added: "
                + numRecordsInSourceAfterRestore);
        AerospikeLogger.info("Records in source after adding data without replication factor: "
                + numRecordsInSourceAfterRestore / 2);
        assertThat(numRecordsInSourceAfterRestore).isEqualTo(numRecordsInSourceAfterAddingData);
    }

    @Test
    void deleteBackupData() {
        AerospikeLogger.info("Number of records after adding data: " + numRecordsInSourceAfterAddingData);
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);

        Awaitility.waitAtMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    long objectCountInBackup2 = AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME, BACKUP_NAMESPACE);
                    AerospikeLogger.info("Records count in backup: " + objectCountInBackup2);
                    AerospikeLogger.info("Records count in source after adding data: " + numRecordsInSourceAfterAddingData);
                    assertThat(objectCountInBackup2).isEqualTo(numRecordsInSourceAfterAddingData);
                });

        AdrLogHandler AdrLogHandler = new AdrLogHandler();
        Response response = MetadataAPI.deleteBackupData(BACKUP_NAME, SET_NAME, 0, System.currentTimeMillis());
        String restBackendLog = AdrLogHandler.getRestBackendLog();

        String deleteBackupDataID = StringUtils.substringBetween(restBackendLog, "Background job ", " ");
        AerospikeLogger.info("deleteBackupDataID: " + deleteBackupDataID);
        assertThat(response.getStatusCode()).isEqualTo(202);

        DockerManager.stopContainer(DockerManager.REST_BACKEND);
        DockerManager.startAndWaitForRestBackend();
        try {
            JobAPI.resumeJob(deleteBackupDataID, 1);
        } catch (Exception e) {
            AutoUtils.runBashCommand("docker logs --tail 300 adr-rest-backend");
            fail("resume job failed due to: " + e.getMessage());
        }
        // Wait till all the records are deleted from the backup cluster
        Awaitility.waitAtMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    long objectCountInBackup = AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME, BACKUP_NAMESPACE);
                    AerospikeLogger.info("Records count in backup: " + objectCountInBackup);
                    assertThat(objectCountInBackup).isZero();
                });
    }
}
