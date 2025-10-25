package api.abs.end2end;

import api.abs.*;
import api.abs.generated.model.*;
import com.aerospike.client.Key;
import com.aerospike.client.cluster.Partition;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.abs.AbsLogHandler;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.AerospikeScanner;
import utils.aerospike.abs.AerospikeDataUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-SEQUENTIAL-TESTS-2")
@Execution(ExecutionMode.SAME_THREAD)
class BackupOptionsTest extends AbsRunner {
    private static String POLICY_NAME;
    private static String STORAGE_NAME;
    private static String ROUTINE_NAME;
    private static final String CLUSTER_NAME = "absDefaultCluster";
    private static String SOURCE_NAMESPACE;
    private static final String SET1 = "setBackupOptions";
    private static DtoBackupPolicy POLICY;
    private static DtoStorage STORAGE;
    private static DtoBackupRoutine ROUTINE;

    @BeforeEach
    public void setUpEach() {
        POLICY_NAME = "BackupWithNewConfigTestPolicy" + System.currentTimeMillis();
        STORAGE_NAME = "BackupWithNewConfigTestStorage" + System.currentTimeMillis();
        ROUTINE_NAME = "BackupWithNewConfigTestRoutine" + System.currentTimeMillis();

        createBackupEntities();

        AbsPolicyApi.createPolicy(POLICY_NAME, POLICY);
        AbsStorageApi.createStorage(STORAGE_NAME, STORAGE);
        AbsRoutineApi.createRoutine(ROUTINE_NAME, ROUTINE);

        SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void recordsPerSecond() {
        final int RPS = 1_000;
        final int KEYS = 10_000;

        AbsPolicyApi.updatePolicy(POLICY_NAME, POLICY.recordsPerSecond(RPS));

        ASBench.on(SOURCE_NAMESPACE, SET1).keys(KEYS).threads(10).batchSize(100).run();
        int numberOfRecordsToBackup = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsToBackup).isEqualTo(KEYS);

        DtoBackupDetails dtoBackupDetails = AbsBackupApi.startFullBackupSync(ROUTINE_NAME, Duration.ofMinutes(10));
        AerospikeLogger.info("Backup for %d records took %s".formatted(numberOfRecordsToBackup,
                Duration.ofSeconds(dtoBackupDetails.getDuration())));

        var expectedDuration = KEYS / RPS;
        assertThat(dtoBackupDetails.getDuration()).isCloseTo(expectedDuration, Percentage.withPercentage(10));
    }

    @Test
    void totalTimeout() {
        AbsPolicyApi.updatePolicy(POLICY_NAME, POLICY.totalTimeout(1));

        String SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);

