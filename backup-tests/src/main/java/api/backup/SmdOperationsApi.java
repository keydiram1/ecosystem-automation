package api.backup;

import api.RestUtils;
import io.restassured.RestAssured;
import io.restassured.response.Response;

public class SmdOperationsApi {

    public static Response getLatestSmdBackup(String srcClusterName, long toTime) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec("/v1/smd/latest")
            .queryParam("srcClusterName", srcClusterName).queryParam("toTime", toTime)).when()).get();
        RestUtils.printResponse(response, "getLatestSmdBackup");
        return response;
    }
    
    public static Response getLatestSmdBackup(String srcClusterName, long fromTime, long toTime) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec("/v1/smd/latest")
            .queryParam("srcClusterName", srcClusterName).queryParam("fromTime", fromTime).queryParam("toTime", toTime)).when()).get();
        RestUtils.printResponse(response, "getLatestSmdBackup");
        return response;
    }

    public static Response getSmdUsers(String srcClusterName) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec("/v1/smd/users")
            .queryParam("srcClusterName", srcClusterName)).when()).get();
        RestUtils.printResponse(response, "getSmdUsers");
        return response;
    }

    public static Response getSmdUdf(String srcClusterName) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec("/v1/smd/udf")
            .queryParam("srcClusterName", srcClusterName)).when()).get();
        RestUtils.printResponse(response, "getSmdUdf");
        return response;
    }

    public static Response getSmdSindex(String srcClusterName) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec("/v1/smd/sindex")
            .queryParam("srcClusterName", srcClusterName)).when()).get();
        RestUtils.printResponse(response, "getSmdSindex");
        return response;
    }

    public static Response getSmdRoles(String srcClusterName) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec("/v1/smd/roles")
            .queryParam("srcClusterName", srcClusterName)).when()).get();
        RestUtils.printResponse(response, "getSmdRoles");
        return response;
    }
    
    public static Response removeSmdBackup(String srcClusterName, long olderThan) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec("/v1/smd/remove")
            .queryParam("srcClusterName", srcClusterName).queryParam("olderThan", olderThan)).when()).post();
        RestUtils.printResponse(response, "getLatestSmdBackup");
        return response;
    }

}
