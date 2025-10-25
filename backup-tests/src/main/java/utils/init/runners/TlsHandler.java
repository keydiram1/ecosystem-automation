package utils.init.runners;

import api.backup.dto.TLSPolicy;
import utils.AutoUtils;
import utils.VaultUtils;
import utils.ConfigParametersHandler;

import java.util.List;

public class TlsHandler {

    public static final boolean TLS_ENABLED = Boolean.parseBoolean(ConfigParametersHandler.getParameter("TLS_ENABLED"));
    public static final String TLS_NAME_SOURCE = AutoUtils.isRunningOnGCP() ? "source.cluster" : "source.server";
    public static final String TLS_NAME_BACKUP = AutoUtils.isRunningOnGCP() ? "backup.cluster" : "backup.server";

    /**
     * For source cluster:
     * Truststore (.jks) and Keystore (.p12) are loaded twice:
     * 1. Locally during initialization.
     * 2. When creating a cluster connection.
     * This method is responsible for loading the truststore and keystore during cluster connection creation (on rest-backend).
     */
    public static TLSPolicy constructSourceTLSPolicy() {
        TLSPolicy adrTLSPolicy = constructTLSPolicyWithCommonConfig();
        adrTLSPolicy.setName(TLS_NAME_SOURCE);
        if (AutoUtils.isRunningOnGCP()) {
            adrTLSPolicy.setKeystorePath("/etc/tls/source.cluster.keystore.p12");
            adrTLSPolicy.setTruststorePath("/etc/tls/source.cluster.truststore.jks");
        } else {
            adrTLSPolicy.setKeystorePath("/adr/conf/tls/etc/pki/private/source/source.client.chain.p12");
            adrTLSPolicy.setTruststorePath("/adr/conf/tls/etc/pki/certs/source/source.ca.jks");
        }
        return adrTLSPolicy;
    }

    /**
     * For source cluster:
     * Truststore (.jks) and Keystore (.p12) are loaded twice:
     * 1. Locally during initialization.
     * 2. When creating a cluster connection.
     * This method is responsible for loading the truststore and keystore during initialization locally (by automation).
     */
    public static TLSPolicy constructSourceTLSPolicyLocally() {
        TLSPolicy adrTLSPolicy = constructTLSPolicyWithCommonConfig();
        adrTLSPolicy.setName(TLS_NAME_SOURCE);
        if (AutoUtils.isRunningOnGCP()) {
            adrTLSPolicy.setKeystorePath(VaultUtils.getInstance().createSourceKeystoreP12());
            adrTLSPolicy.setTruststorePath(VaultUtils.getInstance().createSourceTruststoreJks());
        } else {
            adrTLSPolicy.setKeystorePath(getTlsDirectoryLocation() + "/etc/pki/private/source/source.client.chain.p12");
            adrTLSPolicy.setTruststorePath(getTlsDirectoryLocation() + "/etc/pki/certs/source/source.ca.jks");
        }
        return adrTLSPolicy;
    }

    /**
     * For backup cluster:
     * Truststore (.jks) and Keystore (.p12) are loaded once during installation (by automation).
     */
    public static TLSPolicy constructBackupTLSPolicyLocally() {
        TLSPolicy adrTLSPolicy = constructTLSPolicyWithCommonConfig();
        adrTLSPolicy.setName(TLS_NAME_BACKUP);
        if (AutoUtils.isRunningOnGCP()) {
            adrTLSPolicy.setKeystorePath(VaultUtils.getInstance().createBackupKeystoreP12());
            adrTLSPolicy.setTruststorePath(VaultUtils.getInstance().createBackupTruststoreJks());
        } else {
            adrTLSPolicy.setKeystorePath(getTlsDirectoryLocation() + "/etc/pki/private/backup/backup.client.chain.p12");
            adrTLSPolicy.setTruststorePath(getTlsDirectoryLocation() + "/etc/pki/certs/backup/backup.ca.jks");
        }
        return adrTLSPolicy;
    }

    private static TLSPolicy constructTLSPolicyWithCommonConfig() {
        TLSPolicy adrTLSPolicy = new TLSPolicy();
        adrTLSPolicy.setEnabled(true);
        adrTLSPolicy.setStoreType("PKCS12");
        adrTLSPolicy.setKeystorePassword("changeit");
        adrTLSPolicy.setKeyPassword("changeit");
        adrTLSPolicy.setTruststorePassword("changeit");
        adrTLSPolicy.setAllowedProtocols(List.of("TLSv1.2"));
        adrTLSPolicy.setAllowedCiphers(List.of(
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_RSA_WITH_AES_128_GCM_SHA256"
        ));
        adrTLSPolicy.setForLoginOnly(false);
        return adrTLSPolicy;
    }

    public static String getTlsDirectoryLocation() {
        if (ConfigParametersHandler.getParameter("PULL_FROM_DOCKER_REPOSITORY").equals("false")) {
            String pathToBackupProject = ConfigParametersHandler.getParameter("PATH_TO_BACKUP_PROJECT");
            if (pathToBackupProject.contains("~"))
                pathToBackupProject = pathToBackupProject.replace("~", ConfigParametersHandler.getParameter("user.home"));
            return pathToBackupProject + "/docker/conf/tls";
        }
        return ConfigParametersHandler.getParameter("user.dir") + "../devops/install/backup/conf/tls";
    }

    public static String getPathToSourceCaCert() {
        String pemFileOnAwsCluster = "/etc/aerospike/certs/rootCA.pem";
        if (AutoUtils.isRunningOnGCP())
            return pemFileOnAwsCluster;
        return getTlsDirectoryLocation() + "/certs/source/source.ca.crt";
    }

    public static String getTlsName() {
        if (AutoUtils.isRunningOnGCP())
            return "asd.aerospike.com";
        return "source.server";
    }

}
