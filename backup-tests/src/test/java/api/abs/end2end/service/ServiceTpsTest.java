package api.abs.end2end.service;

import api.abs.AbsApi;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AerospikeLogger;
import utils.abs.AbsRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Tag("ABS-SERVICE-TEST")
class ServiceTpsTest extends AbsRunner {

    @Test
    // If you run this test locally you need to run the editService.sh script before the installation.
    void lowTpsTest() {
        String exception = "No Exception";
        for (int i = 0; i < 100; i++) {
            AerospikeLogger.info("iteration number " + i);
            try {
                AbsApi.getAbsVersion();
            } catch (Exception e) {
                exception = e.getMessage();
                break;
            }
        }
        assertThat(exception).contains("Too Many Requests");
    }
}
