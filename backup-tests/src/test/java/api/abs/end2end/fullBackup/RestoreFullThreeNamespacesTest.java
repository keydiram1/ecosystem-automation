package api.abs.end2end.fullBackup;

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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.ASBench;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-E2E")
class RestoreFullThreeNamespacesTest extends AbsRunner {
    private static final String STRING_BIN = "RestoreTestBin";
    private static final String FIRST_NS_SET = "FIRST_NS_SET";
    private static final String SECOND_NS_SET = "SECOND_NS_SET";
    private static final String THIRD_NS_SET = "THIRD_NS_SET";
    private static final String ROUTINE_NAME = "fullBackup3Namespaces";
    private static final String FIRST_NS_VALUE = "FIRST_NS_VALUE" + System.currentTimeMillis();
    private static final String SECOND_NS_VALUE = "SECOND_NS_VALUE" + System.currentTimeMillis();
    private static Key firstNsKey;
    private static Key secondNsKey;
    private static String firstNsName;
    private static String secondNsName;
    private static String thirdNsName;
    private static DtoAerospikeCluster SOURCE_CLUSTER;

    @BeforeAll
    static void setUp() {
        final var routine = AbsRoutineApi.getRoutine(ROUTINE_NAME);
        assertThat(routine.getNamespaces()).isNotNull()
                .hasSizeGreaterThanOrEqualTo(3);
        firstNsName = routine.getNamespaces().get(0);
        secondNsName = routine.getNamespaces().get(1);
        thirdNsName = routine.getNamespaces().get(2);
        assertThat(firstNsName).isNotEqualTo(secondNsName).isNotEqualTo(thirdNsName);

        firstNsKey = new Key(firstNsName, FIRST_NS_SET, "IT1");
        secondNsKey = new Key(secondNsName, SECOND_NS_SET, "IT2");
        SOURCE_CLUSTER = AbsClusterApi.getCluster("absDefaultCluster");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(firstNsName);
        AerospikeDataUtils.truncateSourceNamespace(secondNsName);
        AerospikeDataUtils.truncateSourceNamespace(thirdNsName);
    }

    @Test
    void restoreThreeNamespacesRoutine() {
        long putTime = AerospikeDataUtils.put(firstNsKey, STRING_BIN, FIRST_NS_VALUE);
        AerospikeDataUtils.put(secondNsKey, STRING_BIN, SECOND_NS_VALUE);
        ASBench.on(thirdNsName, THIRD_NS_SET).duration(1).run();

        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, THIRD_NS_SET, thirdNsName);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        AbsBackupApi.waitForFullBackups(ROUTINE_NAME, putTime, firstNsName, secondNsName, thirdNsName);


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

        Record retrievedRecordFirstNS = AerospikeDataUtils.get( firstNsKey);
        assertThat(retrievedRecordFirstNS).isNotNull();
        assertThat(retrievedRecordFirstNS.getString(STRING_BIN)).isEqualTo(FIRST_NS_VALUE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, FIRST_NS_SET, firstNsName)).isEqualTo(1);

        Record retrievedRecordSecondNS = AerospikeDataUtils.get( secondNsKey);
        assertThat(retrievedRecordSecondNS).isNotNull();
        assertThat(retrievedRecordSecondNS.getString(STRING_BIN)).isEqualTo(SECOND_NS_VALUE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SECOND_NS_SET, secondNsName)).isEqualTo(1);

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, THIRD_NS_SET, thirdNsName)).isEqualTo(numberOfRecordsBeforeTruncate);
    }

    @Test
    void restoreOneNamespace() {
        AerospikeDataUtils.put(firstNsKey, STRING_BIN, FIRST_NS_VALUE);
        AerospikeDataUtils.put(secondNsKey, STRING_BIN, SECOND_NS_VALUE);
        ASBench.on(thirdNsName, THIRD_NS_SET).duration(1).run();

        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, THIRD_NS_SET, thirdNsName);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        long beforeBackup = System.currentTimeMillis();
        AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        AbsBackupApi.waitForFullBackups(ROUTINE_NAME, beforeBackup, firstNsName, secondNsName, thirdNsName);
        DtoBackupDetails secondNsBackup = AbsBackupApi.firstFullBackupAfter(ROUTINE_NAME, beforeBackup, secondNsName);

        AerospikeDataUtils.truncateSourceNamespace(firstNsName);
        AerospikeDataUtils.truncateSourceNamespace(secondNsName);
        AerospikeDataUtils.truncateSourceNamespace(thirdNsName);

        // restore the second NS only
        AbsRestoreApi.restoreFullSync(secondNsBackup.getKey(), ROUTINE_NAME);

        Record retrievedRecordFirstNS = AerospikeDataUtils.get( firstNsKey);
        assertThat(retrievedRecordFirstNS).isNull();

        Record retrievedRecordSecondNS = AerospikeDataUtils.get( secondNsKey);
        assertThat(retrievedRecordSecondNS).isNotNull();
        assertThat(retrievedRecordSecondNS.getString(STRING_BIN)).isEqualTo(SECOND_NS_VALUE);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SECOND_NS_SET, secondNsName)).isEqualTo(1);

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, THIRD_NS_SET, thirdNsName)).isEqualTo(0);
    }

}