package api.abs.end2end.config;

import api.abs.*;
import api.abs.generated.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import utils.ASBench;
import utils.ConfigParametersHandler;
import utils.DockerManager;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-CONFIGURATIONS")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisabledIfSystemProperty(named = "qa_environment", matches = "GCP")
class BackupWithNewConfigTest extends AbsRunner {
    private static final String POLICY_NAME = "BackupWithNewConfigTestPolicy" + System.currentTimeMillis();
    private static final String STORAGE_NAME = "BackupWithNewConfigTestStorage" + System.currentTimeMillis();
    private static final String ROUTINE_NAME = "BackupWithNewConfigTestRoutine" + System.currentTimeMillis();
    private static final String CLUSTER_NAME = "absDefaultCluster";

    private final DtoBackupPolicy POLICY = new DtoBackupPolicy()
            .parallel(1)
            .sealed(true)
            .retryPolicy(new DtoRetryPolicy()
                    .maxRetries(1)
                    .baseTimeout(1)
                    .multiplier(BigDecimal.ONE));

    private final DtoStorage STORAGE = new DtoStorage().localStorage(
            new DtoLocalStorage().path("/etc/aerospike-backup-service/conf.d/BackupWithNewConfigTestPath"));

    final DtoBackupRoutine ROUTINE = new DtoBackupRoutine()
            .backupPolicy(POLICY_NAME)
            .intervalCron("@yearly")
            .namespaces(List.of("source-ns20"))
            .sourceCluster(CLUSTER_NAME)
            .storage(STORAGE_NAME);

    private static final String SET1 = "SET_RELOAD_CONFIG";

    @Test
    @Order(1)
    void backupWithNewConfig() {
        AbsPolicyApi.createPolicy(POLICY_NAME, POLICY);
        AbsStorageApi.createStorage(STORAGE_NAME, STORAGE);
        DockerManager.stopContainer("backup-service");
        DockerManager.startAndWaitForBackupService();
        AbsRoutineApi.createRoutine(ROUTINE_NAME, ROUTINE);

        String sourceNamespace = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);

        ASBench.on(sourceNamespace, SET1).duration(1).run();
        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, sourceNamespace);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        String backupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME).getKey();

        AerospikeDataUtils.truncateSourceNamespace(sourceNamespace);

        AbsRestoreApi.restoreFullSync(backupKey, ROUTINE_NAME);

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, sourceNamespace)).isEqualTo(numberOfRecordsBeforeTruncate);
    }

    @Test
    @Order(2)
    void validateNoDefaultValuesAddedToConfigFile() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        String pathToConfFile = Paths.get(ConfigParametersHandler.getParameter("user.dir")).getParent().toString() + "/devops/install/abs/conf/service/config.yml";
        File configFile = new File(pathToConfFile);

        JsonNode rootNode = objectMapper.readTree(configFile);

        assertThat(rootNode.path("backup-policies").path("defaultPolicy").has("retry-policy")).isTrue();
        assertThat(rootNode.path("backup-policies").path("defaultPolicy").has("encryption")).isFalse();
        assertThat(rootNode.path("backup-policies").path("defaultPolicy").has("compression")).isFalse();

        assertThat(rootNode.path("storage").path("local").has("s3-region")).isFalse();
        assertThat(rootNode.path("storage").path("local").has("s3-profile")).isFalse();
        assertThat(rootNode.path("storage").path("local").has("s3-endpoint-override")).isFalse();

        assertThat(rootNode.path("aerospike-clusters").path("absDefaultCluster").has("conn-timeout")).isFalse();
        assertThat(rootNode.path("aerospike-clusters").path("absDefaultCluster").has("label")).isFalse();
    }
}
