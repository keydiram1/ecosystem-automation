package api.abs.end2end.config;

import api.abs.AbsClusterApi;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static api.abs.AbsClusterApi.configurationApi;
import static org.assertj.core.api.Assertions.assertThat;

@DisabledIfSystemProperty(named = "qa_environment", matches = "GCP")
@Tag("ABS-SEQUENTIAL-TESTS-2")
@Execution(ExecutionMode.SAME_THREAD)
class ClusterTest extends ConfigCRUD {

    private static final String CLUSTER_NAME = "ClusterTestCluster";

    @BeforeEach
    public void setUp() {
        if (AbsClusterApi.getAllClusters().containsKey(CLUSTER_NAME)) {
            AbsClusterApi.deleteCluster(CLUSTER_NAME);
        }
    }

    @Test
    void addCluster() {
        assertThat(AbsClusterApi.getAllClusters()).doesNotContainKey(CLUSTER_NAME);

        var response = AbsClusterApi.createCluster(CLUSTER_NAME);
        assertThat(response.getStatusCode()).isGreaterThanOrEqualTo(HttpStatus.SC_CREATED);

        assertThat(AbsClusterApi.getAllClusters()).containsKey(CLUSTER_NAME);
        assertThat(AbsClusterApi.getCluster(CLUSTER_NAME)).isNotNull();
    }

    @Test
    void deleteCluster() {
        AbsClusterApi.createCluster(CLUSTER_NAME);
        assertThat(AbsClusterApi.getAllClusters()).containsKey(CLUSTER_NAME);

        var response = configurationApi.deleteClusterWithHttpInfo(CLUSTER_NAME);

        assertThat(response.getStatusCode()).isGreaterThanOrEqualTo(HttpStatus.SC_NO_CONTENT);
        assertThat(AbsClusterApi.getAllClusters().containsKey(CLUSTER_NAME)).isFalse();
    }
}
