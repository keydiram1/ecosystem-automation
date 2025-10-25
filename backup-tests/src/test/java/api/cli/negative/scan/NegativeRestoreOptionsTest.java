package api.cli.negative.scan;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import api.cli.RestoreProcessException;
import com.aerospike.client.Key;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import utils.AutoUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("CLI-BACKUP-NEGATIVE")
class NegativeRestoreOptionsTest extends CliBackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns2";
    private static Key KEY1;
    private static final String SET1 = "SetFastRestore";
    private static final String STRING_BIN = "fastRestoreBin";
    private static BackupResult negativeRestoreBackup;

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        negativeRestoreBackup = CliBackup.on(SOURCE_NAMESPACE, "fastRestoreBackup").run();
    }

    @Test
    void recordsPerSecondNegative() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir()).setRecordsPerSecond(-1).run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("rps value must not be negative");
    }

    @Test
    void extraTtlNegative() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir()).setExtraTtl(-10).run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("extra ttl value must not be negative");
    }

    @Test
    void setBandwidthNegative() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir()).setBandwidth(-50).run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("bandwidth value must not be negative");
    }

    @Test
    void setMaxAsyncBatchesNegative() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir()).setMaxAsyncBatches(-1).run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("max async batches must be positive");
    }

    @Test
    void setBatchSizeNegative() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir()).setBatchSize(-5).run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("batch size must be positive");
    }

    @Test
    void setTotalTimeoutNegative() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir()).setTotalTimeout(-100).run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("total-timeout must be non-negative");
    }

    @Test
    void setSocketTimeoutNegative() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir()).setSocketTimeout(-200).run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("socket-timeout must be non-negative");
    }

    @Test
    void setEncryptionModeInvalid() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir()).setEncryptionMode("INVALID_MODE").run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("invalid encryption mode");
    }

    @Test
    void setWrongBucketName() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir()).setS3BucketName("wrongBucketName").run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining(" bucket wrongBucketName does not exist or you don't have access");
    }


    @Test
    void setInvalidParallelValue() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir(), -2).run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("parallel must be non-negative");
    }

    @Test
    void setInvalidCompressionMode() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir()).setCompressMode("INVALID_COMPRESSION").run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("invalid compression mode");
    }

    @Test
    void setAzureContainerNameInvalid() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir()).setAzureContainerName("invalid-name").run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("unable to get container properties: no Host in request URL");
    }

    @Test
    void setNegativeRecordsPerSecondValue() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir()).setRecordsPerSecond(-9999).run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("rps value must not be negative");
    }

    @Test
    void unknownFlag() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir()).setCustomFlag("UnknownFlag", "someValue").run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("unknown flag: --UnknownFlag");

    }

    @Test
    @DisabledIfSystemProperty(named = "IS_RUNNING_ON_LOCAL_3_NODES_ENV", matches = "true")
    void host() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir()).setHost("111").run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("failed to create aerospike client");
    }

    @Test
    @DisabledIfSystemProperty(named = "IS_RUNNING_ON_LOCAL_3_NODES_ENV", matches = "true")
    void port() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir()).setHost("localhost").setPort(-5).run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("invalid port");
    }

    @Test
    void tlsCafile() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir()).setEnableTls().setTlsCafile("-1").run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("flag: failed to read from file");
    }

    @Test
    void user() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir()).setUser("-1").run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("Invalid user");
    }

    @Test
    void password() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir()).setPassword("-1").run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("INVALID_CREDENTIAL");
    }

    @Test
    void directoryListWithComma() {
        String set1 = "set1", set2 = "set2";
        String key1Value = "key1", key2Value = "key2";
        String bin1Name = "bin1", bin2Name = "bin2";
        String bin1Value = "firstValue", bin2Value = "secondValue";
        String backupDirA = "dirAwithComma,", backupDirB = "dirB";

        Key key1 = new Key(SOURCE_NAMESPACE, set1, key1Value);
        AerospikeDataUtils.put(key1, bin1Name, bin1Value);

        BackupResult fastRestoreBackup = CliBackup.on(SOURCE_NAMESPACE, backupDirA).run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        Key key2 = new Key(SOURCE_NAMESPACE, set2, key2Value);
        AerospikeDataUtils.put(key2, bin2Name, bin2Value);
        BackupResult fastRestoreBackup2 = CliBackup.on(SOURCE_NAMESPACE, backupDirB).run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE).setDirectoryList(fastRestoreBackup.getBackupDir(), fastRestoreBackup2.getBackupDir())
                    .removeFlag("directory")
                    .run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("no such file or directory");
    }

    @Test
    void corruptedFile() {
        BackupResult corruptedFileBackup =
                CliBackup.on(SOURCE_NAMESPACE, "corruptedFileBackup")
                        .run();

        AutoUtils.replaceFileContent(
                corruptedFileBackup.getBackupDir() + "/" + AutoUtils.getFileNameFromDir(corruptedFileBackup.getBackupDir()),
                "corrupted backup file"
        );

        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, corruptedFileBackup.getBackupDir())
                    .run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("restore failed: failed to perform asb restore: failed to read data");
    }

    @Test
    void wrongBackupFileExtension() {
        BackupResult corruptedFileBackup =
                CliBackup.on(SOURCE_NAMESPACE, "scanWrongBackupFileExtension", 1)
                        .run();

        String wrongFileExtension = "file.txt";
        String backupFilePath = corruptedFileBackup.getBackupDir() + "/" + AutoUtils.getFileNameFromDir(corruptedFileBackup.getBackupDir());
        AutoUtils.renameFile(backupFilePath, wrongFileExtension);

        assertThat(AutoUtils.countFilesInDir(corruptedFileBackup.getBackupDir())).isGreaterThan(0);

        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, corruptedFileBackup.getBackupDir())
                    .run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("failed to create asb reader: empty storage: /tmp/scanWrongBackupFileExtension is empty");
    }

    @Test
    void validateFileWrongFile() {
        String validationString = CliRestore.on()
                .setValidateFile("wrongFile")
                .validate();

        assertThat(validationString).contains("failed to get path info wrongFile: stat wrongFile: no such file or directory");
    }

    @Test
    void validateDirWrongDir() {
        String validationString = CliRestore.on()
                .setValidateDirectory("wrongDir")
                .validate();

        assertThat(validationString).contains("validation initialization failed: failed to create restore reader: failed to create asb reader: empty storage: failed to get path info wrongDir: stat wrongDir: no such file or directory");
    }

    @Test
    void niceFlagDeprecated() {
        assertThatThrownBy(() -> {
            int bandwidth = 7;
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir()).setNice(bandwidth).run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("Flag --nice has been deprecated, use --bandwidth instead");
    }

    @Test
    void validateBandwidth() {
        assertThatThrownBy(() -> {
            int bandwidth = -1;
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir()).setBandwidth(bandwidth).run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("failed to validate restore config: bandwidth value must not be negative");
    }
}