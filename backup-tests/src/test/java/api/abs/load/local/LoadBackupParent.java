package api.abs.load.local;

import api.abs.AbsBackupApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import com.aerospike.client.IAerospikeClient;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;


class LoadBackupParent extends AbsRunner {
    private String sourceNamespace;
    protected String routineName;
    protected static final String SET = "LoadSet";
    protected static int numberOfBackups = 0;
    protected static final IAerospikeClient srcClient2 = AerospikeDataUtils.createSourceClient(3003);
    public static volatile boolean testFinished = false;

    protected void setUpParent() {
        sourceNamespace = AbsRoutineApi.getAnyNamespaceForRoutine(routineName);
        AerospikeDataUtils.truncateSourceNamespace(sourceNamespace);
    }

    protected void testBackupParent(int sourcePort, IAerospikeClient client) {
        for (int i = 0; i < 300; i++) {
            numberOfBackups++;
            AerospikeLogger.info("Backup number " + numberOfBackups);
            AerospikeDataUtils.truncateNamespace(client, sourceNamespace);

            ASBench.on(sourceNamespace, SET)
                    .keys(1000)
                    .batchSize(1000)
                    .threads(1)
                    .recordSize(1000)
                    .port(sourcePort)
                    .run();
            int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(client, SET, sourceNamespace);
            assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

            String backupKey = AbsBackupApi.startFullBackupSync(routineName, Duration.ofMinutes(5)).getKey();

            AerospikeDataUtils.truncateSourceNamespace(sourceNamespace);

            AbsRestoreApi.restoreFullSync(backupKey, routineName);

            assertThat(AerospikeCountUtils.getSetObjectCount(client, SET, sourceNamespace)).isEqualTo(numberOfRecordsBeforeTruncate);
        }
        testFinished = true;
    }
}
