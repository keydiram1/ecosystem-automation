package utils.init.runners;

import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.TlsPolicy;
import lombok.NonNull;
import org.junit.jupiter.api.AfterAll;
import utils.*;
import utils.files.PropertiesHandler;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BackupRunner {

    public static final String AEROSPIKE_SOURCE_SERVER_IP;
    public static final int AEROSPIKE_SOURCE_SERVER_PORT;
    public static final String AEROSPIKE_BACKUP_SERVER_IP;
    public static final int AEROSPIKE_BACKUP_SERVER_PORT;
    public static final ClientPolicy CLIENT_POLICY_SOURCE;
    public static final ClientPolicy CLIENT_POLICY_BACKUP;
    private static final AdrLogHandler adrLogHandler;
    public static volatile boolean printAllLogs = true;
    private static volatile Map<String, String> mapAerospikeClusterIps;

    static {
        adrLogHandler = new AdrLogHandler();
        PropertiesHandler.setProperties();

        if (AutoUtils.isRunningOnGCP()) {
            AEROSPIKE_BACKUP_SERVER_IP = AwsUtils.getEc2PublicIp(getIdList(ConfigParametersHandler.getParameter("backupInstanceIds")).get(0));
            AEROSPIKE_SOURCE_SERVER_IP = AwsUtils.getEc2PublicIp(getIdList(ConfigParametersHandler.getParameter("sourceInstanceIds")).get(0));
            AEROSPIKE_BACKUP_SERVER_PORT = 3000;
            AEROSPIKE_SOURCE_SERVER_PORT = 3000;
            System.setProperty("rest.backend.url", "http://" + ConfigParametersHandler.getParameter("restBackendUrl"));
            mapAerospikeClusterIps = getAerospikeAwsClustersIpsMap();
        } else {
            AEROSPIKE_BACKUP_SERVER_IP = ConfigParametersHandler.getParameter("aerospike.backup.ip");
            AEROSPIKE_SOURCE_SERVER_IP = ConfigParametersHandler.getParameter("aerospike.source.ip");
            AEROSPIKE_BACKUP_SERVER_PORT = Integer.parseInt(ConfigParametersHandler.getParameter("aerospike.backup.port"));
            AEROSPIKE_SOURCE_SERVER_PORT = Integer.parseInt(ConfigParametersHandler.getParameter("aerospike.source.port"));
            if (ConfigParametersHandler.getParameter("INSTALL_3_NODES_SOURCE_CLUSTER").equals("true")) {
                mapAerospikeClusterIps = getAerospikeLocalClustersIpsMap("127.0.0.1");
            }
        }

        if (TlsHandler.TLS_ENABLED) {
            CLIENT_POLICY_SOURCE = setUpClientPolicyWithTLS(TlsHandler.constructSourceTLSPolicyLocally().build());
            CLIENT_POLICY_BACKUP = setUpClientPolicyWithTLS(TlsHandler.constructBackupTLSPolicyLocally().build());
        } else {
            CLIENT_POLICY_SOURCE = setUpClientPolicy();
            CLIENT_POLICY_BACKUP = setUpClientPolicy();
        }
    }

    //    @BeforeEach //Enable this when you want to monitor the Docker containers
    public void setupAllTestInAllClasses() {
        AutoUtils.printDockerStats();
    }

    @AfterAll
    public static void cleanupClass() {
        if (printAllLogs)
            adrLogHandler.printAllLogs();
    }

    public static String getDockerHost() {
        if (AutoUtils.isRunningOnGCP())
            return AEROSPIKE_SOURCE_SERVER_IP;
        return "host.docker.internal";
    }

    private static synchronized Map<String, String> getAerospikeAwsClustersIpsMap() {
        Map<String, String> mapPrivatePublicIds = new HashMap<>();
        List<String> listSourceIds = getIdList(ConfigParametersHandler.getParameter("sourceInstanceIds"));
        List<String> listBackupIds = getIdList(ConfigParametersHandler.getParameter("backupInstanceIds"));
        List<String> allIds = Stream.concat(listSourceIds.stream(), listBackupIds.stream()).toList();
        for (String id : allIds) {
            mapPrivatePublicIds.put(AwsUtils.getEc2PrivateIp(id), AwsUtils.getEc2PublicIp(id));
        }
        AerospikeLogger.info("Map of all Aerospike ips: " + mapPrivatePublicIds);
        return mapPrivatePublicIds;
    }

    public static synchronized Map<String, String> getAerospikeLocalClustersIpsMap(String ip) {
        List<String> sources = List.of("aerospike-source", "aerospike-source2", "aerospike-source3");
        return sources.stream()
                .filter(DockerManager::containerExist)
                .collect(Collectors.toMap(
                        DockerManager::getHostname,
                        it -> ip
                ));
    }

    private static List<String> getIdList(@NonNull String listString) {
        return new ArrayList<>(Arrays.asList(listString.split(",")));
    }

    private static ClientPolicy setUpClientPolicy() {
        ClientPolicy clientPolicy = new ClientPolicy();
        clientPolicy.user = "tester";
        clientPolicy.password = "psw";
        clientPolicy.ipMap = mapAerospikeClusterIps;
        return clientPolicy;
    }

    private static ClientPolicy setUpClientPolicyWithTLS(TlsPolicy policy) {
        ClientPolicy clientPolicy = setUpClientPolicy();
        clientPolicy.tlsPolicy = policy;
        return clientPolicy;
    }
}
