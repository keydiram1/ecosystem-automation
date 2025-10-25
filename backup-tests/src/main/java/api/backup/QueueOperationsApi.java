package api.backup;

import api.RestUtils;
import io.restassured.RestAssured;
import io.restassured.response.Response;

public class QueueOperationsApi {

    public static Response getQueueRecordsAndSetInProcess(String sourceNamespace, String set) {
        String urlSuffix = "/v1/queue/get/" + sourceNamespace + "/as-backup-queue/" + set + "/100";
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec(urlSuffix)).when()).post();
        RestUtils.printResponse(response, "getRecordsFromQueue");
        return response;
    }
}
