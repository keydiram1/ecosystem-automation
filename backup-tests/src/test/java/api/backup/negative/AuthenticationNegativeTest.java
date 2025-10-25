package api.backup.negative;

import api.backup.BackupBaseRequests;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.init.runners.BackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ADR-NEGATIVE-TESTS-E2E")
class AuthenticationNegativeTest extends BackupRunner {
    @Test
    void noToken() {
        String url = "/v1/connect/all";
        Response response = RestAssured.given(BackupBaseRequests.getBaseRequestSpecNoAuth(url)).get();
        int statusCode = response.statusCode();
        assertThat(statusCode).isEqualTo(HttpStatus.SC_FORBIDDEN);
    }
}
