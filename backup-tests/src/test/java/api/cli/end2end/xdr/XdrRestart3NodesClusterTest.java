package api.cli.end2end.xdr;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import api.cli.RestoreResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("CLI-3-NODES-CLUSTER")
@Execution(ExecutionMode.SAME_THREAD)
@Disabled
class XdrRestart3NodesClusterTest extends CliBackupRunner {
    private static final String SET = "SetRestartDuringBackup";
    private static final String SOURCE_NAMESPACE = "source-ns1";
    private static final String DC = "DcRestartTest";
    private static final String BACKUP_DIR = "BackupRestartTestDir";
    private static final int LOCAL_PORT = 9092;
    private static final int MIN_EXPECTED_DURATION_MS = 10_000;

    @BeforeEach
    void clean() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void xdrBackupWithPodRestartInTheMiddle() throws ExecutionException, InterruptedException {
        ASBench.on(SOURCE_NAMESPACE, SET).duration(60).keys(2_000_000_000).run();
        int baseCount = AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE);
        assertThat(baseCount).isGreaterThan(100_000);

        AutoUtils.sleep(10_000);

        // Start slow async backup which should take longer than 10 seconds
        CompletableFuture<BackupResult> backupFuture = CompletableFuture.supplyAsync(() -> {
            AerospikeLogger.info("Starting Backup...");
            long start = System.nanoTime();
            BackupResult result = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                    .setLocalAddress()
                    .setDc(DC)
                    .setLocalPort(LOCAL_PORT)
                    .setParallelWrite(1)
                    .setVerbose()
                    .run(true);
            double duration = (System.nanoTime() - start) / 1_000_000_000.0;
            AerospikeLogger.info("Backup completed in " + duration + "s");
            return result;
        });

        // Wait a bit for the backup to fully start, then restart the pod
        AutoUtils.sleep(2000);

        AerospikeLogger.info("Restarting pod in the middle of backup...");
        AutoUtils.restartPod("aerospike", "aerocluster-0-0");
        srcClient = createSourceClient();

        // Wait for backup to finish
        BackupResult backupResult = backupFuture.get();

        assertThat(backupResult.getRecordsRead()).isGreaterThanOrEqualTo(baseCount);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE)).isEqualTo(0);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir())
                .setUnique()
                .setRetryMaxRetries(1000)
                .run();

        // For XDR restore it is enough to check records in db as it is our aim.
        // In the report the number of records can be higher.
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE)).isEqualTo(baseCount);
        assertThat(backupResult.getDurationMillis()).isGreaterThan(MIN_EXPECTED_DURATION_MS);
    }

    @Test
    void xdrRestoreWithPodRestartInTheMiddle() throws ExecutionException, InterruptedException {
        ASBench.on(SOURCE_NAMESPACE, SET).duration(60).keys(2_000_000_000).run();
        int baseCount = AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE);
        assertThat(baseCount).isGreaterThan(100_000);

        AutoUtils.sleep(10_000);

        AerospikeLogger.info("Running synchronous backup before restore test...");
        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalAddress()
                .setDc(DC)
                .setLocalPort(LOCAL_PORT)
                .run();

        AerospikeLogger.info("Truncating namespace before restore...");
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE)).isEqualTo(0);

        AerospikeLogger.info("Starting async restore...");
        CompletableFuture<RestoreResult> restoreFuture = CompletableFuture.supplyAsync(() -> {
            long start = System.nanoTime();
            RestoreResult restoreResult = CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir())
                    .setRetryMaxRetries(1000)
                    .setUnique()
                    .run(true);
            double duration = (System.nanoTime() - start) / 1_000_000_000.0;
            AerospikeLogger.info("Restore completed in " + duration + "s");
            return restoreResult;
        });

        AutoUtils.sleep(2000);

        AerospikeLogger.info("Restarting pod in the middle of restore...");
        AutoUtils.restartPod("aerospike", "aerocluster-0-0");
        srcClient = createSourceClient();

        RestoreResult restoreResult = restoreFuture.get();

        // For XDR restore it is enough to check records in db as it is our aim.
        // In the report the number of records can be higher.
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE)).isEqualTo(baseCount);
        assertThat(restoreResult.getDurationMillis()).isGreaterThan(MIN_EXPECTED_DURATION_MS);
    }
}