package api.backup.stress.lotsofsets;

import api.backup.BackupManager;
import api.backup.RestoreApi;
import api.backup.dto.BackgroundJob;
import api.backup.stress.StressRunner;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.ConfigParametersHandler;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.adr.AerospikeDataUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static utils.aerospike.adr.AerospikeDataUtils.getSourceClient;

//@Tag("ADR-STRESS-TEST")
class StressRestoreLotsOfSetsTest extends StressRunner {
    private static final String SET_NAME = "lotsOfSet";
    private static final String SOURCE_NAMESPACE = "source-ns15";
    private static final String SOURCE_CLUSTER_NAME = "StressRestoreLotsOfSetsTestSrcCluster";
    private static final String BACKUP_NAMESPACE = "adr-ns15";
    private static final String BACKUP_NAME = "StressRestoreLotsOfSetsTestContinuousBackup";
    private static final String POLICY_NAME = "StressRestoreLotsOfSetsTestPolicy";
    private static final String DC_NAME = "StressRestoreLotsOfSetsTestDC";
    private static final IAerospikeClient srcClient = getSourceClient();

    private final String STRING_BIN = "stringBin";
    private final int NUMBER_OF_SETS = 1022;
    private final int NUMBER_OF_KEYS_PER_SET = 50;
    private static int testLoopCount;

    @BeforeEach
    public void setUp() {
        setPerformanceVariables();
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);
        waitGroup.wait(minutesToWaitForAllStressClassesToFinishSetup); // wait for all tests to finish setup
    }

    @AfterAll
    public static void afterAll() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @Test
    void restoreNamespaceWithLotsOfSets() {
        for (int i = 0; i < testLoopCount; i++) {
            BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
            BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);

            List<String> allSets = prepareDataAndBackup();
            long afterFirstBackup = System.currentTimeMillis();

            AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

            BackgroundJob backgroundJob = RestoreApi.restoreNamespace(afterFirstBackup, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE);
            assertThat(backgroundJob.getProcessed()).isEqualTo(NUMBER_OF_SETS * NUMBER_OF_KEYS_PER_SET);

            int namespaceObjectCount = AerospikeCountUtils.getNamespaceObjectCount(srcClient, SOURCE_NAMESPACE);
            assertThat(namespaceObjectCount).isEqualTo(NUMBER_OF_SETS * NUMBER_OF_KEYS_PER_SET);
            // Assert that the value of all the records is at it was before the truncate
            for (String set : allSets) {
                for (int j = 0; j < NUMBER_OF_KEYS_PER_SET; j++) {
                    Key key = new Key(SOURCE_NAMESPACE, set, "IT" + j);
                    Record record = srcClient.get(null, key);
                    String value = record.getString(STRING_BIN);
                    assertThat(value).isEqualTo("RestoreTestInitialValue");
                }
            }
            BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
            BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);
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
