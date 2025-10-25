package api.abs.negative;

import api.abs.AbsClusterApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import api.abs.JobID;
import api.abs.generated.model.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.abs.AbsRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("ABS-NEGATIVE-TESTS")
class NegativeRestoreTimestampTest extends AbsRunner {
    private static final String ROUTINE_NAME = "timestamp";
    private static final DtoBackupRoutine ROUTINE = AbsRoutineApi.getRoutine(ROUTINE_NAME);
    private static final String CLUSTER_NAME = ROUTINE.getSourceCluster();
    private static final DtoAerospikeCluster SOURCE_CLUSTER = getCluster();
    private static final long timestampAfterbackup = 100;

    private static DtoAerospikeCluster getCluster() {
        return AbsClusterApi.getCluster(CLUSTER_NAME);
    }

    private static DtoRestoreTimestampRequest getRestoreRequest(long time, DtoRestorePolicy policy, DtoAerospikeCluster cluster) {
        return new DtoRestoreTimestampRequest()
                .time(time)
                .destination(cluster)
                .policy(policy)
                .routine(ROUTINE_NAME);
    }

    @Test
    void negativeTimestamp() {
        DtoRestoreTimestampRequest restoreRequest = getRestoreRequest(-1, new DtoRestorePolicy(), SOURCE_CLUSTER);
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreTimestamp(restoreRequest);
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("\"time\" -1 invalid, should not be negative number");
    }

    @Test
    void negativeTps() {
        DtoRestorePolicy policy = new DtoRestorePolicy().tps(-1);
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreTimestamp(getRestoreRequest(timestampAfterbackup, policy, SOURCE_CLUSTER));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("\"tps\" -1 invalid, should be positive number");
    }

    @Test
    void negativeBandwidth() {
        DtoRestorePolicy policy = new DtoRestorePolicy().bandwidth(-1);
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreTimestamp(getRestoreRequest(timestampAfterbackup, policy, SOURCE_CLUSTER));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("negative value validation error: \"bandwidth\" -1 invalid, should not be negative number");
    }

    @Test
    void negativeMaxAsyncBatches() {
        DtoRestorePolicy policy = new DtoRestorePolicy().maxAsyncBatches(-1);
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreTimestamp(getRestoreRequest(timestampAfterbackup, policy, SOURCE_CLUSTER));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("\"max-async-batches\" -1 invalid, should be positive number");
    }

    @Test
    void negativeTimeout() {
        DtoRestorePolicy policy = new DtoRestorePolicy().totalTimeout(-1);
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreTimestamp(getRestoreRequest(timestampAfterbackup, policy, SOURCE_CLUSTER));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("\"total-timeout\" -1 invalid, should not be negative number");
    }

    @Test
    void destinationNull() {
        DtoRestorePolicy policy = new DtoRestorePolicy();
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreTimestamp(getRestoreRequest(timestampAfterbackup, policy, SOURCE_CLUSTER).destination(null));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("must specify either \"destination\" or \"destination-name\"");
    }

    @Test
    void negativeBatchSize() {
        DtoRestorePolicy policy = new DtoRestorePolicy().batchSize(-1);
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreTimestamp(getRestoreRequest(timestampAfterbackup, policy, SOURCE_CLUSTER));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("\"batch-size\" -1 invalid, should be positive number");
    }

    @Test
    void negativeParallel() {
        DtoRestorePolicy policy = new DtoRestorePolicy().parallel(-1);
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreTimestamp(getRestoreRequest(timestampAfterbackup, policy, SOURCE_CLUSTER));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("\"parallel\" -1 invalid, should be positive number");
    }

    @Test
    void nullHost() {
        DtoAerospikeCluster cluster = getCluster().seedNodes(List.of(new DtoSeedNode().hostName(null)));
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreTimestamp(getRestoreRequest(timestampAfterbackup, new DtoRestorePolicy(), cluster));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("empty field validation error: \"hostname\" required");
    }

    @Test
    void nullPort() {
        DtoAerospikeCluster cluster = getCluster().seedNodes(List.of(
                new DtoSeedNode().hostName("localhost").port(null)));
        assertThatThrownBy(() -> {
            AbsRestoreApi.restoreTimestamp(getRestoreRequest(timestampAfterbackup, new DtoRestorePolicy(), cluster));
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("port number 0 invalid: must be between 1 and 65535");
    }

    @Test
    void getRestoreStatusWrongJobId() {
        JobID wrongJobId = new JobID(-100);
        assertThatThrownBy(() -> {
            AbsRestoreApi.getRestoreStatus(wrongJobId);
        });
    }
}