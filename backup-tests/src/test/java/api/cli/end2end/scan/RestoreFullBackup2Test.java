package api.cli.end2end.scan;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import api.cli.RestoreResult;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import org.junit.jupiter.api.*;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("CLI-BACKUP")
class RestoreFullBackup2Test extends CliBackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns3";

    private static final String STRING_BIN = "FullBackup2Bin";
    private static final String SET1 = "SetRestoreFullBackup2Test";
    private static Key KEY1;
    private static final String SET2 = "SetFullBackupTest2";
    private static Key KEY2;

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
        KEY2 = new Key(SOURCE_NAMESPACE, SET2, "IT2");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void backupFilterBySetAndBin() {
        String backupSet = "backupSet";
        String noBackupSet = "noBackupSet";
        String backupBin = "backupBin";
        String noBackupBin = "noBackupBin";

        final Key backupKey = new Key(SOURCE_NAMESPACE, backupSet, "IT1");
        final Key noBackupKey = new Key(SOURCE_NAMESPACE, noBackupSet, "IT1");

        // init data
        long timeMillis = System.currentTimeMillis();
        var initialValue = "init value " + timeMillis;

        List.of(backupKey, noBackupKey).forEach(key -> {
            AerospikeDataUtils.put(key,
                    new Bin(backupBin, initialValue),
                    new Bin(noBackupBin, initialValue)
            );
        });

        String backupDir = CliBackup.on(SOURCE_NAMESPACE, "backupFilterBySetAndBinBackup1").setBinList("backupBin").setSetList("backupSet").run().getBackupDir();

        AerospikeDataUtils.delete(backupKey, noBackupKey);

        CliRestore.on(SOURCE_NAMESPACE, backupDir).run();

        // assertion
        Record record = srcClient.get(null, backupKey);

        assertThat(record).isNotNull();
        assertThat(record.bins)
                .hasSize(1)
                .containsEntry(backupBin, initialValue);

        Record noRecord = srcClient.get(null, noBackupKey);
        assertThat(noRecord).isNull();
    }

    @Test
    void restoreFilterBySetAndBin() {
        String restoreSet = "restoreSet";
        String noRestoreSet = "noRestoreSet";
        String restoreBin = "restoreBin";
        String noRestoreBin = "noRestoreBin";

        final Key backupKey = new Key(SOURCE_NAMESPACE, restoreSet, "IT1");
        final Key noBackupKey = new Key(SOURCE_NAMESPACE, noRestoreSet, "IT1");

        // init data
        long timeMillis = System.currentTimeMillis();
        var initialValue = "init value " + timeMillis;

        List.of(backupKey, noBackupKey).forEach(key -> {
            AerospikeDataUtils.put(key,
                    new Bin(restoreBin, initialValue),
                    new Bin(noRestoreBin, initialValue)
            );
        });

        Record record = srcClient.get(null, backupKey);

        assertThat(record).isNotNull();

        String backupDir = CliBackup.on(SOURCE_NAMESPACE, "restoreFilterBySetAndBinBackup1").run().getBackupDir();

        AerospikeDataUtils.delete(backupKey, noBackupKey);

        CliRestore.on(SOURCE_NAMESPACE, backupDir).setSetList(restoreSet).setBinList(restoreBin).run();

        // assertion
        record = srcClient.get(null, backupKey);

        assertThat(record).isNotNull();
        assertThat(record.bins)
                .hasSize(1)
                .containsEntry(restoreBin, initialValue);

        Record noRecord = srcClient.get(null, noBackupKey);
        assertThat(noRecord).isNull();
    }

    @Test
    void noBins() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, "noBinsBackup").setNoBins().run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        RestoreResult restoreResult = CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();
        assertThat(restoreResult.getRecordsRead()).isEqualTo(1);
        assertThat(restoreResult.getSkippedRecords()).isEqualTo(1);

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNull();
    }

    @Test
    void noTtlOnly() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        String secondValueCreate = "secondValueCreate" + System.currentTimeMillis();

        WritePolicy policy = new WritePolicy();
        policy.setExpiration(-1);
        AerospikeDataUtils.put(policy, KEY1, STRING_BIN, firstValueCreate);

        policy.setExpiration(1000);
        AerospikeDataUtils.put(policy, KEY2, STRING_BIN, secondValueCreate);

        String backupKey = CliBackup.on(SOURCE_NAMESPACE, "noTtlOnlyBackup1").setNoTtlOnly().run().getBackupDir();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupKey).run();

        Record retrievedRecord1 = srcClient.get(null, KEY1);
        assertThat(retrievedRecord1).isNotNull();

        Record retrievedRecord2 = srcClient.get(null, KEY2);
        assertThat(retrievedRecord2).isNull();
    }

    @Test
    void estimate() {
        ASBench.on(SOURCE_NAMESPACE, SET1)
                .keys(10)
                .batchSize(1)
                .threads(1)
                .recordSize(1)
                .run();
        long backupSizeEstimation = CliBackup.on(SOURCE_NAMESPACE).setEstimateSamples(10).estimate();
        assertThat(backupSizeEstimation).isGreaterThan(1_000).isLessThan(2_000);

        ASBench.on(SOURCE_NAMESPACE, SET1)
                .keys(10)
                .batchSize(1)
                .threads(1)
                .recordSize(500)
                .run();
        backupSizeEstimation = CliBackup.on(SOURCE_NAMESPACE).setEstimateSamples(20).estimate();
        assertThat(backupSizeEstimation).isGreaterThan(7_500).isLessThan(9_000);
    }

    @Test
    void directoryList_MultipleBackupsInLoop() {
        int numberOfBackups = 20;
        String baseDir = "backupDir";
        List<String> backupDirectories = new ArrayList<>();

        for (int i = 1; i <= numberOfBackups; i++) {
            String backupDir = baseDir + i; // Keep it simple for backup creation
            backupDirectories.add(backupDir);

            String setName = "set" + i;
            String keyValue = "key" + i;
            String binName = "bin" + i;
            String binValue = "value" + i;

            Key key = new Key(SOURCE_NAMESPACE, setName, keyValue);
            AerospikeDataUtils.put(key, binName, binValue);

            BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, backupDir).run();
            assertThat(backupResult.getBackupDir()).isEqualTo("/tmp/" + backupDir); // Validate the path

            AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        }

        String[] restoreDirectories = backupDirectories.stream()
                .map(dir -> "/tmp/" + dir)
                .toArray(String[]::new);

        RestoreResult restoreResult = CliRestore.on(SOURCE_NAMESPACE)
                .setDirectoryList(true, restoreDirectories)
                .run();

        assertThat(restoreResult.getInsertedRecords()).isEqualTo(numberOfBackups);

        for (int i = 1; i <= numberOfBackups; i++) {
            String setName = "set" + i;
            String keyValue = "key" + i;
            String binName = "bin" + i;
            String binValue = "value" + i;

            Key key = new Key(SOURCE_NAMESPACE, setName, keyValue);
            Record restoredRecord = srcClient.get(null, key);

            assertThat(restoredRecord).isNotNull();
            assertThat(restoredRecord.getString(binName)).isEqualTo(binValue);
        }
    }

    @Test
    void parentDir() {
        String set1 = "set1", set2 = "set2";
        String key1Value = "key1", key2Value = "key2";
        String bin1Name = "bin1", bin2Name = "bin2";
        String bin1Value = "firstValue", bin2Value = "secondValue";
        String backupDirA = "dirA", backupDirB = "dirB";

        Key key1 = new Key(SOURCE_NAMESPACE, set1, key1Value);
        AerospikeDataUtils.put(key1, bin1Name, bin1Value);

        CliBackup.on(SOURCE_NAMESPACE, backupDirA).run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        Key key2 = new Key(SOURCE_NAMESPACE, set2, key2Value);
        AerospikeDataUtils.put(key2, bin2Name, bin2Value);
        CliBackup.on(SOURCE_NAMESPACE, backupDirB).run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        RestoreResult restoreResult = CliRestore.on(SOURCE_NAMESPACE)
                .setParentDirectory("/tmp").setDirectoryList(true, backupDirA, backupDirB)
                .run();

        assertThat(restoreResult.getInsertedRecords()).isEqualTo(2);

        Record retrievedRecord = srcClient.get(null, key1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(bin1Name)).isEqualTo(bin1Value);

        retrievedRecord = srcClient.get(null, key2);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(bin2Name)).isEqualTo(bin2Value);
    }


    @Test
    @Disabled
        // The restore in random both in asbackup and cli tools. We'll open it once we'll decide how it should work.
    void directoryListOverlappingKeys() {
        String set1 = "set1";
        String key1Value = "key1";
        String bin1Name = "bin1";
        String bin1ValueA = "valueFromDirA"; // Value for the first backup
        String bin1ValueB = "valueFromDirB"; // Overlapping value for the second backup
        String backupDirA = "dirA", backupDirB = "dirB";

        Key key1 = new Key(SOURCE_NAMESPACE, set1, key1Value);
        AerospikeDataUtils.put(key1, bin1Name, bin1ValueA);

        BackupResult fastRestoreBackup = CliBackup.on(SOURCE_NAMESPACE, backupDirA).run();
        assertThat(fastRestoreBackup.getBackupDir()).isEqualTo("/tmp/" + backupDirA);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        // Create second backup with an overlapping key
        AerospikeDataUtils.put(key1, bin1Name, bin1ValueB);
        BackupResult fastRestoreBackup2 = CliBackup.on(SOURCE_NAMESPACE, backupDirB).run();
        assertThat(fastRestoreBackup2.getBackupDir()).isEqualTo("/tmp/" + backupDirB);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        RestoreResult restoreResult = CliRestore.on(SOURCE_NAMESPACE)
                .setDirectoryList(fastRestoreBackup.getBackupDir(), fastRestoreBackup2.getBackupDir())
                .run();

        assertThat(restoreResult.getInsertedRecords()).isEqualTo(1);

        // Validate the restored value (last directory wins)
        Record retrievedRecord = srcClient.get(null, key1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(bin1Name)).isEqualTo(bin1ValueB); // Last backup should win
    }

    @Test
    void scanRestoreMixedTTLRecords() {
        Key keyShortTTL = new Key(SOURCE_NAMESPACE, SET1, "KeyWithShortTTL");
        Key keyLongTTL = new Key(SOURCE_NAMESPACE, SET1, "KeyWithLongTTL");
        Key keyNoExpire = new Key(SOURCE_NAMESPACE, SET1, "KeyWithNoExpiration");
        Key keyDefaultTTL = new Key(SOURCE_NAMESPACE, SET1, "KeyWithDefaultTTL");
        Key keyPreserveTTLNew = new Key(SOURCE_NAMESPACE, SET1, "KeyWithPreserveTTLNew");
        Key keyPreserveTTLUpdated = new Key(SOURCE_NAMESPACE, SET1, "KeyWithPreserveTTLUpdated");

        int shortTTL = 300;  // 300 seconds
        int longTTL = 3000;  // 3000 seconds
        int neverExpire = -1;  // Never expires
        int defaultTTL = 0;  // Uses server-defined default TTL
        int preserveTTL = -2;  // Preserves void-time if updating

        WritePolicy shortTtlPolicy = new WritePolicy();
        shortTtlPolicy.setExpiration(shortTTL);

        WritePolicy longTtlPolicy = new WritePolicy();
        longTtlPolicy.setExpiration(longTTL);

        WritePolicy noExpirePolicy = new WritePolicy();
        noExpirePolicy.setExpiration(neverExpire);

        WritePolicy defaultTtlPolicy = new WritePolicy();
        defaultTtlPolicy.setExpiration(defaultTTL);

        WritePolicy preserveTtlPolicy = new WritePolicy();
        preserveTtlPolicy.setExpiration(preserveTTL);

        srcClient.put(shortTtlPolicy, keyShortTTL, new Bin(STRING_BIN, "ShortTTLValue"));
        srcClient.put(longTtlPolicy, keyLongTTL, new Bin(STRING_BIN, "LongTTLValue"));
        srcClient.put(noExpirePolicy, keyNoExpire, new Bin(STRING_BIN, "NoExpirationValue"));
        srcClient.put(defaultTtlPolicy, keyDefaultTTL, new Bin(STRING_BIN, "DefaultTTLValue"));
        srcClient.put(preserveTtlPolicy, keyPreserveTTLNew, new Bin(STRING_BIN, "PreserveTTLNewValue"));

        // Perform an update on the record with preserve TTL
        srcClient.put(preserveTtlPolicy, keyPreserveTTLUpdated, new Bin(STRING_BIN, "PreserveTTLUpdatedValue"));

        // Save the original expiration times
        int currentTimeBeforeBackup = (int) (System.currentTimeMillis() / 1000);

        Record recordShortTTL = srcClient.get(null, keyShortTTL);
        Record recordLongTTL = srcClient.get(null, keyLongTTL);
        Record recordPreserveTTLUpdated = srcClient.get(null, keyPreserveTTLUpdated);

        int originalShortTTL = recordShortTTL.expiration;
        int originalLongTTL = recordLongTTL.expiration;
        int originalPreserveTTLUpdated = recordPreserveTTLUpdated.expiration;

        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, "scanBackupRestoreMixedTTLRecordsDIR")
                .run();

        assertThat(backupResult.getRecordsRead()).isEqualTo(6);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        // Fetch restored records
        int currentTimeAfterRestore = (int) (System.currentTimeMillis() / 1000);

        Record restoredShortTTL = srcClient.get(null, keyShortTTL);
        Record restoredLongTTL = srcClient.get(null, keyLongTTL);
        Record restoredNoExpire = srcClient.get(null, keyNoExpire);
        Record restoredDefaultTTL = srcClient.get(null, keyDefaultTTL);
        Record restoredPreserveTTLNew = srcClient.get(null, keyPreserveTTLNew);
        Record restoredPreserveTTLUpdated = srcClient.get(null, keyPreserveTTLUpdated);

        int timeElapsedDuringBackupRestore = currentTimeAfterRestore - currentTimeBeforeBackup;
        int allowedDrift = 5;  // Allow a small-time drift

        assertThat(restoredShortTTL.expiration)
                .isBetween(originalShortTTL - allowedDrift, originalShortTTL + timeElapsedDuringBackupRestore);
        assertThat(restoredLongTTL.expiration)
                .isBetween(originalLongTTL - allowedDrift, originalLongTTL + timeElapsedDuringBackupRestore);
        assertThat(restoredDefaultTTL.expiration).isGreaterThan(0); // Should have server-defined TTL
        assertThat(restoredNoExpire.expiration).isEqualTo(0);
        assertThat(restoredPreserveTTLNew.expiration).isGreaterThan(0);
        assertThat(restoredPreserveTTLUpdated.expiration).isEqualTo(originalPreserveTTLUpdated);
    }

    @Test
    void nodeList() {
        // Step 1: Generate records
        ASBench.on(SOURCE_NAMESPACE, SET1).duration(1).run();
        int numberOfRecordsInAllTheNodes = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);

        // Step 2: Get node addresses (IP:port)
        List<String> nodeAddresses = AerospikeCountUtils.getNodeAddresses(srcClient);
        assertThat(nodeAddresses.size()).isGreaterThanOrEqualTo(2);

        String addr1 = nodeAddresses.get(0);
        String addr2 = nodeAddresses.get(1);

        // Step 3: Get record count for each selected node
        int countNode1 = AerospikeCountUtils.getObjectCountForNode(srcClient, SOURCE_NAMESPACE, addr1);
        AerospikeLogger.info("countNode1=" + countNode1);
        int countNode2 = AerospikeCountUtils.getObjectCountForNode(srcClient, SOURCE_NAMESPACE, addr2);
        AerospikeLogger.info("countNode2=" + countNode2);


        int expectedRecordCount = countNode1 + countNode2;
        AerospikeLogger.info("expectedRecordCount=" + expectedRecordCount);

        assertThat(countNode1).isGreaterThan(0);
        assertThat(countNode2).isGreaterThan(0);
        assertThat(expectedRecordCount).isLessThan(numberOfRecordsInAllTheNodes);

        // Step 4: Backup using node list
        String nodeList = addr1 + "," + addr2;
        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, "nodeListBackupDir")
                .setNodeList(nodeList)
                .run();

        // Step 5: Truncate and restore
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(0);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        // Step 6: Assert restored records match expected
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(expectedRecordCount);
    }

    @Test
    void twoRestoresInParallel() throws Exception {
        // Step 1: Generate many records
        ASBench.on(SOURCE_NAMESPACE, SET1)
                .keys(500_000)
                .threads(64)
                .batchSize(100)
                .run();

        int countBefore = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        assertThat(countBefore).isGreaterThan(300_000);
        Map<String, Map<String, Object>> allRecordsBefore = AerospikeDataUtils.getAllRecords(SOURCE_NAMESPACE, SET1);

        // backup
        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, "twoRestoresInParallel")
                .run();

        // Step 3: Truncate DB
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isZero();

        // Step 4: Restore twice in parallel (noGeneration true)
        CompletableFuture<Void> restore1 = CompletableFuture.runAsync(() ->
                CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).setNoGeneration().run());
        CompletableFuture<Void> restore2 = CompletableFuture.runAsync(() ->
                CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).setNoGeneration().run());
        restore1.get();
        restore2.get();

        int countAfter = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        assertThat(countAfter).isEqualTo(countBefore);

        Map<String, Map<String, Object>> recordsAfterRestore = AerospikeDataUtils.getAllRecords(SOURCE_NAMESPACE, SET1);
        assertThat(recordsAfterRestore)
                .as("After noGeneration=true restore")
                .isEqualTo(allRecordsBefore);

        // Step 3: Truncate DB
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isZero();

        // Step 4: Restore twice in parallel (noGeneration false)
        restore1 = CompletableFuture.runAsync(() ->
                CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run());
        restore2 = CompletableFuture.runAsync(() ->
                CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run());
        restore1.get();
        restore2.get();

        countAfter = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        assertThat(countAfter).isEqualTo(countBefore);

        recordsAfterRestore = AerospikeDataUtils.getAllRecords(SOURCE_NAMESPACE, SET1);
        assertThat(recordsAfterRestore)
                .as("After noGeneration=false restore")
                .isEqualTo(allRecordsBefore);
    }
}