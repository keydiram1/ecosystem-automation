package utils;

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.Map;

public class VaultUtils {
    private static volatile VaultUtils instance;
    private final Vault vault;

    private VaultUtils() {
        VaultConfig vaultConfig;
        try {
            vaultConfig = new VaultConfig()
                    .address("https://" + ConfigParametersHandler.getParameter("vaultIP") + ":8200")
                    .token(ConfigParametersHandler.getParameter("vaultToken"))
                    .sslConfig(new SslConfig().verify(false).build());
        } catch (VaultException e) {
            throw new RuntimeException(e);
        }

        vault = new Vault(vaultConfig);
    }

    public static VaultUtils getInstance() {
        if (instance == null) {
            synchronized (VaultUtils.class) {
                if (instance == null) {
                    instance = new VaultUtils();
                }
            }
        }
        return instance;
    }

    @SneakyThrows
    public String createBackupTruststoreJks() {
        return getFileFromVault("/secret/backup.cluster.truststore.jks", "truststore", "backup.truststore.jks");
    }

    @SneakyThrows
    public String getBackupTruststorePassword() {
        return vault.logical().read("/secret/backup.cluster.truststore.password.txt").getData().get("truststore_password");
    }

    @SneakyThrows
    public String createBackupKeystoreP12() {
        return getFileFromVault("/secret/backup.cluster.keystore.p12", "keystore", "backup.keystore.p12");
    }

    @SneakyThrows
    public String getBackupKeyPassword() {
        return vault.logical().read("/secret/backup.cluster.key.password.txt").getData().get("key_password");
    }

    @SneakyThrows
    public String getBackupKeystorePassword() {
        return vault.logical().read("/secret/backup.cluster.keystore.password.txt").getData().get("keystore_password");
    }

    @SneakyThrows
    public String createSourceTruststoreJks() {
        return getFileFromVault("/secret/source.cluster.truststore.jks", "truststore", "source.truststore.jks");
    }

    @SneakyThrows
    public String getSourceTruststorePassword() {
        return vault.logical().read("/secret/source.cluster.truststore.password.txt").getData().get("truststore_password");
    }

    @SneakyThrows
    public String createSourceKeystoreP12() {
        return getFileFromVault("/secret/source.cluster.keystore.p12", "keystore", "source.keystore.p12");
    }

    @SneakyThrows
    public String getSourceKeyPassword() {
        return vault.logical().read("/secret/source.cluster.key.password.txt").getData().get("key_password");
    }

    @SneakyThrows
    public String getSourceKeystorePassword() {
        return vault.logical().read("/secret/source.cluster.keystore.password.txt").getData().get("keystore_password");
    }

    @SneakyThrows
    public String getSourceCaCert() {
        return getFileFromVault("source/source-cert-file", "key", "source.ca.cert");
    }


    @SneakyThrows
    private String getFileFromVault(String path, String key, String fileName) {
        Map<String, String> data = vault.logical().read(path).getData();
        if (data != null && data.containsKey(key)) {
            String base64Truststore = data.get(key);
            byte[] truststoreBytes = Base64.getDecoder().decode(base64Truststore);

            File truststoreFile = new File(fileName);
            try (FileOutputStream fos = new FileOutputStream(truststoreFile)) {
                fos.write(truststoreBytes);
            }

            return truststoreFile.getAbsolutePath();
        } else {
            throw new RuntimeException(key + " not found in Vault");
        }
    }
}
