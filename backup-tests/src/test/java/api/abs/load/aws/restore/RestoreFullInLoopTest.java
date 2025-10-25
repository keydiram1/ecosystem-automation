package api.abs.load.aws.restore;

import api.abs.AbsBackupApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import api.abs.generated.model.DtoBackupDetails;
import api.abs.load.aws.LoadRunner;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.ConfigParametersHandler;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-LOAD-TEST")
class RestoreFullInLoopTest extends LoadRunner {
    private static final String SET1 = "SetRestoreInLoop";
    private static final String ROUTINE_NAME = "edgeCases";
    private static String SOURCE_NAMESPACE;

    private static Key KEY1;
    private static int loopCount = 5;

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
    void restoreFullInLoop() {
        for (int i = 0; i < loopCount; i++) {
            AerospikeLogger.info("Backup number " + i + 1);
            AerospikeLogger.info("Start restore with asbench");

            AerospikeDataUtils.truncateNamespace(srcClient, SOURCE_NAMESPACE);
            AerospikeLogger.info("Number of records after truncate before backup");
            AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);

            ASBench.on(SOURCE_NAMESPACE, SET1).duration(1).run();
            int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
            assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

            var timeout = Duration.ofMinutes(ConfigParametersHandler.getParameter("LOAD_LEVEL").equals("low") ? 2 : 3);

            AutoUtils.sleep(3000);

            DtoBackupDetails backup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME, timeout, 1);
            String backupKey = backup.getKey();
            AerospikeLogger.info("backupKey=" + backupKey);
            AerospikeLogger.info("backup.getRecordCount=" + backup.getRecordCount());
            AerospikeLogger.info("backup.getByteCount=" + backup.getByteCount());

            AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
            AerospikeLogger.info("Number of records after truncate before restore");
            AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);

            AbsRestoreApi.restoreFullSync(backupKey, ROUTINE_NAME, timeout);

            assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);


            AerospikeDataUtils.truncateNamespace(srcClient, SOURCE_NAMESPACE);
            AerospikeLogger.info("Start restore big record");
            final int ITERATIONS = 1_000_000;
            createBigRecord(ITERATIONS, KEY1);
            String firstBackupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME, timeout).getKey();

            AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

            AbsRestoreApi.restoreFullSync(firstBackupKey, ROUTINE_NAME, timeout);

            Record retrievedRecord = AerospikeDataUtils.get( KEY1);
            assertThat(retrievedRecord).isNotNull();
            assertThat(retrievedRecord.getList("list")).hasSize(ITERATIONS);
            AerospikeLogger.info("Finished iteration number " + i);
        }
    }

    private void createBigRecord(int number, Key key) {
        List<Integer> integers = IntStream.range(0, number).boxed().toList();
        Bin bin = new Bin("list", integers);
        WritePolicy policy = new WritePolicy(srcClient.getWritePolicyDefault());
        policy.setTimeout(5_000);
        srcClient.put(policy, key, bin);
    }
}