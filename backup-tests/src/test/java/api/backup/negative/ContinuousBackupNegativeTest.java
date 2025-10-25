package api.backup.negative;

import api.backup.BackupApi;
import api.backup.ClusterConnectionApi;
import api.backup.BackupManager;
import api.backup.PolicyApi;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.init.runners.BackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ADR-NEGATIVE-TESTS-E2E")
class ContinuousBackupNegativeTest extends BackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns2";
    private static final String SOURCE_CLUSTER_NAME = "ContinuousBackupNegativeCluster";
    private static final String BACKUP_NAMESPACE = "adr-ns2";
    private static final String BACKUP_NAME = "ContinuousBackupNegativeBackup";
    private static final String POLICY_NAME = "ContinuousBackupNegativePolicy";
    private static final String DC_NAME = "ContinuousBackupNegativeDC";

    @AfterAll
    static void tearDown() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @BeforeEach
    public void setUp() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @Test
    void createBackupConflictHTTP409() {
        PolicyApi.createPolicy(POLICY_NAME, 1);
        ClusterConnectionApi.createConnection(SOURCE_CLUSTER_NAME, DC_NAME, 9999);

        Response firstCreate =
                BackupApi.createBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME);
        assertThat(firstCreate.statusCode()).isEqualTo(HttpStatus.SC_CREATED);

        Response secondCreate =
                BackupApi.createBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME);
        assertThat(secondCreate.statusCode()).isEqualTo(HttpStatus.SC_CONFLICT);
    }

    @Test
    void createBackupInvalidPolicyHTTP400() {
        String invalidPolicyName = "invalidPolicyName";
        ClusterConnectionApi.createConnection(SOURCE_CLUSTER_NAME, DC_NAME, 9999);
        BackupApi.createBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME);
        Response response = BackupApi.createBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, invalidPolicyName);
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void createBackupSetsValueNull() {
        ClusterConnectionApi.createConnection(SOURCE_CLUSTER_NAME, DC_NAME, 9999);
        PolicyApi.createPolicy(POLICY_NAME, 1);
        Response response = BackupApi.createBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, null);
        assertThat(response.body().asPrettyString()).contains("must not be null");
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void createBackupSrcClusterNameNull() {
        ClusterConnectionApi.createConnection(SOURCE_CLUSTER_NAME, DC_NAME, 9999);
        PolicyApi.createPolicy(POLICY_NAME, 1);
        Response response = BackupApi.createBackup(BACKUP_NAME, null, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME);
        assertThat(response.body().asPrettyString()).contains("must not be blank");
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void createBackupInvalidClusterConnectionHTTP400() {
        String invalidClusterConnectionName = "invalidClusterConnectionName";
        PolicyApi.createPolicy(POLICY_NAME, 1);
        Response response = BackupApi.createBackup(BACKUP_NAME, invalidClusterConnectionName, SOURCE_NAMESPACE, BACKUP_NAMESPACE,
                POLICY_NAME);
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void createBackupNotValidSourceNSHTTP400() {
        String notExistSourceNamespace = "notExistSourceNamespace";
        PolicyApi.createPolicy(POLICY_NAME, 1);
        ClusterConnectionApi.createConnection(SOURCE_CLUSTER_NAME, DC_NAME, 9999);
        Response response = BackupApi.createBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, notExistSourceNamespace, BACKUP_NAMESPACE,
                POLICY_NAME);
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void createBackupNotValidBackupNSHTTP400() {
        String notExistBackupNamespace = "notExistBackupNamespace";
        PolicyApi.createPolicy(POLICY_NAME, 1);
        ClusterConnectionApi.createConnection(SOURCE_CLUSTER_NAME, DC_NAME, 9999);
        Response response = BackupApi.createBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, notExistBackupNamespace,
                POLICY_NAME);
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void updateBackupNotExistHTTP404() {
        String notExistBackupName = "notExistBackupName";
        PolicyApi.createPolicy(POLICY_NAME, 1);
        ClusterConnectionApi.createConnection(SOURCE_CLUSTER_NAME, DC_NAME, 9999);
        BackupApi.createBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME);
        Response response = BackupApi.updateBackup(notExistBackupName, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE,
                POLICY_NAME);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void enableNotExistBackupHTTP404() {
        String notExistBackup = "notExistBackup";
        Response response = BackupApi.enableBackup(notExistBackup);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void disableNotExistBackupHTTP404() {
        String notExistBackup = "notExistBackup";
        Response response = BackupApi.disableBackup(notExistBackup);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void getBackupNotExistBackupHTTP404() {
        String notExistBackup = "notExistBackup";
        Response response = BackupApi.getBackupResponse(notExistBackup);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void deleteNotExistBackupHTTP404() {
        String notExistBackup = "notExistBackup";
        Response response = BackupApi.deleteBackup(notExistBackup);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void getBackupSetsNotExistBackupHTTP404() {
        String notExistBackup = "notExistBackup";
        Response response = BackupApi.getBackupSets(notExistBackup);
        assertThat(response.statusCode()).isEqualTo(404);
    }
}
