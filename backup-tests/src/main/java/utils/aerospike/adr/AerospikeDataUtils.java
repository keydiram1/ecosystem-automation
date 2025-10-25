package utils.aerospike.adr;

import com.aerospike.client.*;
import lombok.experimental.UtilityClass;
import org.awaitility.core.ConditionTimeoutException;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.init.runners.BackupRunner;
import utils.init.runners.TlsHandler;

import java.util.HexFormat;

import static utils.aerospike.AerospikeCountUtils.waitForNamespaceTruncate;
import static utils.aerospike.AerospikeCountUtils.waitForSetTruncate;

@UtilityClass
public class AerospikeDataUtils {

    private static volatile IAerospikeClient sourceClient;
    private static volatile IAerospikeClient backupClient;

    public static void put(Key key, String binName, String binValue) {
        getSourceClient().put(null, key, new Bin(binName, binValue));
        String[] keyArr = key.toString().split(":");
        AerospikeLogger.info("Added the value " + binValue + " to the set " + keyArr[1]
                + " in the namespace " + keyArr[0]);
    }

    public static void put(Key key, String binName, Long binValue) {
        getSourceClient().put(null, key, new Bin(binName, binValue));
        String[] keyArr = key.toString().split(":");
        AerospikeLogger.info("Added the value " + binValue + " to the set " + keyArr[1]
                + " in the namespace " + keyArr[0]);
    }

    public static void put(Key key, Bin bin) {
        getSourceClient().put(null, key, bin);
        String[] keyArr = key.toString().split(":");
        AerospikeLogger.info("Added the bin " + bin.toString() + " to the set " + keyArr[1]
                + " in the namespace " + keyArr[0]);
    }

    public static void delete(Key key) {
        getSourceClient().delete(null, key);
        AerospikeLogger.info("Deleted the given key: " + key);
    }

    public static void truncateSourceNamespace(String namespace) {
        truncateNamespace(getSourceClient(), namespace);
    }

    public static void truncateBackupNamespace(String namespace) {
        truncateNamespace(getBackupClient(), namespace);
    }

    private static void truncateNamespace(IAerospikeClient client, String namespace) {
        int tries = 0;
        while (tries++ < 10) {
            try {
                AerospikeLogger.info("Try truncate: " + namespace);
                client.truncate(null, namespace, null, null);
                waitForNamespaceTruncate(client, namespace);
                return;
            } catch (AerospikeException e) {
                AerospikeLogger.info("Exception during truncate: " + e.getMessage());
                AutoUtils.sleep(tries * 1000L);
            } catch (ConditionTimeoutException ignored) {
            }
        }
        throw new RuntimeException("Could not truncate namespace " + namespace);
    }

    public static void truncateSourceSet(String namespace, String... sets) {
        truncateSet(getSourceClient(), namespace, sets);
    }

    public static void truncateSet(IAerospikeClient client, String namespace, String... sets) {
        if (sets != null) {
            for (String set : sets) {
                try {
                    AerospikeLogger.info("Try truncate: " + namespace + " set " + set);
                    client.truncate(null, namespace, set, null);
                    waitForSetTruncate(client, namespace, set);
                } catch (AerospikeException e) {
                    AerospikeLogger.info(e.getMessage());
                }
            }
        }
    }

    public static String getDCStats(String dc) {
        IAerospikeClient sourceClient = getSourceClient();
        return Info.request(sourceClient.getInfoPolicyDefault(), sourceClient.getCluster().getRandomNode(),
                "get-stats:context=xdr;dc=" + dc);
    }

    public static String getXdrConfig(String dc, String namespace) {
        IAerospikeClient sourceClient = getSourceClient();
        return Info.request(sourceClient.getInfoPolicyDefault(), sourceClient.getCluster().getRandomNode(),
                "get-config:context=xdr;dc=" + dc + ";namespace=" + namespace);
    }

    public static IAerospikeClient createSourceClient(int sourceServerPort) {
        if (TlsHandler.TLS_ENABLED) {
            return new AerospikeClient(BackupRunner.CLIENT_POLICY_SOURCE, new Host(
                    BackupRunner.AEROSPIKE_SOURCE_SERVER_IP,
                    TlsHandler.TLS_NAME_SOURCE,
                    sourceServerPort));
        } else {
            return new AerospikeClient(BackupRunner.CLIENT_POLICY_SOURCE, BackupRunner.AEROSPIKE_SOURCE_SERVER_IP,
                    sourceServerPort);
        }
    }

    public static IAerospikeClient getSourceClient() {
        if (sourceClient == null) {
            synchronized (AerospikeDataUtils.class) {
                if (sourceClient == null) {
                    sourceClient = createSourceClient(BackupRunner.AEROSPIKE_SOURCE_SERVER_PORT);
                }
            }
        }
        return sourceClient;
    }

    public static IAerospikeClient getBackupClient() {
        if (backupClient == null) {
            synchronized (AerospikeDataUtils.class) {
                if (backupClient == null) {
                    if (TlsHandler.TLS_ENABLED) {
                        backupClient = new AerospikeClient(BackupRunner.CLIENT_POLICY_BACKUP, new Host(
                                BackupRunner.AEROSPIKE_BACKUP_SERVER_IP,
                                TlsHandler.TLS_NAME_BACKUP,
                                BackupRunner.AEROSPIKE_BACKUP_SERVER_PORT));
                    } else {
                        backupClient = new AerospikeClient(BackupRunner.CLIENT_POLICY_BACKUP,
                                BackupRunner.AEROSPIKE_BACKUP_SERVER_IP,
                                BackupRunner.AEROSPIKE_BACKUP_SERVER_PORT);
                    }
                }
            }
        }
        return backupClient;
    }

    public String getDigestFromKey(Key key){
        return HexFormat.of().formatHex(key.digest);
    }
}
