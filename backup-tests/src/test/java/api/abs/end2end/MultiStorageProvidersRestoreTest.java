package api.abs.end2end;

import api.abs.*;
import api.abs.generated.model.*;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.s3.model.StorageClass;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.ConfigParametersHandler;
import utils.abs.AbsRunner;
import utils.aerospike.abs.AerospikeDataUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static utils.storage.AwsStorageUtils.*;
import static utils.storage.AzureStorageUtils.getAzureStorageClass;
import static utils.storage.AzureStorageUtils.listAzureBlobs;
import static utils.storage.GcpStorageUtils.getGcpStorageClass;
import static utils.storage.GcpStorageUtils.listGcpObjects;

// In order to run the tests, you need to set the following env on your machine: AZURE_CLIENT_ID,
// AZURE_TENANT_ID, AZURE_CLIENT_SECRET, AZURE_ACCOUNT_NAME, AZURE_ACCOUNT_KEY
@Tag("ABS-SEQUENTIAL-TESTS")
@Execution(ExecutionMode.SAME_THREAD)
@DisabledIfSystemProperty(named = "qa_environment", matches = "GCP")
class MultiStorageProvidersRestoreTest extends AbsRunner {
    private static final String STRING_BIN = "providersBin";
    private static final String SET1 = "SetGcpAzure";
    private static Key KEY1;
    private static final String SOURCE_NAMESPACE = "source-ns22";

    private static final String CLUSTER_NAME = "absDefaultCluster";
    private static final String POLICY_NAME = "MultiStorageProvidersPolicyTestPolicy";
    private static final String ROUTINE_NAME = "MultiStorageProvidersRoutineTestRoutine";
    private static final String STORAGE_NAME = "MultiStorageProvidersStorageTestStorage";

    @BeforeAll
    static void setUp() {
        String command = String.format("docker cp %s backup-service:%s", ConfigParametersHandler.getParameter("GCP_SA_KEY_FILE"), "/tmp");
        AutoUtils.runBashCommand(command);
        AutoUtils.runBashCommand("cp " + ConfigParametersHandler.getParameter("GCP_SA_KEY_FILE") + " /tmp");
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        deleteExistingEntities();
    }

