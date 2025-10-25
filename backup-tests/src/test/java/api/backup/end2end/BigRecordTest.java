package api.backup.end2end;

import api.backup.BackupManager;
import api.backup.RestoreApi;
import api.backup.RetrieveAPI;
import api.backup.dto.RestoreSetRequest;
import api.backup.dto.RetrieveEntityRecord;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ADR-E2E")
class BigRecordTest extends BackupRunner {
    private static final String SET_NAME = "BigRecordTestSet";
    private static final String SOURCE_NAMESPACE = "source-ns17";
    private static final String SOURCE_CLUSTER_NAME = "BigRecordTestSrcCluster";
    private static final String BACKUP_NAMESPACE = "adr-ns17";
    private static final String BACKUP_NAME = "BigRecordTestContinuousBackup";
    private static final String POLICY_NAME = "BigRecordTestPolicy";
    private static final String DC_NAME = "BigRecordTestDC";
    private static final IAerospikeClient srcClient = AerospikeDataUtils.getSourceClient();
    private static final Key BIG_RECORD_KEY = new Key(SOURCE_NAMESPACE, SET_NAME, "key");

    @AfterAll
    static void afterAll() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    private static long restore(long afterFirstBackup) {
        return RestoreApi.restoreSet(RestoreSetRequest.builder()
                .toTime(afterFirstBackup)
                .srcClusterName(SOURCE_CLUSTER_NAME)
                .trgClusterName(SOURCE_CLUSTER_NAME)
                .srcNS(SOURCE_NAMESPACE)
                .trgNS(SOURCE_NAMESPACE)
                .set(SET_NAME)
                .build()).getProcessed();
    }

    private static void createBigRecord(int number) {
        List<Integer> integers = IntStream.range(0, number).boxed().toList();
        Bin bin = new Bin("list", integers);
        srcClient.put(null, BIG_RECORD_KEY, bin);
        BackupManager.waitForBackup(BACKUP_NAME, BIG_RECORD_KEY, 1);
    }

    @SneakyThrows
    public static void createComplexRecord(int number) {
        // Main map to store all iterations data
        Map<String, Object> mainMap = new HashMap<>();
        for (int i = 0; i < number; i++) {
            // Generate dynamic field values
            String bookId = "12345_" + i;
            String authorFirstName = "Author" + i;
            String authorLastName = "Last" + i;
            double average = 4.5 + (i * 0.1);
            int reviews = 320 + (i * 10);

            // Create a dynamic JSON structure
            Map<String, Object> bookMap = new HashMap<>();
            bookMap.put("id", bookId);
            bookMap.put("title", "Book Title " + i);

            // Nested author object
            Map<String, Object> authorMap = new HashMap<>();
            authorMap.put("firstName", authorFirstName);
            authorMap.put("lastName", authorLastName);

            bookMap.put("author", authorMap);

            bookMap.put("publishedDate", "2022-01-15");
            bookMap.put("genres", Arrays.asList("Adventure", "Fantasy"));
            bookMap.put("price", 19.99 + i); // Increment price
            bookMap.put("availability", "In Stock");

            // Nested ratings object
            Map<String, Object> ratingsMap = new HashMap<>();
            ratingsMap.put("average", average);
            ratingsMap.put("reviews", reviews);

            bookMap.put("ratings", ratingsMap);
            bookMap.put("description", "A thrilling adventure of a young hero in a magical world.");

            // Add to the main map
            mainMap.put("book_" + i, bookMap);
        }

        // Store the main map as a bin in Aerospike
        Bin bin = new Bin("books", mainMap);
        srcClient.put(null, BIG_RECORD_KEY, bin);
    }

