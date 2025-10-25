package utils.aerospike.abs;

import com.aerospike.client.Record;
import com.aerospike.client.*;
import com.aerospike.client.cluster.Node;
import com.aerospike.client.cluster.Partition;
import com.aerospike.client.policy.InfoPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.task.RegisterTask;
import com.google.common.base.Preconditions;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.awaitility.core.ConditionTimeoutException;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.ConfigParametersHandler;
import utils.abs.AbsRunner;
import utils.abs.TlsHandler;
import utils.aerospike.AerospikeCountUtils;
import utils.cliBackup.CliBackupRunner;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static utils.aerospike.AerospikeCountUtils.waitForNamespaceTruncate;
import static utils.aerospike.AerospikeCountUtils.waitForSetTruncate;

@UtilityClass
public class AerospikeDataUtils {

    private static volatile IAerospikeClient sourceClient;
    private static final int POLICY_TIMEOUT = 8_000;

    public Record get(Key key) {
        Policy getPolicy = new Policy();
        getPolicy.setTimeout(POLICY_TIMEOUT);
        getPolicy.setConnectTimeout(POLICY_TIMEOUT);
        getPolicy.setTotalTimeout(POLICY_TIMEOUT);
        AerospikeLogger.info("Trying to get record for key: " + key.toString());
        return getSourceClient().get(null, key);

    }

    public static long put(WritePolicy policy, Key key, String binName, String binValue) {
        getSourceClient().put(policy, key, new Bin(binName, binValue));
        long putTime = System.currentTimeMillis() - 1;
        String[] keyArr = key.toString().split(":");
        AerospikeLogger.info("Added the value " + binValue + " to the set " + keyArr[1]
                + " in the namespace " + keyArr[0]);

        Preconditions.checkArgument(
                getSourceClient().get(null, key).getString(binName).equals(binValue),
                "%s was not written".formatted(binName));
        return putTime;
    }

    public static void putTransaction(WritePolicy writePolicy, Key key, String binName, String binValue, boolean doCommit) {
        Txn txn = new Txn();

        writePolicy.setTxn(txn);

        Bin bin1 = new Bin(binName, binValue);

        getSourceClient().operate(writePolicy, key,
                Operation.put(bin1));

        if (doCommit)
            getSourceClient().commit(txn);
    }

    public static List<Key> putTransactions(Key baseKey, String binName, String binValue, int numRecords) {
        return putTransactions(baseKey, binName, binValue, numRecords, 0);
    }

    public static List<Key> putTransactions(Key baseKey, String binName, String binValue, int numRecords, int sleepDurationInSeconds) {
        WritePolicy writePolicy = new WritePolicy();
        Txn txn = new Txn();
        List<Key> keyList = new ArrayList<>();

        writePolicy.setTxn(txn);

        for (int i = 1; i <= numRecords; i++) {
            Key newKey = new Key(baseKey.namespace, baseKey.setName, baseKey.userKey.toString() + "_" + i);
            Bin bin = new Bin(binName, binValue + i);
            getSourceClient().operate(writePolicy, newKey, Operation.put(bin));
            keyList.add(newKey);
            if (sleepDurationInSeconds > 0)
                AutoUtils.sleep(1000 * sleepDurationInSeconds);
        }
        CommitStatus commitStatus = getSourceClient().commit(txn);

        AerospikeCountUtils.getSetObjectCount(getSourceClient(), baseKey.setName, baseKey.namespace);

        assertThat(commitStatus).isEqualTo(CommitStatus.OK);

        return keyList;
    }

    public static List<Key> putTransactionsInUniquePartition(Key baseKey, String binName, String binValue, int numRecords, int sleepDurationInSeconds) {
        if (numRecords > 4096) {
            throw new IllegalArgumentException("Cannot ensure unique partitions beyond 4096 records.");
        }

        WritePolicy writePolicy = new WritePolicy();
        Txn txn = new Txn();
        List<Key> keyList = new ArrayList<>();
        writePolicy.setTxn(txn);

        Set<Integer> usedPartitions = new HashSet<>();

        for (int i = 0; i < numRecords; i++) {
            Key newKey;
            int partitionId;

            do {
                newKey = new Key(baseKey.namespace, baseKey.setName, UUID.randomUUID().toString());
                partitionId = Partition.getPartitionId(newKey.digest);
            } while (usedPartitions.contains(partitionId));

            usedPartitions.add(partitionId);

            Bin bin = new Bin(binName, binValue + i);
            getSourceClient().operate(writePolicy, newKey, Operation.put(bin));
            keyList.add(newKey);

            if (sleepDurationInSeconds > 0) {
                AutoUtils.sleep(1000 * sleepDurationInSeconds);
            }
        }

        AerospikeLogger.info("Used partitions: " + usedPartitions.size() + " / 4096");

        CommitStatus commitStatus = getSourceClient().commit(txn);
        assertThat(commitStatus).isEqualTo(CommitStatus.OK);

        return keyList;
    }

