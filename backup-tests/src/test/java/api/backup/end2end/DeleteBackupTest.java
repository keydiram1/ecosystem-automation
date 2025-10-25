package api.backup.end2end;

import api.backup.BackupManager;
import api.backup.MetadataAPI;
import api.backup.RetrieveAPI;
import api.backup.RetrieveAPI.WhatToRetrieve;
import api.backup.dto.RetrieveEntityRecord;
import com.aerospike.client.Key;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ADR-E2E")
class DeleteBackupTest extends BackupRunner {
    private static final String SET_NAME = "BackupMetadataTestSet";
    private static final String SOURCE_NAMESPACE = "source-ns14";
    private static final String SOURCE_CLUSTER_NAME = "BackupMetadataTestSrcCluster";
    private static final String BACKUP_NAMESPACE = "adr-ns14";
    private static final String INITIAL_VALUE = "BackupMetadataTestInitialValue";
    private static final String UPDATED_VALUE = "BackupMetadataTestUpdatedValue";
    private static final String BACKUP_NAME = "BackupMetadataTestContinuousBackup";
    private static final String POLICY_NAME = "BackupMetadataTestPolicy";
    private static final String DC_NAME = "BackupMetadataDC";
    private static long afterFirstBackup;
    private static final Key KEY_BACKUP_METADATA_TEST = new Key(SOURCE_NAMESPACE, SET_NAME, "IT");
    private static final String DIGEST_BACKUP_METADATA_TEST = AerospikeDataUtils.getDigestFromKey(KEY_BACKUP_METADATA_TEST);

    @BeforeEach
    public void setUp() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);
    }

    @AfterAll
    static void afterAll() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @Test
    void deleteUpdatedBackupData() {
        prepareTwoSpreadBackups();
        long toTime = System.currentTimeMillis();
        printTimestamps(toTime, "Delete backups between " + afterFirstBackup + " and now (" + toTime + ")");
        Response response = MetadataAPI.deleteBackupData(BACKUP_NAME, SET_NAME, afterFirstBackup, toTime);
        assertThat(response.getStatusCode()).isEqualTo(202);

        Awaitility.waitAtMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(5))
                .alias("Backup should contain only initial value")
                .failFast(() -> assertThat(retrieve()).isNotEmpty())
                .untilAsserted(() -> {
                    String retrieve = retrieve().toString();
                    assertThat(retrieve)
                            .contains(INITIAL_VALUE)
                            .doesNotContain(UPDATED_VALUE);
                });
    }

    @Test
    void deleteInitialBackupData() {
        prepareTwoSpreadBackups();
        long toTime = System.currentTimeMillis();
        printTimestamps(toTime, "Delete backups before " + afterFirstBackup);
        Response response = MetadataAPI.deleteBackupData(BACKUP_NAME, SET_NAME, 0, afterFirstBackup);
        assertThat(response.getStatusCode()).isEqualTo(202);

        Awaitility.waitAtMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(5))
                .alias("Backup should contain only initial value")
                .failFast(() -> assertThat(retrieve()).isNotEmpty())
                .untilAsserted(() -> {
                    String retrieve = retrieve().toString();
                    assertThat(retrieve)
                            .doesNotContain(INITIAL_VALUE)
                            .contains(UPDATED_VALUE);
                });
    }

    @Test
    void deleteAllBackupData() {
        prepareTwoSpreadBackups();
        long toTime = System.currentTimeMillis();
        printTimestamps(toTime, "Delete backups before " + toTime);
        Response response = MetadataAPI.deleteBackupData(BACKUP_NAME, SET_NAME, 0, toTime);
        assertThat(response.getStatusCode()).isEqualTo(202);

        Awaitility.waitAtMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(5))
                .alias("Backup should be empty")
                .until(() -> retrieve().isEmpty());
    }

    private static void printTimestamps(long toTime, String label) {
        List<Long> ts = retrieve().stream().map(RetrieveEntityRecord::getTimestamp).sorted().toList();
        AerospikeLogger.info("\n1st: \t" + ts.get(0) + "\nafter:\t" + afterFirstBackup + "\n2nd:\t" + ts.get(1) + "\nnow:\t" + toTime);
        AerospikeLogger.info(label);
    }

    @Test
    void deleteCloseBackups() {
        prepareTwoCloseBackups();
        long toTime = System.currentTimeMillis();
        printTimestamps(toTime, "Delete backups before " + afterFirstBackup);
        Response response = MetadataAPI.deleteBackupData(BACKUP_NAME, SET_NAME, 0, afterFirstBackup);
        assertThat(response.getStatusCode()).isEqualTo(202);

        Awaitility.waitAtMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(5))
                .alias("Backup should be empty")
                .until(() -> retrieve().isEmpty());
    }

    private static List<RetrieveEntityRecord> retrieve() {
        long initialTimeStamp = System.currentTimeMillis();
        return RetrieveAPI.retrieve(WhatToRetrieve.ALL, initialTimeStamp, DIGEST_BACKUP_METADATA_TEST, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, SET_NAME);
    }

    //each backup is in its own batch
    private void prepareTwoSpreadBackups() {
        AerospikeDataUtils.put(KEY_BACKUP_METADATA_TEST, "value", INITIAL_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, KEY_BACKUP_METADATA_TEST, 1);

        AutoUtils.sleep(5000);
        afterFirstBackup = System.currentTimeMillis() - 2500;

        AerospikeDataUtils.put(KEY_BACKUP_METADATA_TEST, "value", UPDATED_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, KEY_BACKUP_METADATA_TEST, 2);

        String retrieveLatest = retrieve().toString();
        assertThat(retrieveLatest)
                .contains(INITIAL_VALUE)
                .contains(UPDATED_VALUE);
    }

    private void prepareTwoCloseBackups() {
        AerospikeDataUtils.put(KEY_BACKUP_METADATA_TEST, "value", INITIAL_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, KEY_BACKUP_METADATA_TEST, 1);
        afterFirstBackup = System.currentTimeMillis();

        AerospikeDataUtils.put(KEY_BACKUP_METADATA_TEST, "value", UPDATED_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, KEY_BACKUP_METADATA_TEST, 2);
        String retrieveLatest = retrieve().toString();
        assertThat(retrieveLatest)
                .contains(INITIAL_VALUE)
                .contains(UPDATED_VALUE);
    }
}
