package utils.abs;

import api.backup.dto.TLSPolicy;
import utils.AerospikeLogger;
import utils.ConfigParametersHandler;

import java.util.List;

public class TlsHandler {

    public static final String TLS_NAME = "asd.aerospike.com";

    public static TLSPolicy getTLSPolicy() {
        TLSPolicy tlsPolicy = getTLSPolicyWithCommonConfig();
        tlsPolicy.setEnabled(true);
        tlsPolicy.setName(TLS_NAME);
        AerospikeLogger.info("CA_AEROSPIKE_COM_PEM_JKS_PATH=" + ConfigParametersHandler.getParameter("CA_AEROSPIKE_COM_PEM_JKS_PATH"));
        tlsPolicy.setTruststorePath(ConfigParametersHandler.getParameter("CA_AEROSPIKE_COM_PEM_JKS_PATH"));
        return tlsPolicy;
    }

    private static TLSPolicy getTLSPolicyWithCommonConfig() {
        TLSPolicy tlsPolicy = new TLSPolicy();
        tlsPolicy.setEnabled(true);
        tlsPolicy.setStoreType("jks");
        tlsPolicy.setKeystorePassword("password");
        tlsPolicy.setKeyPassword("password");
        tlsPolicy.setTruststorePassword("password");
        tlsPolicy.setAllowedProtocols(List.of("TLSv1.2"));
        tlsPolicy.setAllowedCiphers(List.of(
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_RSA_WITH_AES_128_GCM_SHA256"
        ));
        tlsPolicy.setForLoginOnly(false);
        return tlsPolicy;
    }
}
