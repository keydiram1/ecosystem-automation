package api.abs.negative;

import api.abs.AbsPolicyApi;
import api.abs.AbsRoutineApi;
import api.abs.AbsStorageApi;
import api.abs.generated.model.DtoBackupPolicy;
import api.abs.generated.model.DtoBackupRoutine;
import api.abs.generated.model.DtoLocalStorage;
import api.abs.generated.model.DtoStorage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.abs.AbsLogHandler;
import utils.abs.AbsRunner;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("ABS-NEGATIVE-TESTS")
class RoutineWithWrongValuesTest extends AbsRunner {
    private static String POLICY_NAME;
    private static String STORAGE_NAME;
    private static String ROUTINE_NAME;
    private static final String CLUSTER_NAME = "absDefaultCluster";
    private static DtoBackupPolicy POLICY;
    private static DtoStorage STORAGE;
    private static DtoBackupRoutine ROUTINE;

    @BeforeEach
    public void setUpEach() {
        POLICY_NAME = "RoutineWithWrongValuesPolicy" + System.currentTimeMillis();
        STORAGE_NAME = "RoutineWithWrongValuesStorage" + System.currentTimeMillis();
        ROUTINE_NAME = "RoutineWithWrongValuesRoutine" + System.currentTimeMillis();

        createBackupEntities();

        AbsPolicyApi.createPolicy(POLICY_NAME, POLICY);
        AbsStorageApi.createStorage(STORAGE_NAME, STORAGE);
        AbsRoutineApi.createRoutine(ROUTINE_NAME, ROUTINE);
    }

    @AfterAll
    static void afterAll() {
        AbsRoutineApi.deleteRoutine(ROUTINE_NAME);
        AbsPolicyApi.deletePolicy(POLICY_NAME);
        AbsStorageApi.deleteStorage(STORAGE_NAME);
    }

    @Test
    void notExistCluster() {
        ROUTINE = new DtoBackupRoutine()
                .backupPolicy(POLICY_NAME)
                .intervalCron("@yearly")
                .namespaces(List.of("source-ns21"))
                .sourceCluster("notExistCluster")
                .storage(STORAGE_NAME);

        assertThatThrownBy(() -> AbsRoutineApi.updateRoutine(ROUTINE_NAME, ROUTINE))
                .hasMessageContaining("Aerospike cluster \"notExistCluster\"");
    }

    @Test
    void notExistNS() {
        AbsLogHandler logHandler = new AbsLogHandler();
        ROUTINE = new DtoBackupRoutine()
                .backupPolicy(POLICY_NAME)
                .intervalCron("@yearly")
                .namespaces(List.of("notExistNS"))
                .sourceCluster(CLUSTER_NAME)
                .storage(STORAGE_NAME);

        AbsRoutineApi.updateRoutine(ROUTINE_NAME, ROUTINE);
        String lastLogs = logHandler.getBackupServiceLog();
        assertThat(lastLogs).contains("missingNamespaces=[notExistNS]");
    }

    @Test
    void notExistStorage() {
        ROUTINE = new DtoBackupRoutine()
                .backupPolicy(POLICY_NAME)
                .intervalCron("@yearly")
                .namespaces(List.of("source-ns21"))
                .sourceCluster(CLUSTER_NAME)
                .storage("notExistStorage");

        assertThatThrownBy(() -> AbsRoutineApi.updateRoutine(ROUTINE_NAME, ROUTINE))
                .hasMessageContaining("storage \"notExistStorage\"");
    }

    @Test
    void notExistPolicy() {
        ROUTINE = new DtoBackupRoutine()
                .backupPolicy("notExistPolicy")
                .intervalCron("@yearly")
                .namespaces(List.of("source-ns21"))
                .sourceCluster(CLUSTER_NAME)
                .storage(STORAGE_NAME);

        assertThatThrownBy(() -> AbsRoutineApi.updateRoutine(ROUTINE_NAME, ROUTINE))
                .hasMessageContaining("backup policy \"notExistPolicy\"");
    }

    @Test
    void nullCluster() {
        ROUTINE = new DtoBackupRoutine()
                .backupPolicy(POLICY_NAME)
                .intervalCron("@yearly")
                .namespaces(List.of("source-ns21"))
                .sourceCluster(null)
                .storage(STORAGE_NAME);

        assertThatThrownBy(() -> AbsRoutineApi.updateRoutine(ROUTINE_NAME, ROUTINE))
                .hasMessageContaining("\"source-cluster\" required");
    }

    @Test
    void nullStorage() {
        ROUTINE = new DtoBackupRoutine()
                .backupPolicy(POLICY_NAME)
                .intervalCron("@yearly")
                .namespaces(List.of("source-ns21"))
                .sourceCluster(CLUSTER_NAME)
                .storage(null);

        assertThatThrownBy(() -> AbsRoutineApi.updateRoutine(ROUTINE_NAME, ROUTINE))
                .hasMessageContaining("\"storage\" required");
    }

    private void createBackupEntities() {
        POLICY = new DtoBackupPolicy()
                .parallel(1)
                .sealed(true);

        STORAGE = new DtoStorage().localStorage(
                new DtoLocalStorage().path("BackupWithNewConfigTestPath"));

        ROUTINE = new DtoBackupRoutine()
                .backupPolicy(POLICY_NAME)
                .intervalCron("@yearly")
                .namespaces(Collections.emptyList())
                .sourceCluster(CLUSTER_NAME)
                .storage(STORAGE_NAME);
    }
}