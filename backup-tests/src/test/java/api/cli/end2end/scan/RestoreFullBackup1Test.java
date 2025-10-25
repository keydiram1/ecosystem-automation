package api.cli.end2end.scan;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import api.cli.RestoreResult;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import org.junit.jupiter.api.*;
import utils.AutoUtils;
import utils.aerospike.AerospikeScanner;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("CLI-BACKUP")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RestoreFullBackup1Test extends CliBackupRunner {
    private static final String STRING_BIN = "RestoreTestBin";
    private static final String SET1 = "SetFullBackupTest1";
    private static final String SET2 = "SetFullBackupTest2";
    private static final String SET3 = "SetFullBackupTest3";
    private static Key KEY1;
    private static Key KEY2;
    private static Key KEY3;
    private static final String SOURCE_NAMESPACE = "source-ns2";

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
    @Order(1)
    void restoreBackup() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);
        AutoUtils.sleep(1000);
        long startTime = System.currentTimeMillis();
        BackupResult createFirstValue = CliBackup.on(SOURCE_NAMESPACE, "createFirstValue").run();
        assertThat(createFirstValue.getRecordsRead()).isEqualTo(1);
        assertThat(createFirstValue.getFilesWritten()).isEqualTo(Integer.valueOf(1));
        assertThat(createFirstValue.getBytesWritten()).isGreaterThan(150).isLessThan(1000);
        assertThat(createFirstValue.getExitCode()).isEqualTo(0);
        assertThat(createFirstValue.getDurationMillis()).isGreaterThan(1).isLessThan(1000);
        if (AutoUtils.isRunningOnMacos()) {
            assertThat(createFirstValue.getStartTime())
                    .isBetween(startTime - 10_000, startTime + 10_000);
        } else {
            assertThat(createFirstValue.getStartTime())
                    .isBetween(startTime - 10_800_000 - 10_000, startTime - 10_800_000 + 10_000);
        }

        BackupResult keyNoChanges = CliBackup.on(SOURCE_NAMESPACE, "noChanges").run();
        assertThat(keyNoChanges.getBackupDir()).isNotEqualTo(createFirstValue.getBackupDir());

        String firstValueUpdate = "firstValueUpdate" + System.currentTimeMillis();
        String secondValueCreate = "secondValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueUpdate);
        AerospikeDataUtils.put(KEY2, STRING_BIN, secondValueCreate);
        BackupResult firstValueUpdateSecondValueCreate = CliBackup.on(SOURCE_NAMESPACE, "firstValueUpdateSecondValueCreate").run();
        assertThat(firstValueUpdateSecondValueCreate.getBytesWritten()).isGreaterThan(300).isLessThan(1000);

        String secondValueUpdate = "secondValueUpdate" + System.currentTimeMillis();
        String thirdValueCreate = "thirdValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY2, STRING_BIN, secondValueUpdate);
        AerospikeDataUtils.put(KEY3, STRING_BIN, thirdValueCreate);
        AerospikeDataUtils.delete(KEY1);
        BackupResult keyFirstValueDeleteSecondValueUpdateThirdValueCreate =
                CliBackup.on(SOURCE_NAMESPACE, "keyFirstValueDeleteSecondValueUpdateThirdValueCreate").run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, keyNoChanges.getBackupDir()).setNoRecords().run();
        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNull();

        CliRestore.on(SOURCE_NAMESPACE, keyNoChanges.getBackupDir()).run();
        retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);

        CliRestore.on(SOURCE_NAMESPACE, createFirstValue.getBackupDir()).run();
        retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);

        CliRestore.on(SOURCE_NAMESPACE, firstValueUpdateSecondValueCreate.getBackupDir()).run();
        retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueUpdate);
        retrievedRecord = srcClient.get(null, KEY2);
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(secondValueCreate);
        retrievedRecord = srcClient.get(null, KEY3);
        assertThat(retrievedRecord).isNull();

        CliRestore.on(SOURCE_NAMESPACE, keyFirstValueDeleteSecondValueUpdateThirdValueCreate.getBackupDir()).run();
        retrievedRecord = srcClient.get(null, KEY1);
        // For now, we don't support delete actions in incremental backup so the first record will stay as it was.
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueUpdate);
        retrievedRecord = srcClient.get(null, KEY2);
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(secondValueUpdate);
        retrievedRecord = srcClient.get(null, KEY3);
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(thirdValueCreate);
    }

    @Test
    @Order(2)
    void testRestoreWithGenerationOption() {
        // Create a record and update it till the generation value is 1
        String originalValue = "originalValue" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, originalValue);
        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord.generation).isEqualTo(1);

        // Update the record until the generation value is 2
        AerospikeDataUtils.put(KEY1, STRING_BIN, "secondValue");
        retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord.generation).isEqualTo(2);

        // Save backup key when generation=2
        String backupKey = CliBackup.on(SOURCE_NAMESPACE, "testRestoreWithGenerationOptionBackup1").run().getBackupDir();

        // Update the record until the generation value is 3
        AerospikeDataUtils.put(KEY1, STRING_BIN, "thirdValue");
        retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord.generation).isEqualTo(3);

        CliRestore.on(SOURCE_NAMESPACE, backupKey).run();

        retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord.generation).isEqualTo(3);

        // no truncate && noGeneration=true
        CliRestore.on(SOURCE_NAMESPACE, backupKey).setNoGeneration().run();
        retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord.generation).isEqualTo(4);

        // truncate && noGeneration=false
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        CliRestore.on(SOURCE_NAMESPACE, backupKey).run();
        retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.generation).isEqualTo(1);

        // truncate && noGeneration=true
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        CliRestore.on(SOURCE_NAMESPACE, backupKey).setNoGeneration().run();
        retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.generation).isEqualTo(1);
    }

    @Test
    @Order(3)
    void testRestoreWithUniqueOption2() {
        String originalValue = "originalValue" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, originalValue);

        String backupKey = CliBackup.on(SOURCE_NAMESPACE, "testRestoreWithUniqueOptionBackup1").run().getBackupDir();

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
    @Order(4)
    void testRestoreWithReplaceOption() {
        String originalValueBin1 = "originalValueBin1" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, "Bin1", originalValueBin1);

        String backupKey = CliBackup.on(SOURCE_NAMESPACE, "testRestoreWithReplaceOptionBackup1").run().getBackupDir();

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
    @Order(6)
    void restoreToAnotherNamespace() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);
        String backupKey = CliBackup.on(SOURCE_NAMESPACE, "restoreToAnotherNamespaceBackup1").run().getBackupDir();

        // source-ns15 should be available for testing.
        AerospikeDataUtils.truncateSourceNamespace("source-ns15");
        CliRestore.on(SOURCE_NAMESPACE, "source-ns15", backupKey).setNoGeneration().setReplace().run();

        Key keyForNewNS = new Key("source-ns15", SET1, "IT1");
        Record retrievedRecord = srcClient.get(null, keyForNewNS);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
    }

    @Test
    void restoreExpiredRecord() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        String secondValueCreate = "secondValueCreate" + System.currentTimeMillis();
        String thirdValueCreate = "thirdValueCreate" + System.currentTimeMillis();

        WritePolicy policy = new WritePolicy();
        policy.setExpiration(10);
        AerospikeDataUtils.put(policy, KEY1, STRING_BIN, firstValueCreate);

        policy.setExpiration(0);
        AerospikeDataUtils.put(policy, KEY2, STRING_BIN, secondValueCreate);

        policy.setExpiration(-1);
        AerospikeDataUtils.put(policy, KEY3, STRING_BIN, thirdValueCreate);

        String backupKey = CliBackup.on(SOURCE_NAMESPACE, "restoreExpiredRecordBackup1").run().getBackupDir();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        AutoUtils.sleep(11_000);

        RestoreResult restoreResult = CliRestore.on(SOURCE_NAMESPACE, backupKey).run();

        assertThat(restoreResult.getExpiredRecords()).isEqualTo(1);

        Record retrievedRecord1 = srcClient.get(null, KEY1);
        assertThat(retrievedRecord1).isNull();

        CliRestore.on(SOURCE_NAMESPACE, backupKey).setExtraTtl(300).run();

        retrievedRecord1 = srcClient.get(null, KEY1);
        assertThat(retrievedRecord1).isNotNull();
        int ttl1 = retrievedRecord1.getTimeToLive();
        // The initial ttl was 10 and the extra ttl was 300 so the ttl must be lower than 310.
        assertThat(ttl1).isLessThan(310).isGreaterThan(200);

        Record retrievedRecord2 = srcClient.get(null, KEY2);
        assertThat(retrievedRecord2).isNotNull();
        int ttl2 = retrievedRecord2.getTimeToLive();
        // the ttl default value is 30 days
        assertThat(ttl2).isGreaterThan(29 * 86400).isLessThan(31 * 86400);

        Record retrievedRecord3 = srcClient.get(null, KEY3);
        assertThat(retrievedRecord3).isNotNull();
        int ttl3 = retrievedRecord3.getTimeToLive();
        assertThat(ttl3).isEqualTo(-1);
    }

    @Test
    void restoreWithExistedRecord() {
        AerospikeDataUtils.put(KEY1, STRING_BIN, "initial");

        String backupKey = CliBackup.on(SOURCE_NAMESPACE, "restoreWithExistedRecordBackup1").run().getBackupDir();

        AerospikeDataUtils.put(KEY1, STRING_BIN, "updated");

        RestoreResult restoreResult = CliRestore.on(SOURCE_NAMESPACE, backupKey).setUnique().run();

        assertThat(restoreResult.getExistedRecords()).isEqualTo(1);
        assertThat(srcClient.get(null, KEY1).getString(STRING_BIN)).isEqualTo("updated");
    }

    @Test
    void restoreWithFailedFresher() {
        AerospikeDataUtils.put(KEY1, STRING_BIN, "value");

        String backupKey = CliBackup.on(SOURCE_NAMESPACE, "restoreWithFailedFresherBackup1").run().getBackupDir();

        AerospikeDataUtils.put(KEY1, STRING_BIN, "updatedValue");

        RestoreResult restoreResult = CliRestore.on(SOURCE_NAMESPACE, backupKey).run();

        // was not updated during restore because of no-generation = false by default.
        assertThat(srcClient.get(null, KEY1).getString(STRING_BIN)).isEqualTo("updatedValue");
        assertThat(restoreResult.getFresherRecords()).isEqualTo(1); // One record should fail (fresher)
    }

    @Test
    void restoreUserKey() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        WritePolicy writePolicy = new WritePolicy();
        writePolicy.setSendKey(true);
        AerospikeDataUtils.put(writePolicy, KEY1, STRING_BIN, firstValueCreate);
        AerospikeScanner scanner = new AerospikeScanner();
        scanner.scanKeys(srcClient, SOURCE_NAMESPACE, SET1);
        assertThat(scanner.getAllKeys().size()).isEqualTo(1);

        String backupKey = CliBackup.on(SOURCE_NAMESPACE, "restoreUserKeyBackup").run().getBackupDir();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        scanner.scanKeys(srcClient, SOURCE_NAMESPACE, SET1);
        assertThat(scanner.getAllKeys().size()).isEqualTo(0);

        CliRestore.on(SOURCE_NAMESPACE, backupKey).run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);

        scanner.scanKeys(srcClient, SOURCE_NAMESPACE, SET1);
        assertThat(scanner.getAllKeys().size()).isEqualTo(1);
    }

    @Test
    void restoreDoublePrecisionTest() {
        AerospikeDataUtils.put(KEY1, STRING_BIN, 2.779745911202054e-161);
        AerospikeDataUtils.put(KEY2, STRING_BIN, 97.47637592329345);
        AerospikeDataUtils.put(KEY3, STRING_BIN, 0.05972567867873778);

        String backupKey = CliBackup.on(SOURCE_NAMESPACE, "restoreDoublePrecisionTest").run().getBackupDir();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupKey).run();

        assertThat(srcClient.get(null, KEY1).getDouble(STRING_BIN)).isEqualTo(2.779745911202054e-161);
        assertThat(srcClient.get(null, KEY2).getDouble(STRING_BIN)).isEqualTo(97.47637592329345);
        assertThat(srcClient.get(null, KEY3).getDouble(STRING_BIN)).isEqualTo(0.05972567867873778);
    }

    @Test
    void restoreEmptyString() {
        String emptyValue = "";
        AerospikeDataUtils.put(KEY1, STRING_BIN, emptyValue);
        AutoUtils.sleep(2000);

        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, "emptyStringBackup").run();

        assertThat(backupResult.getRecordsRead()).isEqualTo(1);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(emptyValue);
    }

    @Test
    void scanRestoreEmptySetName() {
        Key keyWithoutSet = new Key(SOURCE_NAMESPACE, "", "NoSetKey");
        String value = "NoSetValue" + System.currentTimeMillis();

        AerospikeDataUtils.put(keyWithoutSet, STRING_BIN, value);

        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, "noSetBackup").run();

        assertThat(backupResult.getRecordsRead()).isEqualTo(1);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        Record retrievedRecord = srcClient.get(null, keyWithoutSet);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(value);
    }
}