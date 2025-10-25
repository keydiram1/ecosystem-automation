package api.backup.negative;

import api.backup.BackupManager;
import api.backup.RetrieveAPI.WhatToRetrieve;
import api.backup.RetrieveAPI;
import com.aerospike.client.Key;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ADR-NEGATIVE-TESTS-E2E")
class RetrieveNegativeTest extends BackupRunner {
    private static final String SET_NAME = "RetrieveTestSet";
    private static final String SOURCE_NAMESPACE = "source-ns5";
    private static final String SOURCE_CLUSTER_NAME = "RetrieveNegativeCluster";
    private static final String BACKUP_NAMESPACE = "adr-ns5";
    private static final String BACKUP_NAME = "RetrieveNegativeBackup";
    private static final String POLICY_NAME = "RetrieveNegativePolicy";
    private static final String DC_NAME = "RetrieveNegativeDC";
    private static final Key KEY_RESTORE_RETRIEVE_TEST = new Key(SOURCE_NAMESPACE, SET_NAME, "IT");
    private static final String DIGEST_RESTORE_RETRIEVE_TEST = AerospikeDataUtils.getDigestFromKey(KEY_RESTORE_RETRIEVE_TEST);

    @AfterAll
    static void tearDown() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @BeforeEach
    public void setUp() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);
    }

    @Test
    void retrieveLatestToTimeInvalidHTTP400() {
        int invalidToTime = -1;
        Response response = RetrieveAPI.retrieveResponse(WhatToRetrieve.LATEST, invalidToTime, DIGEST_RESTORE_RETRIEVE_TEST, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, SET_NAME);
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void retrieveSrcClusterNameNull() {
        Response response = RetrieveAPI.retrieveResponse(WhatToRetrieve.LATEST, 1, DIGEST_RESTORE_RETRIEVE_TEST, null, SOURCE_NAMESPACE, SET_NAME);
        assertThat(response.body().asPrettyString()).contains("must not be blank");
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void retrieveLatestNotExistClusterHTTP404() {
        String notExistCluster = "notExistCluster";
        Response response = RetrieveAPI.retrieveResponse(WhatToRetrieve.LATEST, 1, DIGEST_RESTORE_RETRIEVE_TEST, notExistCluster, SOURCE_NAMESPACE, SET_NAME);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void retrieveLatestNotExistSourceNsHTTP404() {
        String notExistSourceNS = "notExistSourceNS";
        Response response = RetrieveAPI.retrieveResponse(WhatToRetrieve.LATEST, 1, DIGEST_RESTORE_RETRIEVE_TEST, SOURCE_CLUSTER_NAME, notExistSourceNS, SET_NAME);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void retrieveAllToTimeInvalidHTTP400() {
        int invalidToTime = -1;
        Response response = RetrieveAPI.retrieveResponse(WhatToRetrieve.ALL, invalidToTime, DIGEST_RESTORE_RETRIEVE_TEST, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, SET_NAME);
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void retrieveAllNotExistClusterHTTP404() {
        String notExistCluster = "notExistCluster";
        Response response = RetrieveAPI.retrieveResponse(WhatToRetrieve.ALL, 1, DIGEST_RESTORE_RETRIEVE_TEST, notExistCluster, SOURCE_NAMESPACE, SET_NAME);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void retrieveAllNotExistSourceNsHTTP404() {
        String notExistSourceNS = "notExistSourceNS";
        Response response = RetrieveAPI.retrieveResponse(WhatToRetrieve.ALL, 1, DIGEST_RESTORE_RETRIEVE_TEST, SOURCE_CLUSTER_NAME, notExistSourceNS, SET_NAME);
        assertThat(response.statusCode()).isEqualTo(404);
    }
}
