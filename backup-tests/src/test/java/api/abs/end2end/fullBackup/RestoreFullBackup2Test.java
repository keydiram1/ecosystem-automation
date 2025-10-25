package api.abs.end2end.fullBackup;

import api.abs.AbsBackupApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import api.abs.JobID;
import api.abs.generated.ApiResponse;
import api.abs.generated.model.DtoBackupDetails;
import api.abs.generated.model.DtoRestoreJobStatus;
import api.abs.generated.model.DtoRestorePolicy;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static api.abs.AbsBackupApi.parseDate;
import static api.abs.generated.model.DtoJobStatus.JobStatusCancelled;
import static api.abs.generated.model.DtoJobStatus.JobStatusRunning;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-E2E")
class RestoreFullBackup2Test extends AbsRunner {
    private static final String STRING_BIN = "RestoreTestBin";

    private static final String ROUTINE_NAME = "fullBackup2";
    private static final String SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        assertThat(AerospikeCountUtils.getNamespaceObjectCount(srcClient, SOURCE_NAMESPACE)).isZero();
    }

    private static Stream<Arguments> deleteFunctions() {
        return Stream.of(
                Arguments.of("delete", (Consumer<Key>) AerospikeDataUtils::delete),
                Arguments.of("update", (Consumer<Key>) key -> AerospikeDataUtils.put(key, STRING_BIN, "new value"))
        );
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("deleteFunctions")
    void restoreFullBackup(String name, Consumer<Key> deleteFunction) {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        final Key key = new Key(SOURCE_NAMESPACE, "restoreFullBackup", "IT1");

        // init data
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        long timeMillis = System.currentTimeMillis();
        String initialValue = "val" + timeMillis;
        AerospikeDataUtils.put(key, STRING_BIN, initialValue);

        // wait till backup
        DtoBackupDetails backupDetails = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        assertThat(backupDetails.getByteCount())
                .isNotNull()
                .isGreaterThan(42);
        AerospikeLogger.info("size " + backupDetails.getByteCount());

        // corrupt data
        deleteFunction.accept(key);

        // run restore
        DtoRestoreJobStatus restoreStatus = AbsRestoreApi.restoreFullSync(backupDetails.getKey(), ROUTINE_NAME);
        assertThat(restoreStatus.getInsertedRecords()).isEqualTo(1);

        // assertion
        Record record = AerospikeDataUtils.get(key);
        assertThat(record)
                .as("record for routine " + ROUTINE_NAME)
                .isNotNull();
        assertThat(record.getString(STRING_BIN))
                .as("value for routine " + ROUTINE_NAME)
                .isEqualTo(initialValue);
    }

    @Test
    void restoreFilterBySetAndBin() {
        String restoreSet = "restoreSet";
        String noRestoreSet = "noRestoreSet";
        String restoreBin = "restoreBin";
        String noRestoreBin = "noRestoreBin";

        final Key backupKey = new Key(SOURCE_NAMESPACE, restoreSet, "IT1");
        final Key noBackupKey = new Key(SOURCE_NAMESPACE, noRestoreSet, "IT1");

        var initialValue = "init value " + UUID.randomUUID();
        List.of(backupKey, noBackupKey).forEach(key -> {
            AerospikeDataUtils.put(key,
                    new Bin(restoreBin, initialValue),
                    new Bin(noRestoreBin, initialValue)
            );
        });

        Record record = AerospikeDataUtils.get(backupKey);

        assertThat(record).isNotNull();

        // wait till backup
        DtoBackupDetails backupDetails = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);

        AerospikeDataUtils.delete(backupKey, noBackupKey);

        // run restore
        DtoRestorePolicy policy = new DtoRestorePolicy()
                .setList(List.of(restoreSet))
                .binList(List.of(restoreBin));
        AbsRestoreApi.restoreFullSync(backupDetails.getKey(), ROUTINE_NAME, policy);

        // assertion
        record = AerospikeDataUtils.get(backupKey);

        assertThat(record).isNotNull();
        assertThat(record.bins)
                .hasSize(1)
                .containsEntry(restoreBin, initialValue);

        Record noRecord = AerospikeDataUtils.get(noBackupKey);
        assertThat(noRecord).isNull();
    }

    @ParameterizedTest(name = "restorePreservesDataSize (sendUserKey = {0})")
    @ValueSource(booleans = {false, true})
    void restorePreservesDataSize(boolean sendUserKey) {
        int recordCount = 1_000;

        String testSet = "testSet";
        ASBench.on(SOURCE_NAMESPACE, testSet)
                .keys(recordCount)
                .threads(1)
                .sendKey(sendUserKey)
                .run();

        int namespaceObjectCountBeforeBackup = AerospikeCountUtils.getNamespaceObjectCount(srcClient, SOURCE_NAMESPACE);
        AerospikeLogger.info("Namespace object count before backup: " + namespaceObjectCountBeforeBackup);
        assertThat(namespaceObjectCountBeforeBackup).isBetween(900, 1200);

        long dataSizeBeforeBackup = AerospikeDataUtils.getDataTotalBytes(SOURCE_NAMESPACE);
        AerospikeLogger.info("Data size in NS " + SOURCE_NAMESPACE + "before backup: " + dataSizeBeforeBackup);

        DtoBackupDetails backup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        long dataSizeAfterTruncate = AerospikeDataUtils.getDataTotalBytes(SOURCE_NAMESPACE);
        AerospikeLogger.info("Data size after truncate: " + dataSizeAfterTruncate);
        assertThat(dataSizeAfterTruncate).isEqualTo(0);

        AbsRestoreApi.restoreFullSync(backup.getKey(), ROUTINE_NAME);

        int namespaceObjectCountAfterRestore = AerospikeCountUtils.getNamespaceObjectCount(srcClient, SOURCE_NAMESPACE);
        assertThat(namespaceObjectCountBeforeBackup).isEqualTo(namespaceObjectCountAfterRestore);

        long dataSizeAfterRestore = AerospikeDataUtils.getDataTotalBytes(SOURCE_NAMESPACE);
        AerospikeLogger.info("Data size after restore: " + dataSizeAfterRestore);

        assertThat(dataSizeAfterRestore)
                .as("Data size after restore should match pre-backup size (sendUserKey = " + sendUserKey + ")")
                .isEqualTo(dataSizeBeforeBackup);
    }

    @Test
    void cancelJobTest() {
        var set = "cancelJobTestSet";
        ASBench.on(SOURCE_NAMESPACE, set).keys(100_000).recordSize(1_000).run();
        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, set, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);
        DtoBackupDetails backupDetails = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        assertThat(backupDetails.getByteCount()).isGreaterThan(100_000);
        String backupKey = backupDetails.getKey();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        DtoRestorePolicy policy = new DtoRestorePolicy().bandwidth(10);

        JobID jobID = AbsRestoreApi.restoreFull(backupKey, ROUTINE_NAME, policy);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    DtoRestoreJobStatus initialRestoreStatus = AbsRestoreApi.getRestoreStatus(jobID);
                    assertThat(initialRestoreStatus.getCurrentJob()).isNotNull();
                    assertThat(initialRestoreStatus.getStatus()).isEqualTo(JobStatusRunning);
                    assertThat(initialRestoreStatus.getInsertedRecords()).isPositive();
                });

        ApiResponse<String> response = AbsRestoreApi.cancelRestore(jobID.value());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_ACCEPTED);

        int setObjectCount = AerospikeCountUtils.getSetObjectCount(srcClient, set, SOURCE_NAMESPACE);
        assertThat(setObjectCount).isLessThan(numberOfRecordsBeforeTruncate);
        AtomicInteger previousCount = new AtomicInteger(setObjectCount);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // First check job status is cancelled
                    assertThat(AbsRestoreApi.getRestoreStatus(jobID).getStatus()).isEqualTo(JobStatusCancelled);

                    // Then compare current count with previous count
                    int currentCount = AerospikeCountUtils.getSetObjectCount(srcClient, set, SOURCE_NAMESPACE);
                    int lastCount = previousCount.get();
                    previousCount.set(currentCount);

                    // If counts differ, fail the assertion
                    assertThat(currentCount).isEqualTo(lastCount);
                });

        int objectCountAfterCancel = AerospikeCountUtils.getSetObjectCount(srcClient, set, SOURCE_NAMESPACE);
        assertThat(objectCountAfterCancel).isLessThan(numberOfRecordsBeforeTruncate);

        response = AbsRestoreApi.cancelRestore(jobID.value());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_ACCEPTED);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    DtoRestoreJobStatus initialRestoreStatus = AbsRestoreApi.getRestoreStatus(jobID);
                    assertThat(initialRestoreStatus.getCurrentJob()).isNotNull();
                    assertThat(initialRestoreStatus.getStatus()).isNotEqualTo(JobStatusRunning);
                });
    }

    @Test
    void twoRestoresInParallel() throws Exception {
        var set = "twoRestoresInParallel";
        // Step 1: Generate many records
        ASBench.on(SOURCE_NAMESPACE, set)
                .keys(500_000)
                .threads(64)
                .batchSize(100)
                .run();

        int countBefore = AerospikeCountUtils.getSetObjectCount(srcClient, set, SOURCE_NAMESPACE);
        assertThat(countBefore).isGreaterThan(300_000);
        Map<String, Map<String, Object>> allRecordsBefore = AerospikeDataUtils.getAllRecords(SOURCE_NAMESPACE, set);

        // Step 2: Backup
        DtoBackupDetails backup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        String backupKey = backup.getKey();

        // Step 3: Truncate DB
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, set, SOURCE_NAMESPACE)).isZero();

        // Step 4: Restore twice in parallel (noGeneration true)
        DtoRestorePolicy overwritePolicy = new DtoRestorePolicy().noGeneration(true);
        CompletableFuture<Void> restore1 = CompletableFuture.runAsync(() ->
                AbsRestoreApi.restoreFullSync(backupKey, ROUTINE_NAME, overwritePolicy));
        CompletableFuture<Void> restore2 = CompletableFuture.runAsync(() ->
                AbsRestoreApi.restoreFullSync(backupKey, ROUTINE_NAME, overwritePolicy));
        restore1.get();
        restore2.get();

        int countAfter = AerospikeCountUtils.getSetObjectCount(srcClient, set, SOURCE_NAMESPACE);
        assertThat(countAfter).isEqualTo(countBefore);

        Map<String, Map<String, Object>> recordsAfterRestore = AerospikeDataUtils.getAllRecords(SOURCE_NAMESPACE, set);
        assertThat(recordsAfterRestore)
                .as("After noGeneration=true restore")
                .isEqualTo(allRecordsBefore);

        // Step 5: Restore twice in parallel (noGeneration false)
        DtoRestorePolicy skipPolicy = new DtoRestorePolicy().noGeneration(false);
        restore1 = CompletableFuture.runAsync(() ->
                AbsRestoreApi.restoreFullSync(backupKey, ROUTINE_NAME, skipPolicy));
        restore2 = CompletableFuture.runAsync(() ->
                AbsRestoreApi.restoreFullSync(backupKey, ROUTINE_NAME, skipPolicy));
        restore1.get();
        restore2.get();

        countAfter = AerospikeCountUtils.getSetObjectCount(srcClient, set, SOURCE_NAMESPACE);
        assertThat(countAfter).isEqualTo(countBefore);

        recordsAfterRestore = AerospikeDataUtils.getAllRecords(SOURCE_NAMESPACE, set);
        assertThat(recordsAfterRestore)
                .as("After noGeneration=false restore")
                .isEqualTo(allRecordsBefore);
    }

    @Test
    void getAllRestoreJobs() {
        var set = "getAllRestoreJobs";
        ASBench.on(SOURCE_NAMESPACE, set).keys(500_000).recordSize(5_000).run();
        String backupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        // Create a job before 'from' to verify time filtering
        JobID oldJob = AbsRestoreApi.restoreFull(backupKey, ROUTINE_NAME, new DtoRestorePolicy().bandwidth(10));
        DtoRestoreJobStatus oldJobStatus = AbsRestoreApi.getRestoreStatus(oldJob);
        long from = parseDate(oldJobStatus.getCurrentJob().getStartTime()) + 1;

        JobID job1 = AbsRestoreApi.restoreFull(backupKey, ROUTINE_NAME, new DtoRestorePolicy().bandwidth(10));
        JobID job2 = AbsRestoreApi.restoreFull(backupKey, ROUTINE_NAME, new DtoRestorePolicy().bandwidth(10));

        // Wait until job2 is running
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
            var status = AbsRestoreApi.getRestoreStatus(job2);
            assertThat(status.getStatus()).isEqualTo(JobStatusRunning);
        });

        // Cancel job2
        ApiResponse<String> cancelResponse = AbsRestoreApi.cancelRestore(job2.value());
        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.SC_ACCEPTED);

        // Wait for job2 to be cancelled
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.SECONDS)
                .untilAsserted(() ->
                assertThat(AbsRestoreApi.getRestoreStatus(job2).getStatus()).isEqualTo(JobStatusCancelled)
        );

        long to = System.currentTimeMillis();

        String oldId = String.valueOf(oldJob.value());
        String id1 = String.valueOf(job1.value());
        String id2 = String.valueOf(job2.value());

        // Filter only jobs started after 'from'
        var all = AbsRestoreApi.getJobs(from, null, null);
        assertThat(all.keySet()).contains(id1, id2);
        assertThat(all.keySet()).doesNotContain(oldId);

        var cancelled = AbsRestoreApi.getJobs(from, null, JobStatusCancelled.toString());
        assertThat(cancelled.keySet()).contains(id2);

        var notRunning = AbsRestoreApi.getJobs(from, null, "!" + JobStatusRunning);
        assertThat(notRunning.keySet()).contains(id2);
        assertThat(notRunning.keySet()).doesNotContain(id1);

        var inRange = AbsRestoreApi.getJobs(from, to, null);
        assertThat(inRange.keySet()).contains(id1, id2);
        assertThat(inRange.keySet()).doesNotContain(oldId);

        DtoRestoreJobStatus status = inRange.get(id1);

        assertThat(status).isNotNull();
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    DtoRestoreJobStatus restoreStatus = AbsRestoreApi.getJobs(from, to, null).get(id1);
                    assertThat(restoreStatus).isNotNull();
                    assertThat(restoreStatus.getCurrentJob().getDoneRecords()).isGreaterThan(10);
                });

        ApiResponse<String> response = AbsRestoreApi.cancelRestore(job1.value());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_ACCEPTED);

        response = AbsRestoreApi.cancelRestore(oldJob.value());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_ACCEPTED);

        Awaitility.await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var restoreStatus = AbsRestoreApi.getRestoreStatus(job1);
                    assertThat(restoreStatus.getStatus()).isNotEqualTo(JobStatusRunning);
                });
        Awaitility.await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
            var restoreStatus = AbsRestoreApi.getRestoreStatus(oldJob);
            assertThat(restoreStatus.getStatus()).isNotEqualTo(JobStatusRunning);
        });
    }
}