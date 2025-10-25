package api.abs.negative;

import api.abs.*;
import api.abs.generated.model.DtoBackupPolicy;
import api.abs.generated.model.DtoBackupRoutine;
import api.abs.generated.model.DtoLocalStorage;
import api.abs.generated.model.DtoStorage;
import com.aerospike.client.Key;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AerospikeLogger;
import utils.abs.AbsRunner;
import utils.aerospike.abs.AerospikeDataUtils;

import java.util.List;

@Tag("ABS-NEGATIVE-TESTS")
class NegativeRestoreWithGeneratedRoutineTest extends AbsRunner {
    private static String POLICY_NAME;
    private static String STORAGE_NAME;
    private static String ROUTINE_NAME;
    private static final String CLUSTER_NAME = "absDefaultCluster";
    private static String SOURCE_NAMESPACE;
    private static final String SET1 = "setNegative";
    private static DtoBackupPolicy POLICY;
    private static DtoStorage STORAGE;
    private static DtoBackupRoutine ROUTINE;
    private static final String STRING_BIN = "NegativeBin";
    private static Key KEY1;

    @BeforeEach
    public void setUpEach() {
        POLICY_NAME = "NegativeRestoreWithGeneratedRoutineTestPolicy" + System.currentTimeMillis();
        STORAGE_NAME = "NegativeRestoreWithGeneratedRoutineTestStorage" + System.currentTimeMillis();
        ROUTINE_NAME = "NegativeRestoreWithGeneratedRoutineTestRoutine" + System.currentTimeMillis();

        createBackupEntities();

        AbsPolicyApi.createPolicy(POLICY_NAME, POLICY);
        AbsStorageApi.createStorage(STORAGE_NAME, STORAGE);
        AbsRoutineApi.createRoutine(ROUTINE_NAME, ROUTINE);

        SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void restoreFromTwoBackupsAtTheSameDir() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        // create the first backup in the root directory
        AbsBackupApi.startFullBackupSync(ROUTINE_NAME);

        AbsRestoreApi.restoreFullSync(ROUTINE_NAME, ROUTINE_NAME);
        AerospikeLogger.info("Restore with one backup in the directory passed");

        // create the second backup in the root directory
        AbsBackupApi.startFullBackupSync(ROUTINE_NAME);

        // It will fail since we have two backup at the root directory
        Assertions.assertThatThrownBy(() -> AbsRestoreApi.restoreFullSync(ROUTINE_NAME, ROUTINE_NAME))
                .hasMessageContaining("Failed to restore");
    }

    private void createBackupEntities() {
        POLICY = new DtoBackupPolicy()
                .parallel(1)
                .sealed(true);

        STORAGE = new DtoStorage().localStorage(new DtoLocalStorage().path("/etc/aerospike-backup-service/conf.d/BackupWithNewConfigTestPath"));

        ROUTINE = new DtoBackupRoutine()
                .backupPolicy(POLICY_NAME)
                .intervalCron("@yearly")
                .namespaces(List.of("source-ns21"))
                .sourceCluster(CLUSTER_NAME)
                .storage(STORAGE_NAME);
    }
}