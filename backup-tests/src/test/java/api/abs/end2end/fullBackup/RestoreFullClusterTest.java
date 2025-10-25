package api.abs.end2end.fullBackup;

import api.abs.AbsBackupApi;
import api.abs.AbsClusterApi;
import api.abs.AbsRestoreApi;
import api.abs.annotations.AssertMetrics;
import api.abs.annotations.BackupsRunIncrease;
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
class RestoreFullClusterTest extends AbsRunner {
    private static final String STRING_BIN = "RestoreTestBin";
    private static final String FIRST_NS_SET = "FIRST_NS_SET";
    private static final String SECOND_NS_SET = "SECOND_NS_SET";
    private static final String THIRD_NS_SET = "THIRD_NS_SET";
    private static final String ROUTINE_NAME = "fullBackupFullCluster";
    private static final String FIRST_NS_VALUE = "FIRST_NS_VALUE" + System.currentTimeMillis();
    private static final String SECOND_NS_VALUE = "SECOND_NS_VALUE" + System.currentTimeMillis();
    private static final String firstNsName = "source-ns1";
    private static final String secondNsName = "source-ns2";
    private static final String thirdNsName = "source-ns3";
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
    }

    @Test
    @BackupsRunIncrease
    void restoreFullCluster() {
        AerospikeDataUtils.put(FIRST_NS_KEY, STRING_BIN, FIRST_NS_VALUE);
        AerospikeDataUtils.put(SECOND_NS_KEY, STRING_BIN, SECOND_NS_VALUE);
        ASBench.on(thirdNsName, THIRD_NS_SET).duration(1).run();

        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, THIRD_NS_SET, thirdNsName);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        long beforeBackup = System.currentTimeMillis() - 500;
        AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        AbsBackupApi.waitForAllFullBackups(ROUTINE_NAME, beforeBackup);

        long afterBackup = System.currentTimeMillis();

        AerospikeDataUtils.truncateSourceNamespace(firstNsName);
        AerospikeDataUtils.truncateSourceNamespace(secondNsName);
        AerospikeDataUtils.truncateSourceNamespace(thirdNsName);

        DtoRestoreTimestampRequest restoreRequest = new DtoRestoreTimestampRequest()
                .destination(SOURCE_CLUSTER)
                .routine(ROUTINE_NAME)
                .policy(new DtoRestorePolicy())
                .time(afterBackup);

        AbsRestoreApi.restoreTimestampSync(restoreRequest);

        Record retrievedRecordFirstNS = AerospikeDataUtils.get( FIRST_NS_KEY);
        assertThat(retrievedRecordFirstNS).isNotNull();
        assertThat(retrievedRecordFirstNS.getString(STRING_BIN)).isEqualTo(FIRST_NS_VALUE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SECOND_NS_SET, secondNsName)).isEqualTo(1);

        Record retrievedRecordSecondNS = AerospikeDataUtils.get( SECOND_NS_KEY);
        assertThat(retrievedRecordSecondNS).isNotNull();
        assertThat(retrievedRecordSecondNS.getString(STRING_BIN)).isEqualTo(SECOND_NS_VALUE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, FIRST_NS_SET, firstNsName)).isEqualTo(1);

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, THIRD_NS_SET, thirdNsName)).isEqualTo(numberOfRecordsBeforeTruncate);
    }

    @Test
    @BackupsRunIncrease
    void restoreOneNamespace() {
        AerospikeDataUtils.put(FIRST_NS_KEY, STRING_BIN, FIRST_NS_VALUE);
        AerospikeDataUtils.put(SECOND_NS_KEY, STRING_BIN, SECOND_NS_VALUE);
        ASBench.on(thirdNsName, THIRD_NS_SET).duration(1).run();

        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, THIRD_NS_SET, thirdNsName);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        long beforeBackup = System.currentTimeMillis() - 500;
        AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        AerospikeLogger.info("time: " + beforeBackup + " namespace: " + secondNsName);
        DtoBackupDetails secondNsBackup = AbsBackupApi.waitForFullBackup(ROUTINE_NAME, beforeBackup, secondNsName);
        AbsBackupApi.waitForAllFullBackups(ROUTINE_NAME, beforeBackup);

        AerospikeDataUtils.truncateSourceNamespace(firstNsName);
        AerospikeDataUtils.truncateSourceNamespace(secondNsName);
        AerospikeDataUtils.truncateSourceNamespace(thirdNsName);

        // restore the second NS only
        AbsRestoreApi.restoreFullSync(secondNsBackup.getKey(), ROUTINE_NAME);

        Record retrievedRecordFirstNS = AerospikeDataUtils.get( FIRST_NS_KEY);
        assertThat(retrievedRecordFirstNS).isNull();

        Record retrievedRecordSecondNS = AerospikeDataUtils.get( SECOND_NS_KEY);
        assertThat(retrievedRecordSecondNS).isNotNull();
        assertThat(retrievedRecordSecondNS.getString(STRING_BIN)).isEqualTo(SECOND_NS_VALUE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SECOND_NS_SET, secondNsName)).isEqualTo(1);

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, THIRD_NS_SET, thirdNsName)).isEqualTo(0);
    }

}