    public static List<Key> putAndUpdateTransactions(Key baseKey, String binName, String binValue, int numRecords) {
        WritePolicy writePolicy = new WritePolicy();
        Txn txn = new Txn();
        List<Key> keyList = new ArrayList<>();

        writePolicy.setTxn(txn);

        for (int i = 1; i <= numRecords; i++) {
            Key newKey = new Key(baseKey.namespace, baseKey.setName, baseKey.userKey.toString() + "_" + i);

            Bin bin = new Bin(binName, binValue + i);
            getSourceClient().operate(writePolicy, newKey, Operation.put(bin));

            keyList.add(newKey);

            Bin updatedBin = new Bin(binName, (binValue + i) + 10);
            getSourceClient().operate(writePolicy, newKey, Operation.put(updatedBin));
        }
        getSourceClient().commit(txn);

        AerospikeCountUtils.getSetObjectCount(getSourceClient(), baseKey.setName, baseKey.namespace);

        return keyList;
    }


    public static void putTransaction(Key key, String binName, String binValue) {
        WritePolicy writePolicy = new WritePolicy();
        putTransaction(writePolicy, key, binName, binValue, true);
    }

    public static void putTransaction(Key key, String binName, String binValue, boolean doCommit) {
        WritePolicy writePolicy = new WritePolicy();
        putTransaction(writePolicy, key, binName, binValue, doCommit);
    }

    public static void put(Key key, String binName, long binValue) {
        getSourceClient().put(null, key, new Bin(binName, binValue));
        String[] keyArr = key.toString().split(":");
        AerospikeLogger.info("Added the value " + binValue + " to the set " + keyArr[1]
                + " in the namespace " + keyArr[0]);
    }

    public static long put(Key key, String binName, double binValue) {
        getSourceClient().put(null, key, new Bin(binName, binValue));
        long putTime = System.currentTimeMillis() - 1;
        String[] keyArr = key.toString().split(":");
        AerospikeLogger.info("Added the value " + binValue + " to the set " + keyArr[1]
                + " in the namespace " + keyArr[0]);

        Preconditions.checkArgument(
                getSourceClient().get(null, key).getDouble(binName) == binValue,
                "%s was not written".formatted(binName));
        return putTime;
    }

    public static long put(Key key, String binName, String binValue) {
        return put(null, key, binName, binValue);
    }

    public static void put(Key key, String binName, Long binValue) {
        getSourceClient().put(null, key, new Bin(binName, binValue));
        String[] keyArr = key.toString().split(":");
        AerospikeLogger.info("Added the value " + binValue + " to the set " + keyArr[1]
                + " in the namespace " + keyArr[0]);
    }

    public static void putNoLogs(Key key, Bin... bins) {
        getSourceClient().put(null, key, bins);
    }

    public static void put(Key key, Bin... bins) {
        putNoLogs(key, bins);
        String[] keyArr = key.toString().split(":");
        AerospikeLogger.info("Added the bin %s to the set %s in the namespace %s".formatted(Arrays.toString(bins), keyArr[1], keyArr[0]));
    }

    public static long delete(Key... keys) {
        long deleteTime = 0;
        for (Key key : keys) {
            getSourceClient().delete(null, key);
            deleteTime = System.currentTimeMillis() - 1;
            AerospikeLogger.info("Deleted the given key: " + key);
        }
        return deleteTime;
    }

    public static void truncateSourceNamespace(String namespace) {
        truncateNamespace(getSourceClient(), namespace);
    }

    public static void truncateNamespace(IAerospikeClient client, String namespace) {
        InfoPolicy truncatePolicy = new InfoPolicy();
        truncatePolicy.timeout = 120_000;

        int tries = 0;
        while (tries++ < 10) {
            try {
                AerospikeLogger.info("Try truncate: " + namespace);
                client.truncate(truncatePolicy, namespace, null, null);
                waitForNamespaceTruncate(client, namespace);
                return;
            } catch (AerospikeException e) {
                AerospikeLogger.info("Exception during truncate: " + e.getMessage());
                AutoUtils.sleep(tries * 100L);
            } catch (ConditionTimeoutException ignored) {
            }
        }
        throw new RuntimeException("Could not truncate namespace " + namespace);
    }

