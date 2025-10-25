package api.backup.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClusterConnection {
    private String srcClusterName;
    private String srcClusterHost;
    private String backupDCName;
    private int srcClusterPort;
    private String srcClusterUser;
    private String srcClusterPwd;
    private SMDPolicy smdPolicy;
    private long smdLastExecuted;
    private List<String> clusterRoles;
    private TLSPolicy tlsPolicy;
    private Map<String, String> ipMap;
}


