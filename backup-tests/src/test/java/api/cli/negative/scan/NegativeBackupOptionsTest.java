package api.cli.negative.scan;

import api.cli.BackupProcessException;
import api.cli.CliBackup;
import api.cli.EstimationProcessException;
import com.aerospike.client.Key;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("CLI-BACKUP-NEGATIVE")
class NegativeBackupOptionsTest extends CliBackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns1";
    private static final String BACKUP_DIR = "BackupOptionsDir";

    @Test
    void recordsPerSecondNegative() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setRecordsPerSecond(-1).run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("rps value must not be negative");
    }

    @Test
    void totalTimeout() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setTotalTimeout(-1).run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("total-timeout must be non-negative");
    }

    @Test
    void socketTimeout() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setSocketTimeout(-1).run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("socket-timeout must be non-negative");
    }

    @Test
    void fileLimit() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setFileLimit(-1).run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("invalid argument \\\"-1\\\" for \\\"-F, --file-limit\\\" flag");
    }

    @Test
    void parallel() {
        String errorMessage = "parallel must be non-negative";
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR, -1).run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining(errorMessage);
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR, 0).run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("parallel read must be between 1 and 1024");
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR, 1025).run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("parallel read must be between 1 and 1024");
    }

    @Test
    void afterDigest() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setAfterDigest("1").run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("illegal base64 data");
    }

    @Test
    void modifiedAfter() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setModifiedAfter("a").run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("failed to parse modified after date");


    }

    @Test
    void modifiedBefore() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setModifiedBefore("a").run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("failed to parse modified before date");
    }

    @Test
    void setBandwidth() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setBandwidth(-1).run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("bandwidth value must not be negative");
    }

    @Test
    void setEncryptionMode() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setEncryptionMode("a").run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("encryption policy invalid");
    }

    @Test
    void setCompressLevel() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setCompressLevel(-2).run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("invalid compression level");
    }

    @Test
    void setCompressMode() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setCompressMode("a").run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("compression policy invalid");
    }

    @Test
    void setEstimateSamples() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE).setEstimateSamples(-1).removeFlag("directory").estimate();
        }).isInstanceOf(EstimationProcessException.class)
                .hasMessageContaining("estimate with estimate-samples < 0 is not allowed");
    }

    @Test
    void estimate() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).estimate(false);
        }).isInstanceOf(EstimationProcessException.class)
                .hasMessageContaining("estimate with output-file or directory is not allowed");
    }

    @Test
    void encryptionModeWithoutEncryptionKey() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setEncryptionMode("aes128").run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("encryption key location not specified");
    }

    @Test
    void secretAgentWithoutConnection() {
        AerospikeDataUtils.put(new Key(SOURCE_NAMESPACE, "SET1", "IT1"), "STRING_BIN", "firstValueCreate");
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setEncryptionMode("aes128")
                    .setEncryptionKeySecret("secrets:TestEnvTls:encryption_key_pem").run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("unable to read secret config key");
    }

    @Test
    void unknownFlag() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setCustomFlag("UnknownFlag", "someValue").run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("unknown flag: --UnknownFlag");
    }

    @Test
    @DisabledIfSystemProperty(named = "IS_RUNNING_ON_LOCAL_3_NODES_ENV", matches = "true")
    void host() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setHost("111").run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("failed to create aerospike client");
    }

    @Test
    @DisabledIfSystemProperty(named = "IS_RUNNING_ON_LOCAL_3_NODES_ENV", matches = "true")
    void port() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setHost("localhost").setPort(-5).run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("invalid port");
    }

    @Test
    void tlsCafile() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setEnableTls().setTlsCafile("-1").run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("invalid argument \\\"-1\\\" for \\\"--tls-cafile");
    }

    @Test
    void user() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setUser("-1").run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("Invalid user");
    }

    @Test
    void password() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setPassword("-1").run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("INVALID_CREDENTIAL");
    }
}