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
import utils.AerospikeLogger;
import utils.ConfigParametersHandler;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

// In order to run the tests, you need to set the following env on your machine: AZURE_CLIENT_ID,
// AZURE_TENANT_ID, AZURE_CLIENT_SECRET, AZURE_ACCOUNT_NAME, AZURE_ACCOUNT_KEY
@Tag("CLI-BACKUP")
class AzureBackupRestoreTest extends CliBackupRunner {
    private static final String STRING_BIN = "AzureBin";
    private static final String SET1 = "SetAzure";
    private static Key KEY1;
    private static final String SOURCE_NAMESPACE = "source-ns11";

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void azureRestoreSecretAuth() {
        AerospikeLogger.info("AZURE_CLIENT_ID=" + ConfigParametersHandler.getParameter("AZURE_CLIENT_ID"));
        AerospikeLogger.info("AZURE_TENANT_ID=" + ConfigParametersHandler.getParameter("AZURE_TENANT_ID"));
        AerospikeLogger.info("AZURE_CLIENT_SECRET=" + ConfigParametersHandler.getParameter("AZURE_CLIENT_SECRET"));
        AerospikeLogger.info("AZURE_STORAGE_ACCOUNT=" + ConfigParametersHandler.getParameter("AZURE_STORAGE_ACCOUNT"));

        String firstValueCreate = "azureRestoreSecretAuthValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        BackupResult fastRestoreBackup = CliBackup.on(SOURCE_NAMESPACE, "testAzure", true)
                .setAzureClientId(ConfigParametersHandler.getParameter("AZURE_CLIENT_ID"))
                .setAzureTenantId(ConfigParametersHandler.getParameter("AZURE_TENANT_ID"))
                .setAzureClientSecret(ConfigParametersHandler.getParameter("AZURE_CLIENT_SECRET"))
                .setAzureEndpoint("https://"+ ConfigParametersHandler.getParameter("AZURE_STORAGE_ACCOUNT") +".blob.core.windows.net/")
                .setAzureContainerName("abs-testing-bucket").setRemoveFiles().run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, fastRestoreBackup.getBackupDir())
                .setAzureContainerName("abs-testing-bucket")
                .setAzureEndpoint("https://"+ ConfigParametersHandler.getParameter("AZURE_STORAGE_ACCOUNT") +".blob.core.windows.net/")
                .setAzureClientId(ConfigParametersHandler.getParameter("AZURE_CLIENT_ID"))
                .setAzureTenantId(ConfigParametersHandler.getParameter("AZURE_TENANT_ID"))
                .setAzureClientSecret(ConfigParametersHandler.getParameter("AZURE_CLIENT_SECRET")).run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
    }

    @Test
    void azureRestoreAccountAuth() {
        String firstValueCreate = "azureRestoreAccountAuthValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        BackupResult fastRestoreBackup = CliBackup.on(SOURCE_NAMESPACE, "testAzure", true)
                .setAzureAccountName(ConfigParametersHandler.getParameter("AZURE_ACCOUNT_NAME"))
                .setAzureAccountKey(ConfigParametersHandler.getParameter("AZURE_ACCOUNT_KEY"))
                .setAzureEndpoint("https://"+ ConfigParametersHandler.getParameter("AZURE_STORAGE_ACCOUNT") +".blob.core.windows.net/")
                .setAzureContainerName("abs-testing-bucket").setRemoveFiles().run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, fastRestoreBackup.getBackupDir())
                .setAzureContainerName("abs-testing-bucket")
                .setAzureEndpoint("https://"+ ConfigParametersHandler.getParameter("AZURE_STORAGE_ACCOUNT") +".blob.core.windows.net/")
                .setAzureAccountName(ConfigParametersHandler.getParameter("AZURE_ACCOUNT_NAME"))
                .setAzureAccountKey(ConfigParametersHandler.getParameter("AZURE_ACCOUNT_KEY")).run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
    }
}