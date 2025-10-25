package api.abs.end2end.incrementalBackup;

import api.abs.AbsBackupApi;
import api.abs.AbsPolicyApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import api.abs.generated.model.*;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeScanner;
import utils.aerospike.abs.AerospikeDataUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static api.abs.AbsBackupApi.*;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-E2E")
class RestoreIncrementalBackup1Test extends AbsRunner {
    private static final String STRING_BIN = "RestoreTestBin";
    private static final String SET1 = "SetIncrementalTest1";
    private static final String SET2 = "SetIncrementalTest2";
    private static final String SET3 = "SetIncrementalTest3";
    private static final String ROUTINE_NAME = "localStorageIncremental3";
    private static Key KEY1;
    private static Key KEY2;
    private static Key KEY3;
    private static String SOURCE_NAMESPACE;
    private final Function<Record, String> getStringBin = f -> f.getString(STRING_BIN);

    @BeforeAll
    static void setUp() {
        SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT");
        KEY2 = new Key(SOURCE_NAMESPACE, SET2, "IT");
        KEY3 = new Key(SOURCE_NAMESPACE, SET3, "IT");
        AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void restoreIncrementalBackup() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        long putTime = AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        DtoRoutineState currentBackup = getCurrentBackup(ROUTINE_NAME);
        assertThat(currentBackup.getNextFull()).isNotBlank();
        assertThat(currentBackup.getNextIncremental()).isNotBlank();
        assertThat(parseDate(currentBackup.getNextIncremental()) - Instant.now().toEpochMilli())
                .as("time in millis to next incremental backup")
                .isLessThan(10_000);

        var createFirstValue = waitForIncrementalBackup(ROUTINE_NAME, putTime); // 1
        AerospikeLogger.info("Backup size is: " + createFirstValue.getByteCount());
        assertThat(createFirstValue.getByteCount()).isGreaterThan(150).isLessThan(1000);
        assertThat(createFirstValue.getFrom()).isNotBlank();

        String firstValueUpdate = "firstValueUpdate" + System.currentTimeMillis();
        String secondValueCreate = "secondValueCreate" + System.currentTimeMillis();
        putTime = AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueUpdate);
        var firstValueUpdateBackup = waitForIncrementalBackup(ROUTINE_NAME, putTime); // 2
        assertThat(firstValueUpdateBackup.getFrom()).isNotBlank();
        assertThat(getFrom(firstValueUpdateBackup)).isEqualTo(getCreated(createFirstValue));
        putTime = AerospikeDataUtils.put(KEY2, STRING_BIN, secondValueCreate);
        var secondValueCreateBackup = waitForIncrementalBackup(ROUTINE_NAME, putTime); // 3

        String secondValueUpdate = "secondValueUpdate" + System.currentTimeMillis();
        String thirdValueCreate = "thirdValueCreate" + System.currentTimeMillis();
        putTime = AerospikeDataUtils.put(KEY2, STRING_BIN, secondValueUpdate);
        var secondValueUpdateBackup = waitForIncrementalBackup(ROUTINE_NAME, putTime);
        srcClient.delete(null, KEY1);
        putTime = AerospikeDataUtils.put(KEY3, STRING_BIN, thirdValueCreate) - 1;//minus one because of the deletion
        var firstValueDeleteThirdValueCreateBackup = waitForIncrementalBackup(ROUTINE_NAME, putTime);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        AbsRestoreApi.restoreIncrementalSync(createFirstValue.getKey(), ROUTINE_NAME);

        assertThat(AerospikeDataUtils.get(KEY1))
                .isNotNull()
                .as("Restore first key to initial value")
                .extracting(getStringBin)
                .isEqualTo(firstValueCreate);

        AbsRestoreApi.restoreIncrementalSync(firstValueUpdateBackup.getKey(), ROUTINE_NAME);
        AbsRestoreApi.restoreIncrementalSync(secondValueCreateBackup.getKey(), ROUTINE_NAME);

        assertThat(AerospikeDataUtils.get(KEY1))
                .isNotNull()
                .as("Restore first key to updated value")
                .extracting(getStringBin)
                .isEqualTo(firstValueUpdate);

        assertThat(AerospikeDataUtils.get(KEY2))
                .isNotNull()
                .as("Restore second key to initial value")
                .extracting(getStringBin)
                .isEqualTo(secondValueCreate);

        assertThat(AerospikeDataUtils.get(KEY3))
                .as("Key3 was not created yet")
                .isNull();

        AbsRestoreApi.restoreIncrementalSync(secondValueUpdateBackup.getKey(), ROUTINE_NAME);
        AbsRestoreApi.restoreIncrementalSync(firstValueDeleteThirdValueCreateBackup.getKey(), ROUTINE_NAME);

        // For now, we don't support delete actions in incremental backup so the first record will stay as it was.
        assertThat(AerospikeDataUtils.get(KEY1))
                .isNotNull()
                .as("First key stays to initial value")
                .extracting(getStringBin)
                .isEqualTo(firstValueUpdate);
        assertThat(AerospikeDataUtils.get(KEY2))
                .isNotNull()
                .as("Second key to updated value")
                .extracting(getStringBin)
                .isEqualTo(secondValueUpdate);
        assertThat(AerospikeDataUtils.get(KEY3))
                .isNotNull()
                .as("Third key to initial value")
                .extracting(getStringBin)
                .isEqualTo(thirdValueCreate);

        // The user key shouldn't be restored since writePolicy.setSendKey default value is false
        AerospikeScanner scanner = new AerospikeScanner();
        scanner.scanKeys(srcClient, SOURCE_NAMESPACE, SET1);
        AerospikeLogger.info("Number of keys: " + scanner.getAllKeys().size());
        assertThat(scanner.getAllKeys().size()).isEqualTo(0);
    }

    @Test
    void restoreUserKey() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        WritePolicy writePolicy = new WritePolicy();
        writePolicy.setSendKey(true);
        long putTime = AerospikeDataUtils.put(writePolicy, KEY1, STRING_BIN, firstValueCreate);
        var createFirstValue = waitForIncrementalBackup(ROUTINE_NAME, putTime);
        AerospikeScanner scanner = new AerospikeScanner();
        scanner.scanKeys(srcClient, SOURCE_NAMESPACE, SET1);
        assertThat(scanner.getAllKeys().size()).isEqualTo(1);

        AerospikeDataUtils.delete(KEY1);
        scanner.scanKeys(srcClient, SOURCE_NAMESPACE, SET1);
        assertThat(scanner.getAllKeys().size()).isEqualTo(0);

        AbsRestoreApi.restoreIncrementalSync(createFirstValue.getKey(), ROUTINE_NAME);

        Record retrievedRecord = AerospikeDataUtils.get(KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);

        scanner.scanKeys(srcClient, SOURCE_NAMESPACE, SET1);
        assertThat(scanner.getAllKeys().size()).isEqualTo(1);
    }
}