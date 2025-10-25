package api.backup.negative;

import api.backup.BackupManager;
import api.backup.PolicyApi;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.init.runners.BackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ADR-NEGATIVE-TESTS-E2E")
class PolicyNegativeTest extends BackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns3";
    private static final String SOURCE_CLUSTER_NAME = "PolicyNegativeNegativeCluster";
    private static final String BACKUP_NAMESPACE = "adr-ns3";
    private static final String BACKUP_NAME = "PolicyNegativeNegativeBackup";
    private static final String POLICY_NAME = "PolicyNegativeNegativePolicy";

    @AfterAll
    static void tearDown() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @BeforeEach
    public void setUp() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @Test
    void updatePolicyWrongParameterHTTP400() {
        int duration = -1;
        Response policyResponse = PolicyApi.updatePolicy(POLICY_NAME, duration, 90000, 100000, 100000, 1);
        assertThat(policyResponse.statusCode()).isEqualTo(400);
    }

    @Test
    void updatePolicyNameNull() {
        Response policyResponse = PolicyApi.updatePolicy(null, 10, 90000, 100000, 100000, 1);
        assertThat(policyResponse.body().asPrettyString()).contains("must not be blank");
        assertThat(policyResponse.statusCode()).isEqualTo(400);
    }

    @Test
    void updatePolicyNotFoundHTTP404() {
        Response policyResponse = PolicyApi.updatePolicy(POLICY_NAME, 1, 90000, 100000, 100000, 1);
        assertThat(policyResponse.statusCode()).isEqualTo(404);
    }

    @Test
    void createPolicyWrongParameterHTTP400() {
        int duration = -1;
        Response policyResponse = PolicyApi.createPolicy(POLICY_NAME, duration, 90000, 100000, 100000, 1);
        assertThat(policyResponse.statusCode()).isEqualTo(400);
    }

    @Test
    void createPolicyConflictHTTP409() {
        Response policyResponseOK = PolicyApi.createPolicy(POLICY_NAME, 2, 90000, 100000, 100000, 1);
        assertThat(policyResponseOK.statusCode()).isEqualTo(201);

        Response policyResponseFail = PolicyApi.createPolicy(POLICY_NAME, 2, 90000, 100000, 100000, 1);
        assertThat(policyResponseFail.statusCode()).isEqualTo(409);
    }

    @Test
    void getPolicyNotFoundHTTP404() {
        Response policyResponse = PolicyApi.getPolicyResponse("notExistPolicy");
        assertThat(policyResponse.statusCode()).isEqualTo(404);
    }

    @Test
    void deleteNotExistingPolicyOK() {
        Response policyResponse = PolicyApi.deletePolicy("notExistPolicy");
        assertThat(policyResponse.statusCode()).isEqualTo(200);
    }
}
