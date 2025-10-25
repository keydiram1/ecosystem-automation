package api.abs.end2end;

import api.abs.AbsApi;
import api.abs.generated.ApiResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import utils.AerospikeLogger;
import utils.abs.AbsRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Tag("ABS-E2E")
class AbsSystemApiTest extends AbsRunner {
    @Test
    void getAbsVersion() {
        assertThat(AbsApi.getAbsVersion()).isNotBlank();
    }

    @Test
    void getAbsVersionHighTpsTest() {
        String exception = "No Exception";
        for (int i = 0; i < 10; i++) {
            AerospikeLogger.info("iteration number " + i);
            try {
                AbsApi.getAbsVersion();
            } catch (Exception e) {
                exception = e.getMessage();
                break;
            }
        }
        assertThat(exception).isEqualTo("No Exception");
    }

    @Test
    void getAbsSwagger() {
        ApiResponse<String> apiDocs = AbsApi.getApiDocs();
        assertThat(apiDocs.getStatusCode()).isEqualTo(200);
        assertThat(apiDocs.getData()).contains("<title>Swagger UI</title>");
    }
}
