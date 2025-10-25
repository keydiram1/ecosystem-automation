package api.backup;

import com.aerospike.client.Key;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import io.restassured.response.ResponseBody;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AutoUtils;
import utils.AdrLogHandler;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ADR-QUEUE-RECOVER")
class QueueOperationsTest extends BackupRunner {
    private static final String SET_NAME = "QueueOperationsTestSet";
    private static final String SOURCE_NAMESPACE = "source-ns9";
    private static final String SOURCE_CLUSTER_NAME = "QueueOperationsTestSourceCluster";
    private static final String BACKUP_NAMESPACE = "adr-ns9";
    private static final String INITIAL_VALUE = "QueueOperationsTestInitialValue";
    private static final String BACKUP_NAME = "QueueOperationsTestBackup";
    private static final String POLICY_NAME = "QueueOperationsTestPolicy";
    private static final String DC_NAME = "QueueOperationsDC";
    private static final Key key = new Key(SOURCE_NAMESPACE, SET_NAME, "QueueOperationsTestKey");

    @BeforeEach
    void setUp() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    /**
     * This test must run right after the ADR auto installation ends.
     * Before you run the auto installation, you need to configure the
     * queue-handler-config.yml the way it's configured in the method
     * configQueueHandlerForQueueRecoverTest in the AdrScript.groovy.
     * The queuePullingIntervalMs=40000 makes sure the record won't be pulled from
     * the queue before we run the getQueueRecordsAndSetInProcess method.
     * The zombieRecoverIntervalMs=120000 makes sure that the record won't be
     * recovered before the first waitForBackup method in the test.
     */
    @Test
    void testQueueRecover() {
        AdrLogHandler AdrLogHandler = new AdrLogHandler();
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);
        AerospikeDataUtils.put(key, "value", INITIAL_VALUE);

        Gson gson = new GsonBuilder().serializeNulls().create();
        Awaitility.waitAtMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
                    ResponseBody<?> body = QueueOperationsApi.getQueueRecordsAndSetInProcess(BACKUP_NAMESPACE, SET_NAME).body();
                    String queueRecordInStatusNew = body.asPrettyString();
                    if (queueRecordInStatusNew.length() > 10) {
                        String status = gson.fromJson(queueRecordInStatusNew, JsonArray.class).get(0).getAsJsonObject().get("status").toString();
                        assertThat(status).isEqualTo("\"NEW\"");
                    }
                });

        Assertions.assertThatThrownBy(() -> BackupManager.waitForBackup(BACKUP_NAME, key, 1, 30))
                .isInstanceOf(ConditionTimeoutException.class);

        AutoUtils.sleep(61000); // Wait another minute(after the waitForBackup) for queue recovery
        // (zombieRecoverIntervalMs: 120000)

        BackupManager.waitForBackup(BACKUP_NAME, key, 1, 210);
        Assertions.assertThat(AdrLogHandler.getRestBackendLog()).doesNotContain("ERROR");
    }
}
