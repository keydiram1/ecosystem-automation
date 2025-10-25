package api.abs.end2end.config;

import api.abs.AbsClusterApi;
import api.abs.AbsPolicyApi;
import api.abs.AbsRoutineApi;
import api.abs.AbsStorageApi;
import api.abs.generated.ApiResponse;
import api.abs.generated.model.*;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledIfSystemProperty(named = "qa_environment", matches = "GCP")
@Tag("ABS-SEQUENTIAL-TESTS-2")
@Execution(ExecutionMode.SAME_THREAD)
class RoutineTest extends ConfigCRUD {
    private static final String ROUTINE_NAME = "RoutineTestRoutine";
    private static final String POLICY_NAME = "RoutineTestPolicy";
    private static final String STORAGE_NAME = "RoutineTestStorage";
    private static final String CLUSTER_NAME = "RoutineTestCluster";

    final DtoBackupRoutine routineAllFields = new DtoBackupRoutine()
            .backupPolicy(POLICY_NAME)
            .binList(List.of("bin"))
            .incrIntervalCron("@hourly")
            .intervalCron("@daily")
            .namespaces(List.of("source-ns1"))
            .partitionList("1-1")
            .setList(List.of("set"))
            .sourceCluster(CLUSTER_NAME)
            .storage(STORAGE_NAME);

    @BeforeEach
    public void setUp() {
        createAllRequiredObjectsForRoutine();
        if (AbsRoutineApi.getAllRoutines().containsKey(ROUTINE_NAME)) {
            AbsRoutineApi.deleteRoutine(ROUTINE_NAME);
        }
    }

    private void createAllRequiredObjectsForRoutine() {
        if (!AbsClusterApi.getAllClusters().containsKey(CLUSTER_NAME)) {
            AbsClusterApi.createCluster(CLUSTER_NAME);
        }
        if (!AbsStorageApi.getAllStorage().containsKey(STORAGE_NAME)) {
            AbsStorageApi.createStorage(STORAGE_NAME, new DtoStorage().localStorage(
                    new DtoLocalStorage().path("testPath"))
            );
        }
        if (!AbsPolicyApi.getAllPolicies().containsKey(POLICY_NAME)) {
            AbsPolicyApi.createPolicy(POLICY_NAME, new DtoBackupPolicy());
        }
    }

    @Test
    void createRoutine() {
        AbsRoutineApi.createRoutine(ROUTINE_NAME, routineAllFields);
        assertThat(AbsRoutineApi.getRoutine(ROUTINE_NAME))
                .isNotNull()
                .isEqualTo(routineAllFields)
                .isEqualTo(AbsRoutineApi.getRoutine(ROUTINE_NAME));
    }

    @Test
    void deleteRoutine() {
        AbsRoutineApi.createRoutine(ROUTINE_NAME, new DtoBackupRoutine()
                .storage(STORAGE_NAME)
                .backupPolicy(POLICY_NAME)
                .intervalCron("@daily")
                .sourceCluster(CLUSTER_NAME)
        );
        assertThat(AbsRoutineApi.getAllRoutines()).containsKey(ROUTINE_NAME);
        AbsRoutineApi.deleteRoutine(ROUTINE_NAME);
        assertThat(AbsRoutineApi.getAllRoutines()).doesNotContainKey(ROUTINE_NAME);
    }

    @Test
    void updateRoutine() {
        ApiResponse<Void> response = AbsRoutineApi.createRoutine(ROUTINE_NAME, new DtoBackupRoutine()
                .storage(STORAGE_NAME)
                .backupPolicy(POLICY_NAME)
                .intervalCron("@daily")
                .sourceCluster(CLUSTER_NAME));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);

        AbsRoutineApi.updateRoutine(ROUTINE_NAME, routineAllFields);
        assertThat(AbsRoutineApi.getRoutine(ROUTINE_NAME))
                .isNotNull()
                .isEqualTo(routineAllFields);
    }
}
