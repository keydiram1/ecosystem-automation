package api.backup.end2end;

import api.backup.BackupApi;
import api.backup.ClusterConnectionApi;
import api.backup.BackupManager;
import api.backup.dto.ClusterConnection;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import utils.AutoUtils;
import utils.init.runners.BackupRunner;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("ADR-E2E")
public class ClusterConnectionTest extends BackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns6";
    private static final String SOURCE_CLUSTER_NAME = "ClusterConnectionTestClusterName";
    private static final String BACKUP_NAMESPACE = "adr-ns6";
    private static final String BACKUP_NAME = "ClusterConnectionTestBackupName";
    private static final String POLICY_NAME = "ClusterConnectionTestPolicy";
    private static final String DC_NAME = "ClusterConnectionDC";

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
    @EnabledIfSystemProperty(named = "STATIC_CONFIGURATION", matches = "false")
    void deleteClusterConnection() {
        ClusterConnection clusterConnection = ClusterConnectionApi.getClusterConnection(SOURCE_CLUSTER_NAME);
        assertNotNull(clusterConnection);
        BackupApi.deleteBackup(BACKUP_NAME);
        Awaitility.await()
                .pollInterval(Duration.ofSeconds(4))
                .until(() -> !BackupApi.isBackupExists(BACKUP_NAME));

        Response response = ClusterConnectionApi.deleteClusterConnection(SOURCE_CLUSTER_NAME);
        assertEquals(202, response.getStatusCode());
        assertFalse(ClusterConnectionApi.isExists(SOURCE_CLUSTER_NAME));
    }

    @Test
    @EnabledIfSystemProperty(named = "STATIC_CONFIGURATION", matches = "false")
    void updateClusterConnection() {
        ClusterConnectionApi.updateConnection(SOURCE_CLUSTER_NAME, BackupRunner.AEROSPIKE_SOURCE_SERVER_PORT,
            DC_NAME, BackupRunner.CLIENT_POLICY_SOURCE.user, BackupRunner.CLIENT_POLICY_SOURCE.password,
                777, 888, 999);

        ClusterConnection clusterConnection = ClusterConnectionApi.getClusterConnection(SOURCE_CLUSTER_NAME);
        AutoUtils.sleep(2000);

        assertEquals(BackupRunner.getDockerHost(), clusterConnection.getSrcClusterHost());
        assertEquals(SOURCE_CLUSTER_NAME, clusterConnection.getSrcClusterName());
        assertEquals(BackupRunner.AEROSPIKE_SOURCE_SERVER_PORT, clusterConnection.getSrcClusterPort());
        assertEquals(BackupRunner.CLIENT_POLICY_SOURCE.user, clusterConnection.getSrcClusterUser());
        assertEquals(BackupRunner.CLIENT_POLICY_SOURCE.password, clusterConnection.getSrcClusterPwd());
        assertEquals(777, clusterConnection.getSmdLastExecuted());
        assertEquals(888.0, clusterConnection.getSmdPolicy().getDuration());
        assertEquals(999.0, clusterConnection.getSmdPolicy().getKeepFor());
    }

    @Test
    void getAllClusterConnections() {
        ClusterConnection clusterConnection = ClusterConnectionApi.getClusterConnectionFromAllClusterConnections(SOURCE_CLUSTER_NAME);
        assertEquals(BackupRunner.getDockerHost(), clusterConnection.getSrcClusterHost());
        assertEquals(SOURCE_CLUSTER_NAME, clusterConnection.getSrcClusterName());
        assertEquals(BackupRunner.AEROSPIKE_SOURCE_SERVER_PORT, clusterConnection.getSrcClusterPort());
        assertEquals(BackupRunner.CLIENT_POLICY_SOURCE.user, clusterConnection.getSrcClusterUser());
        assertEquals(BackupRunner.CLIENT_POLICY_SOURCE.password, clusterConnection.getSrcClusterPwd());
        assertEquals(86400.0, clusterConnection.getSmdPolicy().getDuration());
    }

    @Test
    void getClusterConnection() {
        ClusterConnection clusterConnection = ClusterConnectionApi.getClusterConnection(SOURCE_CLUSTER_NAME);
        assertEquals(BackupRunner.getDockerHost(), clusterConnection.getSrcClusterHost());
        assertEquals(SOURCE_CLUSTER_NAME, clusterConnection.getSrcClusterName());
        assertEquals(BackupRunner.AEROSPIKE_SOURCE_SERVER_PORT, clusterConnection.getSrcClusterPort());
        assertEquals(BackupRunner.CLIENT_POLICY_SOURCE.user, clusterConnection.getSrcClusterUser());
        assertEquals(BackupRunner.CLIENT_POLICY_SOURCE.password, clusterConnection.getSrcClusterPwd());
        assertEquals(86400.0, clusterConnection.getSmdPolicy().getDuration());
    }

    @Test
    void getAllSmdClusterConnections() {
        ClusterConnection clusterConnection = ClusterConnectionApi.getSmdClusterConnectionFromAllSmdClusterConnections(SOURCE_CLUSTER_NAME);
        assertEquals(86400.0, clusterConnection.getSmdPolicy().getDuration());
        assertNull(clusterConnection.getSrcClusterHost());
        assertNull(clusterConnection.getSrcClusterUser());
    }
}
