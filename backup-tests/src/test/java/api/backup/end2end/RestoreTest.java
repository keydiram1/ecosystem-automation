package api.backup.end2end;

import api.backup.BackupManager;
import api.backup.ClusterConnectionApi;
import api.backup.RestoreApi;
import api.backup.dto.BackgroundJob;
import api.backup.dto.BackgroundJobPolicy;
import api.backup.dto.ClusterConnection;
import api.backup.dto.RestoreSetRequest;
import com.aerospike.client.Record;
import com.aerospike.client.*;
import com.aerospike.client.operation.HLLOperation;
import com.aerospike.client.operation.HLLPolicy;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ADR-E2E")
class RestoreTest extends BackupRunner {
    private static final String SET_NAME = "RestoreTestSet";
    private static final String SET_NAME2 = "RestoreTestSet2";
    private static final String SOURCE_NAMESPACE = "source-ns1";
    private static final String SOURCE_CLUSTER_NAME = "RestoreTestSrcCluster";
    private static final String BACKUP_NAMESPACE = "adr-ns1";
    private static final String INITIAL_VALUE = "RestoreTestInitialValue";
    private static final String UPDATED_VALUE = "RestoreTestUpdatedValue";
    private static final String INITIAL_VALUE_SAME_SET = "RestoreTestInitialValueSameSet";
    private static final String UPDATED_VALUE_SAME_SET = "RestoreTestUpdatedValueSameSet";
    private static final String INITIAL_VALUE_DIFFERENT_SET = "RestoreTestInitialValueDifferentSet";
    private static final String UPDATED_VALUE_DIFFERENT_SET = "RestoreTestUpdatedValueDifferentSet";
    private static final String BACKUP_NAME = "RestoreTestContinuousBackup";
    private static final String POLICY_NAME = "RestoreTestPolicy";
    private static final String DC_NAME = "RestoreDC";
    private static final Key RESTORE_TEST_KEY = new Key(SOURCE_NAMESPACE, SET_NAME, "IT1");
    private static final String RESTORE_TEST_DIGEST = AerospikeDataUtils.getDigestFromKey(RESTORE_TEST_KEY);
    private static final Key RESTORE_TEST_KEY_SAME_SET = new Key(SOURCE_NAMESPACE, SET_NAME, "IT2");
    private static final Key RESTORE_TEST_KEY_DIFFERENT_SET = new Key(SOURCE_NAMESPACE, SET_NAME2, "IT3");
    private static final IAerospikeClient srcClient = AerospikeDataUtils.getSourceClient();
    private static final IAerospikeClient backupClient = AerospikeDataUtils.getBackupClient();
    private static long beforeFirstBackup;
    private static long afterFirstBackup;
    private static long afterSecondBackup;
    private final String STRING_BIN = "stringBin";

    @AfterAll
    public static void afterAll() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @BeforeEach
    public void setUp() {
        // In restore related tests we have to wait before truncate and validate because the same records will
        // be restored to the source cluster and backed up to ADR again.
        AutoUtils.sleep(5000);
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);
        for (ClusterConnection connection : ClusterConnectionApi.getAllClusterConnections())
            AerospikeLogger.info("Cluster connection name: " + connection.getSrcClusterName());

