package api.abs.end2end;

import api.abs.AbsBackupApi;
import api.abs.AbsRoutineApi;
import api.abs.generated.model.DtoBackupDetails;
import com.aerospike.client.Key;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import utils.AerospikeLogger;
import utils.abs.AbsRunner;
import utils.aerospike.abs.AerospikeDataUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-SEQUENTIAL-TESTS-2")
@Execution(ExecutionMode.SAME_THREAD)
@DisabledIfSystemProperty(named = "qa_environment", matches = "GCP")
@EnabledIfSystemProperty(named = "STORAGE_PROVIDER", matches = "local")
public class SealedTest extends AbsRunner {
    @SneakyThrows
    @ParameterizedTest
    @CsvSource({"localStorageSealed, true", "localStorage, false"})
    void fullBackup(String routineName, boolean shouldHaveBackup) {
        final var namespace = AbsRoutineApi.getAnyNamespaceForRoutine(routineName);
        final Key key = new Key(namespace, "set", "IT");

        AbsBackupApi.scheduleFullBackup(routineName, 100);
        // backup will be triggered during generation
        AerospikeLogger.info("Generation started");
        long before = System.currentTimeMillis();
        while (System.currentTimeMillis() - before < 1_000) {
            AerospikeDataUtils.put(key, "TimeBin", System.currentTimeMillis());
        }

        long after = System.currentTimeMillis();

        List<DtoBackupDetails> backupsDuringGeneration = AbsBackupApi.getFullBackupsInRange(routineName, before, after);
        assertThat(backupsDuringGeneration).isNotEmpty();
        if (shouldHaveBackup) {
            assertThat(backupsDuringGeneration.get(0).getRecordCount()).isEqualTo(1);
        } else {
            assertThat(backupsDuringGeneration.get(0).getRecordCount()).isZero();
        }
    }

    @SneakyThrows
    @ParameterizedTest
    @CsvSource({"incrementalBackupClusterNotSealed, true", "incrementalBackupCluster, false"})
    void incrementalBackup(String routineName, boolean shouldHaveBackup) {
        AbsBackupApi.startFullBackupSync(routineName);
        final var namespace = AbsRoutineApi.getAnyNamespaceForRoutine(routineName);
        final Key key = new Key(namespace, "set", "IT");

        AerospikeLogger.info("Generation started");
        // incremental backup will happen during generation
        long before = System.currentTimeMillis();
        while (System.currentTimeMillis() - before < 10_000) {
            AerospikeDataUtils.put(key, "TimeBin", System.currentTimeMillis());
        }
        long after = System.currentTimeMillis();

        List<DtoBackupDetails> backupsDuringGeneration = AbsBackupApi.getIncrementalBackupsInRange(routineName, before, after);
        if (shouldHaveBackup) {
            assertThat(backupsDuringGeneration).isNotEmpty();
        } else {
            assertThat(backupsDuringGeneration).isEmpty();
        }
    }
}