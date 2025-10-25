package api.abs.load.local;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.abs.AbsRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-LOCAL-LOAD-TEST")
class AssertMemoryAndCpuTest extends AbsRunner {

    @Test
    void assertMemoryAndCpu() {
        while (!LoadBackupParent.testFinished) {
            try {
                checkMemoryUsage();
                checkCpuUsage();
                AutoUtils.sleep(60_000);
            } catch (Exception e) {
                AerospikeLogger.info("Stopping due to resource overuse or error: " + e.getMessage());
                break;
            }
        }
    }

    /**
     * Observed peak memory usage during local load tests: ~2.5%
     * Threshold: 5%
     */
    private void checkMemoryUsage() {
        String output = AutoUtils.runBashCommand("docker stats --no-stream --format \"{{.MemPerc}}\" backup-service");

        String memStr = output.trim().replace("\"", "").replace("%", "");
        double memPercent;
        try {
            memPercent = Double.parseDouble(memStr);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Failed to parse memory percentage: " + memStr, e);
        }

        AerospikeLogger.info("backup-service memory usage: " + memPercent + "%");
        assertThat(memPercent)
                .withFailMessage("backup-service memory usage too high: %.2f%% (threshold: 5%%)", memPercent)
                .isLessThanOrEqualTo(5.0);
    }

    /**
     * Observed peak CPU usage during local load tests: ~70%
     * Threshold: 150% (on 8 vCPUs = 800% max)
     */
    private void checkCpuUsage() {
        String output = AutoUtils.runBashCommand("docker stats --no-stream --format \"{{.CPUPerc}}\" backup-service");

        String cpuStr = output.trim().replace("\"", "").replace("%", "");
        double cpuPercent;
        try {
            cpuPercent = Double.parseDouble(cpuStr);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Failed to parse CPU percentage: " + cpuStr, e);
        }

        AerospikeLogger.info("backup-service CPU usage: " + cpuPercent + "%");
        assertThat(cpuPercent)
                .withFailMessage("backup-service CPU usage too high: %.2f%% (threshold: 150%%)", cpuPercent)
                .isLessThanOrEqualTo(150.0);
    }
}