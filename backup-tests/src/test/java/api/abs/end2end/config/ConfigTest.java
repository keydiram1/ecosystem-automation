package api.abs.end2end.config;

import api.abs.AbsConfigApi;
import api.abs.generated.model.DtoConfig;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Tag("ABS-SEQUENTIAL-TESTS-2")
@Execution(ExecutionMode.SAME_THREAD)
class ConfigTest extends ConfigCRUD {

    @Test
    void getConfiguration() {
        DtoConfig currentConfiguration = AbsConfigApi.getConfiguration();
        Assertions.assertThat(currentConfiguration).isNotNull();
    }
}
