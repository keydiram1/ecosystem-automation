package api.backup;

import api.RestUtils;
import api.backup.dto.Policy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.Arrays;
import java.util.List;

public class PolicyApi {

    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    public static Response createPolicy(String policyName, int duration) {
        String urlSuffix = "/v1/policy/create";
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec(urlSuffix)).when()
                .body(gson.toJson(new Policy(policyName, duration)))).post();
        RestUtils.printResponse(response, "createPolicy");
        return response;
    }

    public static Response createPolicy(String policyName, int duration, int retention, int keepFor, int maxThroughput, Integer initialSync) {
        String urlSuffix = "/v1/policy/create";
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec(urlSuffix)).when()
                .body(gson.toJson(new Policy(policyName, duration, retention, keepFor, maxThroughput, initialSync)))).post();
        RestUtils.printResponse(response, "createPolicy");
        return response;
    }

    public static Response updatePolicy(String policyName, int duration) {
        String urlSuffix = "/v1/policy/update";
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec(urlSuffix)).when()
                .body(gson.toJson(new Policy(policyName, duration)))).put();
        RestUtils.printResponse(response, "update policy");
        return response;
    }

    public static Response updatePolicy(String policyName, int duration, int retention, int keepFor, int maxThroughput, Integer initialSync) {
        String urlSuffix = "/v1/policy/update";
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec(urlSuffix)).when()
                .body(gson.toJson(new Policy(policyName, duration, retention, keepFor, maxThroughput, initialSync)))).put();
        RestUtils.printResponse(response, "update policy");
        return response;
    }

    public static Policy getPolicy(String policyName) {
        return new Gson().fromJson(getPolicyResponse(policyName).body().asString(), Policy.class);
    }

    public static Response getPolicyResponse(String policyName) {
        String urlSuffix = "/v1/policy/" + policyName;

        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec(urlSuffix)).when()).get();
        RestUtils.printResponse(response, "get policy");
        return response;
    }

    public static List<Policy> getAllPolicies() {
        String urlSuffix = "/v1/policy/all";

        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec(urlSuffix)).when()).get();
        RestUtils.printResponse(response, "get all plicies");
        return Arrays.asList(new GsonBuilder().create().fromJson(response.body().asString(), Policy[].class));
    }

    public static boolean isPolicyExists(String policyName) {
        List<Policy> listOfPolicies = getAllPolicies();
        for (Policy listOfPolicy : listOfPolicies) {
            if (listOfPolicy.getName().equals(policyName))
                return true;
        }
        return false;
    }

    public static Response deletePolicy(String policyName) {
        String urlSuffix = "/v1/policy/" + policyName;

        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec(urlSuffix)).when()).delete();
        RestUtils.printResponse(response, "Policy");
        return response;
    }

    public static int getNumberOfPolicies() {
        return getAllPolicies().size();
    }
}