        ASBench.on(SOURCE_NAMESPACE, SET1).duration(1).run();
        int numberOfRecordsToBackup = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsToBackup).isGreaterThan(5_000);

        AbsLogHandler logHandler = new AbsLogHandler();

        long backupTime = System.currentTimeMillis();
        AbsBackupApi.scheduleFullBackup(ROUTINE_NAME);

        AutoUtils.sleep(5_000);

        if (!AutoUtils.isRunningOnGCP()) {
            String backupServiceLog = logHandler.getBackupServiceLog();
            AerospikeLogger.info(backupServiceLog);
            assertThat(backupServiceLog).contains("ResultCode: TIMEOUT", "command execution timed out on client");
        }

        AutoUtils.sleep(5_000);
        assertThat(AbsBackupApi.firstFullBackupAfter(ROUTINE_NAME, backupTime, SOURCE_NAMESPACE)).isNull();
        assertThat(AbsBackupApi.getCurrentBackup(ROUTINE_NAME).getFull()).isNull();
    }

    @Test
    void socketTimeout() {
        AbsPolicyApi.updatePolicy(POLICY_NAME, POLICY.socketTimeout(1));

        String SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);

        ASBench.on(SOURCE_NAMESPACE, SET1).duration(1).run();
        int numberOfRecordsToBackup = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsToBackup).isGreaterThan(5_000);

        AbsLogHandler logHandler = new AbsLogHandler();

        long backupTime = System.currentTimeMillis();
        AbsBackupApi.scheduleFullBackup(ROUTINE_NAME);

        AutoUtils.sleep(5_000);

        if (!AutoUtils.isRunningOnGCP()) {
            String backupServiceLog = logHandler.getBackupServiceLog();
            AerospikeLogger.info(backupServiceLog);
            assertThat(backupServiceLog).containsAnyOf(
                    "ResultCode: MAX_RETRIES_EXCEEDED", "ResultCode: TIMEOUT", "ResultCode: FAIL_FORBIDDEN");
        }

        AutoUtils.sleep(5_000);
        assertThat(AbsBackupApi.firstFullBackupAfter(ROUTINE_NAME, backupTime, SOURCE_NAMESPACE)).isNull();
        assertThat(AbsBackupApi.getCurrentBackup(ROUTINE_NAME).getFull()).isNull();
    }

    @Test
    void bandwidth() {
        int bandwidth = 10;
        AbsPolicyApi.updatePolicy(POLICY_NAME, POLICY.bandwidth(bandwidth));

        String SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);

        ASBench.on(SOURCE_NAMESPACE, SET1).duration(5).run();
        int numberOfRecordsToBackup = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsToBackup).isGreaterThan(10_000);

        Instant startTime = Instant.now();
        DtoBackupDetails backupDetails = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        Duration backupDuration = Duration.between(startTime, Instant.now());

        AerospikeLogger.info("Backup took " + backupDuration.get(ChronoUnit.SECONDS) + " seconds");

        assertThat(backupDuration.toSeconds()).isCloseTo(backupDetails.getByteCount() / bandwidth / 1024 / 1024,
                Percentage.withPercentage(20));
    }

    @Test
    void fileLimit2() {
        int fileLimit = 2;
        AbsPolicyApi.updatePolicy(POLICY_NAME, POLICY.fileLimit(2));

        String SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);

        ASBench.on(SOURCE_NAMESPACE, SET1).duration(10).run();
        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        var backupDetails = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        AbsRestoreApi.restoreFullSync(backupDetails.getKey(), ROUTINE_NAME);

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);

        long byteCount = backupDetails.getByteCount();

        AerospikeLogger.info("backup byteCount: " + byteCount);
        AerospikeLogger.info("backup FileCount: " + backupDetails.getFileCount());

        assertThat(backupDetails.getByteCount()).isGreaterThan(2_000_000);
        long estimatedFileCount = byteCount / (fileLimit * 1_048_576);
        AerospikeLogger.info("estimatedFileCount: " + estimatedFileCount);
        assertThat(backupDetails.getFileCount()).isBetween(estimatedFileCount, estimatedFileCount + 5);
    }

    @Test
    void partitionList() {
        ASBench.on(SOURCE_NAMESPACE, SET1).keys(100000).run();
        AerospikeScanner aerospikeScanner = new AerospikeScanner();
        aerospikeScanner.scan(srcClient, SOURCE_NAMESPACE, SET1);

        // List with all the keys
        List<Key> allKeys = aerospikeScanner.getAllKeys();
        int numberOfKeysBefore = allKeys.size();
        Key firstKey = allKeys.get(0);
        int partitionId = Partition.getPartitionId(firstKey.digest);

        List<Key> samePartitionKeys = AerospikeDataUtils.filterKeysByPartition(allKeys, partitionId);

        int numberOfKeysSamePartition = samePartitionKeys.size();
        AerospikeLogger.info("Total number of keys at start: " + numberOfKeysBefore);
        AerospikeLogger.info("Number of keys in the same partition: " + numberOfKeysSamePartition);

        // Use a string filter for the partition range
        String partitionFilter = partitionId + "-1";

        ROUTINE.setPartitionList(partitionFilter);
        AbsRoutineApi.updateRoutine(ROUTINE_NAME, ROUTINE);

        var backupDetails = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        AerospikeLogger.info("Records backed up using partition filter '" + partitionFilter);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        AbsRestoreApi.restoreFullSync(backupDetails.getKey(), ROUTINE_NAME);

        // Validate that only the records from the specified partition are restored
        int restoredRecordCount = AerospikeCountUtils.getNamespaceObjectCount(srcClient, SOURCE_NAMESPACE);
        AerospikeLogger.info("Restored record count: " + restoredRecordCount);
        assertThat(numberOfKeysSamePartition).isEqualTo(restoredRecordCount);
        assertThat(numberOfKeysBefore).isGreaterThan(numberOfKeysSamePartition);
    }

    @Test
    @EnabledIfSystemProperty(named = "qa_environment", matches = "GCP")
    void nodeListAbs() {
        // Step 1: Generate records
        ASBench.on(SOURCE_NAMESPACE, SET1).duration(1).run();
        int numberOfRecordsInAllTheNodes = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);

        List<String> nodeAddressesJavaClient = AerospikeCountUtils.getNodeAddresses(srcClient);
        AerospikeLogger.info("nodeAddressesJavaClient:\n" + String.join("\n", nodeAddressesJavaClient));

        // Step 2: Get node addresses (internal IP:port)
        List<String> nodeAddresses = AerospikeCountUtils.getNodeAddresses(srcClient);
        assertThat(nodeAddresses).as("Cluster has at least 2 nodes").hasSizeGreaterThanOrEqualTo(2);

        String addr1 = nodeAddresses.get(0); // e.g. 10.0.48.18:4333
        AerospikeLogger.info("addr1= " + addr1);
        String addr2 = nodeAddresses.get(1);
        AerospikeLogger.info("addr2= " + addr2);

        // Step 3: Get record count per node using only IP for reliable internal matching
        int countNode1 = AerospikeCountUtils.getObjectCountForNode(srcClient, SOURCE_NAMESPACE, addr1);
        AerospikeLogger.info("countNode1=" + countNode1);
        int countNode2 = AerospikeCountUtils.getObjectCountForNode(srcClient, SOURCE_NAMESPACE, addr2);
        AerospikeLogger.info("countNode2=" + countNode2);

        int expectedRecordCount = countNode1 + countNode2;
        AerospikeLogger.info("expectedRecordCount=" + expectedRecordCount);

        assertThat(countNode1).isGreaterThan(0);
        assertThat(countNode2).isGreaterThan(0);
        assertThat(expectedRecordCount).isLessThan(numberOfRecordsInAllTheNodes);

        // Step 4: Run ABS full backup using node IDs (or IP:port if ABS accepts it here)
        AbsRoutineApi.updateRoutine(ROUTINE_NAME, ROUTINE.nodeList(List.of(addr1, addr2)));
        var backupDetails = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);

        // Step 5: Truncate and restore
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(0);

        AbsRestoreApi.restoreFullSync(backupDetails.getKey(), ROUTINE_NAME);

        // Step 6: Assert restored record count matches expected
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(expectedRecordCount);
    }

    private void createBackupEntities() {
        POLICY = new DtoBackupPolicy()
                .parallel(1)
                .sealed(false);

        STORAGE = new DtoStorage().localStorage(new DtoLocalStorage().path("/etc/aerospike-backup-service/conf.d/BackupWithNewConfigTestPath"));

        ROUTINE = new DtoBackupRoutine()
                .backupPolicy(POLICY_NAME)
                .intervalCron("@yearly")
                .namespaces(List.of("source-ns21"))
                .sourceCluster(CLUSTER_NAME)
                .storage(STORAGE_NAME);
    }
}