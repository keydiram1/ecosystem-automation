package api.abs.negative;

import api.abs.*;
import api.abs.generated.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.abs.AbsRunner;

import java.util.List;

import static api.abs.AbsRestoreApi.waitForRestoreFail;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("ABS-NEGATIVE-TESTS")
class NegativeRestoreFullTest extends AbsRunner {
    private static final String ROUTINE_NAME = "fullBackup1";
    private static final DtoBackupRoutine ROUTINE = AbsRoutineApi.getRoutine(ROUTINE_NAME);
    private static final String CLUSTER_NAME = ROUTINE.getSourceCluster();
    private static final DtoAerospikeCluster SOURCE_CLUSTER = getCluster();
    private static String backupKey;

    private static DtoAerospikeCluster getCluster() {
        return AbsClusterApi.getCluster(CLUSTER_NAME);
    }

    private static DtoRestoreRequest getRestoreRequest(String backupKey, DtoRestorePolicy policy, DtoAerospikeCluster cluster) {
        return new DtoRestoreRequest()
                .destination(cluster)
                .policy(policy)
                .backupDataPath(backupKey)
                .source(new DtoStorage().localStorage(new DtoLocalStorage().path("/")));
    }

    @BeforeAll
    static void beforeAll() {
        backupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();
    }

    @Test
    void wrongDirectory() {
        var restoreRequest = getRestoreRequest("wrongDirectory", new DtoRestorePolicy(), SOURCE_CLUSTER);
        var jobId = AbsRestoreApi.restoreFull(restoreRequest);
        waitForRestoreFail(jobId);
    }

    @Test
    void directoryNull() {
        var restoreRequest = getRestoreRequest(null, new DtoRestorePolicy(), SOURCE_CLUSTER);
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreFull(restoreRequest);
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("\"backup-data-path\" required");
    }

    @Test
    void negativeTps() {
        DtoRestorePolicy policy = new DtoRestorePolicy().tps(-1);
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreFull(getRestoreRequest(backupKey, policy, SOURCE_CLUSTER));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("\"tps\" -1 invalid, should be positive number");
    }

    @Test
    void negativeBandwidth() {
        DtoRestorePolicy policy = new DtoRestorePolicy().bandwidth(-1);
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreFull(getRestoreRequest(backupKey, policy, SOURCE_CLUSTER));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("negative value validation error: \"bandwidth\" -1 invalid, should not be negative number");
    }

    @Test
    void negativeMaxAsyncBatches() {
        DtoRestorePolicy policy = new DtoRestorePolicy().maxAsyncBatches(-1);
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreFull(getRestoreRequest(backupKey, policy, SOURCE_CLUSTER));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("\"max-async-batches\" -1 invalid, should be positive number");
    }

    @Test
    void negativeSocketTimeout() {
        DtoRestorePolicy policy = new DtoRestorePolicy().socketTimeout(-1);
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreFull(getRestoreRequest(backupKey, policy, SOURCE_CLUSTER));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("\"socket-timeout\" -1 invalid, should not be negative number");
    }

    @Test
    void negativeTotalTimeout() {
        DtoRestorePolicy policy = new DtoRestorePolicy().totalTimeout(-1);
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreFull(getRestoreRequest(backupKey, policy, SOURCE_CLUSTER));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("\"total-timeout\" -1 invalid, should not be negative number");
    }

    @Test
    void destinationNull() {
        DtoRestorePolicy policy = new DtoRestorePolicy();
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreFull(getRestoreRequest(backupKey, policy, SOURCE_CLUSTER).destination(null));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("must specify either \"destination\" or \"destination-name\"");
    }

    @Test
    void negativeBatchSize() {
        DtoRestorePolicy policy = new DtoRestorePolicy().batchSize(-1);
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreFull(getRestoreRequest(backupKey, policy, SOURCE_CLUSTER));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("\"batch-size\" -1 invalid, should be positive number");
    }

    @Test
    void negativeParallel() {
        DtoRestorePolicy policy = new DtoRestorePolicy().parallel(-1);
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreFull(getRestoreRequest(backupKey, policy, SOURCE_CLUSTER));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("\"parallel\" -1 invalid, should be positive number");
    }

    @Test
    void wrongHost() {
        JobID jobId = AbsRestoreApi.restoreFull(getRestoreRequest(backupKey, new DtoRestorePolicy(),
                getCluster().seedNodes(List.of(new DtoSeedNode().hostName("wrongHost").port(3000)))));
        waitForRestoreFail(jobId);
    }

    @Test
    void nullHost() {
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreFull(getRestoreRequest(backupKey, new DtoRestorePolicy(),
                    getCluster().seedNodes(List.of(new DtoSeedNode().hostName(null)))));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("empty field validation error: \"hostname\" required");
    }

    @Test
    void nullPort() {
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreFull(getRestoreRequest(backupKey, new DtoRestorePolicy(),
                    getCluster().seedNodes(List.of(new DtoSeedNode().hostName("localhost").port(null)))));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("port number 0 invalid: must be between 1 and 65535");
    }

    @Test
    void getRestoreStatusNullJobId() {
        assertThatThrownBy(AbsRestoreApi::getRestoreStatusForNull).isInstanceOf(Exception.class)
                .hasMessageContaining("\"jobId\" is null");
    }

    @Test
    void getRestoreJobsNegativeFrom() {
        assertThatThrownBy(() -> {
            AbsRestoreApi.getJobs(-1L, null, null);
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("invalid query param time bounds: negative value validation error: \"from\" -1 invalid, should not be negative number");
    }

    @Test
    void getRestoreJobsNegativeTo() {
        assertThatThrownBy(() -> {
            AbsRestoreApi.getJobs(null, -1L, null);
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("invalid query param time bounds: negative value validation error: \"to\" -1 invalid, should not be negative number");
    }

    @Test
    void getRestoreJobsWrongStatus() {
        assertThatThrownBy(() -> {
            AbsRestoreApi.getJobs(null, null, "wrong");
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("invalid query param status: invalid value validation error: 'wrong' is not a valid status.");
    }
}