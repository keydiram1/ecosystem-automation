package api.abs.end2end.incrementalBackup;

import api.abs.AbsBackupApi;
import api.abs.AbsClusterApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import api.abs.generated.model.DtoAerospikeCluster;
import api.abs.generated.model.DtoBackupDetails;
import api.abs.generated.model.DtoRestorePolicy;
import api.abs.generated.model.DtoRestoreTimestampRequest;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.ConfigParametersHandler;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-SEQUENTIAL-TESTS")
@Execution(ExecutionMode.SAME_THREAD)
class RestoreIncrementalMultipleNSsTest extends AbsRunner {
    private static final String STRING_BIN = "RestoreTestBin";
    private static final String FIRST_NS_SET = "FIRST_NS_SET";
    private static final String SECOND_NS_SET = "SECOND_NS_SET";
    private static final String THIRD_NS_SET = "THIRD_NS_SET";
    private static final String ROUTINE_NAME = "incrementalBackupMultipleNSs";
    private static final String FIRST_NS_VALUE = "FIRST_NS_VALUE" + System.currentTimeMillis();
    private static final String SECOND_NS_VALUE = "SECOND_NS_VALUE" + System.currentTimeMillis();
    private static String firstNsName;
    private static String secondNsName;
    private static String thirdNsName;
    private static Key firstNsKey;
    private static Key secondNsKey;
    private static final DtoAerospikeCluster SOURCE_CLUSTER = AbsClusterApi.getCluster("absDefaultCluster");


    @BeforeEach
    public void setUpEach() {
        final var routine = AbsRoutineApi.getRoutine(ROUTINE_NAME);
        assertThat(routine.getNamespaces()).isNotNull()
                .hasSizeGreaterThanOrEqualTo(3);
        firstNsName = routine.getNamespaces().get(0);
        secondNsName = routine.getNamespaces().get(1);
        thirdNsName = routine.getNamespaces().get(2);

        firstNsKey = new Key(firstNsName, FIRST_NS_SET, "IT1");
        secondNsKey = new Key(secondNsName, SECOND_NS_SET, "IT1");

        AerospikeDataUtils.truncateSourceNamespace(firstNsName);
        AerospikeDataUtils.truncateSourceNamespace(secondNsName);
        AerospikeDataUtils.truncateSourceNamespace(thirdNsName);
        AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
    }

    @Test
    void restoreClusterIncremental() {
        // Dummy backup to make sure the second backup will be 10 seconds after
        AerospikeDataUtils.put(firstNsKey, "someBin", "someValue");
        AbsBackupApi.waitForIncrementalBackup(ROUTINE_NAME);

        long putTime = AerospikeDataUtils.put(firstNsKey, STRING_BIN, FIRST_NS_VALUE);
        AerospikeDataUtils.put(secondNsKey, STRING_BIN, SECOND_NS_VALUE);
        ASBench.on(thirdNsName, THIRD_NS_SET).duration(1).run();
        AbsBackupApi.waitForIncrementalBackups(ROUTINE_NAME, putTime, firstNsName, secondNsName, thirdNsName);
        long afterBackup = System.currentTimeMillis();

        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, THIRD_NS_SET, thirdNsName);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        AerospikeLogger.info("first ns key: " + AerospikeDataUtils.get( firstNsKey));
        AerospikeDataUtils.truncateSourceNamespace(firstNsName);
        AerospikeDataUtils.truncateSourceNamespace(secondNsName);
        AerospikeDataUtils.truncateSourceNamespace(thirdNsName);

        var policyNoIndexNoUdf = new DtoRestorePolicy().parallel(Integer.valueOf(ConfigParametersHandler.getParameter("CONFIG_RESTORE_PARALLEL"))).noIndexes(true).noUdfs(true);
        DtoRestoreTimestampRequest restoreRequest = new DtoRestoreTimestampRequest()
                .destination(SOURCE_CLUSTER)
                .routine(ROUTINE_NAME)
                .policy(new DtoRestorePolicy().parallel(Integer.valueOf(ConfigParametersHandler.getParameter("CONFIG_RESTORE_PARALLEL"))).noGeneration(true))
                .time(afterBackup)
                .policy(policyNoIndexNoUdf);

        AbsRestoreApi.restoreTimestampSync(restoreRequest);

        Record retrievedRecord = AerospikeDataUtils.get( firstNsKey);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isNotNull().isEqualTo(FIRST_NS_VALUE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, FIRST_NS_SET, firstNsName)).isEqualTo(1);

        retrievedRecord = AerospikeDataUtils.get( secondNsKey);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(SECOND_NS_VALUE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SECOND_NS_SET, secondNsName)).isEqualTo(1);

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, THIRD_NS_SET, thirdNsName)).isEqualTo(numberOfRecordsBeforeTruncate);
    }

    @Test
    void restoreOneNamespaceIncremental() {
        // Dummy backup to make sure the second backup will be 10 seconds after
        AerospikeDataUtils.put(firstNsKey, "someBin", "someValue");
        AbsBackupApi.waitForIncrementalBackup(ROUTINE_NAME);

        long putTime = AerospikeDataUtils.put(firstNsKey, STRING_BIN, FIRST_NS_VALUE);
        AerospikeDataUtils.put(secondNsKey, STRING_BIN, SECOND_NS_VALUE);
        ASBench.on(thirdNsName, THIRD_NS_SET).duration(1).run();
        AbsBackupApi.waitForIncrementalBackups(ROUTINE_NAME, putTime, firstNsName, secondNsName, thirdNsName);
        DtoBackupDetails secondNsBackup = AbsBackupApi.incrementalBackupAfter(ROUTINE_NAME, putTime, secondNsName);

        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, THIRD_NS_SET, thirdNsName);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        AerospikeLogger.info("first ns key: " + AerospikeDataUtils.get( firstNsKey));
        AerospikeDataUtils.truncateSourceNamespace(firstNsName);
        AerospikeDataUtils.truncateSourceNamespace(secondNsName);
        AerospikeDataUtils.truncateSourceNamespace(thirdNsName);

        var policyNoIndexNoUdf = new DtoRestorePolicy().parallel(Integer.valueOf(ConfigParametersHandler.getParameter("CONFIG_RESTORE_PARALLEL"))).noIndexes(true).noUdfs(true);
        AbsRestoreApi.restoreIncrementalSync(secondNsBackup.getKey(), ROUTINE_NAME, policyNoIndexNoUdf);

        Record retrievedRecord = AerospikeDataUtils.get( firstNsKey);
        assertThat(retrievedRecord).isNull();

        retrievedRecord = AerospikeDataUtils.get( secondNsKey);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(SECOND_NS_VALUE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SECOND_NS_SET, secondNsName)).isEqualTo(1);

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, THIRD_NS_SET, thirdNsName)).isZero();
    }
}