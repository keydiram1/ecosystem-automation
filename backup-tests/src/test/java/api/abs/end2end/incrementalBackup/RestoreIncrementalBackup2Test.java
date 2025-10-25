package api.abs.end2end.incrementalBackup;

import api.abs.AbsBackupApi;
import api.abs.AbsRoutineApi;
import com.aerospike.client.Key;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import utils.abs.AbsRunner;
import utils.aerospike.abs.AerospikeDataUtils;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-E2E")
class RestoreIncrementalBackup2Test extends AbsRunner {
    private static final String STRING_BIN = "RestoreTestBin";

    @Test
    @DisabledIfSystemProperty(named = "qa_environment", matches = "GCP")
    void keepFilesForIncrementalBackup() {
        String routineName = "localStorageIncremental1";
        final var routine = AbsRoutineApi.getRoutine(routineName);
        String set = routine.getSetList().get(0);
        final var namespace = AbsRoutineApi.getAnyNamespaceForRoutine(routine);
        final Key key = new Key(namespace, set, "IT");
        long putTime = AerospikeDataUtils.put(key, STRING_BIN, "someValue");

        AbsBackupApi.waitForIncrementalBackup(routineName, putTime).getKey();

        int numberOfBackups = AbsBackupApi.getIncrementalBackups(routineName).size();
        assertThat(numberOfBackups).isGreaterThan(0);

        AbsBackupApi.startFullBackupSync(routineName);
        numberOfBackups = AbsBackupApi.getIncrementalBackups(routineName).size();
        assertThat(numberOfBackups).isGreaterThan(0);
    }

    @Test
    @DisabledIfSystemProperty(named = "qa_environment", matches = "GCP")
    void getIncrementalBackupsInRange() {
        String routineName = "localStorageIncremental1";
        AbsBackupApi.startFullBackupSync(routineName);
        final var routine = AbsRoutineApi.getRoutine(routineName);
        String set = routine.getSetList().get(0);
        final var namespace = AbsRoutineApi.getAnyNamespaceForRoutine(routine);
        final Key key = new Key(namespace, set, "IT");

        long beforeBackups = System.currentTimeMillis();
        long putTime = AerospikeDataUtils.put(key, STRING_BIN, "firstValue");
        AbsBackupApi.waitForIncrementalBackup(routineName, putTime).getKey();
        putTime = AerospikeDataUtils.put(key, STRING_BIN, "secondValue");
        AbsBackupApi.waitForIncrementalBackup(routineName, putTime).getKey();
        long afterTwoBackups = System.currentTimeMillis();

        putTime = AerospikeDataUtils.put(key, STRING_BIN, "thirdValue");
        AbsBackupApi.waitForIncrementalBackup(routineName, putTime).getKey();
        long afterFourBackups = System.currentTimeMillis();

        int allBackups = AbsBackupApi.getIncrementalBackupsInRange(routineName, beforeBackups, null).size();
        assertThat(allBackups).isEqualTo(3);
        allBackups = AbsBackupApi.getAllIncrementalBackupsInRange(beforeBackups, null).get(routineName).size();
        assertThat(allBackups).isEqualTo(3);

        int firstTwoBackups = AbsBackupApi.getIncrementalBackupsInRange(routineName, beforeBackups, afterTwoBackups).size();
        assertThat(firstTwoBackups).isEqualTo(2);
        firstTwoBackups = AbsBackupApi.getAllIncrementalBackupsInRange(beforeBackups, afterTwoBackups).get(routineName).size();
        assertThat(firstTwoBackups).isEqualTo(2);

        int lastBackup = AbsBackupApi.getIncrementalBackupsInRange(routineName, afterTwoBackups, afterFourBackups).size();
        assertThat(lastBackup).isEqualTo(1);
        lastBackup = AbsBackupApi.getAllIncrementalBackupsInRange(afterTwoBackups, afterFourBackups).get(routineName).size();
        assertThat(lastBackup).isEqualTo(1);
    }
}