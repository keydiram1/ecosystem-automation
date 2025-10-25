package api.backup;

import api.backup.dto.ClusterConnection;
import api.backup.dto.SMDPolicy;
import utils.init.runners.BackupRunner;
import utils.init.runners.TlsHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static utils.init.runners.BackupRunner.getAerospikeLocalClustersIpsMap;

public class JsonBodyCreator {

    private static final String STATIC_DC_NAME = "AdrDC";

    public static ClusterConnection getConnection(String srcClusterName, String dcName, int duration, int srcClusterPort) {
        return getConnection(srcClusterName, srcClusterPort,
                dcName, BackupRunner.CLIENT_POLICY_SOURCE.user, BackupRunner.CLIENT_POLICY_SOURCE.password,
                0, duration, 5000);
    }

    public static ClusterConnection getConnection(String srcClusterName, int srcClusterPort, String backupDCName,
                                                  String srcClusterUser, String srcClusterPwd,
                                                  int smdLastExecuted, int duration, int keepFor) {
        ClusterConnection clusterConnection = ClusterConnection.builder()
                .backupDCName(backupDCName)
                .srcClusterUser(srcClusterUser)
                .srcClusterPwd(srcClusterPwd)
                .smdLastExecuted(smdLastExecuted)
                .srcClusterHost(BackupRunner.getDockerHost())
                .srcClusterPort(srcClusterPort)
                .srcClusterName(srcClusterName)
                .clusterRoles(List.of("SHIPPING", "RESTORE"))
                .smdPolicy(SMDPolicy.builder()
                        .duration(duration)
                        .keepFor(keepFor)
                        .build())
                .ipMap(getAerospikeLocalClustersIpsMap(BackupRunner.getDockerHost()))
                .build();
        if (TlsHandler.TLS_ENABLED) {
            clusterConnection.setTlsPolicy(TlsHandler.constructSourceTLSPolicy());
        }
        if (!BackupManager.isDynamicXdr) { //static require use same dc
            clusterConnection.setBackupDCName(STATIC_DC_NAME);
        }
        return clusterConnection;
    }

    public static Map<String, Object> getBackup(String name, String description, String srcClusterName,
                                                String srcNS, String backupNS, String policy) {
        return getBackup(name, description, srcClusterName, srcNS, backupNS, policy, new ArrayList<>());
    }

    public static Map<String, Object> getBackup(String name, String description, String srcClusterName,
                                                String srcNS, String backupNS, String policy, List<String> sets) {
        Map<String, Object> backup = new LinkedHashMap<>();
        backup.put("name", name);
        backup.put("description", description);
        backup.put("srcClusterName", srcClusterName);
        backup.put("srcNS", srcNS);
        backup.put("backupNS", backupNS);
        backup.put("policy", policy);
        backup.put("sets", sets);

        return backup;
    }
}
