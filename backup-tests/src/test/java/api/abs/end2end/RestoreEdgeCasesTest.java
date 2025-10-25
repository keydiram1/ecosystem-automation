package api.abs.end2end;

import api.abs.AbsBackupApi;
import api.abs.AbsPolicyApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import api.abs.generated.model.DtoBackupDetails;
import api.abs.generated.model.DtoBackupPolicy;
import api.abs.generated.model.DtoRestoreJobStatus;
import com.aerospike.client.Record;
import com.aerospike.client.*;
import com.aerospike.client.operation.HLLOperation;
import com.aerospike.client.operation.HLLPolicy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AutoUtils;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static utils.aerospike.abs.AerospikeDataUtils.createComplexRecord;

@Tag("ABS-E2E")
class RestoreEdgeCasesTest extends AbsRunner {
    private static final String STRING_BIN = "RestoreTestBin";
    private static final String SET = "SetRestoreEdgeCasesTest";
    private static final String ROUTINE_NAME = "edgeCases";
    private static Key KEY;
    private static String SOURCE_NAMESPACE;

    @BeforeAll
    static void setUp() {
        SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);
        KEY = new Key(SOURCE_NAMESPACE, SET, "IT1");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
    }

    @Test
    void restoreAllDataTypes() {
        String location = "{ \"type\": \"Point\", \"coordinates\": [%s, %s] }".formatted(34.85, 32.10);
        Map<String, Object> scalarValues = putAllDataTypesToTheSourceCluster(location);
        Value.HLLValue hllValue = AerospikeDataUtils.get(KEY).getHLLValue("hllBin");
        String firstBackupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        AbsRestoreApi.restoreFullSync(firstBackupKey, ROUTINE_NAME);

        Record record = AerospikeDataUtils.get(KEY);

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
        assertThat(record.getDouble("smallDouble1")).isEqualTo(scalarValues.get("smallDouble1"));
        assertThat(record.getDouble("smallDouble2")).isEqualTo(scalarValues.get("smallDouble2"));
        assertThat(record.getDouble("smallFloat1")).isEqualTo(scalarValues.get("smallFloat1"));
        assertThat(record.getDouble("smallFloat2")).isEqualTo(scalarValues.get("smallFloat2"));
        assertThat(record.getDouble("smallDouble3")).isEqualTo(scalarValues.get("smallDouble3"));
        assertThat(record.getDouble("smallDouble4")).isEqualTo(scalarValues.get("smallDouble4"));
        assertThat(record.getDouble("smallFloat3")).isEqualTo(scalarValues.get("smallFloat3"));
        assertThat(record.getDouble("smallDouble5")).isEqualTo(scalarValues.get("smallDouble5"));
        assertThat(record.getDouble("smallFloat4")).isEqualTo(scalarValues.get("smallFloat4"));
        assertThat(record.getDouble("smallFloat5")).isEqualTo(scalarValues.get("smallFloat5"));
        assertThat(record.getFloat("bigFloat1")).isEqualTo(scalarValues.get("bigFloat1"));
        assertThat(record.getFloat("bigFloat2")).isEqualTo(scalarValues.get("bigFloat2"));
        assertThat(record.getDouble("bigDouble1")).isEqualTo(scalarValues.get("bigDouble1"));
        assertThat(record.getDouble("bigDouble2")).isEqualTo(scalarValues.get("bigDouble2"));
    }

    @Test
    void restoreNamespaceWithLotsOfSets() {
        //Version 3.1
        //# namespace source-ns7
        int headerSize = 35;

        //# first-file
        int firstFile = 13;

        int numberOfSets = 20;
        int numberOfKeysPerSet = 3;
        int expectedElementSize = 150; //every record is expected to be 150 bytes in the backup
        int expectedDataSize = expectedElementSize * numberOfSets * numberOfKeysPerSet;

        String initialValue = "RestoreTestInitialValue";
        List<Key> keys = putLotsOfSetsToTheSourceCluster(numberOfSets, numberOfKeysPerSet, initialValue);

        DtoBackupPolicy policy = AbsPolicyApi.getPolicy(AbsRoutineApi.getRoutine(ROUTINE_NAME).getBackupPolicy());
        DtoBackupDetails backupDetails = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        assertThat(backupDetails.getFileCount().intValue()).isEqualTo(policy.getParallel());
        assertThat(backupDetails.getRecordCount()).isEqualTo(numberOfSets * numberOfKeysPerSet);


        int expectedTotalSize = expectedDataSize + headerSize * policy.getParallel() + firstFile;

        // We assert the backup size only in local installation since in AWS we use the same installation for many tests
        if (!AutoUtils.isRunningOnGCP())
            assertThat(backupDetails.getByteCount()).as("backup size").isEqualTo(expectedTotalSize);
        String firstBackupKey = backupDetails.getKey();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        DtoRestoreJobStatus restored = AbsRestoreApi.restoreFullSync(firstBackupKey, ROUTINE_NAME);
        // We assert the backup size only in local installation since in AWS we use the same installation for many tests
        if (!AutoUtils.isRunningOnGCP())
            assertThat(restored.getTotalBytes()).as("restore size").isEqualTo(expectedDataSize);

        int namespaceObjectCount = AerospikeCountUtils.getNamespaceObjectCount(srcClient, SOURCE_NAMESPACE);
        assertThat(namespaceObjectCount).isEqualTo(numberOfSets * numberOfKeysPerSet);
        // Assert that the value of all the records is at it was before the truncate
        for (Key key : keys) {
            Record record = AerospikeDataUtils.get(key);
            String value = record.getString(STRING_BIN);
            assertThat(value).isEqualTo(initialValue);
        }
    }

    @Test
    void restoreBigRecord() {
        final int ITERATIONS = 1_000_000;
        createBigRecord(ITERATIONS, KEY);
        String firstBackupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        AbsRestoreApi.restoreFullSync(firstBackupKey, ROUTINE_NAME);

        Record retrievedRecord = AerospikeDataUtils.get(KEY);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getList("list")).hasSize(ITERATIONS);
    }

    @Test
    @SuppressWarnings("unchecked")
    void restoreBigComplexRecord() {
        final int ITERATIONS = 10_000;
        var bin = createComplexRecord(ITERATIONS);
        srcClient.put(null, KEY, bin);

        String firstBackupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        AbsRestoreApi.restoreFullSync(firstBackupKey, ROUTINE_NAME);

        Record retrievedRecord = AerospikeDataUtils.get(KEY);
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
        Map<String, Object> scalarValues = new HashMap<>();

        scalarValues.put("intBin", Integer.MAX_VALUE);
        scalarValues.put("longBin", Long.MAX_VALUE);
        scalarValues.put("doubleBin", 3.14);
        scalarValues.put("shortBin", Short.MAX_VALUE);
        scalarValues.put("booleanBin", true);
        scalarValues.put("byteBin", Byte.MAX_VALUE);
        scalarValues.put("floatBin", 2.71f);
        scalarValues.put("blobBin", "blob".getBytes());
        scalarValues.put("listBin", List.of("a", "b", "c"));
        scalarValues.put("mapBin", Map.of("a", true, "b", false));

        scalarValues.put("smallDouble1", 2.779745911202054e-161);
        scalarValues.put("smallDouble2", 0.05972567867873778);
        scalarValues.put("smallFloat1", 97.47637592329345);
        scalarValues.put("smallFloat2", 2.3394288543275787e-55);
        scalarValues.put("smallDouble3", 3.201428347272551e-47);
        scalarValues.put("smallDouble4", 2.5091384578803992e-26);
        scalarValues.put("smallFloat3", 1.3842560990143293e-81);
        scalarValues.put("smallDouble5", 1.6843432519718654e-110);
        scalarValues.put("smallFloat4", 2.7630727007715237e-13);
        scalarValues.put("smallFloat5", 8.042975202384266e-36);
        scalarValues.put("bigFloat1", 3.4028235e+38f);
        scalarValues.put("bigFloat2", 1.0e+30f);
        scalarValues.put("bigDouble1", 1.7976931348623157e+308);
        scalarValues.put("bigDouble2", 1.0e+300);

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