package api.abs.longDuration.aws;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.AwsUtils;
import utils.K8sUtils;
import utils.abs.AbsRunner;

@Tag("ABS-LONG-DURATION-TEST")
class PrintLogsTest extends AbsRunner {

    @Test
    void printLogsInLoop() {
        while (!LongDurationParent.testFinished) {
            try {
                printAllLogs();
                AutoUtils.sleep(300_000);
            } catch (Exception e) {
                AerospikeLogger.info(e.getMessage());
            }
        }
        printAllLogs();
    }

    private static void printSourceClusterLogs() {
        String clusterLog = AwsUtils.runGcCloudCommandOnCluster("sudo tail -700 /var/log/aerospike/aerospike.log");
        if (clusterLog.contains("ERROR") || clusterLog.contains("WARN")) {
            AerospikeLogger.info("Cluster log contained error or warning: " + clusterLog);
        }
    }

    private static void printAllLogs() {
        if (LongDurationParent.testLog.equals(""))
            AerospikeLogger.info("No errors in tests");
        else
            AerospikeLogger.info(LongDurationParent.testLog);
        K8sUtils.printPodsStatistics();
        K8sUtils.printAllK8sPodLogs();
        printSourceClusterLogs();
    }
}