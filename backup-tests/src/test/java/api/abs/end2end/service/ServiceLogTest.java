package api.abs.end2end.service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AutoUtils;
import utils.abs.AbsRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Tag("ABS-SERVICE-TEST")
class ServiceLogTest extends AbsRunner {

    @Test
    // If you run this test locally you need to run the editService.sh script before the installation.
    void logFieExist() {
        String log = AutoUtils.runBashCommand("docker exec backup-service cat /var/log/aerospike-backup-service/aerospike-backup-service.log", false);
        assertThat(log).contains("level=INFO");
    }
}
