package api.abs;

import api.abs.generated.ApiResponse;
import api.abs.generated.api.ConfigurationApi;
import api.abs.generated.model.DtoAerospikeCluster;
import api.abs.generated.model.DtoCredentials;
import api.abs.generated.model.DtoSeedNode;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import utils.abs.AbsRunner;

import java.util.List;
import java.util.Map;

import static api.abs.API.apiClient;

@UtilityClass
@Slf4j
public class AbsClusterApi {

    public ConfigurationApi configurationApi = new ConfigurationApi(apiClient);

    public static Map<String, DtoAerospikeCluster> getAllClusters() {
        return configurationApi.readAllClusters();
    }

    public static DtoAerospikeCluster getCluster(String name) {
        return configurationApi.readCluster(name);
    }

    public static ApiResponse<Void> addCluster(String host, String name, String password, int port, String user) {
        DtoAerospikeCluster cluster = new DtoAerospikeCluster()
                .credentials(new DtoCredentials()
                        .user(user)
                        .password(password))
                .seedNodes(List.of(new DtoSeedNode()
                                .hostName(host)
                                .port(port)
                        )
                );
        return configurationApi.addClusterWithHttpInfo(name, cluster);
    }

    public static ApiResponse<Void> createCluster(String name) {
        return addCluster(AbsRunner.getDockerHost(), name, "psw", 3000, "tester");
    }

    @SneakyThrows
    public static ApiResponse<Void> deleteCluster(String clusterName) {
        return configurationApi.deleteClusterWithHttpInfo(clusterName);
    }
}
