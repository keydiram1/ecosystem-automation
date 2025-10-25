package api.backup.negative;

import api.backup.BackupManager;
import api.backup.RestoreApi;
import api.backup.dto.RestoreSetRequest;
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
class RestoreNegativeTest extends BackupRunner {
    private static final String SET_NAME = "RestoreTestSet";
    private static final String SOURCE_NAMESPACE = "source-ns4";
    private static final String TARGET_NAMESPACE = "RestoreTestNS";
    private static final String SOURCE_CLUSTER_NAME = "RestoreNegativeCluster";
    private static final String TARGET_CLUSTER_NAME = "RestoreTestSrcCluster";
    private static final String BACKUP_NAMESPACE = "adr-ns4";
    private static final String BACKUP_NAME = "RestoreNegativeBackup";
    private static final String POLICY_NAME = "RestoreNegativePolicy";
    private static final String DC_NAME = "RestoreNegativeDC";
    private static final Key RESTORE_TEST_KEY = new Key(SOURCE_NAMESPACE, SET_NAME, "IT1");
    private static final String RESTORE_TEST_DIGEST = AerospikeDataUtils.getDigestFromKey(RESTORE_TEST_KEY);

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
    void restoreRecordsTimeStampLessThan0InvalidParameterHTTP400() {
        Response response = RestoreApi.restoreRecordResponse(-1, RESTORE_TEST_DIGEST, SET_NAME, SOURCE_CLUSTER_NAME,
                TARGET_CLUSTER_NAME, SOURCE_NAMESPACE, TARGET_NAMESPACE);
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void restoreRecordsTargetNsNull() {
        Response response = RestoreApi.restoreRecordResponse(1, RESTORE_TEST_DIGEST, SET_NAME, SOURCE_CLUSTER_NAME,
                TARGET_CLUSTER_NAME, SOURCE_NAMESPACE, null);
        assertThat(response.body().asPrettyString()).contains("must not be blank");
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void restoreRecordNotExistSourceNSInvalidParameterHTTP404() {
        String notExistSourceNamespace = "notExistSourceNamespace";
        Response response = RestoreApi.restoreRecordResponse(1, RESTORE_TEST_DIGEST, SET_NAME, SOURCE_CLUSTER_NAME,
                TARGET_CLUSTER_NAME, notExistSourceNamespace, TARGET_NAMESPACE);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void restoreRecordNotExistTargetNSInvalidParameterHTTP404() {
        String notExistTargetNamespace = "notExistTargetNamespace";
        Response response = RestoreApi.restoreRecordResponse(1, RESTORE_TEST_DIGEST, SET_NAME, SOURCE_CLUSTER_NAME,
                TARGET_CLUSTER_NAME, SOURCE_NAMESPACE, notExistTargetNamespace);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void restoreRecordNotExistSourceClusterNameInvalidParameterHTTP404() {
        String notExistSourceClusterName = "notExistSourceClusterName";
        Response response = RestoreApi.restoreRecordResponse(1, RESTORE_TEST_DIGEST, SET_NAME, notExistSourceClusterName,
                TARGET_CLUSTER_NAME, SOURCE_NAMESPACE, TARGET_NAMESPACE);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void restoreRecordNotExistTargetClusterNameInvalidParameterHTTP404() {
        String notExistTargetClusterName = "notExistTargetClusterName";
        Response response = RestoreApi.restoreRecordResponse(1, RESTORE_TEST_DIGEST, SET_NAME, SOURCE_CLUSTER_NAME,
                notExistTargetClusterName, SOURCE_NAMESPACE, TARGET_NAMESPACE);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void restoreNamespaceFromTimeStampLessThan0InvalidParameterHTTP400() {
        Response response = RestoreApi.restoreNamespaceResponse(-1, 1, SOURCE_CLUSTER_NAME, TARGET_CLUSTER_NAME,
                SOURCE_NAMESPACE, TARGET_NAMESPACE);
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void restoreNamespaceToTimeStampLessThan0InvalidParameterHTTP400() {
        Response response = RestoreApi.restoreNamespaceResponse(1, -1, SOURCE_CLUSTER_NAME, TARGET_CLUSTER_NAME,
                SOURCE_NAMESPACE, TARGET_NAMESPACE);
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void restoreNsNotExistSourceNSInvalidParameterHTTP404() {
        String notExistSourceNamespace = "notExistSourceNamespace";
        Response response = RestoreApi.restoreNamespaceResponse(1, 2, SOURCE_CLUSTER_NAME, TARGET_CLUSTER_NAME,
                notExistSourceNamespace, TARGET_NAMESPACE);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void restoreNsNotExistTargetNSInvalidParameterHTTP404() {
        String notExistTargetNamespace = "notExistTargetNamespace";
        Response response = RestoreApi.restoreNamespaceResponse(1, 2, SOURCE_CLUSTER_NAME, TARGET_CLUSTER_NAME,
                SOURCE_NAMESPACE, notExistTargetNamespace);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void restoreNsNotExistSourceClusterNameInvalidParameterHTTP404() {
        String notExistSourceClusterName = "notExistSourceClusterName";
        Response response = RestoreApi.restoreNamespaceResponse(1, 2, notExistSourceClusterName, TARGET_CLUSTER_NAME,
                SOURCE_NAMESPACE, TARGET_NAMESPACE);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void restoreNsNotExistTargetClusterNameInvalidParameterHTTP404() {
        String notExistTargetClusterName = "notExistTargetClusterName";
        Response response = RestoreApi.restoreNamespaceResponse(1, 2, SOURCE_CLUSTER_NAME, notExistTargetClusterName,
                SOURCE_NAMESPACE, TARGET_NAMESPACE);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void restoreSetFromTimeLessThan0InvalidParameterHTTP400() {
        Response response = RestoreApi.restoreSetResponse(RestoreSetRequest.builder()
                .fromTime(-1)
                .toTime(1)
                .srcClusterName(SOURCE_CLUSTER_NAME)
                .trgClusterName(SOURCE_CLUSTER_NAME)
                .srcNS(SOURCE_NAMESPACE)
                .trgNS(SOURCE_NAMESPACE)
                .set(SET_NAME)
                .build());
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void restoreSetToTimeLessThan0InvalidParameterHTTP400() {
        Response response = RestoreApi.restoreSetResponse(RestoreSetRequest.builder()
                .fromTime(1)
                .toTime(-1)
                .srcClusterName(SOURCE_CLUSTER_NAME)
                .trgClusterName(SOURCE_CLUSTER_NAME)
                .srcNS(SOURCE_NAMESPACE)
                .trgNS(SOURCE_NAMESPACE)
                .set(SET_NAME)
                .build());
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void restoreSetToTimeEqualFromTimeInvalidParameterHTTP400() {
        Response response = RestoreApi.restoreSetResponse(RestoreSetRequest.builder()
                .fromTime(1)
                .toTime(1)
                .srcClusterName(SOURCE_CLUSTER_NAME)
                .trgClusterName(SOURCE_CLUSTER_NAME)
                .srcNS(SOURCE_NAMESPACE)
                .trgNS(SOURCE_NAMESPACE)
                .set(SET_NAME)
                .build());
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void restoreSetNotExistSourceNSInvalidParameterHTTP404() {
        String notExistSourceNamespace = "notExistSourceNamespace";
        Response response = RestoreApi.restoreSetResponse(RestoreSetRequest.builder()
                .fromTime(1)
                .toTime(2)
                .srcClusterName(SOURCE_CLUSTER_NAME)
                .trgClusterName(TARGET_CLUSTER_NAME)
                .srcNS(notExistSourceNamespace)
                .trgNS(TARGET_NAMESPACE)
                .set(SET_NAME)
                .build());
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void restoreSetNotExistTargetNSInvalidParameterHTTP404() {
        String notExistTargetNamespace = "notExistTargetNamespace";
        Response response = RestoreApi.restoreSetResponse(RestoreSetRequest.builder()
                .fromTime(1)
                .toTime(2)
                .srcClusterName(SOURCE_CLUSTER_NAME)
                .trgClusterName(TARGET_CLUSTER_NAME)
                .srcNS(SOURCE_NAMESPACE)
                .trgNS(notExistTargetNamespace)
                .set(SET_NAME)
                .build());
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void restoreSetNotExistSourceClusterNameInvalidParameterHTTP404() {
        String notExistSourceClusterName = "notExistSourceClusterName";
        Response response = RestoreApi.restoreSetResponse(RestoreSetRequest.builder()
                .fromTime(1)
                .toTime(2)
                .srcClusterName(notExistSourceClusterName)
                .trgClusterName(TARGET_CLUSTER_NAME)
                .srcNS(SOURCE_NAMESPACE)
                .trgNS(TARGET_NAMESPACE)
                .set(SET_NAME)
                .build());
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void restoreSetNotExistTargetClusterNameInvalidParameterHTTP404() {
        String notExistTargetClusterName = "notExistTargetClusterName";
        Response response = RestoreApi.restoreSetResponse(RestoreSetRequest.builder()
                .fromTime(1)
                .toTime(2)
                .srcClusterName(SOURCE_CLUSTER_NAME)
                .trgClusterName(notExistTargetClusterName)
                .srcNS(SOURCE_NAMESPACE)
                .trgNS(TARGET_NAMESPACE)
                .set(SET_NAME)
                .build());
        assertThat(response.statusCode()).isEqualTo(404);
    }
}
