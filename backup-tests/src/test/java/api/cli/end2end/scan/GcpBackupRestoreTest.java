package api.cli.end2end.scan;

import api.cli.*;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AutoUtils;
import utils.ConfigParametersHandler;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("CLI-BACKUP")
class GcpBackupRestoreTest extends CliBackupRunner {
    private static final String STRING_BIN = "GcpBin";
    private static final String SET1 = "SetGcp";
    private static Key KEY1;
    private static final String SOURCE_NAMESPACE = "source-ns12";

    @BeforeAll
    static void setUp() {
        AutoUtils.runBashCommand("cp " + ConfigParametersHandler.getParameter("GCP_SA_KEY_FILE") + " /tmp");
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void gcpRestoreSecretAuth() {
        String firstValueCreate = "gcpRestoreSecretAuthRecordValue" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        BackupResult gcpRestoreBackup = CliBackup.on(SOURCE_NAMESPACE, "testGcp", true)
                .setGcpBucketName("abs-testing-bucket").setGcpKeyPath().setRemoveFiles().run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, gcpRestoreBackup.getBackupDir())
                .setGcpBucketName("abs-testing-bucket").setGcpKeyPath().run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
    }

    @Test
    void gcpRestore() {
        String firstValueCreate = "gcpRestoreValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, "testGcp", true)
                    .setGcpBucketName("abs-testing-bucket").setGcpKeyPath().setRemoveFiles()
                    .setGcpEndpointOverride("wrongGcpUrl").run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("failed to create GCP client");
    }

    @Test
    void gcpRestoreEndpointOverride() {
        String firstValueCreate = "gcpRestoreEndpointOverrideValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        BackupResult gcpRestoreBackup = CliBackup.on(SOURCE_NAMESPACE, "testGcp", true)
                .setGcpBucketName("abs-testing-bucket").setGcpKeyPath().setRemoveFiles().run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, gcpRestoreBackup.getBackupDir())
                    .setGcpBucketName("abs-testing-bucket").setGcpKeyPath()
                    .setGcpEndpointOverride("wrongGcpUrl").run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("failed to create GCP client");
    }
}