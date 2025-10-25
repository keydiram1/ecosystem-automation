package api.backup.end2end;

import api.backup.JobAPI;
import api.backup.BackupManager;
import api.backup.RestoreApi;
import api.backup.dto.BackgroundJob;
import api.backup.dto.RestoreSetRequest;
import com.aerospike.client.IAerospikeClient;
import io.restassured.response.Response;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.aerospike.AerospikeCountUtils;
import utils.init.runners.BackupRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ADR-E2E")
public class CancelRestoreTest extends BackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns15";
    private static final String SOURCE_CLUSTER_NAME = "CancelJobClusterName";
    private static final String BACKUP_NAMESPACE = "adr-ns15";
    private static final String BACKUP_NAME = "CancelJobBackupName";
    private static final String POLICY_NAME = "CancelJobPolicy";
    private static final String SET_NAME = "setCancelJob";
    private static final String DC_NAME = "CancelJobDC";
    private final IAerospikeClient srcClient = AerospikeDataUtils.getSourceClient();
    private final IAerospikeClient backupClient = AerospikeDataUtils.getBackupClient();
    private static long numRecordsInSourceAfterAddingData;

    @SneakyThrows
    @BeforeEach
    public void setUp() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);

        ASBench.on(SOURCE_NAMESPACE, SET_NAME).duration(1).run();
        Awaitility.await().untilAsserted(() -> {
            numRecordsInSourceAfterAddingData = sourceObjectsCount();
            assertThat(numRecordsInSourceAfterAddingData).isPositive();
        });
        AerospikeLogger.info("Number of records after adding data: " + numRecordsInSourceAfterAddingData);
    }

    @AfterAll
    static void afterAll() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @Test
    void cancelRestore() {
        AerospikeLogger.info("Number of records after adding data: " + numRecordsInSourceAfterAddingData);
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);

        Awaitility.waitAtMost(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    long objectCountInBackup = backupsObjectCounts();
                    AerospikeLogger.info("Records count in backup: " + objectCountInBackup);
                    assertThat(objectCountInBackup).isEqualTo(numRecordsInSourceAfterAddingData);
                });

        AerospikeDataUtils.truncateSourceSet(SOURCE_NAMESPACE, SET_NAME);

        RestoreSetRequest restoreSetRequest = RestoreSetRequest.builder()
                .fromTime(0)
                .toTime(System.currentTimeMillis())
                .srcClusterName(SOURCE_CLUSTER_NAME)
                .trgClusterName(SOURCE_CLUSTER_NAME)
                .srcNS(SOURCE_NAMESPACE)
                .trgNS(SOURCE_NAMESPACE)
                .set(SET_NAME)
                .build();

        Response restoreResponse = RestoreApi.restore("/v1/restore/set").body(restoreSetRequest).post();
        String restoreJobId = restoreResponse.body().asString();

        Awaitility.waitAtMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .alias("Restore started")
                .untilAsserted(() -> assertThat(sourceObjectsCount()).isPositive());

        JobAPI.cancelJob(restoreJobId);

        // wait until number of records in source (restored) stops increasing
        Awaitility.await().untilAsserted(() -> {
            Long initialBackupSize = sourceObjectsCount();
            AutoUtils.sleep(1_000);
            Long currentBackupSize = sourceObjectsCount();
            assertThat(currentBackupSize).isEqualTo(initialBackupSize);
        });

        long objectCountInSource = sourceObjectsCount();
        AerospikeLogger.info("Records count in source after cancel job: " + objectCountInSource);
        assertThat(objectCountInSource).isLessThan(numRecordsInSourceAfterAddingData);
        AutoUtils.sleep(10_000);
        long objectCountInSourceAfterSleep = sourceObjectsCount();
        AerospikeLogger.info("Records count in source after sleep: " + objectCountInSourceAfterSleep);
        assertThat(objectCountInSource).isEqualTo(objectCountInSourceAfterSleep);
        BackgroundJob.BackgroundJobStatus status = JobAPI.getJob(restoreJobId).getStatus();
        assertThat(status).isEqualTo(BackgroundJob.BackgroundJobStatus.CANCELED);
    }

    private long backupsObjectCounts() {
        return AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME, BACKUP_NAMESPACE);
    }

    private long sourceObjectsCount() {
        return AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);
    }
}
