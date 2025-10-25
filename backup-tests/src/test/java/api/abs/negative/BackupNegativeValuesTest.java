package api.abs.negative;

import api.abs.AbsBackupApi;
import api.abs.AbsPolicyApi;
import api.abs.generated.model.DtoBackupPolicy;
import api.abs.generated.model.DtoRetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.abs.AbsRunner;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("ABS-NEGATIVE-TESTS")
class BackupNegativeValuesTest extends AbsRunner {
    private static String POLICY_NAME;
    private static DtoBackupPolicy POLICY;

    @BeforeEach
    public void setUpEach() {
        POLICY_NAME = "policyNegativeValues" + System.currentTimeMillis();

        POLICY = new DtoBackupPolicy()
                .parallel(1)
                .sealed(true);

        AbsPolicyApi.createPolicy(POLICY_NAME, POLICY);
    }

    @Test
    void recordsPerSecond() {
        assertThatThrownBy(() -> AbsPolicyApi.updatePolicy(POLICY_NAME, POLICY.recordsPerSecond(-1000)))
                .hasMessageContaining("\"records-per-second\" -1000 invalid, should not be negative number");
    }

    @Test
    void bandwidth() {
        assertThatThrownBy(() -> AbsPolicyApi.updatePolicy(POLICY_NAME, POLICY.bandwidth(-1000)))
                .hasMessageContaining("negative value validation error: \"bandwidth\" -1000 invalid, should not be negative number");
    }

    @Test
    void socketTimeout() {
        assertThatThrownBy(() -> AbsPolicyApi.updatePolicy(POLICY_NAME, POLICY.socketTimeout(-1000)))
                .hasMessageContaining("\"socket-timeout\" -1000 invalid, should not be negative number");
    }

    @Test
    void parallel() {
        assertThatThrownBy(() -> AbsPolicyApi.updatePolicy(POLICY_NAME, POLICY.parallel(0)))
                .hasMessageContaining("\"parallel\" 0 invalid, should be positive number");
    }

    @Test
    void fileLimit() {
        assertThatThrownBy(() -> AbsPolicyApi.updatePolicy(POLICY_NAME, POLICY.fileLimit(-1)))
                .hasMessageContaining("\"file-limit\" -1 invalid, should not be negative number");
    }

    @Test
    void retryDelay() {
        assertThatThrownBy(() -> AbsPolicyApi.updatePolicy(POLICY_NAME, POLICY.retryPolicy(
                new DtoRetryPolicy()
                        .maxRetries(3)
                        .multiplier(BigDecimal.ONE)
                        .baseTimeout(-1)
        )))
                .hasMessageContaining("\"base-timeout\" -1 invalid, should be positive number");
    }

    @Test
    void currentBackupNotExistRoutine() {
        assertThatThrownBy(() -> AbsBackupApi.getCurrentBackup("notExistRoutine"))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("routine \"notExistRoutine\" not found");
    }

    @Test
    void currentBackupNullRoutine() {
        assertThatThrownBy(() -> AbsBackupApi.getCurrentBackup(null))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Missing the required parameter 'name' when calling getCurrentBackup");
    }
}