    public static void truncateSourceSet(String namespace, String... sets) {
        truncateSet(getSourceClient(), namespace, sets);
    }

    public static void truncateSet(IAerospikeClient client, String namespace, String... sets) {
        if (sets != null) {
            for (String set : sets) {
                try {
                    AerospikeLogger.info("Try truncate: " + namespace + " set " + set);
                    client.truncate(null, namespace, set, null);
                    waitForSetTruncate(client, namespace, set);
                } catch (AerospikeException e) {
                    AerospikeLogger.info(e.getMessage());
                }
            }
        }
    }

    public static IAerospikeClient createSourceClient() {
        AerospikeLogger.info("CLIENT_POLICY_SOURCE AEROSPIKE_SOURCE_SERVER_IP AEROSPIKE_SOURCE_SERVER_PORT =" +
                AbsRunner.CLIENT_POLICY_SOURCE + " " + AbsRunner.AEROSPIKE_SOURCE_SERVER_IP + " " + AbsRunner.AEROSPIKE_SOURCE_SERVER_PORT);
        if (System.getProperty("TESTED_PRODUCT").equals("cli_backup"))
            return new AerospikeClient(CliBackupRunner.CLIENT_POLICY_SOURCE, CliBackupRunner.AEROSPIKE_SOURCE_SERVER_IP, CliBackupRunner.AEROSPIKE_SOURCE_SERVER_PORT);
        else
            return new AerospikeClient(AbsRunner.CLIENT_POLICY_SOURCE, AbsRunner.AEROSPIKE_SOURCE_SERVER_IP, AbsRunner.AEROSPIKE_SOURCE_SERVER_PORT);
    }

    public static IAerospikeClient createTlsClient() {
        AerospikeLogger.info("Starting createTlsClient");
        if (ConfigParametersHandler.getParameter("TESTED_PRODUCT").equals("cli_backup")) {
            AerospikeLogger.info("Creating cli tls client");
            AerospikeLogger.info("Connect to DB with ip: " + CliBackupRunner.AEROSPIKE_SOURCE_SERVER_IP +
                    " tls name: " + TlsHandler.TLS_NAME + " port: " + CliBackupRunner.AEROSPIKE_SOURCE_SERVER_PORT);
            return new AerospikeClient(CliBackupRunner.CLIENT_POLICY_SOURCE, new Host(CliBackupRunner.AEROSPIKE_SOURCE_SERVER_IP, TlsHandler.TLS_NAME, CliBackupRunner.AEROSPIKE_SOURCE_SERVER_PORT));
        } else {
            AerospikeLogger.info("Creating abs tls client");
            AerospikeLogger.info("Connect to DB with ip: " + AbsRunner.AEROSPIKE_SOURCE_SERVER_IP +
                    " tls name: " + TlsHandler.TLS_NAME + " port: " + AbsRunner.AEROSPIKE_SOURCE_SERVER_PORT);
            return new AerospikeClient(AbsRunner.CLIENT_POLICY_SOURCE, new Host(AbsRunner.AEROSPIKE_SOURCE_SERVER_IP, TlsHandler.TLS_NAME, AbsRunner.AEROSPIKE_SOURCE_SERVER_PORT));
        }
    }

    public static synchronized IAerospikeClient getSourceClient() {
        if (sourceClient == null) {
            synchronized (AerospikeDataUtils.class) {
                if (sourceClient == null) {
                    if (AutoUtils.isRunningOnGCP() || ConfigParametersHandler.getParameter("LOCAL_TLS_ENABLED").equals("true")) {
                        AerospikeLogger.info("Creating tls client");
                        sourceClient = createTlsClient();
                    } else {
                        AerospikeLogger.info("Creating regular(not tls) client");
                        sourceClient = createSourceClient();
                    }
                }
            }
        }
        return sourceClient;
    }

    public static synchronized IAerospikeClient createSourceClient(int sourceServerPort) {
        return new AerospikeClient(AbsRunner.CLIENT_POLICY_SOURCE, AbsRunner.AEROSPIKE_SOURCE_SERVER_IP, sourceServerPort);
    }

    public static boolean isIndexExist(String indexName) {
        String indexesInfo = Info.request(getSourceClient().getInfoPolicyDefault(), getSourceClient().getCluster().getRandomNode(), "sindex");
        return indexesInfo.contains("indexname=" + indexName);
    }

