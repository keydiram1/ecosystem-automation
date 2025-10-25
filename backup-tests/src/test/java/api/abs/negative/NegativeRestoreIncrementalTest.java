package api.abs.negative;

import api.abs.*;
import api.abs.generated.model.*;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import utils.abs.AbsRunner;
import utils.aerospike.abs.AerospikeDataUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("ABS-NEGATIVE-TESTS")
class NegativeRestoreIncrementalTest extends AbsRunner {
    private static final String ROUTINE_NAME = "timestamp";
    private static final DtoBackupRoutine ROUTINE = AbsRoutineApi.getRoutine(ROUTINE_NAME);
    private static DtoStorage STORAGE;
    private static final String SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE);
    private static final DtoAerospikeCluster sourceCluster = getCluster();

    private static DtoAerospikeCluster getCluster() {
        return AbsClusterApi.getCluster(ROUTINE.getSourceCluster());
    }

    @BeforeAll
    static void beforeAll() {
        DtoBackupDetails details = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        STORAGE = new DtoStorage().localStorage(new DtoLocalStorage().path(details.getKey()));
    }

    @AfterAll
    static void cleanup() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void wrongFile() {
        var wrongStorage = new DtoStorage().localStorage(new DtoLocalStorage().path("wrongPath"));
        JobID jobId = AbsRestoreApi.restoreIncremental(wrongStorage, sourceCluster);
        AbsRestoreApi.waitForRestoreFail(jobId);
    }

    @Test
    void fileNull() {
        var nullStorage = new DtoStorage().localStorage(new DtoLocalStorage().path(null));
        assertThatThrownBy(() -> AbsRestoreApi.restoreIncremental(nullStorage, sourceCluster))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("storage path is not specified");
    }

    @Test
    void clusterNull() {
        assertThatThrownBy(() -> AbsRestoreApi.restoreIncremental(STORAGE, null))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("must specify either \"destination\" or \"destination-name\"");
    }

    @Test
    void wrongHost() {
        sourceCluster.seedNodes(List.of(new DtoSeedNode().port(3000).hostName("wrongHost")));
        JobID jobId = AbsRestoreApi.restoreIncremental(STORAGE, sourceCluster);
        AbsRestoreApi.waitForRestoreFail(jobId);
    }

    @Test
    void nullHost() {
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreIncremental(STORAGE, getCluster().seedNodes(List.of(new DtoSeedNode().hostName(null))));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("empty field validation error: \"hostname\" required");
    }

    @Test
    void wrongPort() {
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreIncremental(STORAGE, getCluster().seedNodes(
                    List.of(new DtoSeedNode().hostName("localhost").port(0))));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("port number 0 invalid: must be between 1 and 65535");
    }

    @Test
    void nullPort() {
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreIncremental(STORAGE, getCluster().seedNodes(List.of(new DtoSeedNode().hostName("localhost").port(null))));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("port number 0 invalid: must be between 1 and 65535");
    }

    @Test
    void wrongSourceClusterUser() {
        sourceCluster.credentials(new DtoCredentials().user("wrongUser"));
        JobID jobId = AbsRestoreApi.restoreIncremental(STORAGE, sourceCluster);
        AbsRestoreApi.waitForRestoreFail(jobId);
    }

    @Test
    void negativeTimeout() {
        DtoRestorePolicy policy = new DtoRestorePolicy().totalTimeout(-1);
        assertThatThrownBy(() -> AbsRestoreApi.restoreIncremental(STORAGE, sourceCluster, policy))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("\"total-timeout\" -1 invalid, should not be negative number");
    }

    @Test
    void negativeTps() {
        DtoRestorePolicy policy = new DtoRestorePolicy().tps(-1);
        assertThatThrownBy(() -> AbsRestoreApi.restoreIncremental(STORAGE, sourceCluster, policy))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("\"tps\" -1 invalid, should be positive number");
    }

    @Test
    void negativeBandwidth() {
        DtoRestorePolicy policy = new DtoRestorePolicy().bandwidth(-1);
        assertThatThrownBy(() -> AbsRestoreApi.restoreIncremental(STORAGE, sourceCluster, policy))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("negative value validation error: \"bandwidth\" -1 invalid, should not be negative number");
    }

    @Test
    void negativeMaxAsyncBatches() {
        DtoRestorePolicy policy = new DtoRestorePolicy().maxAsyncBatches(-1);
        assertThatThrownBy(() -> AbsRestoreApi.restoreIncremental(STORAGE, sourceCluster, policy))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("\"max-async-batches\" -1 invalid, should be positive number");
    }

    @Test
    void negativeBatchSize() {
        DtoRestorePolicy policy = new DtoRestorePolicy().batchSize(-1);
        assertThatThrownBy(() -> AbsRestoreApi.restoreIncremental(STORAGE, sourceCluster, policy))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("\"batch-size\" -1 invalid, should be positive number");
    }

    @Test
    void negativeParallel() {
        DtoRestorePolicy policy = new DtoRestorePolicy().parallel(-1);
        assertThatThrownBy(() -> AbsRestoreApi.restoreIncremental(STORAGE, sourceCluster, policy))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("\"parallel\" -1 invalid, should be positive number");
    }

    @Test
    void getAllIncrementalBackupsWrongName() {
        assertThatThrownBy(() -> {
            AbsBackupApi.getIncrementalBackups("wrongRoutineName");
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("routine \"wrongRoutineName\" not found");
    }
}