        prepareData();
    }

    @Test
    void restoreRecords() {
        // RESTORE_TEST_DIGEST in upper case just to test that restore works with upper case digest as well
        int restored = RestoreApi.restoreRecord(afterFirstBackup, RESTORE_TEST_DIGEST.toUpperCase(), SET_NAME, SOURCE_CLUSTER_NAME, SOURCE_CLUSTER_NAME,
                SOURCE_NAMESPACE, SOURCE_NAMESPACE);

        assertThat(restored).isEqualTo(1);

        Record record = srcClient.get(null, RESTORE_TEST_KEY);
        String value = record.getString(STRING_BIN);
        assertThat(value).isEqualTo(INITIAL_VALUE);

        record = srcClient.get(null, RESTORE_TEST_KEY_SAME_SET);
        value = record.getString(STRING_BIN);
        assertThat(value).isEqualTo(UPDATED_VALUE_SAME_SET);

        record = srcClient.get(null, RESTORE_TEST_KEY_DIFFERENT_SET);
        value = record.getString(STRING_BIN);
        AerospikeLogger.info("The value in the client is: " + value);
        assertThat(value).isEqualTo(UPDATED_VALUE_DIFFERENT_SET);

        restored = RestoreApi.restoreRecord(afterSecondBackup, RESTORE_TEST_DIGEST, SET_NAME, SOURCE_CLUSTER_NAME, SOURCE_CLUSTER_NAME,
                SOURCE_NAMESPACE, SOURCE_NAMESPACE);

        assertThat(restored).isEqualTo(1);

        record = srcClient.get(null, RESTORE_TEST_KEY);
        value = record.getString(STRING_BIN);
        assertThat(value).isEqualTo(UPDATED_VALUE);

        record = srcClient.get(null, RESTORE_TEST_KEY_SAME_SET);
        value = record.getString(STRING_BIN);
        assertThat(value).isEqualTo(UPDATED_VALUE_SAME_SET);

        record = srcClient.get(null, RESTORE_TEST_KEY_DIFFERENT_SET);
        value = record.getString(STRING_BIN);
        AerospikeLogger.info("The value in the client is: " + value);
        assertThat(value).isEqualTo(UPDATED_VALUE_DIFFERENT_SET);
    }

    @Test
    void restoreSet() {
        long restored = RestoreApi.restoreSet(RestoreSetRequest.builder()
                .fromTime(beforeFirstBackup)
                .toTime(afterFirstBackup)
                .srcClusterName(SOURCE_CLUSTER_NAME)
                .trgClusterName(SOURCE_CLUSTER_NAME)
                .srcNS(SOURCE_NAMESPACE)
                .trgNS(SOURCE_NAMESPACE)
                .set(SET_NAME)
                .build()).getProcessed();

        assertThat(restored).isEqualTo(2);

        Record record = srcClient.get(null, RESTORE_TEST_KEY);
        String value = record.getString(STRING_BIN);
        assertThat(value).isEqualTo(INITIAL_VALUE);

        record = srcClient.get(null, RESTORE_TEST_KEY_SAME_SET);
        value = record.getString(STRING_BIN);
        assertThat(value).isEqualTo(INITIAL_VALUE_SAME_SET);

        record = srcClient.get(null, RESTORE_TEST_KEY_DIFFERENT_SET);
        value = record.getString(STRING_BIN);
        AerospikeLogger.info("The value in the client is: " + value);
        assertThat(value).isEqualTo(UPDATED_VALUE_DIFFERENT_SET);

        restored = RestoreApi.restoreSet(RestoreSetRequest.builder()
                .fromTime(afterFirstBackup)
                .toTime(afterSecondBackup)
                .srcClusterName(SOURCE_CLUSTER_NAME)
                .trgClusterName(SOURCE_CLUSTER_NAME)
                .srcNS(SOURCE_NAMESPACE)
                .trgNS(SOURCE_NAMESPACE)
                .set(SET_NAME)
                .build()).getProcessed();

        assertThat(restored).isEqualTo(2);

        record = srcClient.get(null, RESTORE_TEST_KEY);
        value = record.getString(STRING_BIN);
        assertThat(value).isEqualTo(UPDATED_VALUE);

        record = srcClient.get(null, RESTORE_TEST_KEY_SAME_SET);
        value = record.getString(STRING_BIN);
        assertThat(value).isEqualTo(UPDATED_VALUE_SAME_SET);

        record = srcClient.get(null, RESTORE_TEST_KEY_DIFFERENT_SET);
        value = record.getString(STRING_BIN);
        AerospikeLogger.info("The value in the client is: " + value);
        assertThat(value).isEqualTo(UPDATED_VALUE_DIFFERENT_SET);
    }

    @Test
    void restoreNamespace() {
        long restored = RestoreApi.restoreNamespace(beforeFirstBackup, afterFirstBackup, SOURCE_CLUSTER_NAME, SOURCE_CLUSTER_NAME,
                SOURCE_NAMESPACE, SOURCE_NAMESPACE).getProcessed();

        assertThat(restored).isEqualTo(3);

        Record record = srcClient.get(null, RESTORE_TEST_KEY);
        String value = record.getString(STRING_BIN);
        assertThat(value).isEqualTo(INITIAL_VALUE);

        record = srcClient.get(null, RESTORE_TEST_KEY_SAME_SET);
        value = record.getString(STRING_BIN);
        assertThat(value).isEqualTo(INITIAL_VALUE_SAME_SET);

        record = srcClient.get(null, RESTORE_TEST_KEY_DIFFERENT_SET);
        value = record.getString(STRING_BIN);
        AerospikeLogger.info("The value in the client is: " + value);
        assertThat(value).isEqualTo(INITIAL_VALUE_DIFFERENT_SET);

        restored = RestoreApi.restoreNamespace(afterFirstBackup, afterSecondBackup, SOURCE_CLUSTER_NAME, SOURCE_CLUSTER_NAME,
                SOURCE_NAMESPACE, SOURCE_NAMESPACE).getProcessed();

        assertThat(restored).isEqualTo(3);

        record = srcClient.get(null, RESTORE_TEST_KEY);
        value = record.getString(STRING_BIN);
        assertThat(value).isEqualTo(UPDATED_VALUE);

        record = srcClient.get(null, RESTORE_TEST_KEY_SAME_SET);
        value = record.getString(STRING_BIN);
        assertThat(value).isEqualTo(UPDATED_VALUE_SAME_SET);

        record = srcClient.get(null, RESTORE_TEST_KEY_DIFFERENT_SET);
        value = record.getString(STRING_BIN);
        AerospikeLogger.info("The value in the client is: " + value);
        assertThat(value).isEqualTo(UPDATED_VALUE_DIFFERENT_SET);
    }

    @Test
    void testAllTypes() {
        Map<String, Object> scalarValues = Map.of(
                "intBin", Integer.MAX_VALUE,
                "longBin", Long.MAX_VALUE,
                "doubleBin", 3.14,
                "shortBin", Short.MAX_VALUE,
                "booleanBin", true,
                "byteBin", Byte.MAX_VALUE,
                "floatBin", 2.71f,
                "blobBin", "blob".getBytes(),
                "listBin", List.of("a", "b", "c"),
                "mapBin", Map.of("a", true, "b", false)
        );

        List<Operation> operations = scalarValues.entrySet().stream()
                .map(e -> new Bin(e.getKey(), Value.get(e.getValue())))
                .map(Operation::add)
                .collect(Collectors.toCollection(ArrayList::new));
        String loc = "{ \"type\": \"Point\", \"coordinates\": [%s, %s] }".formatted(34.85, 32.10);
        operations.add(Operation.add(Bin.asGeoJSON("geoJsonBin", loc)));
        operations.add(HLLOperation.add(HLLPolicy.Default, "hllBin", List.of(new Value.IntegerValue(100)), 16));

        srcClient.operate(null, RESTORE_TEST_KEY, operations.toArray(new Operation[0]));
        Value.HLLValue hllValue = srcClient.get(null, RESTORE_TEST_KEY).getHLLValue("hllBin");

        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY, 3);
        long time = System.currentTimeMillis();

        srcClient.delete(null, RESTORE_TEST_KEY);

        RestoreApi.restoreNamespace(time, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE);
        Record record = srcClient.get(null, RESTORE_TEST_KEY);

        assertThat(record.getInt("intBin")).isEqualTo(scalarValues.get("intBin"));
        assertThat(record.getLong("longBin")).isEqualTo(scalarValues.get("longBin"));
        assertThat(record.getBoolean("booleanBin")).isEqualTo(scalarValues.get("booleanBin"));
        assertThat(record.getShort("shortBin")).isEqualTo(scalarValues.get("shortBin"));
        assertThat(record.getDouble("doubleBin")).isEqualTo(scalarValues.get("doubleBin"));
        assertThat(record.getByte("byteBin")).isEqualTo(scalarValues.get("byteBin"));
        assertThat(record.getFloat("floatBin")).isEqualTo(scalarValues.get("floatBin"));
        assertThat(record.getValue("blobBin")).isEqualTo(scalarValues.get("blobBin"));
        assertThat(record.getList("listBin")).isEqualTo(scalarValues.get("listBin"));
        assertThat(record.getMap("mapBin")).isEqualTo(scalarValues.get("mapBin"));
        assertThat(record.getGeoJSONString("geoJsonBin")).isEqualTo(loc);
        assertThat(record.getHLLValue("hllBin")).isEqualTo(hllValue);
    }

    @Test
    void restoreSetOfManyRecords() {
        int N = 1000;

        fillNamespace(N, SET_NAME);

        long numRecordsInSourceAfterAddingData = AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE);
        AerospikeLogger.info("Number of records after generation: " + numRecordsInSourceAfterAddingData);

        Awaitility.await("Backup of " + numRecordsInSourceAfterAddingData + " keys")
                .pollInterval(5, TimeUnit.SECONDS)
                .atMost(2, TimeUnit.MINUTES)
                .until(() -> {
                    long actual = AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME, BACKUP_NAMESPACE);
                    return actual == numRecordsInSourceAfterAddingData;
                });

        long backupDoneTime = System.currentTimeMillis();

        clearNamespace();

        AerospikeLogger.info("Restoring...");
        BackgroundJob restored = RestoreApi.restoreSet(RestoreSetRequest.builder()
                .fromTime(0L)
                .toTime(backupDoneTime)
                .srcClusterName(SOURCE_CLUSTER_NAME)
                .trgClusterName(SOURCE_CLUSTER_NAME)
                .srcNS(SOURCE_NAMESPACE)
                .trgNS(SOURCE_NAMESPACE)
                .set(SET_NAME)
                .backgroundJobPolicy(new BackgroundJobPolicy(100, null, null))
                .build());

        assertThat(restored.getProcessed()).isEqualTo(N + 2);

        AerospikeLogger.info("Restore took " + Duration.ofMillis(restored.getUpdated() - restored.getCreated()));
        AerospikeLogger.info("Start assertions");
        for (int i = 0; i < N; i++) {
            Key key = new Key(SOURCE_NAMESPACE, SET_NAME, i);
            Record data = srcClient.get(null, key);
            assertThat(data.getInt(STRING_BIN)).isEqualTo(i);
        }
    }

    @Test
    void parallelJobs() {
        int N = 1000;
        int jobs = 10;

        List<String> sets = IntStream.range(0, jobs).mapToObj(i -> SET_NAME + "_" + i).toList();
        sets.stream().parallel().forEach(set -> {
            fillNamespace(N, set);
        });

        Awaitility.await("Backup of " + N * jobs + " keys")
                .pollInterval(5, TimeUnit.SECONDS)
                .atMost(2, TimeUnit.MINUTES)
                .until(() -> {
                    long actual = sets.stream().parallel()
                            .mapToInt(set -> AerospikeCountUtils.getSetObjectCount(backupClient, set, BACKUP_NAMESPACE))
                            .sum();
                    return actual == N * jobs;
                });
        long backupDone = System.currentTimeMillis();
        AutoUtils.sleep(1_000);

        clearNamespace();

        var backgroundJobsFutures = sets.stream().parallel().map(set -> {
                    return CompletableFuture.supplyAsync(() ->RestoreApi.restoreSet(RestoreSetRequest.builder()
                            .fromTime(0L)
                            .toTime(backupDone)
                            .srcClusterName(SOURCE_CLUSTER_NAME)
                            .trgClusterName(SOURCE_CLUSTER_NAME)
                            .srcNS(SOURCE_NAMESPACE)
                            .trgNS(SOURCE_NAMESPACE)
                            .set(set)
                            .backgroundJobPolicy(new BackgroundJobPolicy(100, null, null))
                            .build()));
                }
        ).toList();

        Awaitility.await()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(2, TimeUnit.MINUTES)
                        .until(() -> backgroundJobsFutures.stream().allMatch(CompletableFuture::isDone));

        var backgroundJobs = backgroundJobsFutures.stream().map(CompletableFuture::join).toList();

        assertThat(backgroundJobs)
                .hasSize(jobs)
                .allMatch(it -> it.getProcessed() == N &&
                        it.getStatus() == BackgroundJob.BackgroundJobStatus.DONE);
        int namespaceObjectCount = AerospikeCountUtils.getNamespaceObjectCount(srcClient, SOURCE_NAMESPACE);
        assertThat(namespaceObjectCount).isGreaterThanOrEqualTo(N * jobs);
    }

    @Test
    void noDataToRestore() {
        int restoredKey = RestoreApi.restoreRecord(beforeFirstBackup, RESTORE_TEST_DIGEST, SET_NAME, SOURCE_CLUSTER_NAME, SOURCE_CLUSTER_NAME,
                SOURCE_NAMESPACE, SOURCE_NAMESPACE);

        assertThat(restoredKey).isZero();
        long restoredSet = RestoreApi.restoreNamespace(0, beforeFirstBackup, SOURCE_CLUSTER_NAME, SOURCE_CLUSTER_NAME,
                SOURCE_NAMESPACE, SOURCE_NAMESPACE).getProcessed();

        assertThat(restoredSet).isZero();
    }

    @Test
    void restoreNamespaceAfterTruncate() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        Record result1 = srcClient.get(null, RESTORE_TEST_KEY);
        assertThat(result1).isNull();

        RestoreApi.restoreNamespace(afterFirstBackup, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE);

        Record result2 = srcClient.get(null, RESTORE_TEST_KEY);
        assertThat(result2).isNotNull();
        String value = result2.getString(STRING_BIN);
        assertThat(value).isEqualTo(INITIAL_VALUE);
    }

    private void fillNamespace(int N, String setName) {
        for (int i = 0; i < N; i++) {
            Key key = new Key(SOURCE_NAMESPACE, setName, i);
            Bin bin = new Bin(STRING_BIN, i);
            srcClient.put(null, key, bin);
        }
    }

    private void clearNamespace() {
        AerospikeDataUtils.truncateSourceSet(SOURCE_NAMESPACE, SET_NAME);
        Awaitility.waitAtMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(5)).until(() ->
                        AerospikeCountUtils.isSetEmpty(srcClient, SOURCE_NAMESPACE, SET_NAME));
    }

    private void prepareData() {
        beforeFirstBackup = System.currentTimeMillis();
        AerospikeDataUtils.put(RESTORE_TEST_KEY, STRING_BIN, INITIAL_VALUE);
        AerospikeDataUtils.put(RESTORE_TEST_KEY_SAME_SET, STRING_BIN, INITIAL_VALUE_SAME_SET);
        AerospikeDataUtils.put(RESTORE_TEST_KEY_DIFFERENT_SET, STRING_BIN, INITIAL_VALUE_DIFFERENT_SET);

        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY, 1);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY_SAME_SET, 1);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY_DIFFERENT_SET, 1);

        afterFirstBackup = System.currentTimeMillis();

        AerospikeDataUtils.put(RESTORE_TEST_KEY, STRING_BIN, UPDATED_VALUE);
        AerospikeDataUtils.put(RESTORE_TEST_KEY_SAME_SET, STRING_BIN, UPDATED_VALUE_SAME_SET);
        AerospikeDataUtils.put(RESTORE_TEST_KEY_DIFFERENT_SET, STRING_BIN, UPDATED_VALUE_DIFFERENT_SET);

        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY, 2);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY_SAME_SET, 2);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY_DIFFERENT_SET, 2);

        afterSecondBackup = System.currentTimeMillis();
    }
}