    @BeforeEach
    public void setUp() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);
    }

    @Test
    void bigRecords() {
        final int ITERATIONS = 1_000_000;
        createBigRecord(ITERATIONS);
        long afterFirstBackup = System.currentTimeMillis();

        AerospikeDataUtils.truncateSourceSet(SOURCE_NAMESPACE, SET_NAME);

        long restored = restore(afterFirstBackup);

        assertThat(restored).isEqualTo(1);
        Record retrievedRecord = srcClient.get(null, BIG_RECORD_KEY);

        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getList("list")).hasSize(ITERATIONS);
    }

    @Test
    @SuppressWarnings("unchecked")
    void restoreComplexRecord() {
        final int ITERATIONS = 10_000;
        createComplexRecord(ITERATIONS);
        BackupManager.waitForBackup(BACKUP_NAME, BIG_RECORD_KEY, 1);
        long afterFirstBackup = System.currentTimeMillis();
        AerospikeDataUtils.truncateSourceSet(SOURCE_NAMESPACE, SET_NAME);

        long restored = restore(afterFirstBackup);
        assertThat(restored).isEqualTo(1);

        Record retrievedRecord = srcClient.get(null, BIG_RECORD_KEY);
        Map<String, Object> mainMap = (Map<String, Object>) retrievedRecord.getValue("books");
        for (int i = 0; i < ITERATIONS; i++) {
            String bookKey = "book_" + i;
            assertThat(mainMap).containsKey(bookKey);

            Map<String, Object> bookMap = (Map<String, Object>) mainMap.get(bookKey);
            assertThat(bookMap).isNotNull();

            // Verify individual fields in the book map
            String expectedBookId = "12345_" + i;
            String expectedTitle = "Book Title " + i;
            String expectedAuthorFirstName = "Author" + i;
            String expectedAuthorLastName = "Last" + i;
            double expectedAverage = 4.5 + (i * 0.1);
            long expectedReviews = 320 + (i * 10);
            double expectedPrice = 19.99 + i;

            assertThat(bookMap.get("id")).isEqualTo(expectedBookId);
            assertThat(bookMap.get("title")).isEqualTo(expectedTitle);

            Map<String, Object> authorMap = (Map<String, Object>) bookMap.get("author");
            assertThat(authorMap).isNotNull();
            assertThat(authorMap.get("firstName")).isEqualTo(expectedAuthorFirstName);
            assertThat(authorMap.get("lastName")).isEqualTo(expectedAuthorLastName);

            assertThat(bookMap.get("publishedDate")).isEqualTo("2022-01-15");
            assertThat(bookMap.get("genres")).isEqualTo(Arrays.asList("Adventure", "Fantasy"));
            assertThat(bookMap.get("price")).isEqualTo(expectedPrice);
            assertThat(bookMap.get("availability")).isEqualTo("In Stock");

            Map<String, Object> ratingsMap = (Map<String, Object>) bookMap.get("ratings");
            assertThat(ratingsMap).isNotNull();
            assertThat(ratingsMap.get("average")).isEqualTo(expectedAverage);
            assertThat(ratingsMap.get("reviews")).isEqualTo(expectedReviews);

            assertThat(bookMap.get("description")).isEqualTo("A thrilling adventure of a young hero in a magical world.");
        }
    }

    @Test
    void maxNumberOfBins() {
        // Maximum bins per record: 32,767 bins (https://docs.aerospike.com/guide/limitations)
        int max = 32767;
        List<String> bins = IntStream.range(0, max).mapToObj(it -> "bin" + it).toList();

        bins.stream().map(binName -> new Bin(binName, "1"))
                .forEach(bin -> srcClient.put(null, BIG_RECORD_KEY, bin));

        Record record = srcClient.get(null, BIG_RECORD_KEY);
        assertThat(record.bins).hasSize(max);

        Awaitility.await()
                .pollInterval(5, TimeUnit.SECONDS)
                .until(() ->
                        BackupManager.backupForKeyExist(BACKUP_NAME, 0, BIG_RECORD_KEY)
                );

        String digest = AerospikeDataUtils.getDigestFromKey(BIG_RECORD_KEY);
        List<RetrieveEntityRecord> retrieve = RetrieveAPI.retrieve(
                RetrieveAPI.WhatToRetrieve.ALL, System.currentTimeMillis(),
                digest, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, SET_NAME, false);

        assertThat(retrieve).hasSizeGreaterThanOrEqualTo(1);
        long totalBins = retrieve.stream()
                .flatMap(it -> it.getBins() == null ? Stream.of() : it.getBins().keySet().stream())
                .distinct()
                .count();
        assertThat(totalBins).isEqualTo(max);
    }
}
