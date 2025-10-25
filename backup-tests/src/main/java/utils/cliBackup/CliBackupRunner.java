package utils.cliBackup;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Host;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.policy.ClientPolicy;
import org.junit.jupiter.api.AfterAll;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.ConfigParametersHandler;
import utils.abs.TlsHandler;
import utils.files.PropertiesHandler;

public class CliBackupRunner {

    public static String AEROSPIKE_SOURCE_SERVER_IP;
    public static int AEROSPIKE_SOURCE_SERVER_PORT;
    public static ClientPolicy CLIENT_POLICY_SOURCE;
    protected static IAerospikeClient srcClient;
    public static String ENV_WORKSPACE;

    static {
        PropertiesHandler.addSystemProperties("../devops/install/cli-backup/.env");

        CLIENT_POLICY_SOURCE = setUpClientPolicy();

        AerospikeLogger.info("LOCAL_TLS_ENABLED=" + ConfigParametersHandler.getParameter("LOCAL_TLS_ENABLED"));

        if (AutoUtils.isRunningOnGCP()) {
            ENV_WORKSPACE = ConfigParametersHandler.getParameter("ENV_WORKSPACE");
            AerospikeLogger.info("ENV_WORKSPACE=" + ENV_WORKSPACE);

            AEROSPIKE_SOURCE_SERVER_IP = "asd." + ENV_WORKSPACE + ".ecosys.internal";
            AerospikeLogger.info("AEROSPIKE_SOURCE_SERVER_IP: " + AEROSPIKE_SOURCE_SERVER_IP);

            AEROSPIKE_SOURCE_SERVER_PORT = 4333;
            CLIENT_POLICY_SOURCE.tlsPolicy = TlsHandler.getTLSPolicy().build();
        } else if (ConfigParametersHandler.getParameter("IS_RUNNING_ON_LOCAL_3_NODES_ENV").equals("true")) {
            AerospikeLogger.info("SECRET_AGENT_IP: " + ConfigParametersHandler.getParameter("SECRET_AGENT_IP"));
            AerospikeLogger.info("SECRET_AGENT_PORT: " + ConfigParametersHandler.getParameter("SECRET_AGENT_PORT"));
            AerospikeLogger.info("ASDB_IP: " + ConfigParametersHandler.getParameter("ASDB_IP"));
            AerospikeLogger.info("ASDB_PORT: " + ConfigParametersHandler.getParameter("ASDB_PORT"));
            AerospikeLogger.info("CA_AEROSPIKE_COM_PEM_JKS_PATH: " + ConfigParametersHandler.getParameter("CA_AEROSPIKE_COM_PEM_JKS_PATH"));

            AEROSPIKE_SOURCE_SERVER_IP = ConfigParametersHandler.getParameter("ASDB_IP");
            AEROSPIKE_SOURCE_SERVER_PORT = Integer.parseInt(ConfigParametersHandler.getParameter("ASDB_PORT"));
            if (ConfigParametersHandler.getParameter("LOCAL_TLS_ENABLED").equals("true")) {
                CLIENT_POLICY_SOURCE.tlsPolicy = TlsHandler.getTLSPolicy().build();
            }
            CLIENT_POLICY_SOURCE.setTimeout(10000);
        } else {
            AEROSPIKE_SOURCE_SERVER_IP = ConfigParametersHandler.getParameter("AEROSPIKE_SOURCE_IP");
            AEROSPIKE_SOURCE_SERVER_PORT = 3000;
        }
        srcClient = createSourceClient();
    }

    @AfterAll
    public static void cleanupClass() {
        AerospikeLogger.info("test ended");
    }

    private static ClientPolicy setUpClientPolicy() {
        ClientPolicy clientPolicy = new ClientPolicy();
        clientPolicy.user = "tester";
        clientPolicy.password = "psw";
        return clientPolicy;
    }

    public static synchronized IAerospikeClient createSourceClient() {
        AerospikeLogger.info("in createSourceClient");
        AerospikeLogger.info("CLIENT_POLICY_SOURCE AEROSPIKE_SOURCE_SERVER_IP AEROSPIKE_SOURCE_SERVER_PORT =" +
                CLIENT_POLICY_SOURCE + " " + AEROSPIKE_SOURCE_SERVER_IP + " " + AEROSPIKE_SOURCE_SERVER_PORT);
        AutoUtils.sleep(1000);

        if (ConfigParametersHandler.getParameter("LOCAL_TLS_ENABLED").equals("true") || AutoUtils.isRunningOnGCP()) {
            AerospikeLogger.info("Creating TLS client");
            AerospikeLogger.info("Connect to DB with ip: " + AEROSPIKE_SOURCE_SERVER_IP +
                    " tls name: " + TlsHandler.TLS_NAME + " port: " + AEROSPIKE_SOURCE_SERVER_PORT);
            return new AerospikeClient(CLIENT_POLICY_SOURCE, new Host(AEROSPIKE_SOURCE_SERVER_IP, TlsHandler.TLS_NAME, AEROSPIKE_SOURCE_SERVER_PORT));
        } else {
            AerospikeLogger.info("creating regular(not tls) client");
            return new AerospikeClient(CLIENT_POLICY_SOURCE, AEROSPIKE_SOURCE_SERVER_IP, AEROSPIKE_SOURCE_SERVER_PORT);
        }
    }

}