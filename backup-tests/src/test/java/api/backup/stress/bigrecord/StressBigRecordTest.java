package api.backup.stress.bigrecord;

import api.backup.BackupManager;
import api.backup.RestoreApi;
import api.backup.dto.RestoreSetRequest;
import api.backup.stress.StressRunner;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.ConfigParametersHandler;
import utils.aerospike.adr.AerospikeDataUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

//@Tag("ADR-STRESS-TEST")
class StressBigRecordTest extends StressRunner {
    private static final String SET_NAME = "StressBigRecordTestSet";
    private static final String SOURCE_NAMESPACE = "source-ns17";
    private static final String SOURCE_CLUSTER_NAME = "StressBigRecordTestSrcCluster";
    private static final String BACKUP_NAMESPACE = "adr-ns17";
    private static final String BACKUP_NAME = "StressBigRecordTestContinuousBackup";
    private static final String POLICY_NAME = "StressBigRecordTestPolicy";
    private static final String DC_NAME = "StressBigRecordTestDC";
    private static final IAerospikeClient srcClient = AerospikeDataUtils.getSourceClient();
    private static final Key BIG_RECORD_KEY = new Key(SOURCE_NAMESPACE, SET_NAME, "list");
    private static int testLoopCount;

    @BeforeEach
    public void setUp() {
        setPerformanceVariables();
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);
        waitGroup.wait(minutesToWaitForAllStressClassesToFinishSetup); // wait for all tests to finish setup
    }

    @Test
    @SuppressWarnings("unchecked")
    void restoreComplexRecord() {
        for (int i = 0; i < testLoopCount; i++) {
            final int ITERATIONS = 10_000;
            createComplexRecord(ITERATIONS);
            BackupManager.waitForBackup(BACKUP_NAME, BIG_RECORD_KEY, 1);
            long afterFirstBackup = System.currentTimeMillis();
            AerospikeDataUtils.truncateSourceSet(SOURCE_NAMESPACE, SET_NAME);

            long restored = restore(afterFirstBackup);
            assertThat(restored).isEqualTo(1);

            Record retrievedRecord = srcClient.get(null, BIG_RECORD_KEY);
            Map<String, Object> mainMap = (Map<String, Object>) retrievedRecord.getValue("books");
            for (int j = 0; j < ITERATIONS; j++) {
                String bookKey = "book_" + j;
                assertThat(mainMap).containsKey(bookKey);

                Map<String, Object> bookMap = (Map<String, Object>) mainMap.get(bookKey);
                assertThat(bookMap).isNotNull();

                // Verify individual fields in the book map
                String expectedBookId = "12345_" + j;
                String expectedTitle = "Book Title " + j;
                String expectedAuthorFirstName = "Author" + j;
                String expectedAuthorLastName = "Last" + j;
                double expectedAverage = 4.5 + (j * 0.1);
                long expectedReviews = 320 + (j * 10);
                double expectedPrice = 19.99 + j;

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

                BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
                BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);
            }
        }
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

    @SneakyThrows
    public static void createComplexRecord(int number) {
        // Main map to store all iterations' data
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


    private void setPerformanceVariables() {
        testLoopCount = 3;
        if (ConfigParametersHandler.getParameter("asbench_duration_seconds") != null) {
            if (ConfigParametersHandler.getParameter("asbench_duration_seconds").equals("200")) {
                testLoopCount = 20;
                minutesToWaitForAllStressClassesToFinishSetup = 30;
            }
        }
    }
}
