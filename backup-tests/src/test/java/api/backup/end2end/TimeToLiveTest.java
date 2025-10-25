package api.backup.end2end;

import api.backup.BackupManager;
import api.backup.RestoreApi;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.policy.WritePolicy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ADR-E2E")
class TimeToLiveTest extends BackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns13";
    private static final String SOURCE_CLUSTER_NAME = "TimeToLiveTest";
    private static final String BACKUP_NAMESPACE = "adr-ns13";
    private static final String BACKUP_NAME = "TimeToLiveBackup";
    private static final String POLICY_NAME = "TimeToLivePolicy";
    private static final String DC_NAME = "TimeToLiveDC";
    private static final Key KEY = new Key(SOURCE_NAMESPACE, "set1", "ContinuousBackup2TestKey");
    private static final String DIGEST = AerospikeDataUtils.getDigestFromKey(KEY);
    private static final IAerospikeClient srcClient = AerospikeDataUtils.getSourceClient();

    @AfterAll
    static void afterAll() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
    }

    @BeforeEach
    public void setUp() {
        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME, DC_NAME);
    }

    @Test
    void testTTl() {
        WritePolicy policy = new WritePolicy();
        policy.expiration = 20;
        srcClient.put(policy, KEY, new Bin("value", "expiring"));
        BackupManager.waitForBackup(BACKUP_NAME, KEY, 1);

        assertThat(srcClient.get(null, KEY)).isNotNull();
        long beforeExpiration = System.currentTimeMillis();

        // wait till expiration event is shipped. Might take a while
        BackupManager.waitForBackup(BACKUP_NAME, KEY, 2, 240);
        assertThat(srcClient.get(null, KEY)).isNull();
        long afterExpiration = System.currentTimeMillis();

        RestoreApi.restoreRecord(beforeExpiration, DIGEST, KEY.setName, SOURCE_CLUSTER_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, SOURCE_NAMESPACE);
        assertThat(srcClient.get(null, KEY)).isNotNull();

        RestoreApi.restoreRecord(afterExpiration, DIGEST, KEY.setName, SOURCE_CLUSTER_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, SOURCE_NAMESPACE);
        assertThat(srcClient.get(null, KEY)).isNull();
    }
}
