package api.abs.end2end.fullBackup;

import api.abs.AbsBackupApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import api.abs.JobID;
import api.abs.generated.model.DtoRestoreJobStatus;
import api.abs.generated.model.DtoRestoreNamespace;
import api.abs.generated.model.DtoRestorePolicy;
import api.abs.generated.model.DtoRestoreRequest;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import org.junit.jupiter.api.*;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.ConfigParametersHandler;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.AerospikeScanner;
import utils.aerospike.abs.AerospikeDataUtils;

import java.util.List;
import java.util.UUID;

import static api.abs.generated.model.DtoCompressionPolicy.ModeEnum.NONE;
import static api.abs.generated.model.DtoJobStatus.JobStatusDone;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-E2E")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RestoreFullBackup1Test extends AbsRunner {
    private static final String STRING_BIN = "RestoreTestBin";
    private static final String SET1 = "SetFullBackupTest1";
    private static final String SET2 = "SetFullBackupTest2";
    private static final String SET3 = "SetFullBackupTest3";
    private static final String ROUTINE_NAME = "fullBackup1";
    private static Key KEY1;
    private static Key KEY2;
    private static Key KEY3;
    private static String SOURCE_NAMESPACE;
    private final static String DESTINATION_NS = "source-ns8";

    @BeforeAll
    static void setUp() {
        SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
        KEY2 = new Key(SOURCE_NAMESPACE, SET2, "IT2");
        KEY3 = new Key(SOURCE_NAMESPACE, SET3, "IT3");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        assertThat(AerospikeCountUtils.getNamespaceObjectCount(srcClient, SOURCE_NAMESPACE)).isEqualTo(0);
    }

    @Test
    void restoreEmpty() {
        var emptyBackup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        DtoRestoreJobStatus restoreStatus = AbsRestoreApi.restoreFullSync(emptyBackup.getKey(), ROUTINE_NAME);
        assertThat(restoreStatus.getStatus()).isEqualTo(JobStatusDone);
        assertThat(restoreStatus.getInsertedRecords()).isZero();
        assertThat(restoreStatus.getReadRecords()).isZero();
    }

    @Test
    @Order(1)
    void restoreByPathBackup() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);
        AutoUtils.sleepOnCloud(3000);

        AerospikeLogger.info("Number of records in ns before backup=" + AerospikeCountUtils.getNamespaceObjectCount(srcClient, SOURCE_NAMESPACE));
        var createFirstValue = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);

        assertThat(createFirstValue.getRecordCount()).isEqualTo(1);
        assertThat(createFirstValue.getNamespace()).isEqualTo(SOURCE_NAMESPACE);
        assertThat(createFirstValue.getByteCount()).isGreaterThan(150).isLessThan(1000);
        assertThat(createFirstValue.getEncryption()).isEqualTo(NONE.toString());
        assertThat(createFirstValue.getCompression()).isEqualTo(NONE.toString());
        assertThat(createFirstValue.getTimestamp())
                .isEqualTo(AbsBackupApi.parseDate(createFirstValue.getCreated()));

        AutoUtils.sleepOnCloud(3000);
        String keyNoChanges = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();
        assertThat(keyNoChanges).isNotEqualTo(createFirstValue.getKey());

        String firstValueUpdate = "firstValueUpdate" + System.currentTimeMillis();
        String secondValueCreate = "secondValueCreate" + System.currentTimeMillis();
        AutoUtils.sleepOnCloud(2000);
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueUpdate);
        AutoUtils.sleepOnCloud(2000);
        AerospikeDataUtils.put(KEY2, STRING_BIN, secondValueCreate);
        AutoUtils.sleepOnCloud(2000);
        var firstValueUpdateSecondValueCreate = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        assertThat(firstValueUpdateSecondValueCreate.getByteCount()).isGreaterThan(300).isLessThan(1000);

        AutoUtils.sleepOnCloud(3000);
        String secondValueUpdate = "secondValueUpdate" + System.currentTimeMillis();
        String thirdValueCreate = "thirdValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY2, STRING_BIN, secondValueUpdate);
        AerospikeDataUtils.put(KEY3, STRING_BIN, thirdValueCreate);
        AerospikeDataUtils.delete(KEY1);
        String keyFirstValueDeleteSecondValueUpdateThirdValueCreate = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        DtoRestorePolicy policy = new DtoRestorePolicy().parallel(Integer.valueOf(ConfigParametersHandler.getParameter("CONFIG_RESTORE_PARALLEL"))).noRecords(true);
        AbsRestoreApi.restoreFullSync(keyNoChanges, ROUTINE_NAME, policy);
        Record retrievedRecord = AerospikeDataUtils.get(KEY1);
        assertThat(retrievedRecord).isNull();

        DtoRestoreJobStatus restoreStatus = AbsRestoreApi.restoreFullSync(keyNoChanges, ROUTINE_NAME);

        assertThat(restoreStatus.getReadRecords()).isEqualTo(1);
        assertThat(restoreStatus.getTotalBytes()).isGreaterThan(0).isLessThan(1000);
        retrievedRecord = AerospikeDataUtils.get(KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);

        AbsRestoreApi.restoreFullSync(createFirstValue.getKey(), ROUTINE_NAME);

        retrievedRecord = AerospikeDataUtils.get(KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);

        AbsRestoreApi.restoreFullSync(firstValueUpdateSecondValueCreate.getKey(), ROUTINE_NAME);
        retrievedRecord = AerospikeDataUtils.get(KEY1);
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueUpdate);
        retrievedRecord = AerospikeDataUtils.get(KEY2);
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(secondValueCreate);
        retrievedRecord = AerospikeDataUtils.get(KEY3);
        assertThat(retrievedRecord).isNull();

        AbsRestoreApi.restoreFullSync(keyFirstValueDeleteSecondValueUpdateThirdValueCreate, ROUTINE_NAME);
        retrievedRecord = AerospikeDataUtils.get(KEY1);
        // For now, we don't support delete actions in incremental backup so the first record will stay as it was.
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueUpdate);
        retrievedRecord = AerospikeDataUtils.get(KEY2);
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(secondValueUpdate);
        retrievedRecord = AerospikeDataUtils.get(KEY3);
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(thirdValueCreate);

        // The user key shouldn't be restored since writePolicy.setSendKey default value is false
        AerospikeScanner scanner = new AerospikeScanner();
        scanner.scanKeys(srcClient, SOURCE_NAMESPACE, SET1);
        AerospikeLogger.info("Number of keys: " + scanner.getAllKeys().size());
        assertThat(scanner.getAllKeys().size()).isEqualTo(0);
    }

    @Test
    @Order(2)
    void testRestoreWithGenerationOption() {
        // Create a record and update it till the generation value is 1
        String originalValue = "originalValue" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, originalValue);
        Record retrievedRecord = AerospikeDataUtils.get(KEY1);
        assertThat(retrievedRecord.generation).isEqualTo(1);

        // Update the record until the generation value is 2
        AerospikeDataUtils.put(KEY1, STRING_BIN, "secondValue");
        retrievedRecord = AerospikeDataUtils.get(KEY1);
        assertThat(retrievedRecord.generation).isEqualTo(2);

        // Save backup key when generation=2
        String backupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();

        // Update the record until the generation value is 3
        AerospikeDataUtils.put(KEY1, STRING_BIN, "thirdValue");
        retrievedRecord = AerospikeDataUtils.get(KEY1);
        assertThat(retrievedRecord.generation).isEqualTo(3);

        // no truncate && noGeneration=false
        DtoRestorePolicy policyNoGeneration = new DtoRestorePolicy()
                .parallel(Integer.valueOf(ConfigParametersHandler.getParameter("CONFIG_RESTORE_PARALLEL")))
                .noGeneration(false);
        AbsRestoreApi.restoreFullSync(backupKey, ROUTINE_NAME, policyNoGeneration);
        retrievedRecord = AerospikeDataUtils.get(KEY1);
        assertThat(retrievedRecord.generation).isEqualTo(3);

        // no truncate && noGeneration=true
        DtoRestorePolicy policyGeneration = new DtoRestorePolicy()
                .parallel(Integer.valueOf(ConfigParametersHandler.getParameter("CONFIG_RESTORE_PARALLEL")))
                .noGeneration(true);
        AbsRestoreApi.restoreFullSync(backupKey, ROUTINE_NAME, policyGeneration);
        retrievedRecord = AerospikeDataUtils.get(KEY1);
        assertThat(retrievedRecord.generation).isEqualTo(4);

        // truncate && noGeneration=false
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        AbsRestoreApi.restoreFullSync(backupKey, ROUTINE_NAME, policyNoGeneration);
        retrievedRecord = AerospikeDataUtils.get(KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.generation).isEqualTo(1);

        // truncate && noGeneration=true
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        AbsRestoreApi.restoreFullSync(backupKey, ROUTINE_NAME, policyGeneration);
        retrievedRecord = AerospikeDataUtils.get(KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.generation).isEqualTo(1);

    }

    @Test
    @Order(3)
    void testRestoreWithUniqueOption() {
        String originalValue = "originalValue" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, originalValue);

        String backupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();

        String newValue = "newValue" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, newValue);

        DtoRestorePolicy policyUnique = new DtoRestorePolicy()
                .parallel(Integer.valueOf(ConfigParametersHandler.getParameter("CONFIG_RESTORE_PARALLEL")))
                .unique(true);
        AbsRestoreApi.restoreFullSync(backupKey, ROUTINE_NAME, policyUnique);

        // unique=true restore didn't change the record
        assertThat(AerospikeDataUtils.get(KEY1).getString(STRING_BIN)).isEqualTo(newValue);

        DtoRestorePolicy policyNonUnique = new DtoRestorePolicy()
                .noGeneration(true);
        AbsRestoreApi.restoreFullSync(backupKey, ROUTINE_NAME, policyNonUnique);

        // unique=false restore changed the record
        assertThat(AerospikeDataUtils.get(KEY1).getString(STRING_BIN)).isEqualTo(originalValue);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        AbsRestoreApi.restoreFullSync(backupKey, ROUTINE_NAME, policyUnique);

        // unique=true restore worked since it's after truncate
        assertThat(AerospikeDataUtils.get(KEY1).getString(STRING_BIN)).isEqualTo(originalValue);
    }

    @Test
    @Order(4)
    void testRestoreWithReplaceOption() {
        String originalValueBin1 = "originalValueBin1" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, "Bin1", originalValueBin1);

        String backupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();

        String updatedValueBin1 = "updatedValueBin1" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, "Bin1", updatedValueBin1);
        String originalValueBin2 = "originalValueBin2" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, "Bin2", originalValueBin2);

        DtoRestorePolicy policyNoReplace = new DtoRestorePolicy().parallel(Integer.valueOf(ConfigParametersHandler.getParameter("CONFIG_RESTORE_PARALLEL"))).noGeneration(true).unique(false);
        AbsRestoreApi.restoreFullSync(backupKey, ROUTINE_NAME, policyNoReplace);

        // Replace is false -> Bin2 didn't change
        assertThat(AerospikeDataUtils.get(KEY1).getString("Bin1")).isEqualTo(originalValueBin1);
        assertThat(AerospikeDataUtils.get(KEY1).getString("Bin2")).isEqualTo(originalValueBin2);

        // Replace is true -> Record replaced -> Bin2 deleted
        DtoRestorePolicy policyReplace = new DtoRestorePolicy().parallel(Integer.valueOf(ConfigParametersHandler.getParameter("CONFIG_RESTORE_PARALLEL"))).noGeneration(true).replace(true);
        AbsRestoreApi.restoreFullSync(backupKey, ROUTINE_NAME, policyReplace);

        assertThat(AerospikeDataUtils.get(KEY1).getString("Bin1")).isEqualTo(originalValueBin1);
        assertThat(AerospikeDataUtils.get(KEY1).getString("Bin2")).isNull();
    }

    @Test
    @Order(6)
    void restoreToAnotherNamespace() {
        String data = UUID.randomUUID().toString();
        AerospikeDataUtils.put(KEY1, STRING_BIN, data);
        var createFirstValue = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();
        AerospikeDataUtils.truncateSourceSet(DESTINATION_NS, KEY1.setName);

        DtoRestorePolicy policy = new DtoRestorePolicy().namespace(
                new DtoRestoreNamespace().source(SOURCE_NAMESPACE).destination(DESTINATION_NS)
        );
        AbsRestoreApi.restoreFullSync(createFirstValue, ROUTINE_NAME, policy);
        Key keyForNewNS = new Key(DESTINATION_NS, SET1, "IT1");
        Record retrievedRecord = AerospikeDataUtils.get(keyForNewNS);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(data);
    }

    @Test
    void getBackupsInRange() {
        long beforeBackups = System.currentTimeMillis();
        AutoUtils.sleepOnCloud(2000);
        AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        long afterTwoBackups = System.currentTimeMillis();
        AutoUtils.sleepOnCloud(2000);
        AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        long afterFourBackups = System.currentTimeMillis();

        int allBackups = AbsBackupApi.getFullBackupsInRange(ROUTINE_NAME, beforeBackups, null).size();
        assertThat(allBackups).isEqualTo(4);
        allBackups = AbsBackupApi.getAllFullBackupsInRange(beforeBackups, null).get(ROUTINE_NAME).size();
        assertThat(allBackups).isEqualTo(4);

        int firstTwoBackups = AbsBackupApi.getFullBackupsInRange(ROUTINE_NAME, beforeBackups, afterTwoBackups).size();
        assertThat(firstTwoBackups).isEqualTo(2);
        firstTwoBackups = AbsBackupApi.getAllFullBackupsInRange(beforeBackups, afterTwoBackups).get(ROUTINE_NAME).size();
        assertThat(firstTwoBackups).isEqualTo(2);

        int lastTwoBackups = AbsBackupApi.getFullBackupsInRange(ROUTINE_NAME, afterTwoBackups, afterFourBackups).size();
        assertThat(lastTwoBackups).isEqualTo(2);
        lastTwoBackups = AbsBackupApi.getAllFullBackupsInRange(afterTwoBackups, afterFourBackups).get(ROUTINE_NAME).size();
        assertThat(lastTwoBackups).isEqualTo(2);
    }

    @Test
    void scheduleFullBackup() {
        long startTime = System.currentTimeMillis();
        AbsBackupApi.scheduleFullBackup(ROUTINE_NAME, 5_000);
        AutoUtils.sleep(1_000);
        var currentBackups = AbsBackupApi.getCurrentBackup(ROUTINE_NAME);
        assertThat(currentBackups.getFull()).isNull(); // no backup is running;
        AbsBackupApi.waitForFullBackup(ROUTINE_NAME, startTime);
        long duration = System.currentTimeMillis() - startTime;

        AerospikeLogger.info("Backup took " + duration + " milliseconds");
        assertThat(duration).isGreaterThan(5_000);
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

        var backup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        AutoUtils.sleep(11_000);

        DtoRestoreJobStatus restoreStatus = AbsRestoreApi.restoreFullSync(backup.getKey(), ROUTINE_NAME);

        assertThat(restoreStatus.getExpiredRecords()).isEqualTo(1);

        Record retrievedRecord1 = AerospikeDataUtils.get(KEY1);
        assertThat(retrievedRecord1).isNull();

        JobID jobID = AbsRestoreApi.restoreFull(backup.getKey(), ROUTINE_NAME, new DtoRestorePolicy().extraTtl(300));
        AbsRestoreApi.waitForRestore(jobID);

        retrievedRecord1 = AerospikeDataUtils.get(KEY1);
        assertThat(retrievedRecord1).isNotNull();
        int ttl1 = retrievedRecord1.getTimeToLive();
        // The initial ttl was 10 and the extra ttl was 300 so the ttl must be lower than 310.
        assertThat(ttl1).isLessThan(310).isGreaterThan(200);

        Record retrievedRecord2 = AerospikeDataUtils.get(KEY2);
        assertThat(retrievedRecord2).isNotNull();
        int ttl2 = retrievedRecord2.getTimeToLive();
        // the ttl default value is 30 days
        assertThat(ttl2).isGreaterThan(29 * 86400).isLessThan(31 * 86400);

        Record retrievedRecord3 = AerospikeDataUtils.get(KEY3);
        assertThat(retrievedRecord3).isNotNull();
        int ttl3 = retrievedRecord3.getTimeToLive();
        assertThat(ttl3).isEqualTo(-1);
    }

    @Test
    void restoreWithSkippedRecords() {
        AerospikeDataUtils.put(KEY1, STRING_BIN, "valueForSkippedTest");

        var backup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        DtoRestorePolicy restorePolicy = new DtoRestorePolicy()
                .binList(List.of("SelectedBin"));
        DtoRestoreJobStatus restoreStatus = AbsRestoreApi.restoreFullSync(backup.getKey(), ROUTINE_NAME, restorePolicy);
        assertThat(AerospikeDataUtils.get(KEY1)).isNull();
        assertThat(restoreStatus.getSkippedRecords()).isEqualTo(1); // One record should be skipped
    }

    @Test
    void restoreWithExistedRecord() {
        AerospikeDataUtils.put(KEY1, STRING_BIN, "initial");

        var backup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);

        AerospikeDataUtils.put(KEY1, STRING_BIN, "updated");

        DtoRestorePolicy restorePolicy = new DtoRestorePolicy();
        restorePolicy.unique(true);
        DtoRestoreJobStatus restoreStatus = AbsRestoreApi.restoreFullSync(backup.getKey(), ROUTINE_NAME, restorePolicy);

        assertThat(restoreStatus.getExistedRecords()).isEqualTo(1);
        assertThat(AerospikeDataUtils.get(KEY1).getString(STRING_BIN)).isEqualTo("updated");
    }

    @Test
    void restoreWithFailedFresher() {
        AerospikeDataUtils.put(KEY1, STRING_BIN, "value");

        var backup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);

        AerospikeDataUtils.put(KEY1, STRING_BIN, "updatedValue");

        DtoRestoreJobStatus restoreStatus = AbsRestoreApi.restoreFullSync(backup.getKey(), ROUTINE_NAME, new DtoRestorePolicy());

        // was not updated during restore because of no-generation = false by default.
        assertThat(AerospikeDataUtils.get(KEY1).getString(STRING_BIN)).isEqualTo("updatedValue");
        assertThat(restoreStatus.getFresherRecords()).isEqualTo(1); // One record should fail (fresher)
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

        String backupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        scanner.scanKeys(srcClient, SOURCE_NAMESPACE, SET1);
        assertThat(scanner.getAllKeys().size()).isEqualTo(0);

        AbsRestoreApi.restoreFullSync(backupKey, ROUTINE_NAME);

        Record retrievedRecord = AerospikeDataUtils.get(KEY1);
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

        var backup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        AbsRestoreApi.restoreFullSync(backup.getKey(), ROUTINE_NAME);

        assertThat(AerospikeDataUtils.get(KEY1).getDouble(STRING_BIN)).isEqualTo(2.779745911202054e-161);
        assertThat(AerospikeDataUtils.get(KEY2).getDouble(STRING_BIN)).isEqualTo(97.47637592329345);
        assertThat(AerospikeDataUtils.get(KEY3).getDouble(STRING_BIN)).isEqualTo(0.05972567867873778);
    }

    @Test
    void restoreWithoutPolicy() {
        AerospikeDataUtils.put(KEY1, STRING_BIN, "someValue");

        var backup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);

        DtoRestoreJobStatus restoreStatus = AbsRestoreApi.restoreFullSync(backup.getKey(), ROUTINE_NAME, (DtoRestorePolicy) null);

        assertThat(restoreStatus.getFresherRecords()).isEqualTo(1);
    }

    @Test
    void restoreWithClusterAndStorageByName() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        String backupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        DtoRestoreRequest restoreRequest = new DtoRestoreRequest()
                .sourceName(AbsRunner.absStorageName)
                .destinationName("absDefaultCluster")
                .backupDataPath(backupKey)
                .policy(new DtoRestorePolicy());

        AbsRestoreApi.restoreFullSync(restoreRequest);

        Record retrievedRecord = AerospikeDataUtils.get(KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
    }
}