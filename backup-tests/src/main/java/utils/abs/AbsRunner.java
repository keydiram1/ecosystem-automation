package utils.abs;

import api.abs.AbsStorageApi;
import api.abs.generated.model.DtoAzureStorage;
import api.abs.generated.model.DtoGcpStorage;
import api.abs.generated.model.DtoS3Storage;
import api.abs.generated.model.DtoStorage;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.policy.ClientPolicy;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.ConfigParametersHandler;
import utils.K8sUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.files.PropertiesHandler;

public class AbsRunner {

    public static final String AEROSPIKE_SOURCE_SERVER_IP;
    public static final int AEROSPIKE_SOURCE_SERVER_PORT;
    public static final ClientPolicy CLIENT_POLICY_SOURCE;
    public static final String SECRET_AGENT_K8S_DNS_NAME = "gateway.ecosys.internal";
    private static AbsLogHandler ABS_LOG_HANDLER;

    protected static final IAerospikeClient srcClient;
    public static String ENV_WORKSPACE;
    public static String absStorageProvider;
    public static String absStorageName;

    static {
        PropertiesHandler.addSystemProperties("../devops/install/abs/.env");

        absStorageProvider = ConfigParametersHandler.getParameter("ABS_STORAGE_PROVIDER");
        AerospikeLogger.info("absStorageProvider=" + absStorageProvider);

        if (AutoUtils.isRunningOnGCP()) {
            ENV_WORKSPACE = ConfigParametersHandler.getParameter("ENV_WORKSPACE");
            AerospikeLogger.info("ENV_WORKSPACE=" + ENV_WORKSPACE);

            AEROSPIKE_SOURCE_SERVER_IP = "asd." + ENV_WORKSPACE + ".ecosys.internal";
            AerospikeLogger.info("AEROSPIKE_SOURCE_SERVER_IP: " + AEROSPIKE_SOURCE_SERVER_IP);

            AEROSPIKE_SOURCE_SERVER_PORT = 4333;
            System.setProperty("BACKUP_SERVICE_URL", "http://" + StringUtils.chop(ConfigParametersHandler.getParameter("ABS_DNS_NAME")));
            AerospikeLogger.info("BACKUP_SERVICE_URL: " + System.getProperty("BACKUP_SERVICE_URL"));
        } else {
            AerospikeLogger.info("Not running on GCP");
            ABS_LOG_HANDLER = new AbsLogHandler();
            AEROSPIKE_SOURCE_SERVER_IP = ConfigParametersHandler.getParameter("AEROSPIKE_SOURCE_IP");
            AerospikeLogger.info("AEROSPIKE_SOURCE_IP=" + AEROSPIKE_SOURCE_SERVER_IP);
            AEROSPIKE_SOURCE_SERVER_PORT = 3000;
        }
        absStorageName = AutoUtils.isRunningOnGCP() ? getGcpStorageName() : "local";
        AerospikeLogger.info("absStorageName=" + absStorageName);

        CLIENT_POLICY_SOURCE = setUpClientPolicy();
        if (AutoUtils.isRunningOnGCP()) {
            CLIENT_POLICY_SOURCE.tlsPolicy = TlsHandler.getTLSPolicy().build();
            CLIENT_POLICY_SOURCE.setTimeout(30_000);
        }
        srcClient = AerospikeDataUtils.getSourceClient();
    }

    private static boolean providerSet = false;

    @BeforeAll
    public static synchronized void init() {
        if (!providerSet) {
            providerSet = true;
            setStorageProvider();
        }
    }

    @AfterAll
    public static void cleanupClass() {
        if (ABS_LOG_HANDLER != null && !AutoUtils.isRunningOnMacos()) {
            AerospikeLogger.infoToFile(AutoUtils.runBashCommand("docker logs --tail 100000 backup-service", false));
        }
        if (AutoUtils.isRunningOnGCP())
            K8sUtils.printAllK8sPodLogs(false);
        AerospikeLogger.info("test ended");
    }

    public static String getDockerHost() {
        if (AutoUtils.isRunningOnGCP())
            return AEROSPIKE_SOURCE_SERVER_IP;
        return "host.docker.internal";
    }

    private static ClientPolicy setUpClientPolicy() {
        ClientPolicy clientPolicy = new ClientPolicy();
        clientPolicy.user = "tester";
        clientPolicy.password = "psw";
        // clientPolicy.ipMap = mapAerospikeClusterIps;
        clientPolicy.setTimeout(10000);
        clientPolicy.setMaxConnsPerNode(300);
        return clientPolicy;
    }

    public static String extractPublicIp(String input) {
        return input.replaceAll("[^\\d.]", " ").trim().split("\\s+")[0];
    }

    static void setStorageProvider() {
        AerospikeLogger.info("absStorageName=" + absStorageName);
        AerospikeLogger.info("absStorageProvider=" + absStorageProvider);

        DtoStorage testStorage = new DtoStorage();
        if (absStorageProvider.equals("gcp")) {
            AerospikeLogger.info("Setting storage provider to GCP");
            AerospikeLogger.info("GCP_SA_KEY_FILE=" + ConfigParametersHandler.getParameter("GCP_SA_KEY_FILE"));
            testStorage.setGcpStorage(new DtoGcpStorage()
                    .key(AutoUtils.getTextFromFile(ConfigParametersHandler.getParameter("GCP_SA_KEY_FILE"))).bucketName("abs-testing-bucket"));
        } else if (absStorageProvider.equals("azure")) {
            AerospikeLogger.info("Setting storage provider to azure");
            testStorage.setAzureStorage(new DtoAzureStorage()
                    .tenantId(ConfigParametersHandler.getParameter("AZURE_TENANT_ID"))
                    .clientId(ConfigParametersHandler.getParameter("AZURE_CLIENT_ID"))
                    .clientSecret(ConfigParametersHandler.getParameter("AZURE_CLIENT_SECRET"))
                    .containerName("abs-testing-bucket")
                    .endpoint("https://" + ConfigParametersHandler.getParameter("AZURE_STORAGE_ACCOUNT") + ".blob.core.windows.net/"));
        } else if (absStorageProvider.equals("aws")) {
            AerospikeLogger.info("Setting storage provider to aws");
            DtoS3Storage dtoS3Storage = new DtoS3Storage();
            dtoS3Storage.setAccessKeyId(ConfigParametersHandler.getParameter("AWS_ACCESS_KEY_ID"));
            dtoS3Storage.setSecretAccessKey(ConfigParametersHandler.getParameter("AWS_SECRET_ACCESS_KEY"));
            dtoS3Storage.setBucket("abs-testing-bucket");
            dtoS3Storage.setS3Region("il-central-1");
            testStorage.setS3Storage(dtoS3Storage);
        } else if (absStorageProvider.equals("local"))
            return;

        AbsStorageApi.updateStorage(absStorageName, testStorage);
    }

    private static String getGcpStorageName() {
        return AbsStorageApi.getAllStorage()
                .keySet()
                .stream()
                .findFirst()
                .orElse("Could not retrieve name from getGcpStorageName");
    }
}