    public static long getDataTotalBytes(String namespace) {
        long totalBytes = 0;
        InfoPolicy infoPolicy = getSourceClient().getInfoPolicyDefault();
        try {
            // Iterate over each node and sum the data used bytes
            for (Node node : getSourceClient().getCluster().getNodes()) {
                String namespaceInfo = Info.request(infoPolicy, node, "namespace/" + namespace);
                AerospikeLogger.info("Namespace info: " + namespaceInfo);
                totalBytes += extractDataUsedBytes(namespaceInfo);
            }
        } catch (Exception e) {
            AerospikeLogger.info(e.getMessage());
        }
        return totalBytes;
    }

    private static long extractDataUsedBytes(String namespaceInfo) {
        for (String stat : namespaceInfo.split(";")) {
            if (stat.startsWith("data_used_bytes")) {
                return Long.parseLong(stat.split("=")[1]);
            }
        }

        return 0;
    }

    public static void createUDF(String udfCode, String fileName) {
        try {
            Language language = Language.LUA; // Adjust language accordingly
            RegisterTask task = getSourceClient().registerUdfString(null, udfCode, fileName, language);
            task.waitTillComplete();
            AerospikeLogger.info("Created the UDF: " + fileName);
        } catch (AerospikeException e) {
            AerospikeLogger.error("Failed to create UDF %s: %s".formatted(fileName, e.getMessage()));
            throw new RuntimeException(e);
        }
    }

    public static void createUDF(String fileName) {
        String udfCode = "local function getBinValue(rec)\n" +
                "    return rec['binName']\n" +
                "end\n";
        createUDF(udfCode, fileName);
    }

    public static void deleteUDF(String fileName) {
        try {
            getSourceClient().removeUdf(null, fileName);
            AerospikeLogger.info("Deleted the UDF: " + fileName);
        } catch (AerospikeException e) {
            AerospikeLogger.error("Failed to delete UDF %s: %s".formatted(fileName, e.getMessage()));
            throw new RuntimeException(e);
        }
    }

    public static boolean isUdfExist(String udfName) {
        String udfInfo = Info.request(getSourceClient().getInfoPolicyDefault(), getSourceClient().getCluster().getRandomNode(), "udf-list");
        return udfInfo.contains("filename=" + udfName);
    }

    public static String getAllNamespaces() {
        return Info.request(null, getSourceClient().getNodes()[0], "namespaces");
    }

    public static void truncateAllSourceNamespaces(List<String> whiteList) {
        String[] namespaces = getAllNamespaces().split(";");
        for (String namespace : namespaces) {
            if (!whiteList.contains(namespace))
                truncateSourceNamespace(namespace);
        }
        AutoUtils.sleep(5_000);
    }

    public static void truncateAllSourceNamespaces() {
        truncateAllSourceNamespaces(List.of());
    }

