package api.backup.end2end;

import api.backup.BackupManager;
import api.backup.RetrieveAPI.WhatToRetrieve;
import api.backup.RetrieveAPI;
import api.backup.dto.RetrieveEntityRecord;
import com.aerospike.client.Key;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ADR-E2E")
class RetrieveTest extends BackupRunner {
    private static final String SET_NAME = "RetrieveTestSet";
    private static final String SOURCE_NAMESPACE = "source-ns8";
    private static final String SOURCE_CLUSTER_NAME = "RetrieveTestSrcCluster";
    private static final String BACKUP_NAMESPACE = "adr-ns8";
    private static final String INITIAL_VALUE = "RetrieveTestInitialValue";
    private static final String UPDATED_VALUE = "RetrieveTestUpdatedValue";
    private static final String BACKUP_NAME = "RetrieveTestContinuousBackup";
    private static final String POLICY_NAME = "RetrieveTestPolicy";
    private static final String DC_NAME = "RetrieveDC";
    private static long afterFirstBackup;
    private static long afterSecondBackup;
    private static long afterFirstBackupDeleteKey;
    private static long afterDeletion;
    private static final Key KEY_RESTORE_RETRIEVE_TEST = new Key(SOURCE_NAMESPACE, SET_NAME, "IT");
    private static final String DIGEST_RESTORE_RETRIEVE_TEST = AerospikeDataUtils.getDigestFromKey(KEY_RESTORE_RETRIEVE_TEST);
    private static final Key DELETE_KEY_RETRIEVE_TEST = new Key(SOURCE_NAMESPACE, SET_NAME, "DeleteKey");
    private static final String DELETE_DIGEST_RETRIEVE_TEST = AerospikeDataUtils.getDigestFromKey(DELETE_KEY_RETRIEVE_TEST);
    private static final Key UNEXISTING_KEY = new Key(SOURCE_NAMESPACE, SET_NAME, "unexisting");
    private static final String UNEXISTING_DIGEST = AerospikeDataUtils.getDigestFromKey(UNEXISTING_KEY);

    @BeforeEach
    public void setUp() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);
        prepareData();
    }

    @AfterAll
    static void afterAll() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @Test
    void retrieveLatest() {
        // digest in upper case just to test that retrieve works with upper case digest as well
        List<RetrieveEntityRecord> retrieve = RetrieveAPI.retrieve(WhatToRetrieve.LATEST, afterFirstBackup, DIGEST_RESTORE_RETRIEVE_TEST.toUpperCase(), SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, SET_NAME);
        assertThat(retrieve.toString())
                .contains(INITIAL_VALUE)
                .doesNotContain(UPDATED_VALUE);

        long initialTimeStamp = System.currentTimeMillis();
        String retrieveUpdated = RetrieveAPI.retrieve(WhatToRetrieve.LATEST, initialTimeStamp, DIGEST_RESTORE_RETRIEVE_TEST, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, SET_NAME)
                .toString();
        assertThat(retrieveUpdated)
                .doesNotContain(INITIAL_VALUE)
                .contains(UPDATED_VALUE);
    }

    @Test
    void retrieveAll() {
        long initialTimeStamp = System.currentTimeMillis();
        String retrieved = RetrieveAPI.retrieve(WhatToRetrieve.ALL, initialTimeStamp, DIGEST_RESTORE_RETRIEVE_TEST, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, SET_NAME).toString();
        assertThat(retrieved)
                .contains(INITIAL_VALUE)
                .contains(UPDATED_VALUE);

        String retrieveInitial = RetrieveAPI.retrieve(WhatToRetrieve.ALL, afterFirstBackup, DIGEST_RESTORE_RETRIEVE_TEST, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, SET_NAME)
                .toString();
        assertThat(retrieveInitial)
                .contains(INITIAL_VALUE)
                .doesNotContain(UPDATED_VALUE);
    }

    @Test
    void retrieveLatestAfterDeletion() {
        List<RetrieveEntityRecord> retrieve = RetrieveAPI.retrieve(WhatToRetrieve.LATEST, afterFirstBackupDeleteKey, DELETE_DIGEST_RETRIEVE_TEST, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, SET_NAME);
        assertThat(retrieve.get(0).isDelete()).isFalse();
        assertThat(retrieve.toString())
                .contains(INITIAL_VALUE);

        List<RetrieveEntityRecord> retrieveDeleted = RetrieveAPI.retrieve(WhatToRetrieve.LATEST, afterDeletion, DELETE_DIGEST_RETRIEVE_TEST, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, SET_NAME);
        assertThat(retrieveDeleted.get(0).isDelete()).isTrue();
    }

    @Test
    void retrieveAllAfterDeletion() {
        long afterAllOperations = System.currentTimeMillis();
        List<RetrieveEntityRecord> retrieve = RetrieveAPI.retrieve(WhatToRetrieve.ALL, afterSecondBackup, afterAllOperations, DELETE_DIGEST_RETRIEVE_TEST, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, SET_NAME);
        assertThat(retrieve).hasSize(2);
        // One of the operations was a deletion
        assertThat(retrieve.stream().anyMatch(RetrieveEntityRecord::isDelete)).isTrue();
    }

    @Test
    void noDataRetrieved() {
        List<RetrieveEntityRecord> before = RetrieveAPI.retrieve(WhatToRetrieve.ALL, 1000_000L, DIGEST_RESTORE_RETRIEVE_TEST, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, SET_NAME);
        assertThat(before).isEmpty();
        List<RetrieveEntityRecord> after = RetrieveAPI.retrieve(WhatToRetrieve.ALL, afterSecondBackup, System.currentTimeMillis(), DIGEST_RESTORE_RETRIEVE_TEST, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, SET_NAME);
        assertThat(after).isEmpty();
        List<RetrieveEntityRecord> unexisting = RetrieveAPI.retrieve(WhatToRetrieve.ALL, System.currentTimeMillis(), UNEXISTING_DIGEST, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, SET_NAME);
        assertThat(unexisting).isEmpty();
        List<RetrieveEntityRecord> wrongSet = RetrieveAPI.retrieve(WhatToRetrieve.ALL, System.currentTimeMillis(), UNEXISTING_DIGEST, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, "wrongSet");
        assertThat(wrongSet).isEmpty();
    }

    private void prepareData() {
        // Prepare data for retrieve tests
        AerospikeDataUtils.put(KEY_RESTORE_RETRIEVE_TEST, "value", INITIAL_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, KEY_RESTORE_RETRIEVE_TEST, 1);

        afterFirstBackup = System.currentTimeMillis();
        AerospikeDataUtils.put(KEY_RESTORE_RETRIEVE_TEST, "value", UPDATED_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, KEY_RESTORE_RETRIEVE_TEST, 2);
        afterSecondBackup = System.currentTimeMillis();

        // Prepare data for retrieve after deletion
        AerospikeDataUtils.put(DELETE_KEY_RETRIEVE_TEST, "value", INITIAL_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, DELETE_KEY_RETRIEVE_TEST, 1);

        afterFirstBackupDeleteKey = System.currentTimeMillis();
        AerospikeDataUtils.delete(DELETE_KEY_RETRIEVE_TEST);
        BackupManager.waitForBackup(BACKUP_NAME, DELETE_KEY_RETRIEVE_TEST, 2);
        afterDeletion = System.currentTimeMillis();
    }
}
