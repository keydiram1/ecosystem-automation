package api.backup.negative;

import api.backup.BackupManager;
import api.backup.ClusterConnectionApi;
import api.backup.PolicyApi;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import utils.init.runners.BackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ADR-NEGATIVE-TESTS-E2E")
class ClusterConnectionNegativeTest extends BackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns1";
    private static final String SOURCE_CLUSTER_NAME = "ClusterConnectionNegativeCluster";
    private static final String DC_NAME = "ClusterConnectionNegativeDC";
    private static final String BACKUP_NAMESPACE = "adr-ns1";
    private static final String BACKUP_NAME = "ClusterConnectionNegativeBackup";
    private static final String POLICY_NAME = "ClusterConnectionNegativePolicy";

    @AfterAll
    static void tearDown() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @BeforeEach
    public void setUp() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @Test
    void createConnectionInvalidSmdKeepForParametersHTTP400() {
        PolicyApi.createPolicy(POLICY_NAME, 2);
        int smdKeepFor = -1;
        Response response = ClusterConnectionApi.createConnection(SOURCE_CLUSTER_NAME,
                BackupRunner.AEROSPIKE_SOURCE_SERVER_PORT, DC_NAME, BackupRunner.CLIENT_POLICY_SOURCE.user,
                BackupRunner.CLIENT_POLICY_SOURCE.password, 999, 999, smdKeepFor);
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void createConnectionInvalidSmdDurationParametersHTTP400() {
        PolicyApi.createPolicy(POLICY_NAME, 2);
        int smdDuration = -1;
        Response response = ClusterConnectionApi.createConnection(SOURCE_CLUSTER_NAME,
                BackupRunner.AEROSPIKE_SOURCE_SERVER_PORT, DC_NAME, BackupRunner.CLIENT_POLICY_SOURCE.user,
                BackupRunner.CLIENT_POLICY_SOURCE.password, 999, smdDuration, 999);
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void createConnectionConflictHTTP409() {
        ClusterConnectionApi.createConnection(SOURCE_CLUSTER_NAME, DC_NAME, 999);
        Response response = ClusterConnectionApi.createConnection(SOURCE_CLUSTER_NAME, DC_NAME, 999);
        assertThat(response.statusCode()).isEqualTo(409);
    }

    @Test
    void createConnectionWrongPortServerErrorHTTP502() {
        PolicyApi.createPolicy(POLICY_NAME, 2);
        int wrongServerPort = 999;
        Response response = ClusterConnectionApi.createConnection(SOURCE_CLUSTER_NAME, wrongServerPort,
                DC_NAME, BackupRunner.CLIENT_POLICY_SOURCE.user, BackupRunner.CLIENT_POLICY_SOURCE.password,
                9000, 9000, 9000);
        assertThat(response.statusCode()).isEqualTo(502);
    }

    @Test
    void createConnectionWrongUserServerErrorHTTP502() {
        PolicyApi.createPolicy(POLICY_NAME, 2);
        String wrongUser = "wrongUser";
        Response response = ClusterConnectionApi.createConnection(SOURCE_CLUSTER_NAME,
                BackupRunner.AEROSPIKE_SOURCE_SERVER_PORT, DC_NAME, wrongUser, BackupRunner.CLIENT_POLICY_SOURCE.password,
                9000, 9000, 9000);
        assertThat(response.statusCode()).isEqualTo(502);
    }

    @Test
    void updateConnectionInvalidParametersHTTP400() {
        PolicyApi.createPolicy(POLICY_NAME, 2);
        ClusterConnectionApi.createConnection(SOURCE_CLUSTER_NAME, DC_NAME, 999);
        Response response = ClusterConnectionApi.updateConnection(SOURCE_CLUSTER_NAME,
                BackupRunner.AEROSPIKE_SOURCE_SERVER_PORT, DC_NAME, BackupRunner.CLIENT_POLICY_SOURCE.user,
                BackupRunner.CLIENT_POLICY_SOURCE.password, -1, -1, -1);
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    @EnabledIfSystemProperty(named = "STATIC_CONFIGURATION", matches = "true")
    void cantUpdateConnectionInStaticConfiguration() {
        PolicyApi.createPolicy(POLICY_NAME, 2);
        ClusterConnectionApi.createConnection(SOURCE_CLUSTER_NAME, DC_NAME, 999);
        Response response = ClusterConnectionApi.updateConnection(SOURCE_CLUSTER_NAME, BackupRunner.AEROSPIKE_SOURCE_SERVER_PORT, DC_NAME,
                BackupRunner.CLIENT_POLICY_SOURCE.user, BackupRunner.CLIENT_POLICY_SOURCE.password, 9000, 9000, 9000);
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body().asPrettyString()).contains("ClusterConnection cannot be deleted");
    }

    @Test
    @EnabledIfSystemProperty(named = "STATIC_CONFIGURATION", matches = "false")
    void updateConnectionDynamicConfigurationWrongUserServerErrorHTTP502() {
        PolicyApi.createPolicy(POLICY_NAME, 2);
        ClusterConnectionApi.createConnection(SOURCE_CLUSTER_NAME, DC_NAME, 999);
        String wrongUser = "wrongUser";
        Response response = ClusterConnectionApi.updateConnection(SOURCE_CLUSTER_NAME, BackupRunner.AEROSPIKE_SOURCE_SERVER_PORT,
                DC_NAME, wrongUser, BackupRunner.CLIENT_POLICY_SOURCE.password, 777, 888, 999);
        assertThat(response.statusCode()).isEqualTo(502);
        assertThat(response.body().asPrettyString()).contains("Login failed");
    }

    @Test
    void updateNotExistConnectionNotFoundHTTP404() {
        String wrongSourceClusterName = "wrongSourceClusterName";
        Response response = ClusterConnectionApi.updateConnection(wrongSourceClusterName,
                BackupRunner.AEROSPIKE_SOURCE_SERVER_PORT, DC_NAME, BackupRunner.CLIENT_POLICY_SOURCE.user,
                BackupRunner.CLIENT_POLICY_SOURCE.password, 999, 999, 999);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void getNotExistConnectionNotFoundHTTP404() {
        Response response = ClusterConnectionApi.getClusterConnectionResponse(SOURCE_CLUSTER_NAME);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void deleteNotExistConnectionNotFoundHTTP404() {
        Response response = ClusterConnectionApi.deleteClusterConnection(SOURCE_CLUSTER_NAME);
        assertThat(response.statusCode()).isEqualTo(404);
    }
}
