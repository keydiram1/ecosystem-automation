package api.abs.end2end.incrementalBackup;

import api.abs.AbsBackupApi;
import api.abs.AbsClusterApi;
import api.abs.AbsRestoreApi;
import api.abs.annotations.AssertMetrics;
import api.abs.annotations.IncrementalBackupsRunIncrease;
import api.abs.generated.model.DtoAerospikeCluster;
import api.abs.generated.model.DtoBackupDetails;
import api.abs.generated.model.DtoRestorePolicy;
import api.abs.generated.model.DtoRestoreTimestampRequest;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-SEQUENTIAL-TESTS")
@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith(AssertMetrics.class)
class RestoreIncrementalClusterTest extends AbsRunner {
    private static final String STRING_BIN = "RestoreTestBin";
    private static final String FIRST_NS_SET = "FIRST_NS_SET";
    private static final String SECOND_NS_SET = "SECOND_NS_SET";
    private static final String THIRD_NS_SET = "THIRD_NS_SET";
    private static final String ROUTINE_NAME = "incrementalBackupCluster";
    private static final String FIRST_NS_VALUE = "FIRST_NS_VALUE" + System.currentTimeMillis();
    private static final String SECOND_NS_VALUE = "SECOND_NS_VALUE" + System.currentTimeMillis();
    private static final String firstNsName = "source-ns15";
    private static final String secondNsName = "source-ns16";
    private static final String thirdNsName = "source-ns17";
    private static final Key FIRST_NS_KEY = new Key(firstNsName, FIRST_NS_SET, "IT1");
    private static final Key SECOND_NS_KEY = new Key(secondNsName, SECOND_NS_SET, "IT1");
    private static final DtoAerospikeCluster SOURCE_CLUSTER = AbsClusterApi.getCluster("absDefaultCluster");

    @BeforeAll
    static void setUp() {
        AerospikeDataUtils.truncateAllSourceNamespaces(List.of(firstNsName, secondNsName, thirdNsName));
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(firstNsName);
        AerospikeDataUtils.truncateSourceNamespace(secondNsName);
        AerospikeDataUtils.truncateSourceNamespace(thirdNsName);
        AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
    }

    @Test
    @IncrementalBackupsRunIncrease
    void restoreClusterIncremental() {
        // Dummy backup to make sure the second backup will be 10 seconds after
        AerospikeDataUtils.put(FIRST_NS_KEY, "someBin", "someValue");
        AbsBackupApi.waitForIncrementalBackup(ROUTINE_NAME);

        long putTime = AerospikeDataUtils.put(FIRST_NS_KEY, STRING_BIN, FIRST_NS_VALUE);
        AerospikeDataUtils.put(SECOND_NS_KEY, STRING_BIN, SECOND_NS_VALUE);
        ASBench.on(thirdNsName, THIRD_NS_SET).duration(1).run();
        AbsBackupApi.waitForIncrementalBackups(ROUTINE_NAME, putTime, firstNsName, secondNsName, thirdNsName);
        long afterBackup = System.currentTimeMillis();

        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, THIRD_NS_SET, thirdNsName);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        AerospikeLogger.info("first ns key: " + AerospikeDataUtils.get( FIRST_NS_KEY));
        AerospikeDataUtils.truncateSourceNamespace(firstNsName);
        AerospikeDataUtils.truncateSourceNamespace(secondNsName);
        AerospikeDataUtils.truncateSourceNamespace(thirdNsName);

        var policyNoIndexNoUdf = new DtoRestorePolicy().noIndexes(true).noUdfs(true);
        DtoRestoreTimestampRequest restoreRequest = new DtoRestoreTimestampRequest()
                .destination(SOURCE_CLUSTER)
                .routine(ROUTINE_NAME)
                .policy(new DtoRestorePolicy())
                .time(afterBackup)
                .policy(policyNoIndexNoUdf);

        AbsRestoreApi.restoreTimestampSync(restoreRequest);

        Record retrievedRecord = AerospikeDataUtils.get( FIRST_NS_KEY);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(FIRST_NS_VALUE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, FIRST_NS_SET, firstNsName)).isEqualTo(1);

        retrievedRecord = AerospikeDataUtils.get( SECOND_NS_KEY);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(SECOND_NS_VALUE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SECOND_NS_SET, secondNsName)).isEqualTo(1);

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, THIRD_NS_SET, thirdNsName)).isEqualTo(numberOfRecordsBeforeTruncate);
    }

    @Test
    @IncrementalBackupsRunIncrease
    void restoreOneNamespaceIncremental() {
        // Dummy backup to make sure the second backup will be 10 seconds after
        AerospikeDataUtils.put(FIRST_NS_KEY, "someBin", "someValue");
        AbsBackupApi.waitForIncrementalBackup(ROUTINE_NAME);

        long putTime = AerospikeDataUtils.put(FIRST_NS_KEY, STRING_BIN, FIRST_NS_VALUE);
        AerospikeDataUtils.put(SECOND_NS_KEY, STRING_BIN, SECOND_NS_VALUE);
        ASBench.on(thirdNsName, THIRD_NS_SET).duration(1).run();
        AbsBackupApi.waitForIncrementalBackups(ROUTINE_NAME, putTime, firstNsName, secondNsName, thirdNsName);
        DtoBackupDetails secondNsBackup = AbsBackupApi.incrementalBackupAfter(ROUTINE_NAME, putTime, secondNsName);

        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, THIRD_NS_SET, thirdNsName);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        AerospikeLogger.info("first ns key: " + AerospikeDataUtils.get( FIRST_NS_KEY));
        AerospikeDataUtils.truncateSourceNamespace(firstNsName);
        AerospikeDataUtils.truncateSourceNamespace(secondNsName);
        AerospikeDataUtils.truncateSourceNamespace(thirdNsName);

        var policyNoIndexNoUdf = new DtoRestorePolicy().noIndexes(true).noUdfs(true);
        AbsRestoreApi.restoreIncrementalSync(secondNsBackup.getKey(), ROUTINE_NAME, policyNoIndexNoUdf);

        Record retrievedRecord = AerospikeDataUtils.get( FIRST_NS_KEY);
        assertThat(retrievedRecord).isNull();

        retrievedRecord = AerospikeDataUtils.get( SECOND_NS_KEY);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(SECOND_NS_VALUE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SECOND_NS_SET, secondNsName)).isEqualTo(1);

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, THIRD_NS_SET, thirdNsName)).isZero();
    }
}