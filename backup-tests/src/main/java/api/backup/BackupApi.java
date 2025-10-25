package api.backup;

import api.RestUtils;
import api.backup.dto.ContinuousBackup;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import utils.AerospikeLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

public class BackupApi {

    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    public static Response updateBackup(String backupName, String description) {
        return RestUtils.printRequest(RestAssured.given(BackupBaseRequests
                        .getBaseRequestSpec("/v1/backup/update/" + backupName)).when()
                .body(gson.toJson(Map.ofEntries(entry("description", description))))).put();
    }

    public static Response updateBackup(String name, String srcClusterName, String srcNS, String backupNS,
                                        String policy) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests
                        .getBaseRequestSpec("/v1/backup/update/" + name)).when()
                .body(gson.toJson(JsonBodyCreator.getBackup(name, "updated auto description",
                        srcClusterName, srcNS, backupNS, policy)))).put();
        AerospikeLogger.info("Status code for updateBackup: " + response.getStatusCode());
        return response;
    }

    public static Response enableBackup(String backupName) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests
                .getBaseRequestSpec("/v1/backup/enable/" + backupName)).when()).put();
        AerospikeLogger.info("Status code for enableBackup: " + response.getStatusCode());
        return response;
    }

    public static Response disableBackup(String backupName) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests
                .getBaseRequestSpec("/v1/backup/disable/" + backupName)).when()).put();
        AerospikeLogger.info("Status code for disableBackup: " + response.getStatusCode());
        return response;
    }

    public static Response createBackup(String name, String srcClusterName, String srcNS, String backupNS,
                                        String policy) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests
                        .getBaseRequestSpec("/v1/backup/create")).when()
                .body(gson.toJson(JsonBodyCreator.getBackup(name, "auto description", srcClusterName,
                        srcNS, backupNS, policy)))).post();
        AerospikeLogger.info("Status code for createBackup: " + response.getStatusCode());
        return response;
    }

    public static Response createBackup(String name, String srcClusterName, String srcNS, String backupNS,
                                        String policy, List<String> sets) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests
                        .getBaseRequestSpec("/v1/backup/create")).when()
                .body(gson.toJson(JsonBodyCreator.getBackup(name, "auto description", srcClusterName,
                        srcNS, backupNS, policy, sets)))).post();
        RestUtils.printResponse(response, "createBackup");
        return response;
    }

    public static Response getBackupResponse(String backupName) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests
                .getBaseRequestSpec("/v1/backup/" + backupName)).when()).get();
        RestUtils.printResponse(response, "getBackup");
        return response;
    }

    public static ContinuousBackup getBackup(String backupName) {
        return gson.fromJson(getBackupResponse(backupName).body().asString(), ContinuousBackup.class);
    }

    public static Response deleteBackup(String backupName) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests
                .getBaseRequestSpec("/v1/backup/" + backupName)).when()).delete();
        RestUtils.printResponse(response, "deleteBackup");
        return response;
    }

    public static Response getBackupSets(String backupName) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests
                .getBaseRequestSpec("/v1/backup/sets/" + backupName)).when()).get();
        RestUtils.printResponse(response, "getBackupSets");
        return response;
    }

    public static boolean isBackupExists(String backupName) {
        return getAllBackups().stream().anyMatch(backup -> backup.getName().equals(backupName));
    }

    public static List<ContinuousBackup> getAllBackups() {
        String url = "/v1/backup/all";
        Response response = RestAssured.given(BackupBaseRequests.getBaseRequestSpec(url).when()).get();
        return Arrays.asList(gson.fromJson(response.body().asString(), ContinuousBackup[].class));
    }
}
