package api.abs.end2end.fullBackup;

import api.abs.AbsBackupApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import api.abs.generated.model.DtoBackupDetails;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AerospikeLogger;
import utils.abs.AbsRunner;
import utils.aerospike.abs.AerospikeDataUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-E2E")
class FilterBinSetTest extends AbsRunner {
    private static final String ROUTINE_NAME = "filterBySetAndBin";
    private static final String SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);

    private final String backupSet = "backupSet";
    private final String noBackupSet = "noBackupSet";
    private final String backupBin = "backupBin";
    private final String noBackupBin = "noBackupBin";

    @BeforeEach
    void setUpEach() {
        AerospikeDataUtils.truncateSourceSet(SOURCE_NAMESPACE, backupSet, noBackupSet);
    }

    @Test
    void backupFilterBySetAndBin() {
        final var routine = AbsRoutineApi.getRoutine(ROUTINE_NAME);
        AerospikeLogger.info("filter by set and bin routine:\n" + routine);
        assertThat(routine.getSetList()).contains(backupSet).doesNotContain(noBackupSet);
        assertThat(routine.getBinList()).contains(backupBin).doesNotContain(noBackupBin);


        final Key backupKey = new Key(SOURCE_NAMESPACE, backupSet, "IT1");
        final Key noBackupKey = new Key(SOURCE_NAMESPACE, noBackupSet, "IT1");

        var initialValue = "init value " + UUID.randomUUID();
        List.of(backupKey, noBackupKey).forEach(key -> {
            AerospikeDataUtils.put(key,
                    new Bin(backupBin, initialValue),
                    new Bin(noBackupBin, initialValue)
            );
        });

        // wait till backup
        DtoBackupDetails backupDetails = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        assertThat(backupDetails.getRecordCount()).isEqualTo(1);

        AerospikeDataUtils.delete(backupKey, noBackupKey);

        AbsRestoreApi.restoreFullSync(backupDetails.getKey(), ROUTINE_NAME);

        // assertion
        Record record = AerospikeDataUtils.get(backupKey);

        assertThat(record).isNotNull();
        assertThat(record.bins)
                .hasSize(1)
                .containsEntry(backupBin, initialValue);

        Record noRecord = AerospikeDataUtils.get(noBackupKey);
        assertThat(noRecord).isNull();
    }
}