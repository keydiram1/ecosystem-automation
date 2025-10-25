package api.cli.end2end.xdr;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.policy.WritePolicy;
import org.junit.jupiter.api.*;
import utils.ASBench;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("XDR-CLI-BACKUP")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class XdrRestore2Test extends CliBackupRunner {
    private static final String STRING_BIN = "RestoreTestBin";
    private static final String SET1 = "SetFullBackup2-1";
    private static final String SET2 = "SetFullBackup2-2";
    private static final String SET3 = "SetFullBackup2-3";
    private static Key KEY1;
    private static Key KEY2;
    private static Key KEY3;
    private static final String SOURCE_NAMESPACE = "source-ns14";
    private static final String DC = "DcXdrRestore2Test";
    private static final String BACKUP_DIR = "XdrRestore2TestDir";
    private static final int LOCAL_PORT = 8093;

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
        KEY2 = new Key(SOURCE_NAMESPACE, SET2, "IT2");
        KEY3 = new Key(SOURCE_NAMESPACE, SET3, "IT3");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void restoreDuplicateBinValues() {
        String binValue = "SameValue" + System.currentTimeMillis();

        AerospikeDataUtils.put(KEY1, "bin1", binValue);
        AerospikeDataUtils.put(KEY1, "bin2", binValue);
        AerospikeDataUtils.put(KEY1, "bin3", binValue);

        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, "duplicateBinBackup", DC, LOCAL_PORT).run();
        assertThat(backupResult.getRecordsRead()).isEqualTo(1);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString("bin1")).isEqualTo(binValue);
        assertThat(retrievedRecord.getString("bin2")).isEqualTo(binValue);
        assertThat(retrievedRecord.getString("bin3")).isEqualTo(binValue);
    }

    @Test
    void backupRestoreWithLargeBinValue() {
        String largeValue = "L".repeat(900_000); // 900KB large value

        AerospikeDataUtils.put(KEY1, STRING_BIN, largeValue);

        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, "largeBinBackup", DC, LOCAL_PORT).run();
        assertThat(backupResult.getRecordsRead()).isEqualTo(1);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(largeValue);
    }

    @Test
    void restoreUpdatedValue() {
        String binName = "collisionBin";

        AerospikeDataUtils.put(KEY1, binName, 12345);

        BackupResult backupResult1 = CliBackup.onWithXdr(SOURCE_NAMESPACE, "binCollisionBackup1", DC, LOCAL_PORT).run();
        assertThat(backupResult1.getRecordsRead()).isEqualTo(1);

        AerospikeDataUtils.put(KEY1, binName, "stringValue");

        BackupResult backupResult2 = CliBackup.onWithXdr(SOURCE_NAMESPACE, "binCollisionBackup2", DC, LOCAL_PORT).run();
        assertThat(backupResult2.getRecordsRead()).isEqualTo(1);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        CliRestore.on(SOURCE_NAMESPACE, backupResult1.getBackupDir()).run();

        Record recordAfterFirstRestore = srcClient.get(null, KEY1);
        assertThat(recordAfterFirstRestore).isNotNull();
        assertThat(recordAfterFirstRestore.getInt(binName)).isEqualTo(12345);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        CliRestore.on(SOURCE_NAMESPACE, backupResult2.getBackupDir()).run();

        Record recordAfterSecondRestore = srcClient.get(null, KEY1);
        assertThat(recordAfterSecondRestore).isNotNull();
        assertThat(recordAfterSecondRestore.getString(binName)).isEqualTo("stringValue");
    }

    @Test
    void backupRestoreWithEmptyListAndMap() {
        Key key = new Key(SOURCE_NAMESPACE, SET1, "emptyListMapKey");

        Map<String, Object> emptyMap = new HashMap<>(); // Empty map
        List<Object> emptyList = new ArrayList<>(); // Empty list

        Bin mapBin = new Bin("emptyMapBin", emptyMap);
        Bin listBin = new Bin("emptyListBin", emptyList);
        srcClient.put(null, key, mapBin, listBin);

        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, "emptyListMapBackup", DC, LOCAL_PORT).run();
        assertThat(backupResult.getRecordsRead()).isEqualTo(1);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        Record restoredRecord = srcClient.get(null, key);
        assertThat(restoredRecord).isNotNull();

        assertThat(restoredRecord.getMap("emptyMapBin")).isNotNull().isEmpty();
        assertThat(restoredRecord.getList("emptyListBin")).isNotNull().isEmpty();
    }

    @Test
    void restoreDoublePrecisionXdrTest() {
        srcClient.put(null, KEY1, new Bin(STRING_BIN, 2.779745911202054e-161));
        srcClient.put(null, KEY2, new Bin(STRING_BIN, 97.47637592329345));
        srcClient.put(null, KEY3, new Bin(STRING_BIN, 0.05972567867873778));

        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, "xdrDoublePrecisionBackup", DC, LOCAL_PORT).run();
        assertThat(backupResult.getRecordsRead()).isEqualTo(3);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        assertThat(srcClient.get(null, KEY1).getDouble(STRING_BIN)).isEqualTo(2.779745911202054e-161);
        assertThat(srcClient.get(null, KEY2).getDouble(STRING_BIN)).isEqualTo(97.47637592329345);
        assertThat(srcClient.get(null, KEY3).getDouble(STRING_BIN)).isEqualTo(0.05972567867873778);
    }

    @Test
    void xdrRestoreMixedTTLRecords() {
        Key keyShortTTL = new Key(SOURCE_NAMESPACE, SET1, "KeyWithShortTTL");
        Key keyLongTTL = new Key(SOURCE_NAMESPACE, SET1, "KeyWithLongTTL");
        Key keyNoExpire = new Key(SOURCE_NAMESPACE, SET1, "KeyWithNoExpiration");
        Key keyDefaultTTL = new Key(SOURCE_NAMESPACE, SET1, "KeyWithDefaultTTL");
        Key keyPreserveTTLNew = new Key(SOURCE_NAMESPACE, SET1, "KeyWithPreserveTTLNew");
        Key keyPreserveTTLUpdated = new Key(SOURCE_NAMESPACE, SET1, "KeyWithPreserveTTLUpdated");

        int shortTTL = 30;  // 30 seconds
        int longTTL = 300;  // 300 seconds
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

        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalAddress()
                .setDc(DC)
                .setLocalPort(LOCAL_PORT)
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
        int allowedDrift = 10;  // Allow a small-time drift

        assertThat(restoredShortTTL.expiration)
                .isBetween(originalShortTTL - allowedDrift, originalShortTTL + timeElapsedDuringBackupRestore);
        assertThat(restoredLongTTL.expiration)
                .isBetween(originalLongTTL - allowedDrift, originalLongTTL + timeElapsedDuringBackupRestore);
        assertThat(restoredDefaultTTL.expiration).isGreaterThan(0); // Should have server-defined TTL
        assertThat(restoredNoExpire.expiration).isEqualTo(0);
        assertThat(restoredPreserveTTLNew.expiration).isGreaterThan(0);
        assertThat(restoredPreserveTTLUpdated.expiration)
                .as("PreserveTTLUpdated expiration should match original within a small drift")
                .isBetween(originalPreserveTTLUpdated - allowedDrift, originalPreserveTTLUpdated + allowedDrift);
    }


    @Test
    void xdrBackupRestoreLargeBlob() {
        byte[] largeBlob = new byte[1_000_000]; // 1MB random data
        new Random().nextBytes(largeBlob);

        Key blobKey = new Key(SOURCE_NAMESPACE, SET1, "LargeBlobKey");
        Bin blobBin = new Bin("blobBin", Value.get(largeBlob));

        srcClient.put(null, blobKey, blobBin);

        Record originalRecord = srcClient.get(null, blobKey);
        assertThat(originalRecord.getValue("blobBin")).isEqualTo(largeBlob);

        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalAddress()
                .setDc(DC)
                .setLocalPort(LOCAL_PORT)
                .run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        Record restoredRecord = srcClient.get(null, blobKey);
        assertThat(restoredRecord).isNotNull();
        assertThat(restoredRecord.getValue("blobBin")).isEqualTo(largeBlob);
    }

    @Test
    void restoreUnique() {
        String originalValue = "originalValue" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, originalValue);

        String backupKey = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalAddress()
                .setDc(DC)
                .setLocalPort(LOCAL_PORT)
                .run().getBackupDir();

        String newValue = "newValue" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, newValue);

        CliRestore.on(SOURCE_NAMESPACE, backupKey).setNoGeneration().setUnique().run();

        // unique=true restore didn't change the record
        assertThat(srcClient.get(null, KEY1).getString(STRING_BIN)).isEqualTo(newValue);

        CliRestore.on(SOURCE_NAMESPACE, backupKey).setNoGeneration().run();

        // unique=false restore changed the record
        assertThat(srcClient.get(null, KEY1).getString(STRING_BIN)).isEqualTo(originalValue);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupKey).setNoGeneration().setUnique().run();

        // unique=true restore worked since it's after truncate
        assertThat(srcClient.get(null, KEY1).getString(STRING_BIN)).isEqualTo(originalValue);
    }

    @Test
    void restoreReplace() {
        String originalValueBin1 = "originalValueBin1" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, "Bin1", originalValueBin1);

        String backupKey = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalAddress()
                .setDc(DC)
                .setLocalPort(LOCAL_PORT)
                .run().getBackupDir();

        String updatedValueBin1 = "updatedValueBin1" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, "Bin1", updatedValueBin1);
        String originalValueBin2 = "originalValueBin2" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, "Bin2", originalValueBin2);

        CliRestore.on(SOURCE_NAMESPACE, backupKey).setNoGeneration().run();

        // Replace is false -> Bin2 didn't change
        assertThat(srcClient.get(null, KEY1).getString("Bin1")).isEqualTo(originalValueBin1);
        assertThat(srcClient.get(null, KEY1).getString("Bin2")).isEqualTo(originalValueBin2);

        CliRestore.on(SOURCE_NAMESPACE, backupKey).setNoGeneration().setReplace().run();

        // Replace is true -> Record replaced -> Bin2 deleted
        assertThat(srcClient.get(null, KEY1).getString("Bin1")).isEqualTo(originalValueBin1);
        assertThat(srcClient.get(null, KEY1).getString("Bin2")).isNull();
    }

    @Test
    void xdrRestoreEmptyBinName() {
        String emptyBinName = "";
        String value = "EmptyBinValue" + System.currentTimeMillis();

        AerospikeDataUtils.put(KEY1, emptyBinName, value);

        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, "emptyBinBackup", DC, LOCAL_PORT).run();
        assertThat(backupResult.getRecordsRead()).isEqualTo(1);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(emptyBinName)).isEqualTo(value);
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
        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, "twoRestoresInParallel", DC, LOCAL_PORT).run();

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