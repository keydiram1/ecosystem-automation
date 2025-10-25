package api.abs.longDuration.local;

import api.abs.AbsBackupApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.DockerManager;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

@Tag("ABS-LONG-DURATION-TEST-LOCAL")
@Execution(ExecutionMode.CONCURRENT)
public class LongDurationLocalTest extends AbsRunner {
    private static String sourceNamespace;
    private static final String SET_NAME = "setLongTimeSpike4Test";
    private static final String ROUTINE_NAME = "edgeCases";
    private static volatile boolean testFinished = false;


    @BeforeAll
    static void setUp() {
        sourceNamespace = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);
    }

    @Test
    void backupAndRestore() {
        AerospikeLogger.infoToFile(DockerManager.getLogFromContainer("backup-service"));
        int startKey;
        int loopCount = 60;
        int asBenchDuration = 300;// 300 for 5 minutes in each iteration
        long onGoingThroughput = 10;
        int dataSpikesDuration = 5;
        for (int i = 0; i < loopCount; i++) {
            try {
                AutoUtils.printDockerStats();
                AerospikeLogger.info(DockerManager.getLogFromContainer("backup-service", 1_000));
                AerospikeLogger.info(DockerManager.getLogFromContainer("aerospike-source", 1_000));
                startKey = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, sourceNamespace);
                ASBench.on(sourceNamespace, SET_NAME).duration(dataSpikesDuration).startKey(startKey).run();
                startKey = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, sourceNamespace);
                ASBench.on(sourceNamespace, SET_NAME).duration(asBenchDuration).startKey(startKey).throughput(onGoingThroughput).run();

                long backupTime = System.currentTimeMillis();
                var backup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();
                AerospikeLogger.info("Backup size after adding data: " + AbsBackupApi.firstFullBackupAfter(ROUTINE_NAME, backupTime).get().getByteCount());
                AerospikeLogger.info("Object count before truncate");
                AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, sourceNamespace);


                AerospikeDataUtils.truncateSourceNamespace(sourceNamespace);
                AerospikeLogger.info("Object count after truncate:");
                AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, sourceNamespace);

                AbsRestoreApi.restoreFullSync(backup, ROUTINE_NAME);

                AerospikeLogger.info("Object count after restore:");
                AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, sourceNamespace);
                AerospikeLogger.info("Backup size after restore: " + AbsBackupApi.firstFullBackupAfter(ROUTINE_NAME, backupTime).get().getByteCount());
            } catch (Exception e) {
                AerospikeLogger.info("backupAndRestore failed due to: " + e.getMessage());
            }
        }
        testFinished = true;
    }

    @Test
    void printLogs() {
        while (!testFinished) {
            try {
                AutoUtils.printDockerStats();
                AutoUtils.runBashCommand("docker logs --tail 2000 backup-service");
                AutoUtils.sleep(30000);
            } catch (Exception e) {
                AerospikeLogger.info("Print logs failed due to: " + e.getMessage());
            }
        }
    }
}
