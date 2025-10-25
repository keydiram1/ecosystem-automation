package api.backup;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import utils.ConfigParametersHandler;

public class BackupBaseRequests {

    public static final String BASE_URL = ConfigParametersHandler.getParameter("rest.backend.url");
    public static final String QUEUE_HANDLER_BASE_URL = ConfigParametersHandler.getParameter("queue.handler.url");

    public static RequestSpecification getBaseRequestSpec(String urlSuffix) {
        if ("true".equals(ConfigParametersHandler.getParameter("auth"))) {
            return getBaseRequestSpecNoAuth(urlSuffix)
                    .header("Authorization", "Bearer " + AuthenticationApi.getAdminToken());
        }
        return getBaseRequestSpecNoAuth(urlSuffix);
    }

    public static RequestSpecification getBaseRequestSpecNoAuth(String urlSuffix) {
        return RestAssured.given()
                .baseUri(BASE_URL + urlSuffix)
                .contentType(ContentType.JSON);
    }

    public static RequestSpecification getQueueHandlerBaseRequest(String urlSuffix) {
        return RestAssured.given().baseUri(QUEUE_HANDLER_BASE_URL + urlSuffix)
                .contentType(ContentType.JSON);
    }
}
