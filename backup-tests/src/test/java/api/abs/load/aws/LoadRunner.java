package api.abs.load.aws;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import utils.*;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class LoadRunner extends AbsRunner {

    protected static final PhaserWaitGroup waitGroup = new PhaserWaitGroup("stress");
    protected static final String SET = "LoadSet";
    protected static final String SET2 = "LoadSet2";
    protected static int minutesToWaitForAllLoadClassesToFinishSetup = -1;
    public static final String LOAD_LEVEL = ConfigParametersHandler.getParameter("LOAD_LEVEL");

    @BeforeAll
    public static void register() {
        waitGroup.register();
    }

    @AfterAll
    public static void cleanupClass() {
        K8sUtils.printAllK8sPodLogs(false);
        AerospikeLogger.info("test ended");
    }

    protected void createBigData(String sourceNamespace) {
        int asbenchMaxNumberOfKeys = 2_000_000_000;
        int desiredMinNumberOfRecords = 15_000_000;
        int desiredMaxNumberOfRecords = 20_000_000;
        int recordCount = 0;
        if (LOAD_LEVEL.equals("low")) {
            ASBench.on(sourceNamespace, SET).keys(asbenchMaxNumberOfKeys).duration(1).run();
            recordCount = AerospikeCountUtils.getSetObjectCount(srcClient, SET, sourceNamespace);
            assertThat(recordCount).isBetween(1000, 100_000);
        } else {
            ASBench.on(sourceNamespace, SET).keys(asbenchMaxNumberOfKeys).duration(1000).run();
            for (int i = 0; i < 30; i++) {
                ASBench.on(sourceNamespace, SET).startKey(recordCount).keys(asbenchMaxNumberOfKeys).duration(100).run();
                recordCount = AerospikeCountUtils.getSetObjectCount(srcClient, SET, sourceNamespace);
                AerospikeLogger.info("The record count is: " + recordCount);
                if (recordCount < desiredMinNumberOfRecords) {
                    AerospikeLogger.info("The record count is less than " + desiredMinNumberOfRecords + ". Continuing to create data.");
                } else {
                    AerospikeLogger.info("The record count is more than " + desiredMinNumberOfRecords + ". Exiting loop.");
                    break;
                }
            }
            AerospikeLogger.info("Final record count after data creation: " + recordCount);
            assertThat(recordCount).isBetween(desiredMinNumberOfRecords, desiredMaxNumberOfRecords);
        }
    }

}
