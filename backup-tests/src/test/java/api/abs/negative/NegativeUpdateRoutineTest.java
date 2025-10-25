package api.abs.negative;

import api.abs.AbsPolicyApi;
import api.abs.AbsRoutineApi;
import api.abs.AbsStorageApi;
import api.abs.generated.model.DtoBackupPolicy;
import api.abs.generated.model.DtoBackupRoutine;
import api.abs.generated.model.DtoLocalStorage;
import api.abs.generated.model.DtoStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.abs.AbsRunner;
import utils.aerospike.abs.AerospikeDataUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("ABS-NEGATIVE-TESTS")
class NegativeUpdateRoutineTest extends AbsRunner {
    private static String POLICY_NAME;
    private static String STORAGE_NAME;
    private static String ROUTINE_NAME;
    private static final String CLUSTER_NAME = "absDefaultCluster";
    private static String SOURCE_NAMESPACE;
    private static DtoBackupPolicy POLICY;
    private static DtoStorage STORAGE;
    private static DtoBackupRoutine ROUTINE;

    @BeforeEach
    public void setUpEach() {
        POLICY_NAME = "NegativeUpdateBackupTestPolicy" + System.currentTimeMillis();
        STORAGE_NAME = "NegativeUpdateBackupTestStorage" + System.currentTimeMillis();
        ROUTINE_NAME = "NegativeUpdateBackupTestRoutine" + System.currentTimeMillis();

        createBackupEntities();

        AbsPolicyApi.createPolicy(POLICY_NAME, POLICY);
        AbsStorageApi.createStorage(STORAGE_NAME, STORAGE);
        AbsRoutineApi.createRoutine(ROUTINE_NAME, ROUTINE);

        SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void nonNumericPartitionList() {
        ROUTINE.setPartitionList("nonNumeric");
        assertThatThrownBy(() -> AbsRoutineApi.updateRoutine(ROUTINE_NAME, ROUTINE))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("invalid partition list: \"nonNumeric\"");
    }

    @Test
    void tooHighPartitionList() {
        ROUTINE.setPartitionList("50000-50001");
        assertThatThrownBy(() -> AbsRoutineApi.updateRoutine(ROUTINE_NAME, ROUTINE))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("invalid partition list: \"50000-50001\"");
    }

    @Test
    void negativePartitionList() {
        ROUTINE.setPartitionList("-5--6");
        assertThatThrownBy(() -> AbsRoutineApi.updateRoutine(ROUTINE_NAME, ROUTINE))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("invalid partition list: \"-5--6\"");
    }

    private void createBackupEntities() {
        POLICY = new DtoBackupPolicy()
                .parallel(1)
                .sealed(true);

        STORAGE = new DtoStorage().localStorage(new DtoLocalStorage().path("NegativeUpdateBackupTestPath"));

        ROUTINE = new DtoBackupRoutine()
                .backupPolicy(POLICY_NAME)
                .intervalCron("@yearly")
                .namespaces(List.of("source-ns22"))
                .sourceCluster(CLUSTER_NAME)
                .storage(STORAGE_NAME);
    }
}