    @SneakyThrows
    public static Bin createComplexRecord(int complexity) {
        // Main map to store all iterations data
        Map<String, Object> mainMap = new HashMap<>();
        for (int i = 0; i < complexity; i++) {
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
        return new Bin("books", mainMap);
    }

    public static void disableMrtWrites(String namespace) {
        InfoPolicy infoPolicy = getSourceClient().getInfoPolicyDefault();
        for (Node node : getSourceClient().getCluster().getNodes()) {
            String command = String.format("set-config:context=namespace;id=%s;disable-mrt-writes=true", namespace);
            String response = Info.request(infoPolicy, node, command);
            assertThat(response).isEqualTo("ok");
        }
    }

    public static void startXdr(String dc, int nodeAddressPort, String namespace) {
        InfoPolicy infoPolicy = getSourceClient().getInfoPolicyDefault();

        for (Node node : getSourceClient().getCluster().getNodes()) {
            // Step 1: Delete the existing XDR Data Center (if it exists)
            String deleteDcCommand = String.format("set-config:context=xdr;dc=%s;action=delete", dc);
            String deleteDcResponse = Info.request(infoPolicy, node, deleteDcCommand);
            AerospikeLogger.info("Delete DC Response: " + deleteDcResponse);

            // Step 2: Create a new XDR Data Center
            String createDcCommand = String.format("set-config:context=xdr;dc=%s;action=create", dc);
            String createDcResponse = Info.request(infoPolicy, node, createDcCommand);
            AerospikeLogger.info("Create DC Response: " + createDcResponse);

            // Step 3: Enable the XDR Connector
            String enableConnectorCommand = String.format("set-config:context=xdr;dc=%s;connector=true", dc);
            String enableConnectorResponse = Info.request(infoPolicy, node, enableConnectorCommand);
            AerospikeLogger.info("Enable Connector Response: " + enableConnectorResponse);

            // Step 4: Add a node to the XDR Data Center (Must be in "IP:PORT" format)
            String addNodeCommand = String.format("set-config:context=xdr;dc=%s;node-address-port=%s;action=add", dc, nodeAddressPort);
            String addNodeResponse = Info.request(infoPolicy, node, addNodeCommand);
            AerospikeLogger.info("Add Node Response: " + addNodeResponse);

            // Step 5: Add a namespace for replication with rewind set to "all"
            String addNamespaceCommand = String.format("set-config:context=xdr;dc=%s;namespace=%s;action=add;rewind=all", dc, namespace);
            String addNamespaceResponse = Info.request(infoPolicy, node, addNamespaceCommand);
            AerospikeLogger.info("Add Namespace Response: " + addNamespaceResponse);
        }
    }

    public void createBigData(String sourceNamespace, String setName, double desiredMinNumberOfRecordsInMillions, String recordType) {
        int targetTotalRecords = (int) (desiredMinNumberOfRecordsInMillions * 1_000_000);
        AerospikeLogger.info("Starting data creation for: " + recordType);
        AerospikeLogger.info("Target: " + targetTotalRecords + " records");

        int startKey = AerospikeCountUtils.getSetObjectCount(getSourceClient(), setName, sourceNamespace);

        AerospikeLogger.info("Initial DB count: " + startKey);
        ASBench.on(sourceNamespace, setName)
                .startKey(startKey)
                .keys(targetTotalRecords)
                .threads(64)
                .batchSize(100)
                .recordType(recordType)
                .run();

        int finalCount = AerospikeCountUtils.getSetObjectCount(getSourceClient(), setName, sourceNamespace);
        AerospikeLogger.info("Finished data creation for: " + recordType);
        AerospikeLogger.info("Final SET count: " + finalCount + " (Expected to add " + targetTotalRecords + ")");
    }

    public void createBigData(String sourceNamespace, String setName, int desiredMinNumberOfRecordsInMillions) {
        createBigData(sourceNamespace, setName, desiredMinNumberOfRecordsInMillions, "B1024");
    }

    public static List<Key> filterKeysByPartition(List<Key> allKeys, int partitionId) {
        List<Key> samePartitionKeys = new ArrayList<>();
        for (Key key : allKeys) {
            if (Partition.getPartitionId(key.digest) == partitionId) {
                samePartitionKeys.add(key);
            }
        }
        return samePartitionKeys;
    }

    public static Map<String, Map<String, Object>> getAllRecords(String namespace, String set) {
        Map<String, Map<String, Object>> result = new ConcurrentHashMap<>();
        getSourceClient().scanAll(
                null,
                namespace,
                set,
                (key, record) -> {
                    if (record != null) {
                        String digestKey = Base64.getEncoder().encodeToString(key.digest);
                        result.put(digestKey, record.bins);
                    }
                }
        );
        return result;
    }

    public void createData(String sourceNamespace, String setName, int desiredNumberOfRecords, int recordSizeInBytes) {
        int maxAttempts = 15;
        int attempt = 0;
        int recordCount = AerospikeCountUtils.getSetObjectCount(getSourceClient(), setName, sourceNamespace);
        int recordsToCreate = desiredNumberOfRecords - recordCount;

        AerospikeLogger.info("Starting to create exactly " + desiredNumberOfRecords + " records");

        int startKey = recordCount;

        while (recordsToCreate > 0 && attempt < maxAttempts) {
            AerospikeLogger.info("Attempt #" + (attempt + 1) + " — Records to create: " + recordsToCreate);

            ASBench.on(sourceNamespace, setName)
                    .startKey(startKey)
                    .keys(recordsToCreate)
                    .threads(10)
                    .batchSize(100)
                    .recordSize(recordSizeInBytes)
                    .run();

            attempt++;
            startKey += recordsToCreate;

            int newCount = AerospikeCountUtils.getSetObjectCount(getSourceClient(), setName, sourceNamespace);
            recordsToCreate = desiredNumberOfRecords - newCount;
            recordCount = newCount;

            AerospikeLogger.info("Current record count: " + recordCount);
        }

        int finalCount = AerospikeCountUtils.getSetObjectCount(getSourceClient(), setName, sourceNamespace);
        AerospikeLogger.info("Final record count after exact data creation: " + finalCount);
        assertThat(finalCount)
                .as("The number of records should be exactly or slightly more than the desired count")
                .isGreaterThanOrEqualTo(desiredNumberOfRecords);
    }
}