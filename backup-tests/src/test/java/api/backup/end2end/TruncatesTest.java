package api.backup.end2end;

import api.backup.BackupManager;
import api.backup.RestoreApi;
import api.backup.RetrieveAPI;
import api.backup.dto.RestoreSetRequest;
import api.backup.dto.RetrieveEntityRecord;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import utils.AutoUtils;
import utils.VersionValidator;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static utils.aerospike.adr.AerospikeDataUtils.getSourceClient;

@Tag("ADR-E2E")
class TruncatesTest extends BackupRunner {
    private static final String SET_NAME = "RestoreTruncatesTestSet";
    private static final String SET_NAME2 = "RestoreTruncatesTestSet2";
    private static final String SOURCE_NAMESPACE = "source-ns11";
    private static final String BACKUP_NAMESPACE = "adr-ns11";
    private static final String SOURCE_CLUSTER_NAME = "RestoreTruncatesTestSrcCluster";
    private static final String INITIAL_VALUE = "InitialValue";
    private static final String UPDATED_VALUE = "UpdatedValue";
    private static final String BACKUP_NAME = "RestoreTruncatesTestContinuousBackup";
    private static final String POLICY_NAME = "RestoreTruncatesTestPolicy";
    private static final String DC_NAME = "RestoreTruncatesDC";
    private static final Key RESTORE_TEST_KEY = new Key(SOURCE_NAMESPACE, SET_NAME, "IT1");
    private static final String RESTORE_TEST_DIGEST = AerospikeDataUtils.getDigestFromKey(RESTORE_TEST_KEY);
    private static final Key RESTORE_TEST_KEY_DIFFERENT_SET = new Key(SOURCE_NAMESPACE, SET_NAME2, "IT3");
    private static final int DELAY_FOR_TRUNCATES_BACKUP = 10_000;

    private static final IAerospikeClient srcClient = getSourceClient();
    private final static String REQUIRED_VERSION = "6.2";
    private final static String TRUNCATE_INFO_SUPPORTED = "TRUNCATE_INFO_SUPPORTED";
    private static final String STRING_BIN = "stringBin";
    private static volatile boolean isTruncateInfoSupported;

    @BeforeAll
    static void beforeAll() {
        isTruncateInfoSupported = VersionValidator.hasRequiredAerospikeVersion(srcClient, REQUIRED_VERSION);
        // Skip when running with older Aerospike versions
        System.setProperty(TRUNCATE_INFO_SUPPORTED, String.valueOf(isTruncateInfoSupported));
    }

