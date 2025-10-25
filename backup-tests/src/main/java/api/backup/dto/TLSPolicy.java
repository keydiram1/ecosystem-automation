package api.backup.dto;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.policy.TlsPolicy;
import lombok.extern.slf4j.Slf4j;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import utils.AerospikeLogger;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Data
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class TLSPolicy {

    private boolean enabled;
    private String name;
    private String storeType = "jks";
    private String keystorePath;
    private String keystorePassword;
    private String keyPassword;
    private String truststorePath;
    private String truststorePassword;
    private List<String> allowedCiphers;
    private List<String> allowedProtocols;
    private Boolean forLoginOnly;

    public TlsPolicy build() {
        if (!enabled) {
            return null;
        }

        log.info("Init TlsPolicy");
        TlsPolicy policy = new TlsPolicy();
        if (keystorePath != null || truststorePath != null) {
            addSSLContext(policy);
        }
        if (allowedCiphers != null) {
            policy.ciphers = allowedCiphers.toArray(new String[]{});
        }
        if (allowedProtocols != null) {
            policy.protocols = allowedProtocols.toArray(new String[]{});
        }
        if (forLoginOnly != null) {
            policy.forLoginOnly = forLoginOnly;
        }
        return policy;
    }

    private void addSSLContext(TlsPolicy tlsPolicy) {
        tlsPolicy.context = getSSLContext();
    }

    private SSLContext getSSLContext() {
        SSLContextBuilder ctxBuilder = SSLContexts.custom();
        ctxBuilder.setKeyStoreType(storeType);
        if (keystorePath != null) {
            loadKeyStore(ctxBuilder);
        }
        if (truststorePath != null) {
            AerospikeLogger.info("truststorePath: " + truststorePath);
            loadTrustStore(ctxBuilder);
        }

        try {
            return ctxBuilder.build();
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new AerospikeException("Failed to build SSLContext", e);
        }
    }

    private void loadTrustStore(SSLContextBuilder ctxBuilder) {
        File tsFile = new File(truststorePath);
        AerospikeLogger.info("truststorePassword: " + truststorePassword);
        try {
            if (truststorePassword != null) {
                ctxBuilder.loadTrustMaterial(tsFile, truststorePassword.toCharArray());
            } else {
                ctxBuilder.loadTrustMaterial(tsFile);
            }
        } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException e) {
            throw new AerospikeException("Failed to load truststore", e);
        }
    }

    private void loadKeyStore(SSLContextBuilder ctxBuilder) {
        requireNonNull(keystorePassword, "If keystorePath is provided, keystorePassword must be provided");
        File ksFile = new File(keystorePath);
        char[] keystorePasswordChars = keystorePassword.toCharArray();

        try {
            if (keyPassword == null) {
                // If keyPassword is not provided, assume it is the same as the keystorePassword
                ctxBuilder.loadKeyMaterial(ksFile, keystorePasswordChars, keystorePasswordChars);
            } else {
                ctxBuilder.loadKeyMaterial(ksFile, keystorePasswordChars, keyPassword.toCharArray());
            }
        } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | UnrecoverableKeyException |
                 IOException e) {
            throw new AerospikeException("Failed to load keystore", e);
        }
    }
}
