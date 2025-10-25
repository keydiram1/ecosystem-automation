package api.abs.load.aws.restore;

import api.abs.AbsBackupApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import api.abs.load.aws.LoadRunner;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AerospikeLogger;
import utils.ConfigParametersHandler;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import java.time.Duration;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-LOAD-TEST")
class RestoreIncrementalInLoopTest extends LoadRunner {
    private static final String SET1 = "SetRestoreIncInLoop";
    private static final String ROUTINE_NAME = "localStorageIncremental3";
    private static String SOURCE_NAMESPACE;

    private static Key KEY1;
    private static int loopCount = 7;

    private static final String STRING_BIN = "RestoreTestBin";
    private final Function<Record, String> getStringBin = f -> f.getString(STRING_BIN);

    @BeforeAll
    static void setUp() {
        loopCount = LOAD_LEVEL.equals("low") ? 1 : loopCount;
        minutesToWaitForAllLoadClassesToFinishSetup = LOAD_LEVEL.equals("low") ? 5 : 60;
        SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
        waitGroup.wait(minutesToWaitForAllLoadClassesToFinishSetup);
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void restoreIncrementalInLoop() {
        AbsBackupApi.scheduleFullBackup(ROUTINE_NAME);
        for (int i = 0; i < loopCount; i++) {
            AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
            AerospikeLogger.info("Backup number " + i + 1);

            String recordValue = "firstValueCreate" + System.currentTimeMillis();
            AerospikeDataUtils.put(KEY1, STRING_BIN, recordValue);
            var createFirstValue = AbsBackupApi.waitForIncrementalBackup(ROUTINE_NAME);
            AerospikeLogger.info("Backup size is: " + createFirstValue.getByteCount());
            assertThat(createFirstValue.getByteCount()).isGreaterThan(150).isLessThan(1000);

            AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

            var timeout = Duration.ofMinutes(ConfigParametersHandler.getParameter("LOAD_LEVEL").equals("low") ? 2 : 4);
            AbsRestoreApi.restoreIncrementalSync(createFirstValue.getKey(), ROUTINE_NAME, timeout);

            assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(1);

            assertThat(AerospikeDataUtils.get(KEY1))
                    .isNotNull()
                    .as("Restore first key to initial value")
                    .extracting(getStringBin)
                    .isEqualTo(recordValue);

            AerospikeLogger.info("Finished backup number " + i + 1);
        }
    }
}