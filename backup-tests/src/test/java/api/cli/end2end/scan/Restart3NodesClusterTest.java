package api.cli.end2end.scan;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import api.cli.RestoreResult;
import org.junit.jupiter.api.BeforeEach;
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
class Restart3NodesClusterTest extends CliBackupRunner {

    private static final String SET = "SetRestartDuringBackup";
    private static final String SOURCE_NAMESPACE = "source-ns1";
    private static final String BACKUP_DIR = "BackupRestartTestDir";
    private static final int MIN_EXPECTED_DURATION_MS = 10_000;

    @BeforeEach
    void clean() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void scanBackupWithPodRestartInTheMiddle() throws ExecutionException, InterruptedException {
        ASBench.on(SOURCE_NAMESPACE, SET).keys(30).threads(1).batchSize(30).recordSize(1000).run();
        int baseCount = AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE);
        assertThat(baseCount).isEqualTo(30);

        // Start slow async backup which should take longer than 10 seconds
        CompletableFuture<BackupResult> backupFuture = CompletableFuture.supplyAsync(() -> {
            AerospikeLogger.info("Starting Backup...");
            long start = System.nanoTime();
            BackupResult result = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR, 1).setRecordsPerSecond(1).run();
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

        RestoreResult restoreResult = CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir())
                .setUnique()
                .setRetryMaxRetries(1000)
                .run();

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE)).isEqualTo(baseCount);
        assertThat(restoreResult.getInsertedRecords() + restoreResult.getExistedRecords()).isEqualTo(baseCount);
        assertThat(backupResult.getDurationMillis()).isGreaterThan(MIN_EXPECTED_DURATION_MS);
    }

    @Test
    void scanRestoreWithPodRestartInTheMiddle() throws ExecutionException, InterruptedException {
        ASBench.on(SOURCE_NAMESPACE, SET).keys(1_500_000).threads(50).batchSize(100).recordSize(1000).run();
        int baseCount = AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE);
        assertThat(baseCount).isEqualTo(1_500_000);

        AerospikeLogger.info("Running synchronous backup before restore test...");
        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).run();

        AerospikeLogger.info("Truncating namespace before restore...");
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE)).isEqualTo(0);

        AerospikeLogger.info("Starting async restore...");
        CompletableFuture<RestoreResult> restoreFuture = CompletableFuture.supplyAsync(() -> {
            long start = System.nanoTime();
            RestoreResult restoreResult = CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir(), 1)
                    .setRetryMaxRetries(1000)
                    .setMaxRetries(1000)
                    .setSocketTimeout(0)
                    .setTotalTimeout(10000)
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

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET, SOURCE_NAMESPACE)).isEqualTo(baseCount);
        assertThat(restoreResult.getInsertedRecords() + restoreResult.getExistedRecords()).isEqualTo(baseCount);
        assertThat(restoreResult.getDurationMillis()).isGreaterThan(MIN_EXPECTED_DURATION_MS);
    }
}