    static Stream<Object[]> storageProvider() {
        // Azure Storage with Client Secret Auth
        DtoAzureStorage azureSecretStorage = new DtoAzureStorage()
                .clientId(ConfigParametersHandler.getParameter("AZURE_CLIENT_ID"))
                .tenantId(ConfigParametersHandler.getParameter("AZURE_TENANT_ID"))
                .clientSecret(ConfigParametersHandler.getParameter("AZURE_CLIENT_SECRET"))
                .endpoint("https://" + ConfigParametersHandler.getParameter("AZURE_STORAGE_ACCOUNT") + ".blob.core.windows.net/")
                .containerName("abs-testing-bucket")
                .storageClass(new DtoAzureStorageClass()
                        .metadata(DtoAzureStorageClass.MetadataEnum.HOT)
                        .data(DtoAzureStorageClass.DataEnum.COOL));

        DtoStorage azureSecretDtoStorage = new DtoStorage()
                .azureStorage(azureSecretStorage);

        // Azure Storage with Account Auth and Storage Class
        DtoAzureStorage azureAccountStorage = new DtoAzureStorage()
                .tenantId(ConfigParametersHandler.getParameter("AZURE_TENANT_ID"))
                .clientId(ConfigParametersHandler.getParameter("AZURE_CLIENT_ID"))
                .clientSecret(ConfigParametersHandler.getParameter("AZURE_CLIENT_SECRET"))
                .containerName("abs-testing-bucket")
                .endpoint("https://" + ConfigParametersHandler.getParameter("AZURE_STORAGE_ACCOUNT") + ".blob.core.windows.net/")
                .storageClass(new DtoAzureStorageClass()
                        .metadata(DtoAzureStorageClass.MetadataEnum.HOT)
                        .data(DtoAzureStorageClass.DataEnum.COOL));

        DtoStorage azureAccountDtoStorage = new DtoStorage()
                .azureStorage(azureAccountStorage);

        // GCP Storage
        DtoGcpStorage gcpStorage = new DtoGcpStorage()
                .storageClass(new DtoGcpStorageClass().data(DtoGcpStorageClass.DataEnum.NEARLINE))
                .bucketName("abs-testing-bucket")
                .key(AutoUtils.getTextFromFile(ConfigParametersHandler.getParameter("GCP_SA_KEY_FILE")));

        DtoStorage gcpDtoStorage = new DtoStorage()
                .gcpStorage(gcpStorage);

        // AWS S3 Storage
        DtoS3Storage s3Storage = new DtoS3Storage()
                .storageClass(new DtoS3StorageClass()
                        .data(DtoS3StorageClass.DataEnum.ONEZONE_IA)
                        .metadata(DtoS3StorageClass.MetadataEnum.INTELLIGENT_TIERING))
                .accessKeyId(ConfigParametersHandler.getParameter("AWS_ACCESS_KEY_ID"))
                .secretAccessKey(ConfigParametersHandler.getParameter("AWS_SECRET_ACCESS_KEY"))
                .bucket("abs-testing-bucket")
                .s3Region("il-central-1");

        DtoStorage s3DtoStorage = new DtoStorage()
                .s3Storage(s3Storage);

        // Local Storage
        DtoLocalStorage localStorage = new DtoLocalStorage()
                .path("/tmp/abs-backup");

        DtoStorage localDtoStorage = new DtoStorage()
                .localStorage(localStorage);

        return Stream.of(
                new Object[]{"local", localDtoStorage},
                new Object[]{"azureSecretAuth", azureSecretDtoStorage},
                new Object[]{"azureAccountAuth", azureAccountDtoStorage},
                new Object[]{"gcp", gcpDtoStorage},
                new Object[]{"aws", s3DtoStorage}
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("storageProvider")
    @EnabledIfSystemProperty(named = "storage_provider", matches = "local")
    void testStorageBackupAndRestore(String testName, DtoStorage dtoStorage) {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        DtoBackupPolicy policy = new DtoBackupPolicy()
                .parallelWrite(1)
                .withClusterConfiguration(true)
                // retention: leave only one full backup (delete all old data)
                .retention(new DtoRetentionPolicy().full(1))
                .sealed(true);

        DtoBackupRoutine routine = new DtoBackupRoutine()
                .backupPolicy(POLICY_NAME)
                .intervalCron("@yearly")
                .namespaces(List.of(SOURCE_NAMESPACE))
                .sourceCluster(CLUSTER_NAME)
                .storage(STORAGE_NAME);

        AbsPolicyApi.createPolicy(POLICY_NAME, policy);
        AbsStorageApi.createStorage(STORAGE_NAME, dtoStorage);
        AbsRoutineApi.createRoutine(ROUTINE_NAME, routine);

        DtoBackupDetails backup = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);

        AerospikeLogger.info("Record count: " + backup.getRecordCount());
        AerospikeLogger.info("Backup key: " + backup.getKey());

        assertDataClasses(backup);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        AbsRestoreApi.restoreFullSync(backup.getKey(), ROUTINE_NAME);

        Record retrievedRecord = AerospikeDataUtils.get(KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);

        assertRetention(dtoStorage); // should have only one backup left.
    }


    private static void deleteExistingEntities() {
        if (AbsRoutineApi.getAllRoutines().containsKey(ROUTINE_NAME)) {
            AbsRoutineApi.deleteRoutine(ROUTINE_NAME);
        }
        if (AbsPolicyApi.getAllPolicies().containsKey(POLICY_NAME)) {
            AbsPolicyApi.deletePolicy(POLICY_NAME);
        }
        if (AbsStorageApi.getAllStorage().containsKey(STORAGE_NAME)) {
            AbsStorageApi.deleteStorage(STORAGE_NAME);
        }
    }

    /**
     * Asserts storage classes based on the storage type
     */
    private void assertDataClasses(DtoBackupDetails backup) {
        DtoStorage dtoStorage = backup.getStorage();
        assertThat(dtoStorage).isNotNull();

        if (dtoStorage.getS3Storage() != null && dtoStorage.getS3Storage().getStorageClass() != null) {
            assertS3DataClasses(backup, dtoStorage.getS3Storage());
        } else if (dtoStorage.getAzureStorage() != null && dtoStorage.getAzureStorage().getStorageClass() != null) {
            assertAzureDataClasses(backup, dtoStorage.getAzureStorage());
        } else if (dtoStorage.getGcpStorage() != null && dtoStorage.getGcpStorage().getStorageClass() != null) {
            assertGcpDataClasses(backup, dtoStorage.getGcpStorage());
        }

        // No assertions needed for local storage
    }

    private static void assertS3DataClasses(DtoBackupDetails backup, DtoS3Storage dtoS3Storage) {
        String metadataPath = backup.getKey() + "/metadata.yaml";
        String dataPath = backup.getKey() + "/0_" + SOURCE_NAMESPACE + "_1.asb";

        String metadataClass = getS3StorageClass(metadataPath, dtoS3Storage).toString();
        AerospikeLogger.info("Storage class for %s is %s".formatted(metadataPath, metadataClass));

        String dataClass = getS3StorageClass(dataPath, dtoS3Storage).toString();
        AerospikeLogger.info("Storage class for %s is %s".formatted(dataPath, dataClass));

        assertThat(metadataClass).isEqualTo(dtoS3Storage.getStorageClass().getMetadata().toString());
        assertThat(dataClass).isEqualTo(dtoS3Storage.getStorageClass().getData().toString());
    }

    private static void assertAzureDataClasses(DtoBackupDetails backup, DtoAzureStorage storage) {
        String metadataPath = backup.getKey() + "/metadata.yaml";
        String dataPath = backup.getKey() + "/0_" + SOURCE_NAMESPACE + "_1.asb";

        String metadataClass = getAzureStorageClass(metadataPath, storage).toString();
        AerospikeLogger.info("Storage class for %s is %s".formatted(metadataPath, metadataClass));

        AerospikeLogger.info("Check data class for %s".formatted(dataPath));
        String dataClass = getAzureStorageClass(dataPath, storage).toString();
        AerospikeLogger.info("Storage class for %s is %s".formatted(dataPath, dataClass));

        assertThat(metadataClass).isEqualTo(storage.getStorageClass().getMetadata().toString());
        assertThat(dataClass).isEqualTo(storage.getStorageClass().getData().toString());
    }

    @SneakyThrows
    private static void assertGcpDataClasses(DtoBackupDetails backup, DtoGcpStorage storage) {
        String dataPath = backup.getKey() + "/0_" + SOURCE_NAMESPACE + "_1.asb";
        String dataClass = getGcpStorageClass(dataPath, storage).toString();
        AerospikeLogger.info("Storage class for %s is %s".formatted(dataPath, dataClass));

        assertThat(dataClass).isEqualTo(storage.getStorageClass().getData().toString());
    }

    private static void assertRetention(DtoStorage dtoStorage) {
        if (dtoStorage.getLocalStorage() != null) {
            return; // cannot read files in local storage
        }

        List<String> fileNames = listStorageFiles(dtoStorage);
        AerospikeLogger.info("test retention for " + dtoStorage + ": " + String.join(",", fileNames));
        assertThat(fileNames)
                .hasSize(3)
                .anyMatch(file -> file.endsWith(".asb"))
                .anyMatch(file -> file.endsWith(".yaml"))
                .anyMatch(file -> file.endsWith(".conf"));
    }

    @SneakyThrows
    private static List<String> listStorageFiles(DtoStorage dtoStorage) {
        if (dtoStorage.getS3Storage() != null) {
            return listS3Objects(ROUTINE_NAME, dtoStorage.getS3Storage());
        }
        if (dtoStorage.getAzureStorage() != null) {
            return listAzureBlobs(ROUTINE_NAME, dtoStorage.getAzureStorage());
        }
        if (dtoStorage.getGcpStorage() != null) {
            return listGcpObjects(ROUTINE_NAME, dtoStorage.getGcpStorage());
        }

        return Collections.emptyList();
    }
}
