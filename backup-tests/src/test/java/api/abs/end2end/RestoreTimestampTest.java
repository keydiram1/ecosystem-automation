package api.abs.end2end;

import api.abs.AbsBackupApi;
import api.abs.AbsClusterApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import api.abs.annotations.AssertMetrics;
import api.abs.annotations.IncrementalBackupsRunIncrease;
import api.abs.generated.model.*;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import utils.AutoUtils;
import utils.abs.AbsRunner;
import utils.aerospike.abs.AerospikeDataUtils;

import static api.abs.AbsRestoreApi.defaultPolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static utils.aerospike.abs.AerospikeDataUtils.createComplexRecord;

@Tag("ABS-E2E")
@ExtendWith(AssertMetrics.class)
class RestoreTimestampTest extends AbsRunner {
    private static Key KEY1;
    private static Key KEY2;
    private static Key KEY3;
    private static String SOURCE_NAMESPACE;
    private static final String STRING_BIN = "TimestampBin";
    private static final String ROUTINE_NAME = "timestamp";
    private static final String SET = "set";

    private static DtoAerospikeCluster SOURCE_CLUSTER;

    @BeforeEach
    void setUp() {
        final var routine = AbsRoutineApi.getRoutine(ROUTINE_NAME);
        SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(routine);
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        KEY1 = new Key(SOURCE_NAMESPACE, SET, "key1");
        KEY2 = new Key(SOURCE_NAMESPACE, SET, "key2");
        KEY3 = new Key(SOURCE_NAMESPACE, SET, "key3");
        SOURCE_CLUSTER = AbsClusterApi.getCluster(routine.getSourceCluster());

        for (int i = 0; i < 20; i++) {
            AerospikeDataUtils.put(new Key(SOURCE_NAMESPACE, "payload", i), STRING_BIN, "some data");
        }
    }

    @Test
    @IncrementalBackupsRunIncrease
    void restoreTimestamp() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        DtoBackupDetails fullBackup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        assertThat(fullBackup.getRecordCount()).isEqualTo(21);

        String secondValueCreate = "secondValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY2, STRING_BIN, secondValueCreate);
        DtoBackupDetails incrBackup1 = AbsBackupApi.waitForIncrementalBackup(ROUTINE_NAME);
        assertThat(incrBackup1.getRecordCount()).isEqualTo(1);

        String secondValueUpdate = "secondValueUpdate" + System.currentTimeMillis();
        String thirdValueCreate = "thirdValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY2, STRING_BIN, secondValueUpdate);
        AerospikeDataUtils.put(KEY3, STRING_BIN, thirdValueCreate);
        DtoBackupDetails incrBackup2 = AbsBackupApi.waitForIncrementalBackup(ROUTINE_NAME);
        assertThat(incrBackup2.getRecordCount()).isEqualTo(2);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        DtoRestorePolicy policy = defaultPolicy().setList(Lists.list(SET));
        DtoRestoreTimestampRequest restoreRequest = new DtoRestoreTimestampRequest()
                .destination(SOURCE_CLUSTER)
                .disableReordering(true)
                .routine(ROUTINE_NAME)
                .policy(policy)
                .time(AbsBackupApi.getCreated(incrBackup2));

        DtoRestoreJobStatus restoreJob = AbsRestoreApi.restoreTimestampSync(restoreRequest);

        assertThat(AerospikeDataUtils.get(KEY1).getString(STRING_BIN)).isEqualTo(firstValueCreate);
        assertThat(AerospikeDataUtils.get(KEY2).getString(STRING_BIN)).isEqualTo(secondValueUpdate);
        assertThat(AerospikeDataUtils.get(KEY3).getString(STRING_BIN)).isEqualTo(thirdValueCreate);

        assertThat(restoreJob.getInsertedRecords()).isEqualTo(4L);

        assertThat(restoreJob.getReadRecords()).isEqualTo(24);
        assertThat(restoreJob.getSkippedRecords()).isEqualTo(20);
    }

    @Test
    void emptyFullBackup() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        DtoBackupDetails fullBackup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        assertThat(fullBackup.getRecordCount()).isZero();

        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        DtoBackupDetails incrBackup = AbsBackupApi.waitForIncrementalBackup(ROUTINE_NAME);
        assertThat(incrBackup.getRecordCount()).isEqualTo(1);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        DtoRestorePolicy policy = defaultPolicy().setList(Lists.list(SET));
        DtoRestoreTimestampRequest restoreRequest = new DtoRestoreTimestampRequest()
                .destination(SOURCE_CLUSTER)
                .disableReordering(true)
                .routine(ROUTINE_NAME)
                .policy(policy)
                .time(AbsBackupApi.getCreated(incrBackup));

        DtoRestoreJobStatus restoreJob = AbsRestoreApi.restoreTimestampSync(restoreRequest);
        assertThat(restoreJob.getInsertedRecords()).isEqualTo(1);

        assertThat(AerospikeDataUtils.get(KEY1).getString(STRING_BIN)).isEqualTo(firstValueCreate);
    }

    @Test
    void restoreTimestampManyObjects() {
        final var KEYS = 2_000;
        final var ITERATIONS = 10;

        long[] expectedData = new long[KEYS];

        // fill all with zeros
        for (int i = 0; i < KEYS; i++) {
            Key key = new Key(SOURCE_NAMESPACE, "set", i);
            AerospikeDataUtils.putNoLogs(key, new Bin("bin", 0));
        }

        AbsBackupApi.startFullBackupSync(ROUTINE_NAME);

        AutoUtils.waitUntilNextRoundSecond(10);

        // generate many incremental backups with some records overwritten multiple times during the process
        Bin payload = createComplexRecord(700);
        DtoBackupDetails dtoBackupDetails = null;
        for (int iteration = 1; iteration <= ITERATIONS; iteration++) {
            for (int i = 0; i < KEYS; i += iteration) {
                Key key = new Key(SOURCE_NAMESPACE, "set", i);
                AerospikeDataUtils.putNoLogs(key, new Bin("bin", iteration), payload);
                expectedData[i] = iteration;
            }
            dtoBackupDetails = AbsBackupApi.waitForIncrementalBackup(ROUTINE_NAME);
        }

        DtoRestoreTimestampRequest restoreRequest = new DtoRestoreTimestampRequest()
                .destination(SOURCE_CLUSTER)
                .routine(ROUTINE_NAME)
                .policy(defaultPolicy())
                .time(AbsBackupApi.getCreated(dtoBackupDetails));

        // restore with "usual" order
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        AerospikeDataUtils.put(new Key(SOURCE_NAMESPACE, "set", 0), "bin", -1);

        AutoUtils.sleep(5000);// wrong value,should be overwritten
        AbsRestoreApi.restoreTimestampSync(restoreRequest);
        AutoUtils.sleep(5000);

        assertRestoredData(KEYS, expectedData);

        // optimised restore
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        AbsRestoreApi.restoreTimestampSync(restoreRequest);
        assertRestoredData(KEYS, expectedData);
    }


    private static void assertRestoredData(int KEYS, long[] expectedData) {
        for (int i = 0; i < KEYS; i++) {
            Record record = AerospikeDataUtils.getSourceClient().get(null, new Key(SOURCE_NAMESPACE, "set", i));
            long value = record.getLong("bin");
            assertThat(value).isEqualTo(expectedData[i]);
        }
    }
}
