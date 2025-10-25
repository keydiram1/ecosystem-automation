package api.backup;

import api.backup.dto.ClusterConnection;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.google.common.base.Preconditions;
import io.restassured.response.Response;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.ConfigParametersHandler;

import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * High level operation on Aerospike Database Recovery
 */
@UtilityClass
public class BackupManager {

    public static final boolean isDynamicXdr = "false".equals(ConfigParametersHandler.getParameter("STATIC_CONFIGURATION"));
    private static final IAerospikeClient backupClient = AerospikeDataUtils.getBackupClient();

    public static Duration waitForBackup(String backupName, Key key, int numberOfBackup) {
        return waitForBackup(backupName, key, numberOfBackup, 120);
    }

    public static Duration waitForBackup(String backupName, Key key, int numberOfBackup, int waitAtMost) {
        if (!BackupApi.isBackupExists(backupName)) {
            throw new IllegalArgumentException("Backup %s not exist".formatted(backupName));
        }

        final String srcDigest = HexFormat.of().formatHex(key.digest);
        AerospikeLogger.info("Searching for " + numberOfBackup + " backups with key srcDigest: " + srcDigest);
        long start = System.currentTimeMillis();
        Awaitility.waitAtMost(Duration.ofSeconds(waitAtMost))
                .pollDelay(Duration.ofMillis(100))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> {
                    List<Long> timestamps = MetadataAPI.readBackupTimestampsForKey(backupName, 0, Long.MAX_VALUE, key);
                    return timestamps.size() == numberOfBackup;
                });
        Duration duration = Duration.ofMillis(System.currentTimeMillis() - start);
        AerospikeLogger.info("Waiting for backup took: " + duration);
        return duration;
    }

    public static boolean backupForKeyExist(String backupName, Key... keys) {
        return backupForKeyExist(backupName, 0, keys);
    }

    public static boolean backupForKeyExist(String backupName, long from, Key... keys) {
        Map<String, List<Long>> timestamps = MetadataAPI.readBackupTimestampsForKeys(backupName, from, Long.MAX_VALUE, keys);
        return timestamps.values().stream().noneMatch(List::isEmpty);
    }

    public static List<String> backupForKeysExist(String backupName, Key... keys) {
        Map<String, List<Long>> timestamps = MetadataAPI.readBackupTimestampsForKeys(backupName, 0, Long.MAX_VALUE, keys);
        return timestamps.entrySet().stream().filter(it -> !it.getValue().isEmpty()).map(Map.Entry::getKey).toList();
    }

    public static void createEnabledBackup(String backupName, String sourceClusterName, String sourceNS, String backupNS, String policyName, String dcName) {
        createEnabledBackup(backupName, sourceClusterName, sourceNS, backupNS, policyName, null, 86400, dcName);
    }

    public static void createEnabledBackup(String backupName, String sourceClusterName, String sourceNS, String backupNS, String policyName, String dcName, List<String> sets) {
        createEnabledBackup(backupName, sourceClusterName, sourceNS, backupNS, policyName, sets, 86400, dcName);
    }

    public static void createEnabledBackup(String backupName, String sourceClusterName, String sourceNS, String backupNS, String policyName, int smdDuration, String dcName) {
        createEnabledBackup(backupName, sourceClusterName, sourceNS, backupNS, policyName, null, smdDuration, dcName);
    }

    public static void createEnabledBackup(String backupName, String sourceClusterName, String sourceNS, String backupNS, String policyName,
                                           List<String> sets, int smdDuration, String dcName) {
        Response policyResponse = PolicyApi.createPolicy(policyName, 1);
        Preconditions.checkState(policyResponse.getStatusCode() == HttpStatus.SC_CREATED, policyResponse.asPrettyString());

        Response connectionResponse = ClusterConnectionApi.createConnection(sourceClusterName, dcName, smdDuration);
        Preconditions.checkState(connectionResponse.getStatusCode() == HttpStatus.SC_CREATED, connectionResponse.asPrettyString());

        Response backupResponse = createBackup(backupName, sourceClusterName, sourceNS, backupNS, policyName, sets);
        Preconditions.checkState(backupResponse.getStatusCode() == HttpStatus.SC_CREATED, backupResponse.asPrettyString());

        Response enableBackupResponse = BackupApi.enableBackup(backupName);
        Preconditions.checkState(enableBackupResponse.getStatusCode() == HttpStatus.SC_ACCEPTED, enableBackupResponse.asPrettyString());
    }

    private static Response createBackup(String backupName, String sourceClusterName, String sourceNS, String backupNS, String policyName, List<String> sets) {
        if (sets != null) {
            return BackupApi.createBackup(backupName, sourceClusterName, sourceNS, backupNS, policyName, sets);
        }
        return BackupApi.createBackup(backupName, sourceClusterName, sourceNS, backupNS, policyName);
    }

    public static void cleanUp(String backupNamespace, String sourceNamespace, String sourceClusterName, String backupName, String policyName) {
        if (isDynamicXdr) {
            try {
                deleteBackup(backupName);
            } catch (Exception e) {
                AerospikeLogger.info(e.getMessage());
                AerospikeLogger.info("oops... The deletion of " + backupName + " failed. Let's try one more time");
                deleteBackup(backupName);
            }
            // Wait for namespace pending removal from DC, must before you can delete XDR DC
            AutoUtils.sleep(5000);
            deleteConnection(sourceClusterName);
            deletePolicy(policyName);
            SmdOperationsApi.removeSmdBackup(sourceClusterName, Long.MAX_VALUE);
        } else {
            backupClient.delete(null, new Key("catalog", "cluster-connections", sourceClusterName));
            backupClient.delete(null, new Key("catalog", "data-protection-policies", policyName));
            backupClient.delete(null, new Key("catalog", "continuous-backups", backupName));
        }
        AerospikeDataUtils.truncateBackupNamespace(backupNamespace);
        AerospikeDataUtils.truncateSourceNamespace(sourceNamespace);
    }

    private static void deleteBackup(String backupName) {
        if (BackupApi.isBackupExists(backupName)) {
            BackupApi.deleteBackup(backupName);
        }

        Awaitility.await()
                .pollInterval(Duration.ofSeconds(1))
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> !BackupApi.isBackupExists(backupName));
    }

    private static void deleteConnection(String sourceClusterName) {
        ClusterConnection connection = ClusterConnectionApi.getClusterConnectionFromAllClusterConnections(sourceClusterName);
        if (connection != null) {
            ClusterConnectionApi.deleteClusterConnection(sourceClusterName, false);
            Awaitility.await()
                    .pollInterval(Duration.ofSeconds(2))
                    .atMost(20, TimeUnit.SECONDS)
                    .alias("Delete existing DC")
                    .until(() -> {
                        String dcResponse = AerospikeDataUtils.getDCStats(connection.getBackupDCName());
                        if (StringUtils.startsWithIgnoreCase(dcResponse, "ERROR::")) {
                            AerospikeLogger.info("The source cluster " + sourceClusterName + " has been deleted.");
                        } else
                            AerospikeLogger.info("The source cluster " + sourceClusterName + " hasn't been deleted yet.");
                        return StringUtils.startsWithIgnoreCase(dcResponse, "ERROR::");
                    });
        }
    }

    private static void deletePolicy(String policyName) {
        if (PolicyApi.isPolicyExists(policyName)) {
            PolicyApi.deletePolicy(policyName);
            Awaitility.await()
                    .pollInterval(Duration.ofSeconds(1))
                    .until(() -> !PolicyApi.isPolicyExists(policyName));
        }
    }
}
