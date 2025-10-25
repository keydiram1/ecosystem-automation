package api.abs.end2end;

import api.abs.AbsBackupApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import api.abs.JobID;
import com.aerospike.client.IAerospikeClient;
import org.junit.jupiter.api.*;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-RECOVERY")
class AbsRecoveryTest extends AbsRunner {
    private static final IAerospikeClient srcClient = AerospikeDataUtils.getSourceClient();
    private static final String SET1 = "SetAbsRecoveryTest";
    private static final String ROUTINE_NAME = "fullBackup";
    private static String SOURCE_NAMESPACE;
    private static long numRecordsInSourceAfterAddingData;

    @BeforeAll
    static void setUp() {
        final var routine = AbsRoutineApi.getRoutine(ROUTINE_NAME);
        SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(routine);
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    @Disabled// When we enable this test, we need to let it work against a backup with larger intervals.
    // With small intervals the test pass since the new backups after the restart backs up the data.
    void restartOnBackup() {
        ASBench.on(SOURCE_NAMESPACE, SET1).duration(20).run();
        numRecordsInSourceAfterAddingData = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        AerospikeLogger.info("Number of records after adding data: " + numRecordsInSourceAfterAddingData);

        AutoUtils.runBashCommand("docker restart backup-service");

        // wait for the backup to finish
        AutoUtils.sleep(20000);
        String keyCreateFirstValue = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        AbsRestoreApi.restoreFullSync(keyCreateFirstValue, ROUTINE_NAME);

        AerospikeLogger.info("Number of records after restore: " + numRecordsInSourceAfterAddingData);
        AerospikeLogger.info(AutoUtils.runBashCommand("docker logs backup-service | tail -n 1000"));
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(numRecordsInSourceAfterAddingData);
    }

    @Test
    void restartOnRestore() {
        ASBench.on(SOURCE_NAMESPACE, SET1).duration(20).run();
        numRecordsInSourceAfterAddingData = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        AerospikeLogger.info("Number of records after adding data: " + numRecordsInSourceAfterAddingData);

        // wait for the backup to finish
        AutoUtils.sleep(20000);
        String keyCreateFirstValue = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        JobID jobId = AbsRestoreApi.restoreFull(keyCreateFirstValue, ROUTINE_NAME);
        AutoUtils.runBashCommand("docker restart backup-service");
        AbsRestoreApi.waitForRestore(jobId);

        AerospikeLogger.info("Number of records after restore: " + numRecordsInSourceAfterAddingData);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(numRecordsInSourceAfterAddingData);
    }
}