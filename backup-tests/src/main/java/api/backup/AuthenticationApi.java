package api.backup;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.SneakyThrows;
import utils.AerospikeLogger;
import utils.ConfigParametersHandler;

import javax.security.auth.login.FailedLoginException;
import java.util.Map;

public class AuthenticationApi {

    public static final String BASE_URL = ConfigParametersHandler.getParameter("authenticator.url");
    private static volatile String token = null;

    @SneakyThrows
    private static void login() {
        Map<String, String> credentials = Map.of("username", "admin", "password", "adr-4dm1n-p@$$w0rd");
        Response response = RestAssured.given()
                .baseUri(BASE_URL + "/v1/auth/login")
                .body(credentials)
                .contentType(ContentType.JSON)
                .post();
        if (response.getStatusCode() == 403) {
            throw new FailedLoginException("Failed to authenticate " + credentials);
        }
        token = response.asPrettyString();
        AerospikeLogger.info("The token is: " + token);
    }

    public static String getAdminToken() {
        if (token == null) {
            synchronized (AuthenticationApi.class) {
                if (token == null) {
                    login();
                }
            }
        }
        return token;
    }
}
