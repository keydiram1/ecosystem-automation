package api.abs.longDuration.aws;

import api.abs.AbsBackupApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import api.abs.generated.model.DtoBackupDetails;
import utils.*;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import java.time.Duration;

public class LongDurationParent extends AbsRunner {
    protected String sourceNamespace;
    private static final String SET_NAME = "setLongTimeSpike4Test";
    protected static final String TEST_DURATION = ConfigParametersHandler.getParameter("TEST_DURATION");
    private static final long ON_GOING_THROUGHPUT = Long.parseLong(ConfigParametersHandler.getParameter("ON_GOING_THROUGHPUT"));
    private static final int DATA_SPIKES_DURATION = Integer.parseInt(ConfigParametersHandler.getParameter("DATA_SPIKES_DURATION"));
    protected String routineName;
    public static volatile boolean testFinished = false;
    public static String testLog = "";


    protected void setUpParent() {
        sourceNamespace = AbsRoutineApi.getAnyNamespaceForRoutine(routineName);
    }

    protected void restoreFullInLoopParent() {
        int loopCount = TEST_DURATION.equals("short") ? 2 : 48; // When the test duration will be long the test will run more than 24 hours.
        int asBenchDuration = TEST_DURATION.equals("short") ? 2 : 1800;
        printTestParameters(loopCount, asBenchDuration);
        int startKey;
        AutoUtils.runBashCommand("aws eks --region eu-central-1 update-kubeconfig --name abs-" + AwsUtils.AWS_WORKSPACE + "-eks");
        for (int i = 0; i < loopCount; i++) {
            try {
                startKey = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, sourceNamespace);
                ASBench.on(sourceNamespace, SET_NAME).keys(2_000_000_000).duration(DATA_SPIKES_DURATION).startKey(startKey).run();
                startKey = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, sourceNamespace);
                ASBench.on(sourceNamespace, SET_NAME).keys(2_000_000_000).duration(asBenchDuration).throughput(ON_GOING_THROUGHPUT).startKey(startKey).run();

                DtoBackupDetails backup = AbsBackupApi.startFullBackupSync(routineName, Duration.ofMinutes(7));

                AerospikeLogger.info("Backup size after adding data: " + backup.getByteCount());
                AerospikeLogger.info("Backup file count after adding data: " + backup.getFileCount());
                AerospikeLogger.info("Backup record count after adding data: " + backup.getRecordCount());
                int objectCountAfterAddingData = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, sourceNamespace);
                AerospikeLogger.info("Record count in DB after adding data: " + objectCountAfterAddingData);


                AerospikeDataUtils.truncateSourceNamespace(sourceNamespace);
                if (!TEST_DURATION.equals("short"))
                    AutoUtils.sleep(10_000);//wait for truncate to finish
                AerospikeLogger.info("Object count after truncate:");
                AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, sourceNamespace);

                var timeout = Duration.ofMinutes(TEST_DURATION.equals("short") ? 1 : 10);
                AbsRestoreApi.restoreFullSync(backup.getKey(), routineName, timeout);

                int objectCountAfterRestore = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, sourceNamespace);
                AerospikeLogger.info("Record count in DB after adding data: " + objectCountAfterAddingData);
                AerospikeLogger.info("Object count after restore: " + objectCountAfterRestore);
                K8sUtils.printPodsStatistics();
                if (objectCountAfterRestore != objectCountAfterAddingData) {
                    throw new RuntimeException("Assertion failed at iteration " + i +
                            ": Expected object count " + objectCountAfterAddingData +
                            ", but found " + objectCountAfterRestore);
                }
            } catch (Exception e) {
                String errorLog = getClass().getSimpleName() + " failed at iteration number " + i +
                        " with the following exception: " + e.getMessage();
                AerospikeLogger.info(errorLog);
                testLog += errorLog + "\n";
            }
        }
        testFinished = true;
    }

    private static void printTestParameters(int loopCount, int asBenchDuration) {
        AerospikeLogger.info("loopCount: " + loopCount);
        AerospikeLogger.info("asBenchDuration: " + asBenchDuration);
        AerospikeLogger.info("spikeDuration: " + DATA_SPIKES_DURATION);
        AerospikeLogger.info("testDuration: " + TEST_DURATION);
        AerospikeLogger.info("onGoingThroughput: " + ON_GOING_THROUGHPUT);
    }
}
