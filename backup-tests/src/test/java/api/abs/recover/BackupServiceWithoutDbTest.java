package api.abs.recover;

import api.abs.AbsBackupApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.DockerManager;
import utils.abs.AbsLogHandler;
import utils.abs.AbsRunner;
import utils.aerospike.abs.AerospikeDataUtils;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-SEQUENTIAL-TESTS")
@Execution(ExecutionMode.SAME_THREAD)
@DisabledIfSystemProperty(named = "qa_environment", matches = "GCP")
@EnabledIfSystemProperty(named = "STORAGE_PROVIDER", matches = "local")
class BackupServiceWithoutDbTest extends AbsRunner {
    private static final String STRING_BIN = "fastRestoreBin";
    private static final String SET1 = "SetFastRestore";
    private static final String ROUTINE_NAME = "fullBackup1";
    private static Key KEY1;
    private static String SOURCE_NAMESPACE;

    @BeforeAll
    static void setUp() {
        SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
    }

    @AfterAll
    static void Cleanup() {
        DockerManager.restartAbsAerospikeContainer("aerospike-source");
        DockerManager.startAndWaitForBackupService();
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void backupServiceWithoutDbTest() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        String backupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();

        DockerManager.stopContainer("backup-service");
        DockerManager.stopContainer("aerospike-source");

        DockerManager.startAndWaitForBackupService();

        AutoUtils.sleep(10_000);
        AbsLogHandler logHandler = new AbsLogHandler();
        AbsBackupApi.scheduleFullBackup(ROUTINE_NAME, 1);
        AutoUtils.sleep(2000);
        String backupServiceLog = logHandler.getBackupServiceLog();
        AerospikeLogger.info(backupServiceLog);
        assertThat(backupServiceLog).contains("cannot create backup client: failed to connect to aerospike cluster");

        logHandler.initBackupServiceLog();
        AbsRestoreApi.restoreFull(backupKey, ROUTINE_NAME);
        AutoUtils.sleep(2000);
        backupServiceLog = logHandler.getBackupServiceLog();
        AerospikeLogger.info(backupServiceLog);
        assertThat(backupServiceLog).contains("Failed to restore by path", "cannot create backup client: failed to connect to aerospike cluster");

        DockerManager.restartAbsAerospikeContainer("aerospike-source");

        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        logHandler.initBackupServiceLog();

        backupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        AbsRestoreApi.restoreFullSync(backupKey, ROUTINE_NAME);

        AutoUtils.sleep(2000);
        backupServiceLog = logHandler.getBackupServiceLog();
        AerospikeLogger.info(backupServiceLog);

        assertThat(backupServiceLog).doesNotContain("Failed to restore by path");
        assertThat(backupServiceLog).doesNotContain("cannot create backup client: failed to connect to aerospike cluster");

        Record retrievedRecord = AerospikeDataUtils.get( KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
    }
}