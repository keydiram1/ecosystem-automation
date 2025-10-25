package api.abs.recover;

import api.abs.AbsBackupApi;
import api.abs.AbsPolicyApi;
import api.abs.AbsRoutineApi;
import com.aerospike.client.Key;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.DockerManager;
import utils.abs.AbsRunner;
import utils.aerospike.abs.AerospikeDataUtils;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("ABS-SEQUENTIAL-TESTS")
@Execution(ExecutionMode.SAME_THREAD)
@DisabledIfSystemProperty(named = "qa_environment", matches = "GCP")
@EnabledIfSystemProperty(named = "STORAGE_PROVIDER", matches = "local")
public class RetryBackupTest extends AbsRunner {
    private static final String ROUTINE_NAME = "minio";
    private static final String ROUTINE_NAME2 = "fullBackup1";
    private static final String ROUTINE_NAME3 = "fullBackup2";
    private static Key KEY1;
    private static Key KEY2;
    private static Key KEY3;

    private static final String STRING_BIN = "StringBin";

    @BeforeAll
    static void setUp() {
        String SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        KEY1 = new Key(SOURCE_NAMESPACE, "set", "key1");

        String SOURCE_NAMESPACE2 = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME2);
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE2);
        KEY2 = new Key(SOURCE_NAMESPACE2, "set", "key1");

        String SOURCE_NAMESPACE3 = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME3);
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE3);
        KEY3 = new Key(SOURCE_NAMESPACE3, "set", "key1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"aerospike-source", "minio"})
    void serviceNotAvailable(String containerId) {
        AutoUtils.sleep(5_000);
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        DockerManager.stopContainer(containerId);
        long backupTime = System.currentTimeMillis();
        AbsBackupApi.scheduleFullBackup(ROUTINE_NAME); // backup will fail now
        AutoUtils.sleep(5_000);
        DockerManager.startContainer(containerId);

        // backup should happen after retry
        AbsBackupApi.waitForFullBackup(ROUTINE_NAME, backupTime);
    }

    @Test
    void testRetryDelay() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY2, STRING_BIN, firstValueCreate);

        DockerManager.stopContainer("aerospike-source");
        long backupTime = System.currentTimeMillis();
        AbsBackupApi.scheduleFullBackup(ROUTINE_NAME2); // backup will fail now
        AutoUtils.sleep(5_000);
        DockerManager.startContainer("aerospike-source");

        Instant startTime = Instant.now();
        AbsBackupApi.waitForFullBackup(ROUTINE_NAME2, backupTime);
        Duration backupDuration = Duration.between(startTime, Instant.now());
        AerospikeLogger.info("Backup took " + backupDuration.getSeconds() + " seconds");
    }

    @Test
    void testMaxRetries() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY3, STRING_BIN, firstValueCreate);

        DockerManager.stopContainer("aerospike-source");
        long backupTime = System.currentTimeMillis();
        AbsBackupApi.scheduleFullBackup(ROUTINE_NAME3); // backup will fail now
        AutoUtils.sleep(5_000);
        DockerManager.startContainer("aerospike-source");

        String backupPolicyName = AbsRoutineApi.getRoutine(ROUTINE_NAME3).getBackupPolicy();
        var retryPolicy = AbsPolicyApi.getPolicy(backupPolicyName).getRetryPolicy();
        assertThat(retryPolicy).isNotNull();
        assertThat(retryPolicy.getMaxRetries()).isEqualTo(1);
        assertThat(retryPolicy.getBaseTimeout()).isEqualTo(1000);

        // retry delay is 1 second and max retries is 1 so after the 5 seconds sleep there should be no more retries.
        assertThatThrownBy(() -> {
            AbsBackupApi.waitForFullBackup(ROUTINE_NAME3, backupTime);
        }).isInstanceOf(ConditionTimeoutException.class)
                .hasMessageContaining("Full backup was not created within PT1M seconds");
    }
}