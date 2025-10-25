package api.abs.negative;

import api.abs.AbsConfigApi;
import api.abs.generated.ApiException;
import api.abs.generated.model.DtoConfig;
import org.apache.http.HttpStatus;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.abs.AbsConfigFileUtils;
import utils.abs.AbsRunner;

@Tag("ABS-SEQUENTIAL-TESTS-2")
class NegativeEditConfigFileTest extends AbsRunner {

    @Test
    @Disabled
    void editReloadConfigFile() {
        AbsConfigFileUtils.editService(1, 1);

        Assertions.assertThatThrownBy(() -> AbsConfigApi.apply())
                .hasMessageContaining("invalid request: HTTPServer changes: HTTPServer removed")
                .extracting(it -> ((ApiException) it).getCode(), InstanceOfAssertFactories.INTEGER)
                .isEqualTo(HttpStatus.SC_BAD_REQUEST);

        DtoConfig currentConfiguration = AbsConfigApi.getConfiguration();
        Assertions.assertThat(currentConfiguration).isNotNull();
        Assertions.assertThat(currentConfiguration.getService().getHttp()).isNull();
        Assertions.assertThat(currentConfiguration.getService().getLogger()).isNotNull();
    }
}