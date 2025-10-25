package api.abs.load.local;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.abs.AbsRunner;

@Tag("ABS-LOCAL-LOAD-TEST")
class PrintLogsTest extends AbsRunner {

    @Test
    void printLogsInLoop() {
        while (!LoadBackupParent.testFinished) {
            try {
                printAllLogs();
                AutoUtils.sleep(120_000);
            } catch (Exception e) {
                AerospikeLogger.info(e.getMessage());
            }
        }
        printAllLogs();
    }

    private static void printAllLogs() {
        AutoUtils.runBashCommand("docker stats --no-stream");
        AerospikeLogger.infoToFile(AutoUtils.runBashCommand("docker logs --tail 100000 backup-service", false));
        AutoUtils.runBashCommand("docker logs --tail 300 backup-service");
    }
}