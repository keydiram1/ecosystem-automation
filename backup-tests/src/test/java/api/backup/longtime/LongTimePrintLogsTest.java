package api.backup.longtime;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.AwsUtils;
import utils.K8sUtils;
import utils.PhaserWaitGroup;
import utils.init.runners.BackupRunner;

@Tag("ADR-LONG-TIME-TEST")
class LongTimePrintLogsTest extends BackupRunner {

    private static final PhaserWaitGroup waitGroup = PhaserWaitGroup.singleton("long-time");

    @Test
    void printLogs() {
        AutoUtils.sleep(3000); // Wait for test classes to register
        AerospikeLogger.info("Number of unArrivedParties: " + waitGroup.numberOfArrivedParties.get());

        try {
            while (waitGroup.numberOfArrivedParties.get() > 0) {
                printAsLogs();
                K8sUtils.printAllK8sPodLogs();
                AutoUtils.sleep(300_000);
            }
        } catch (Exception e) {
            AerospikeLogger.info("The log printing failed with the following exception:");
            AerospikeLogger.info(e.getMessage());
        }
    }

    private static void printAsLogs() {
        for (int i = 1; i < 4; i++) {
            writeLogsForCluster(AwsUtils.ClusterName.BACKUP, i);
            writeLogsForCluster(AwsUtils.ClusterName.SOURCE, i);
        }
    }

    private static void writeLogsForCluster(AwsUtils.ClusterName clusterName, int nodeNumber) {
        String clusterLog = AwsUtils.runBashCommandOnCluster("tail -300 /var/log/aerospike/aerospike.log",
                clusterName, nodeNumber, false);
        if (clusterLog.contains("ERROR") || clusterLog.contains("WARN")) {
            AerospikeLogger.info(clusterName + " cluster node" + nodeNumber + " logs:");
            AerospikeLogger.info(clusterLog);
        }
    }
}
