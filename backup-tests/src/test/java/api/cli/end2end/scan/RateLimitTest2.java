package api.cli.end2end.scan;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.ConfigParametersHandler;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("CLI-BACKUP")
class RateLimitTest2 extends CliBackupRunner {
    private static final String SET1 = "SetRateLimit2Test";
    private static final String SOURCE_NAMESPACE = "source-ns18";
    private static String backupKey;
    private static int numberOfRecordsBeforeTruncate;

    @BeforeAll
    static void setUp() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        ASBench.on(SOURCE_NAMESPACE, SET1).keys(20).recordSize(8_000_000).run();
        numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsBeforeTruncate).isEqualTo(20);

        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, "RateLimitTestBackup").run();

        assertThat(backupResult.getBytesWritten()).isGreaterThan(100_000);
        backupKey = backupResult.getBackupDir();
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void testRestoreWithLowBandwidth() {
        int bandwidth = 8;

        Instant startTime = Instant.now();
        CliRestore.on(SOURCE_NAMESPACE, backupKey, 1).setBandwidth(bandwidth).run();

        Duration restoreDuration = Duration.between(startTime, Instant.now());

        AerospikeLogger.info("Restore took " + restoreDuration.get(ChronoUnit.SECONDS) + " seconds");

        assertThat(restoreDuration.get(ChronoUnit.SECONDS)).isBetween(3L, 40L);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);
    }

    @Test
    void restoreWithoutLimiter() {
        Instant startTime = Instant.now();
        CliRestore.on(SOURCE_NAMESPACE, backupKey, 1).run();

        Duration restoreDuration = Duration.between(startTime, Instant.now());

        AerospikeLogger.info("Restore took " + restoreDuration.get(ChronoUnit.SECONDS) + " seconds");

        if (AutoUtils.isRunningOnGCP())
            assertThat(restoreDuration.get(ChronoUnit.SECONDS)).isLessThan(10);
        else
            assertThat(restoreDuration.get(ChronoUnit.SECONDS)).isLessThan(3);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);
    }
}