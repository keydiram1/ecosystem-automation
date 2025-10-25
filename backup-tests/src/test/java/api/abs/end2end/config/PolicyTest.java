package api.abs.end2end.config;

import api.abs.AbsPolicyApi;
import api.abs.generated.ApiResponse;
import api.abs.generated.model.DtoBackupPolicy;
import api.abs.generated.model.DtoRetentionPolicy;
import api.abs.generated.model.DtoRetryPolicy;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-SEQUENTIAL-TESTS-2")
@Execution(ExecutionMode.SAME_THREAD)
@DisabledIfSystemProperty(named = "qa_environment", matches = "GCP")
class PolicyTest extends ConfigCRUD {
    private static final String POLICY_NAME = "PolicyTestPolicy";

    private final DtoBackupPolicy policyAllFields = new DtoBackupPolicy()
            .parallel(1)
            .socketTimeout(2)
            .totalTimeout(3)
            .retryPolicy(new DtoRetryPolicy()
                    .maxRetries(1)
                    .baseTimeout(2)
                    .multiplier(BigDecimal.ONE))
            .retention(new DtoRetentionPolicy()
                    .incremental(1)
                    .full(2))
            .noRecords(true)
            .noIndexes(true)
            .noUdfs(true)
            .bandwidth(16)
            .recordsPerSecond(8)
            .fileLimit(9);

    @BeforeEach
    public void setUp() {
        if (AbsPolicyApi.getAllPolicies().containsKey(POLICY_NAME)) {
            AbsPolicyApi.deletePolicy(POLICY_NAME);
        }
    }

    @Test
    void deletePolicy() {
        AbsPolicyApi.createPolicy(POLICY_NAME, new DtoBackupPolicy());
        assertThat(AbsPolicyApi.getAllPolicies()).containsKey(POLICY_NAME);
        AbsPolicyApi.deletePolicy(POLICY_NAME);
        assertThat(AbsPolicyApi.getAllPolicies()).doesNotContainKey(POLICY_NAME);
    }

    @Test
    void createPolicy() {
        AbsPolicyApi.createPolicy(POLICY_NAME, policyAllFields);
        assertThat(AbsPolicyApi.getAllPolicies().get(POLICY_NAME))
                .isNotNull()
                .isEqualTo(policyAllFields)
                .isEqualTo(AbsPolicyApi.getPolicy(POLICY_NAME));
    }

    @Test
    void updatePolicy() {
        ApiResponse<Void> response = AbsPolicyApi.createPolicy(POLICY_NAME, new DtoBackupPolicy());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);

        AbsPolicyApi.updatePolicy(POLICY_NAME, policyAllFields);
        assertThat(AbsPolicyApi.getAllPolicies().get(POLICY_NAME))
                .isNotNull()
                .isEqualTo(policyAllFields);
    }
}
