package api.cli.end2end.scan;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import utils.ConfigParametersHandler;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("CLI-BACKUP")
class S3BackupRestoreTest extends CliBackupRunner {
    private static final String STRING_BIN = "s3RestoreBin";
    private static final String SET1 = "SetS3";
    private static Key KEY1;
    private static final String SOURCE_NAMESPACE = "source-ns10";

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    @DisabledIfSystemProperty(named = "IS_RUNNING_ON_LOCAL_3_NODES_ENV", matches = "true")
    void minioRestore() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        BackupResult fastRestoreBackup = CliBackup.on(SOURCE_NAMESPACE, "test", true).setS3Region("eu-central-1")
                .setS3EndpointOverride("http://localhost:9000").setS3Profile("minio")
                .setS3BucketName("as-backup-bucket").setRemoveFiles().run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, fastRestoreBackup.getBackupDir()).setS3Region("eu-central-1")
                .setS3BucketName("as-backup-bucket").setS3EndpointOverride("http://localhost:9000").setS3Profile("minio").run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
    }

    @Test
    void s3Restore() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        BackupResult fastRestoreBackup = CliBackup.on(SOURCE_NAMESPACE, "abs-automation-storage", true)
                .setS3Region("il-central-1")
                .setS3BucketName("abs-testing-bucket")
                .setRemoveFiles()
                .setS3AccessKeyId(ConfigParametersHandler.getParameter("AWS_ACCESS_KEY_ID"))
                .setS3SecretAccessKey(ConfigParametersHandler.getParameter("AWS_SECRET_ACCESS_KEY"))
                .run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, fastRestoreBackup.getBackupDir())
                .setS3Region("il-central-1")
                .setS3BucketName("abs-testing-bucket")
                .setS3AccessKeyId(ConfigParametersHandler.getParameter("AWS_ACCESS_KEY_ID"))
                .setS3SecretAccessKey(ConfigParametersHandler.getParameter("AWS_SECRET_ACCESS_KEY"))
                .run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
    }
}