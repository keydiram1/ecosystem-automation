package api.abs.longDuration.aws;

import api.abs.AbsBackupApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.ConfigParametersHandler;
import utils.K8sUtils;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import java.time.Duration;

@Tag("ABS-LONG-DURATION-TEST")
class RestoreIncrementalInLoopTest extends AbsRunner {
    private static final String SET1 = "SetRestoreIncInLoop";
    private static final String ROUTINE_NAME = "localStorageIncremental3";
    private static String SOURCE_NAMESPACE;

    private static Key KEY1;

    private static final String STRING_BIN = "RestoreTestBin";

    @BeforeAll
    static void setUp() {
        SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");

        AerospikeDataUtils.put(KEY1, STRING_BIN, "someValue5");

        AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
    }

    @Test
    void restoreIncrementalInLoop() {
        int backupCounter = 1;
        while (!LongDurationParent.testFinished) {
            try {
                AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
                AerospikeLogger.info("Backup number " + backupCounter);

                String recordValue = "firstValueCreate" + System.currentTimeMillis();
                long putTime = AerospikeDataUtils.put(KEY1, STRING_BIN, recordValue);
                var createFirstValue = AbsBackupApi.waitForIncrementalBackup(ROUTINE_NAME, (putTime - 2_000));
                AerospikeLogger.info("Backup size is: " + createFirstValue.getByteCount());

                AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

                var timeout = Duration.ofMinutes(ConfigParametersHandler.getParameter("load_level").equals("low") ? 2 : 4);
                AbsRestoreApi.restoreIncrementalSync(createFirstValue.getKey(), ROUTINE_NAME, timeout);

                int actualObjectCount = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
                if (actualObjectCount != 1) {
                    throw new RuntimeException("Assertion failed: Expected object count 1, but found " + actualObjectCount);
                }

                Record retrievedRecord = AerospikeDataUtils.get(KEY1);
                if (retrievedRecord == null) {
                    throw new RuntimeException("Assertion failed: Record for key " + KEY1 + " is null.");
                }

                String actualRecordValue = retrievedRecord.getString(STRING_BIN);
                if (!recordValue.equals(actualRecordValue)) {
                    throw new RuntimeException("Assertion failed: Expected record value '" + recordValue + "', but found '" + actualRecordValue + "'.");
                }

                K8sUtils.printPodsStatistics();
                AerospikeLogger.info("Finished backup number " + backupCounter);
                backupCounter++;
                AutoUtils.sleep(30_000);
            } catch (Exception e) {
                String errorLog = getClass().getSimpleName() + " failed in backup number " + backupCounter +
                        "with the following exception: " + e.getMessage();
                AerospikeLogger.info(errorLog);
                LongDurationParent.testLog += errorLog + "\n";
                backupCounter++;
                AutoUtils.sleep(30_000);
            }
        }
    }
}