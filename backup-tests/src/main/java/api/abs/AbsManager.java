package api.abs;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AbsManager {

    public static void cleanup(String policy, String clusterName) {
        AbsPolicyApi.deletePolicy(policy);
        AbsClusterApi.deleteCluster(clusterName);
    }
}
