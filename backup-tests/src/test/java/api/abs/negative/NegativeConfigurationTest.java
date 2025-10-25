package api.abs.negative;

import api.abs.AbsConfigApi;
import api.abs.generated.model.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.abs.AbsRunner;

import java.util.List;
import java.util.Map;

import static api.abs.generated.model.DtoLoggerConfig.LevelEnum.INFO;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("ABS-NEGATIVE-TESTS")
class NegativeConfigurationTest extends AbsRunner {
    @Test
    void noName() {
        assertThatThrownBy(() -> AbsConfigApi.updateConfiguration(
                getDtoConfig().putBackupRoutinesItem("", new DtoBackupRoutine())))
                .hasMessageContaining("empty field validation error: \"routine name\" required");
        assertThatThrownBy(() -> AbsConfigApi.updateConfiguration(
                getDtoConfig().putStorageItem("", new DtoStorage())))
                .hasMessageContaining("empty field validation error: \"storage name\" required");
        assertThatThrownBy(() -> AbsConfigApi.updateConfiguration(
                getDtoConfig().putAerospikeClustersItem("", new DtoAerospikeCluster())))
                .hasMessageContaining("empty field validation error: \"cluster name\" required");
    }

    @NotNull
    private static DtoConfig getDtoConfig() { // config identical to what we have in config.yaml
        DtoConfig configuration = AbsConfigApi.getConfiguration();
        DtoBackupServiceConfig service = configuration.getService();
        if (service != null) {
            DtoLoggerConfig logger = service.getLogger();
            if (logger != null) {
                logger.setFormat(null);
                logger.setStdoutWriter(null);
                logger.setLevel(INFO);
            }
        }

        return configuration;
    }

    @Test
    void duplicateNames() {
        assertThatThrownBy(() -> AbsConfigApi.updateConfiguration(
                getDtoConfig().backupRoutines(Map.of(
                        "r", new DtoBackupRoutine(),
                        "r", new DtoBackupRoutine()))))
                .hasMessageContaining("duplicate key: r");
    }

    @Test
    void linkErrors() {
        DtoBackupRoutine routine = new DtoBackupRoutine()
                .namespaces(List.of("source-ns1"))
                .intervalCron("@daily")
                .backupPolicy("p")
                .sourceCluster("c")
                .storage("s");

        DtoStorage DtoStorage = new DtoStorage().localStorage(new DtoLocalStorage().path("path"));
        assertThatThrownBy(() -> AbsConfigApi.updateConfiguration(getDtoConfig()
                .storage(Map.of("s", DtoStorage))
                .backupRoutines(Map.of("r", routine))))
                .hasMessageContaining("not found validation error: backup policy \"p\"");
        DtoAerospikeCluster cluster = new DtoAerospikeCluster().seedNodes(
                List.of(new DtoSeedNode().hostName("localhost").port(3000)));
        assertThatThrownBy(() -> AbsConfigApi.updateConfiguration(getDtoConfig()
                .aerospikeClusters(Map.of("c", cluster))
                .backupRoutines(Map.of("r", routine))))
                .hasMessageContaining("not found validation error: backup policy \"p\"");
        assertThatThrownBy(() -> AbsConfigApi.updateConfiguration(getDtoConfig()
                .aerospikeClusters(Map.of("c", cluster))
                .backupPolicies(Map.of("p", new DtoBackupPolicy()))
                .backupRoutines(Map.of("r", routine))))
                .hasMessageContaining("not found validation error: storage \"s\"");
    }
}
