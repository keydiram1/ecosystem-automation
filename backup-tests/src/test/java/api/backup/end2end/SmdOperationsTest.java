package api.backup.end2end;

import api.backup.ClusterConnectionApi;
import api.backup.BackupManager;
import api.backup.SmdOperationsApi;
import api.backup.dto.ClusterConnection;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import utils.AutoUtils;
import utils.init.runners.BackupRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("ADR-E2E")
class SmdOperationsTest extends BackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns7";
    private static final String SOURCE_CLUSTER_NAME = "SmdOperationsTestSrcCluster";
    private static final String BACKUP_NAMESPACE = "adr-ns7";
    private static final String BACKUP_NAME = "SmdOperationsTestContinuousBackup";
    private static final String POLICY_NAME = "SmdOperationsTestPolicy";
    private static final String DC_NAME = "SmdOperationsDC";
    private static JsonObject LATEST_SMD_BACKUP;

    @BeforeAll
    public static void setUp() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);
        Awaitility.waitAtMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .alias("Wait for next SMD execution")
                .until(() -> ClusterConnectionApi.getClusterConnection(SOURCE_CLUSTER_NAME).getSmdLastExecuted() > 0);

        Response res = SmdOperationsApi.getLatestSmdBackup(SOURCE_CLUSTER_NAME, System.currentTimeMillis());
        assertThat(res.getStatusCode()).isEqualTo(200);
        LATEST_SMD_BACKUP = new Gson().fromJson(res.body().asPrettyString(), JsonObject.class);
    }

    @AfterAll
    static void afterAll() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @Test
    @EnabledIfSystemProperty(named = "STATIC_CONFIGURATION", matches = "false")
    void removeBackups() {
        ClusterConnection clusterConnection = ClusterConnectionApi.getClusterConnection(SOURCE_CLUSTER_NAME);
        clusterConnection.getSmdPolicy().setKeepFor(5000);
        clusterConnection.getSmdPolicy().setDuration(1);
        Response updateConnectionResponse = ClusterConnectionApi.updateConnection(clusterConnection);
        assertThat(updateConnectionResponse.getStatusCode()).isEqualTo(HttpStatus.SC_ACCEPTED);

        long fromTime = System.currentTimeMillis();
        // 6 seconds sleep to wait for a backup due to backupIntervalSeconds=5 seconds.
        AutoUtils.sleep(6000);
        long toTime = System.currentTimeMillis();

        Response response = SmdOperationsApi.getLatestSmdBackup(SOURCE_CLUSTER_NAME, fromTime, toTime);
        assertEquals(200, response.getStatusCode());

        SmdOperationsApi.removeSmdBackup(SOURCE_CLUSTER_NAME, toTime);

        response = SmdOperationsApi.getLatestSmdBackup(SOURCE_CLUSTER_NAME, fromTime, toTime);
        assertEquals(404, response.getStatusCode());
    }

    @Test
    @EnabledIfSystemProperty(named = "STATIC_CONFIGURATION", matches = "false")
    void updateSmdKeepFor() {
        ClusterConnection clusterConnection = ClusterConnectionApi.getClusterConnection(SOURCE_CLUSTER_NAME);
        clusterConnection.getSmdPolicy().setKeepFor(1);
        clusterConnection.getSmdPolicy().setDuration(1);
        Response updateConnectionResponse = ClusterConnectionApi.updateConnection(clusterConnection);
        assertThat(updateConnectionResponse.getStatusCode()).isEqualTo(HttpStatus.SC_ACCEPTED);

        long fromTime = System.currentTimeMillis();
        // 6 seconds sleep to wait for a backup due to backupIntervalSeconds=5 seconds.
        AutoUtils.sleep(6000);
        long toTime = System.currentTimeMillis();

        // 11 seconds sleep to wait for compactor deletion due to smdCompactIntervalSeconds=10 seconds.
        AutoUtils.sleep(11000);

        Response response = SmdOperationsApi.getLatestSmdBackup(SOURCE_CLUSTER_NAME, fromTime, toTime);
        assertEquals(404, response.getStatusCode());
    }

    @Test
    @EnabledIfSystemProperty(named = "STATIC_CONFIGURATION", matches = "false")
    void updateSmdDuration() {
        ClusterConnection clusterConnection = ClusterConnectionApi.getClusterConnection(SOURCE_CLUSTER_NAME);
        clusterConnection.getSmdPolicy().setDuration(1000);
        clusterConnection.getSmdPolicy().setKeepFor(5000);
        Response updateConnectionResponse = ClusterConnectionApi.updateConnection(clusterConnection);
        assertThat(updateConnectionResponse.getStatusCode()).isEqualTo(HttpStatus.SC_ACCEPTED);

        long fromTime = System.currentTimeMillis();
        // 6 seconds sleep to wait for a backup due to backupIntervalSeconds=5 seconds.
        AutoUtils.sleep(6000);

        Response response = SmdOperationsApi.getLatestSmdBackup(SOURCE_CLUSTER_NAME, fromTime, System.currentTimeMillis());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    void getLatestSmdUsers() {
        String smdUsers = SmdOperationsApi.getSmdUsers(SOURCE_CLUSTER_NAME).getBody().asString();
        String latestSmdUsers = LATEST_SMD_BACKUP.getAsJsonArray("users").toString();
        assertThat(latestSmdUsers).isEqualToIgnoringWhitespace(smdUsers);
    }

    @Test
    void getLatestSmdRoles() {
        String smdRoles = SmdOperationsApi.getSmdRoles(SOURCE_CLUSTER_NAME).getBody().asString();
        String latestSmdRoles = LATEST_SMD_BACKUP.getAsJsonArray("roles").toString();
        assertThat(latestSmdRoles).isEqualToIgnoringWhitespace(smdRoles);
    }

    @Test
    void getLatestSmdSindex() {
        String smdSindex = SmdOperationsApi.getSmdSindex(SOURCE_CLUSTER_NAME).getBody().asString();
        String latestSindex = LATEST_SMD_BACKUP.get("indexes").toString();
        assertThat(smdSindex).isEqualToIgnoringWhitespace(latestSindex);
    }

    @Test
    void getLatestSmdUdf() {
        String smdUdf = SmdOperationsApi.getSmdUdf(SOURCE_CLUSTER_NAME).getBody().asString();
        String latestSmdUdf = LATEST_SMD_BACKUP.get("udf").toString();
        assertThat(latestSmdUdf).isEqualToIgnoringWhitespace(smdUdf);
    }
}
