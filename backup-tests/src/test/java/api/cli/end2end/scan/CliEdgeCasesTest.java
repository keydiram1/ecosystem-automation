package api.cli.end2end.scan;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import com.aerospike.client.Record;
import com.aerospike.client.*;
import com.aerospike.client.operation.HLLOperation;
import com.aerospike.client.operation.HLLPolicy;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AutoUtils;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("CLI-BACKUP")
class CliEdgeCasesTest extends CliBackupRunner {
    private static final String STRING_BIN = "RestoreTestBin";
    private static final String SET = "SetEdgeCases";
    private static Key KEY;
    private static String SOURCE_NAMESPACE = "source-ns13";

    @BeforeAll
    static void setUp() {
        if(AutoUtils.isRunningOnGCP())
            SOURCE_NAMESPACE = "source-ns7";
        KEY = new Key(SOURCE_NAMESPACE, SET, "IT1");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void restoreAllDataTypes() {
        String location = "{ \"type\": \"Point\", \"coordinates\": [%s, %s] }".formatted(34.85, 32.10);
        Map<String, Object> scalarValues = putAllDataTypesToTheSourceCluster(location);
        Value.HLLValue hllValue = srcClient.get(null, KEY).getHLLValue("hllBin");
        BackupResult fastRestoreBackup = CliBackup.on(SOURCE_NAMESPACE, "RestoreEdgeCasesTestBackup").run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, fastRestoreBackup.getBackupDir()).run();

        Record record = srcClient.get(null, KEY);

        assertThat(record).isNotNull();
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
        assertThat(record.getGeoJSONString("geoJsonBin")).isEqualTo(location);
        assertThat(record.getHLLValue("hllBin")).isEqualTo(hllValue);
    }

    @Test
    void restoreNamespaceWithLotsOfSets() {
        int numberOfSets = 20;
        int numberOfKeysPerSet = 3;

        String initialValue = "RestoreTestInitialValue";
        List<Key> keys = putLotsOfSetsToTheSourceCluster(numberOfSets, numberOfKeysPerSet, initialValue);

        AutoUtils.sleep(2000);

        BackupResult fastRestoreBackup = CliBackup.on(SOURCE_NAMESPACE, "RestoreEdgeCasesTestBackup").run();
        assertThat(fastRestoreBackup.getRecordsRead()).isEqualTo(numberOfSets * numberOfKeysPerSet);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, fastRestoreBackup.getBackupDir()).run();

        int namespaceObjectCount = AerospikeCountUtils.getNamespaceObjectCount(srcClient, SOURCE_NAMESPACE);
        assertThat(namespaceObjectCount).isEqualTo(numberOfSets * numberOfKeysPerSet);
        // Assert that the value of all the records is at it was before the truncate
        for (Key key : keys) {
            Record record = srcClient.get(null, key);
            String value = record.getString(STRING_BIN);
            assertThat(value).isEqualTo(initialValue);
        }
    }

    @Test
    void restoreBigRecord() {
        final int ITERATIONS = 1_000_000;
        createBigRecord(ITERATIONS, KEY);
        BackupResult fastRestoreBackup = CliBackup.on(SOURCE_NAMESPACE, "RestoreEdgeCasesTestBackup").run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, fastRestoreBackup.getBackupDir()).run();

        Record retrievedRecord = srcClient.get(null, KEY);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getList("list")).hasSize(ITERATIONS);
    }

    @Test
    @SuppressWarnings("unchecked")
    void restoreBigComplexRecord() {
        final int ITERATIONS = 10_000;
        createComplexRecord(ITERATIONS, KEY);
        BackupResult fastRestoreBackup = CliBackup.on(SOURCE_NAMESPACE, "RestoreEdgeCasesTestBackup").run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        CliRestore.on(SOURCE_NAMESPACE, fastRestoreBackup.getBackupDir()).run();

        Record retrievedRecord = srcClient.get(null, KEY);
        assertThat(retrievedRecord).isNotNull();
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

            assertThat(bookMap)
                    .containsEntry("id", expectedBookId)
                    .containsEntry("title", expectedTitle)
                    .containsEntry("publishedDate", "2022-01-15")
                    .containsEntry("genres", Arrays.asList("Adventure", "Fantasy"))
                    .containsEntry("price", expectedPrice)
                    .containsEntry("availability", "In Stock")
                    .containsEntry("description", "A thrilling adventure of a young hero in a magical world.");

            Map<String, Object> authorMap = (Map<String, Object>) bookMap.get("author");
            assertThat(authorMap)
                    .isNotNull()
                    .containsEntry("firstName", expectedAuthorFirstName)
                    .containsEntry("lastName", expectedAuthorLastName);

            Map<String, Object> ratingsMap = (Map<String, Object>) bookMap.get("ratings");
            assertThat(ratingsMap)
                    .isNotNull()
                    .containsEntry("average", expectedAverage)
                    .containsEntry("reviews", expectedReviews);
        }
    }

    @SneakyThrows
    private void createComplexRecord(int number, Key key) {
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
        srcClient.put(null, key, bin);
    }

    private void createBigRecord(int number, Key key) {
        List<Integer> integers = IntStream.range(0, number).boxed().toList();
        Bin bin = new Bin("list", integers);
        srcClient.put(null, key, bin);
    }

    private List<Key> putLotsOfSetsToTheSourceCluster(int numberOfSet, int numberOfKeysPerSet, String binValue) {
        List<Key> keys = IntStream.range(0, numberOfSet)
                .mapToObj(i -> String.format("%s%02d", SET, i))
                .flatMap(set -> generateKeys(set, numberOfKeysPerSet))
                .toList();

        keys.forEach(key -> AerospikeDataUtils.put(key, STRING_BIN, binValue));

        return keys;
    }

    private Stream<Key> generateKeys(String set, int numberOfKeysPerSet) {
        return IntStream.range(0, numberOfKeysPerSet)
                .mapToObj(i -> new Key(SOURCE_NAMESPACE, set, String.format("%s%02d", "K", i)));
    }

    private Map<String, Object> putAllDataTypesToTheSourceCluster(String location) {
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

        operations.add(Operation.add(Bin.asGeoJSON("geoJsonBin", location)));
        operations.add(HLLOperation.add(HLLPolicy.Default, "hllBin", List.of(new Value.IntegerValue(100)), 16));

        srcClient.operate(null, KEY, operations.toArray(new Operation[0]));
        return scalarValues;
    }
}