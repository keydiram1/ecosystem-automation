package api.cli.load.xdr;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AerospikeLogger;
import utils.AutoUtils;

//@Tag("CLI-XDR-LOAD-TEST")
class PrintLogsTest extends LoadBackupParent {

    @Test
    void printLogsInLoop() {
        while (!LoadBackupParent.testFinished) {
            try {
                printAllLogs();
                AutoUtils.sleep(60_000);
            } catch (Exception e) {
                AerospikeLogger.info("printLogsInLoop failed with the following exception: " + e.getMessage());
            }
        }
        printAllLogs();
    }

    private static void printAllLogs() {
        AutoUtils.runBashCommand("docker stats --no-stream");
        AutoUtils.runBashCommand("docker logs --tail 300 aerospike-source");
    }
}