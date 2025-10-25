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
class RateLimitTest extends CliBackupRunner {
    private static final String SET1 = "SetRateLimitTest";
    private static final String SOURCE_NAMESPACE = "source-ns5";
    private static String backupKey;
    private static int numberOfRecordsBeforeTruncate;

    @BeforeAll
    static void setUp() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        ASBench.on(SOURCE_NAMESPACE, SET1)
                .keys(500_000)
                .batchSize(100)
                .threads(10)
                .run();

        numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, "RateLimitTestBackup").run();

        assertThat(backupResult.getBytesWritten()).isGreaterThan(100_000);
        backupKey = backupResult.getBackupDir();
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void disableBatchWrites() {
        Instant startTime = Instant.now();
        CliRestore.on(SOURCE_NAMESPACE, backupKey).disableBatchWrites().run();
        Duration restoreDuration = Duration.between(startTime, Instant.now());

        AerospikeLogger.info("Restore took " + restoreDuration.get(ChronoUnit.SECONDS) + " seconds");

        if (AutoUtils.isRunningOnGCP())
            assertThat(restoreDuration.get(ChronoUnit.SECONDS)).isBetween(10L, 200L);
        else
            assertThat(restoreDuration.get(ChronoUnit.SECONDS)).isBetween(6L, 150L);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);
    }

    @Test
    void lowMaxAsyncBatches() {
        int maxAsyncBatches = 1;
        int batchSize = 10;

        Instant startTime = Instant.now();
        CliRestore.on(SOURCE_NAMESPACE, backupKey).setMaxAsyncBatches(maxAsyncBatches).setBatchSize(batchSize).run();

        Duration restoreDuration = Duration.between(startTime, Instant.now());

        AerospikeLogger.info("Restore took " + restoreDuration.get(ChronoUnit.SECONDS) + " seconds");

        if (AutoUtils.isRunningOnGCP())
            assertThat(restoreDuration.get(ChronoUnit.SECONDS)).isBetween(10L, 200L);
        else if (ConfigParametersHandler.getParameter("IS_RUNNING_ON_LOCAL_3_NODES_ENV").equals("true"))
            assertThat(restoreDuration.get(ChronoUnit.SECONDS)).isBetween(10L, 100L);
        else
            assertThat(restoreDuration.get(ChronoUnit.SECONDS)).isBetween(10L, 60L);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);

        startTime = Instant.now();
        CliRestore.on(SOURCE_NAMESPACE, backupKey).setBatchSize(batchSize).run();

        restoreDuration = Duration.between(startTime, Instant.now());

        AerospikeLogger.info("Restore took " + restoreDuration.get(ChronoUnit.SECONDS) + " seconds");

        assertThat(restoreDuration.get(ChronoUnit.SECONDS)).isLessThanOrEqualTo(10);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);
    }

    @Test
    void lowBatchSize() {
        int batchSize = 1;

        Instant startTime = Instant.now();
        CliRestore.on(SOURCE_NAMESPACE, backupKey).setBatchSize(batchSize).run();

        Duration restoreDuration = Duration.between(startTime, Instant.now());

        AerospikeLogger.info("Restore took " + restoreDuration.get(ChronoUnit.SECONDS) + " seconds");

        assertThat(restoreDuration.get(ChronoUnit.SECONDS)).isBetween(6L, 40L);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);
    }

    @Test
    void restoreWithoutLimiter() {
        Instant startTime = Instant.now();
        CliRestore.on(SOURCE_NAMESPACE, backupKey).run();

        Duration restoreDuration = Duration.between(startTime, Instant.now());

        AerospikeLogger.info("Restore took " + restoreDuration.get(ChronoUnit.SECONDS) + " seconds");

        if (AutoUtils.isRunningOnGCP())
            assertThat(restoreDuration.get(ChronoUnit.SECONDS)).isLessThan(10);
        else
            assertThat(restoreDuration.get(ChronoUnit.SECONDS)).isLessThan(7);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);
    }
}