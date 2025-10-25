package api.backup;

import api.RestUtils;
import api.backup.dto.ClusterConnection;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import utils.init.runners.BackupRunner;

import java.util.Arrays;
import java.util.List;

public class ClusterConnectionApi {

    private static final Gson gson = new Gson();

    public static Response updateConnection(String srcClusterName, int srcClusterPort, String backupDCName,
            String srcClusterUser, String srcClusterPwd, int smdLastExecuted, int duration, int keepFor) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec("/v1/connect/update")).when()
                .body(gson.toJson(JsonBodyCreator.getConnection(srcClusterName, srcClusterPort, backupDCName,
                        srcClusterUser, srcClusterPwd, smdLastExecuted, duration, keepFor))))
                .put();
        RestUtils.printResponse(response, "updateConnection");
        return response;
    }

    public static Response createConnection(String srcClusterName, int srcClusterPort, String backupDCName,
            String srcClusterUser, String srcClusterPwd, int smdLastExecuted, int duration, int keepFor) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec("/v1/connect/create")).when()
                .body(gson.toJson(JsonBodyCreator.getConnection(srcClusterName, srcClusterPort, backupDCName,
                        srcClusterUser, srcClusterPwd, smdLastExecuted, duration, keepFor))))
                .post();
        RestUtils.printResponse(response, "createConnection");
        return response;
    }

    public static Response createConnection(String srcClusterName, String dcName, int duration, int srcClusterPort) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec("/v1/connect/create")).when()
                .body(gson.toJson(JsonBodyCreator.getConnection(srcClusterName, dcName, duration, srcClusterPort)))).post();
        RestUtils.printResponse(response, "createConnection");
        return response;
    }

    public static Response createConnection(String srcClusterName, String dcName, int duration){
        return createConnection(srcClusterName, dcName, duration, BackupRunner.AEROSPIKE_SOURCE_SERVER_PORT);
    }

    public static void createConnection(String srcClusterName, String dcName) {
        List<ClusterConnection> connections = getAllClusterConnections();
        if (connections.stream().noneMatch(it -> it.getSrcClusterName().equals(srcClusterName))) {
            Response connection = createConnection(srcClusterName, dcName, 86400);
            Preconditions.checkState(connection.getStatusCode() == HttpStatus.SC_CREATED);
        }
    }

    public static ClusterConnection getClusterConnection(String srcClusterName) {
        return gson.fromJson(getClusterConnectionResponse(srcClusterName).body().asString(), ClusterConnection.class);
    }
    
    public static Response getClusterConnectionResponse(String srcClusterName) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec("/v1/connect/" + srcClusterName))
                .when()).get();
        RestUtils.printResponse(response, "getClusterConnection");
        return response;
    }

    public static Response deleteClusterConnection(String srcClusterName, boolean disableConnectedBackups) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec("/v1/connect/" + srcClusterName))
                .when().queryParam("disableConnectedBackups", disableConnectedBackups)).delete();
        RestUtils.printResponse(response, "deleteClusterConnection");
        return response;
    }

    public static Response deleteClusterConnection(String srcClusterName) {
        return deleteClusterConnection(srcClusterName, true);
    }

    public static List<ClusterConnection> getAllClusterConnections() {
        String url = "/v1/connect/all";
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec(url)
                .when())).get();
        RestUtils.printResponse(response, "getAllClusterConnections");
        return Arrays.asList(new GsonBuilder().create().fromJson(response.body().asString(), ClusterConnection[].class));
    }
    
    public static ClusterConnection getClusterConnectionFromAllClusterConnections(String srcClusterName) {
        return getAllClusterConnections().stream().filter(carnet -> srcClusterName.equals(carnet.getSrcClusterName())).findFirst().orElse(null);
    }
    
    public static List<ClusterConnection> getAllSmdClusterConnections() {
        String url = "/v1/smd/connections";
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec(url)
                .when())).get();
        RestUtils.printResponse(response, "getAllClusterConnections");
        return Arrays.asList(new GsonBuilder().create().fromJson(response.body().asString(), ClusterConnection[].class));
    }
    
    public static ClusterConnection getSmdClusterConnectionFromAllSmdClusterConnections(String srcClusterName) {
        return getAllSmdClusterConnections().stream().filter(carnet -> srcClusterName.equals(carnet.getSrcClusterName())).findFirst().orElse(null);
    }
    
    public static Response updateConnection(ClusterConnection clusterConnection) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec("/v1/connect/update")).when()
                .body(gson.toJson(clusterConnection))).put();
        RestUtils.printResponse(response, "updateConnection");
        return response;
    }

    public static boolean isExists(String srcClusterName) {
        return getAllClusterConnections().stream().anyMatch(it -> it.getSrcClusterName().equals(srcClusterName));
    }
}
