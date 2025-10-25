package api.backup.negative;

import api.backup.SmdOperationsApi;
import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.init.runners.BackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ADR-NEGATIVE-TESTS-E2E")
class SmdOperationsNegativeTest extends BackupRunner {
    private static final String SOURCE_CLUSTER_NAME = "SmdOperationsNegativeCluster";
    private static final String NOT_EXIST_SOURCE_CLUSTER = "notExistSourceCluster";

    @Test
    void removeBackupsInvalidToTimeHTTP400() {
        int invalidToTime = -1;
        Response response = SmdOperationsApi.removeSmdBackup(SOURCE_CLUSTER_NAME, invalidToTime);
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void removeBackupsNotExistSourceClusterOK() {
        Response response = SmdOperationsApi.removeSmdBackup(NOT_EXIST_SOURCE_CLUSTER, 1);
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void getLatestSmdUsers() {
        Response response = SmdOperationsApi.getSmdUsers(NOT_EXIST_SOURCE_CLUSTER);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void getLatestSmdRoles() {
        Response response = SmdOperationsApi.getSmdRoles(NOT_EXIST_SOURCE_CLUSTER);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void getLatestSmdSindex() {
        Response response = SmdOperationsApi.getSmdSindex(NOT_EXIST_SOURCE_CLUSTER);
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void getLatestSmdUdf() {
        Response response = SmdOperationsApi.getSmdUdf(NOT_EXIST_SOURCE_CLUSTER);
        assertThat(response.statusCode()).isEqualTo(404);
    }
}
