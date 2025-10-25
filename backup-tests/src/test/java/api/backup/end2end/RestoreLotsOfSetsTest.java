package api.backup.end2end;

import api.backup.BackupManager;
import api.backup.RestoreApi;
import api.backup.dto.BackgroundJob;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static utils.aerospike.adr.AerospikeDataUtils.getSourceClient;

@Tag("ADR-E2E")
class RestoreLotsOfSetsTest extends BackupRunner {
    private static final String SET_NAME = "lotsOfSet";
    private static final String SOURCE_NAMESPACE = "source-ns16";
    private static final String SOURCE_CLUSTER_NAME = "RestoreLotsOfSetsTestSrcCluster";
    private static final String BACKUP_NAMESPACE = "adr-ns16";
    private static final String BACKUP_NAME = "RestoreLotsOfSetsTestContinuousBackup";
    private static final String POLICY_NAME = "RestoreLotsOfSetsTestPolicy";
    private static final String DC_NAME = "RestoreLotsOfSetsTestDC";
    private static final IAerospikeClient srcClient = getSourceClient();

    private final String STRING_BIN = "stringBin";
    private final int NUMBER_OF_SETS = 20;
    private final int NUMBER_OF_KEYS_PER_SET = 3;

    @BeforeEach
    public void setUp() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);
    }

    @AfterAll
    public static void afterAll() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @Test
    void restoreNamespaceWithLotsOfSets() {
        List<String> allSets = prepareDataAndBackup();
        long afterFirstBackup = System.currentTimeMillis();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        BackgroundJob backgroundJob = RestoreApi.restoreNamespace(afterFirstBackup, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE);
        assertThat(backgroundJob.getProcessed()).isEqualTo(NUMBER_OF_SETS * NUMBER_OF_KEYS_PER_SET);

        int namespaceObjectCount = AerospikeCountUtils.getNamespaceObjectCount(srcClient, SOURCE_NAMESPACE);
        assertThat(namespaceObjectCount).isEqualTo(NUMBER_OF_SETS * NUMBER_OF_KEYS_PER_SET);
        // Assert that the value of all the records is at it was before the truncate
        for (String set : allSets) {
            for (int i = 0; i < NUMBER_OF_KEYS_PER_SET; i++) {
                Key key = new Key(SOURCE_NAMESPACE, set, "IT" + i);
                Record record = srcClient.get(null, key);
                String value = record.getString(STRING_BIN);
                assertThat(value).isEqualTo("RestoreTestInitialValue");
            }
        }
    }

    private List<String> prepareDataAndBackup() {
        List<String> allSets = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_SETS; i++) {
            allSets.add(SET_NAME + i);
        }

        Set<Key> keys = new HashSet<>();
        for (String set : allSets) {
            for (int i = 0; i < NUMBER_OF_KEYS_PER_SET; i++) {
                Key key = new Key(SOURCE_NAMESPACE, set, "IT" + i);
                keys.add(key);
            }
        }

        for (Key key : keys) {
            AerospikeDataUtils.put(key, STRING_BIN, "RestoreTestInitialValue");
        }

        Awaitility.await()
                .atMost(2, TimeUnit.MINUTES)
                .until(() -> {
                    Key[] toCheck = keys.stream().limit(30).toArray(Key[]::new);
                    List<String> backupsExist = BackupManager.backupForKeysExist(BACKUP_NAME, toCheck);
                    keys.removeIf(key -> backupsExist.contains(HexFormat.of().formatHex(key.digest)));
                    return keys.isEmpty();
                });

        return allSets;
    }
}