    @AfterAll
    public static void afterAll() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @BeforeEach
    public void setUp() {
        // In restore related tests we have to wait before truncate and validate because the same records will
        // be restored to the source cluster and backed up to ADR again.
        AutoUtils.sleep(5000);
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);
    }

    @Test
    @EnabledIfSystemProperty(named = TRUNCATE_INFO_SUPPORTED, matches = "true")
    void truncateNSRestoreNS() {
        AerospikeDataUtils.put(RESTORE_TEST_KEY, STRING_BIN, INITIAL_VALUE);
        AerospikeDataUtils.put(RESTORE_TEST_KEY_DIFFERENT_SET, STRING_BIN, INITIAL_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY, 1);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY_DIFFERENT_SET, 1);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        awaitUntilTruncatesSaved();
        long afterTruncate = System.currentTimeMillis();
        AutoUtils.sleep(DELAY_FOR_TRUNCATES_BACKUP);

        AerospikeDataUtils.put(RESTORE_TEST_KEY, STRING_BIN, UPDATED_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY, 2);

        long restored = RestoreApi.restoreNamespace(0, afterTruncate, SOURCE_CLUSTER_NAME, SOURCE_CLUSTER_NAME,
                SOURCE_NAMESPACE, SOURCE_NAMESPACE).getProcessed();

        assertThat(restored).isEqualTo(2);
        Record record = srcClient.get(null, RESTORE_TEST_KEY);
        assertThat(record).isNull();
        Record record2 = srcClient.get(null, RESTORE_TEST_KEY_DIFFERENT_SET);
        assertThat(record2).isNull();
    }

    @Test
    @EnabledIfSystemProperty(named = TRUNCATE_INFO_SUPPORTED, matches = "true")
    void truncateSetRestoreNS() {
        AerospikeDataUtils.put(RESTORE_TEST_KEY, STRING_BIN, INITIAL_VALUE);
        AerospikeDataUtils.put(RESTORE_TEST_KEY_DIFFERENT_SET, STRING_BIN, INITIAL_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY, 1);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY_DIFFERENT_SET, 1);

        AerospikeDataUtils.truncateSourceSet(SOURCE_NAMESPACE, SET_NAME);
        awaitUntilTruncatesSaved();
        long afterTruncate = System.currentTimeMillis();
        AutoUtils.sleep(DELAY_FOR_TRUNCATES_BACKUP);

        AerospikeDataUtils.put(RESTORE_TEST_KEY, STRING_BIN, UPDATED_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY, 2);

        long restored = RestoreApi.restoreNamespace(0, afterTruncate, SOURCE_CLUSTER_NAME, SOURCE_CLUSTER_NAME,
                SOURCE_NAMESPACE, SOURCE_NAMESPACE).getProcessed();

        assertThat(restored).isEqualTo(2);
        Record record = srcClient.get(null, RESTORE_TEST_KEY);
        assertThat(record).isNull();
        Record recordOtherSet = srcClient.get(null, RESTORE_TEST_KEY_DIFFERENT_SET);
        assertThat(recordOtherSet).isNotNull();
    }

    @Test
    @EnabledIfSystemProperty(named = TRUNCATE_INFO_SUPPORTED, matches = "true")
    void truncateSetRestoreSet() {
        AerospikeDataUtils.put(RESTORE_TEST_KEY, STRING_BIN, INITIAL_VALUE);
        AerospikeDataUtils.put(RESTORE_TEST_KEY_DIFFERENT_SET, STRING_BIN, INITIAL_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY, 1);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY_DIFFERENT_SET, 1);

        AerospikeDataUtils.truncateSourceSet(SOURCE_NAMESPACE, SET_NAME);
        awaitUntilTruncatesSaved();
        long afterTruncate = System.currentTimeMillis();
        AutoUtils.sleep(DELAY_FOR_TRUNCATES_BACKUP);

        AerospikeDataUtils.put(RESTORE_TEST_KEY, STRING_BIN, UPDATED_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY, 2);

        long restored = RestoreApi.restoreSet(RestoreSetRequest.builder().fromTime(0).toTime(afterTruncate)
                .srcClusterName(SOURCE_CLUSTER_NAME).trgClusterName(SOURCE_CLUSTER_NAME).srcNS(SOURCE_NAMESPACE)
                .trgNS(SOURCE_NAMESPACE).set(SET_NAME).build()).getProcessed();

        assertThat(restored).isEqualTo(1);
        Record record = srcClient.get(null, RESTORE_TEST_KEY);
        assertThat(record).isNull();
        Record recordOtherSet = srcClient.get(null, RESTORE_TEST_KEY_DIFFERENT_SET);
        assertThat(recordOtherSet).isNotNull();
    }

    @Test
    @EnabledIfSystemProperty(named = TRUNCATE_INFO_SUPPORTED, matches = "true")
    void truncateNSRestoreSet() {
        AerospikeDataUtils.put(RESTORE_TEST_KEY, STRING_BIN, INITIAL_VALUE);
        AerospikeDataUtils.put(RESTORE_TEST_KEY_DIFFERENT_SET, STRING_BIN, INITIAL_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY, 1);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY_DIFFERENT_SET, 1);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        awaitUntilTruncatesSaved();
        long afterTruncate = System.currentTimeMillis();
        AutoUtils.sleep(DELAY_FOR_TRUNCATES_BACKUP);

        AerospikeDataUtils.put(RESTORE_TEST_KEY, STRING_BIN, UPDATED_VALUE);
        AerospikeDataUtils.put(RESTORE_TEST_KEY_DIFFERENT_SET, STRING_BIN, UPDATED_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY, 2);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY_DIFFERENT_SET, 2);

        long restored = RestoreApi.restoreSet(RestoreSetRequest.builder().fromTime(0).toTime(afterTruncate)
                .srcClusterName(SOURCE_CLUSTER_NAME).trgClusterName(SOURCE_CLUSTER_NAME).srcNS(SOURCE_NAMESPACE)
                .trgNS(SOURCE_NAMESPACE).set(SET_NAME).build()).getProcessed();

        assertThat(restored).isEqualTo(1);
        Record record = srcClient.get(null, RESTORE_TEST_KEY);
        assertThat(record).isNull();
        Record recordOtherSet = srcClient.get(null, RESTORE_TEST_KEY_DIFFERENT_SET);
        assertThat(recordOtherSet).isNotNull();
    }

    @Test
    @EnabledIfSystemProperty(named = TRUNCATE_INFO_SUPPORTED, matches = "true")
    void retrieveAll() {
        long before = System.currentTimeMillis();
        AerospikeDataUtils.put(RESTORE_TEST_KEY, STRING_BIN, INITIAL_VALUE);
        BackupManager.waitForBackup(BACKUP_NAME, RESTORE_TEST_KEY, 1);
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        awaitUntilTruncatesSaved();

        List<RetrieveEntityRecord> retrieve = RetrieveAPI.retrieve(RetrieveAPI.WhatToRetrieve.ALL, before,
                System.currentTimeMillis(), RESTORE_TEST_DIGEST, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, SET_NAME);
        assertThat(retrieve).hasSize(2); //one real backup and one delete
    }

    private static void awaitUntilTruncatesSaved() {
        Awaitility.await()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .untilAsserted(() -> {
                    List<RetrieveEntityRecord> retrieve = RetrieveAPI.retrieve(RetrieveAPI.WhatToRetrieve.LATEST, 0,
                            System.currentTimeMillis(), RESTORE_TEST_DIGEST, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, SET_NAME);
                    assertThat(retrieve).hasSize(1);
                    assertThat(retrieve.get(0).isDelete()).isTrue();
                });
    }

    @Test
    @EnabledIfSystemProperty(named = TRUNCATE_INFO_SUPPORTED, matches = "true")
    void truncateManySets() {
        int N = 1020;
        List<String> sets = Stream.concat( // aerospike limit 1023, one set is reserved of as-backup-queue
                Stream.of(SET_NAME, SET_NAME2),
                IntStream.range(0, N).mapToObj(it -> "testTruncateOfManySets" + it)
        ).toList();

        List<Key> keys = sets.stream()
                .map(it -> new Key(SOURCE_NAMESPACE, it, "key"))
                .toList();

        int backupsBefore = RetrieveAPI.retrieve(RetrieveAPI.WhatToRetrieve.ALL, 0,
                        System.currentTimeMillis(), AerospikeDataUtils.getDigestFromKey(keys.get(0)), SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, sets.get(0))
                .size();

        AerospikeDataUtils.put(RESTORE_TEST_KEY, STRING_BIN, INITIAL_VALUE);
        keys.forEach(it -> AerospikeDataUtils.put(it, "bin", INITIAL_VALUE));

        sets.forEach(it -> {
            getSourceClient().truncate(null, SOURCE_NAMESPACE, it, null);
        });

        Awaitility.await().untilAsserted(() -> {
            assertThat(AerospikeCountUtils.getNamespaceObjectCount(getSourceClient(), SOURCE_NAMESPACE)).isZero();
        });

        awaitUntilTruncatesSaved();
        AutoUtils.sleep(DELAY_FOR_TRUNCATES_BACKUP);

        int backupsAfter = RetrieveAPI.retrieve(RetrieveAPI.WhatToRetrieve.ALL, 0,
                System.currentTimeMillis(), AerospikeDataUtils.getDigestFromKey(keys.get(0)), SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, sets.get(0))
                        .size();
        assertThat(backupsAfter).isGreaterThanOrEqualTo(backupsBefore + 2);
    }
}
