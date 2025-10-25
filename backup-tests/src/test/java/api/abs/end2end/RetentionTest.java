package api.abs.end2end;

import api.abs.AbsBackupApi;
import api.abs.AbsPolicyApi;
import api.abs.AbsRoutineApi;
import api.abs.generated.model.DtoBackupDetails;
import api.abs.generated.model.DtoBackupPolicy;
import api.abs.generated.model.DtoBackupRoutine;
import api.abs.generated.model.DtoRetentionPolicy;
import com.aerospike.client.Key;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import utils.AutoUtils;
import utils.abs.AbsRunner;
import utils.aerospike.abs.AerospikeDataUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static api.abs.AbsBackupApi.*;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-E2E")
public class RetentionTest extends AbsRunner {
    private static String ROUTINE_NAME;
    private static String POLICY_NAME;

    // this namespace is only used with sets, no other test truncates it all
    private static final String SOURCE_NAMESPACE = "source-ns8";
    private static final String CLUSTER_NAME = "absDefaultCluster";
    private static final String SET = "retentionSet";
    private static final String STRING_BIN = "string";
    private static final int INCREMENTAL_BACKUP_INTERVAL = 3;

    @BeforeEach
    void setUp() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        ROUTINE_NAME = "retention-test-routine-" + uniqueId;
        POLICY_NAME = "retention-test-policy-" + uniqueId;
        AbsPolicyApi.createPolicy(POLICY_NAME, new DtoBackupPolicy().sealed(true));

        AbsRoutineApi.createRoutine(ROUTINE_NAME, new DtoBackupRoutine()
                .backupPolicy(POLICY_NAME)
                .intervalCron("@yearly")
                .incrIntervalCron("0/" + INCREMENTAL_BACKUP_INTERVAL + " * * * * *") // every INCREMENTAL_BACKUP_INTERVAL seconds
                .namespaces(List.of(SOURCE_NAMESPACE))
                .setList(List.of(SET))
                .sourceCluster(CLUSTER_NAME)
                .storage(absStorageName));
    }

    @AfterEach
    void tearDown() {
        AbsRoutineApi.deleteRoutine(ROUTINE_NAME);
        AbsPolicyApi.deletePolicy(POLICY_NAME);
    }

    @ParameterizedTest
    @MethodSource("retentionScenarios")
    void retentionTest(DtoRetentionPolicy retentionPolicy,
                       int expectedFulls, int expectedIncrementalsForLastNFulls) {
        AbsPolicyApi.updatePolicy(POLICY_NAME, new DtoBackupPolicy().retention(retentionPolicy));

        List<Long> fullBackupTimestamps = new ArrayList<>();
        Map<Long, Long> incrementalTimestampsByFull = new HashMap<>();

        AutoUtils.waitUntilNextRoundSecond(INCREMENTAL_BACKUP_INTERVAL);
        AutoUtils.sleep(500);
        for (int i = 0; i < 4; i++) {
            Long actualFullTimestamp = startFullBackupSync(ROUTINE_NAME).getTimestamp();

            AerospikeDataUtils.put(new Key(SOURCE_NAMESPACE, SET, "key-" + i), STRING_BIN, "val-" + i);
            DtoBackupDetails incremental = waitForIncrementalBackup(ROUTINE_NAME);

            fullBackupTimestamps.add(actualFullTimestamp);
            incrementalTimestampsByFull.put(actualFullTimestamp, incremental.getTimestamp());
        }

        // 3. Perform verification using the parameters
        List<DtoBackupDetails> allFulls = AbsBackupApi.getFullBackupsInRange(ROUTINE_NAME, null, null);

        // Verify the number of retained full backups
        assertThat(allFulls.size()).isEqualTo(expectedFulls);

        // Get timestamps of the full backups that SHOULD have been retained
        List<Long> expectedRetainedFullTimestamps = fullBackupTimestamps.stream()
                .sorted()
                .skip(fullBackupTimestamps.size() - expectedFulls)
                .collect(Collectors.toList());

        List<Long> actualRetainedFullTimestamps = allFulls.stream()
                .map(DtoBackupDetails::getTimestamp)
                .collect(Collectors.toList());

        assertThat(actualRetainedFullTimestamps).containsExactlyInAnyOrderElementsOf(expectedRetainedFullTimestamps);

        // Verify incrementals
        List<Long> retainedIncrementals = getIncrementalBackups(ROUTINE_NAME).stream()
                .filter(b -> b.getRecordCount() > 0) // only non-empty incrementals
                .map(DtoBackupDetails::getTimestamp)
                .collect(Collectors.toList());

        // Determine which incrementals we expect to see
        List<Long> expectedIncrementals = expectedRetainedFullTimestamps.stream()
                .sorted() // sort to easily skip the correct ones
                .skip(expectedRetainedFullTimestamps.size() - expectedIncrementalsForLastNFulls)
                .map(incrementalTimestampsByFull::get)
                .collect(Collectors.toList());

        assertThat(retainedIncrementals).containsExactlyInAnyOrderElementsOf(expectedIncrementals);
    }

    // This method provides the arguments for the different test scenarios
    private static Stream<Arguments> retentionScenarios() {
        // We create 4 backups in the test. The expectations are based on that.
        return Stream.of(
                // Arguments: DtoRetentionPolicy, expectedFulls, expectedIncrementalsForLastNFulls

                // Scenario: Retain ALL full backups (full = null)
                Arguments.of(new DtoRetentionPolicy().incremental(2), 4, 2),

                // Your original test case
                Arguments.of(new DtoRetentionPolicy().full(3).incremental(2), 3, 2),

                // Scenario: Retain ALL incrementals for retained fulls (incr = null)
                Arguments.of(new DtoRetentionPolicy().full(3), 3, 3),

                // Scenario: Retain ALL backups of both types (both = null)
                Arguments.of(new DtoRetentionPolicy(), 4, 4),

                // Scenario: No incremental backups (incr = 0)
                Arguments.of(new DtoRetentionPolicy().full(3).incremental(0), 3, 1), // only one incremental should be present (after last full)

                // Edge Case: Minimum retention
                Arguments.of(new DtoRetentionPolicy().full(1).incremental(1), 1, 1)
        );
    }
}
