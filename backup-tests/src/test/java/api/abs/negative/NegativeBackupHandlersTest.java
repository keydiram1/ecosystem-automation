package api.abs.negative;

import api.abs.AbsBackupApi;
import api.abs.generated.ApiException;
import org.apache.http.HttpStatus;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.abs.AbsRunner;

@Tag("ABS-NEGATIVE-TESTS")
class NegativeBackupHandlersTest extends AbsRunner {
    @Test
    void getFullBackups() {
        Assertions.assertThatThrownBy(() -> AbsBackupApi.getFullBackupsInRange("NotExist", null, null))
                .hasMessageContaining("routine \"NotExist\" not found")
                .extracting(it -> ((ApiException) it).getCode(), InstanceOfAssertFactories.INTEGER)
                .isEqualTo(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    void getFullBackupsTimeRange() {
        Assertions.assertThatThrownBy(() -> AbsBackupApi.getFullBackupsInRange("localStorage", 1000L, 0L))
                .hasMessageContaining("fromTime should be less than toTime")
                .extracting(it -> ((ApiException) it).getCode(), InstanceOfAssertFactories.INTEGER)
                .isEqualTo(400);
    }

    @Test
    void getIncrementalBackups() {
        Assertions.assertThatThrownBy(() -> AbsBackupApi.getIncrementalBackups("NotExist"))
                .hasMessageContaining("routine \"NotExist\" not found")
                .isInstanceOf(ApiException.class)
                .extracting(it -> ((ApiException) it).getCode(), InstanceOfAssertFactories.INTEGER)
                .isEqualTo(404);
    }

    @Test
    void scheduleFullBackupNegativeDelay() {
        String routineName = "edgeCases";
        Assertions.assertThatThrownBy(() -> AbsBackupApi.scheduleFullBackup(routineName, -1))
                .hasMessageContaining("invalid query param delay: should be a positive integer")
                .isInstanceOf(ApiException.class)
                .extracting(it -> ((ApiException) it).getCode(), InstanceOfAssertFactories.INTEGER)
                .isEqualTo(400);
    }

    @Test
    void getAllIncrementalBackupsNegativeFrom() {
        Assertions.assertThatThrownBy(() -> AbsBackupApi.getAllIncrementalBackupsInRange(-1L, 1L))
                .hasMessageContaining("\"from\" -1 invalid, should not be negative number")
                .isInstanceOf(ApiException.class)
                .extracting(it -> ((ApiException) it).getCode(), InstanceOfAssertFactories.INTEGER)
                .isEqualTo(400);
    }

    @Test
    void getAllIncrementalBackupsNegativeTo() {
        Assertions.assertThatThrownBy(() -> AbsBackupApi.getAllIncrementalBackupsInRange(1L, -1L))
                .hasMessageContaining("\"to\" -1 invalid, should not be negative number")
                .isInstanceOf(ApiException.class)
                .extracting(it -> ((ApiException) it).getCode(), InstanceOfAssertFactories.INTEGER)
                .isEqualTo(400);
    }

    @Test
    void getIncrementalBackupsNegativeFrom() {
        String routineName = "edgeCases";
        Assertions.assertThatThrownBy(() -> AbsBackupApi.getIncrementalBackupsInRange(routineName, -1L, 1L))
                .hasMessageContaining("\"from\" -1 invalid, should not be negative number")
                .isInstanceOf(ApiException.class)
                .extracting(it -> ((ApiException) it).getCode(), InstanceOfAssertFactories.INTEGER)
                .isEqualTo(400);
    }

    @Test
    void getIncrementalBackupsNegativeTo() {
        String routineName = "edgeCases";
        Assertions.assertThatThrownBy(() -> AbsBackupApi.getIncrementalBackupsInRange(routineName, 1L, -1L))
                .hasMessageContaining("\"to\" -1 invalid, should not be negative number")
                .isInstanceOf(ApiException.class)
                .extracting(it -> ((ApiException) it).getCode(), InstanceOfAssertFactories.INTEGER)
                .isEqualTo(400);
    }

    @Test
    void getIncrementalBackupsFromLargerThanTo() {
        String routineName = "edgeCases";
        Assertions.assertThatThrownBy(() -> AbsBackupApi.getIncrementalBackupsInRange(routineName, 2L, 1L))
                .hasMessageContaining("fromTime should be less than toTime")
                .isInstanceOf(ApiException.class)
                .extracting(it -> ((ApiException) it).getCode(), InstanceOfAssertFactories.INTEGER)
                .isEqualTo(400);
    }

    @Test
    void getAllFullBackupsNegativeFrom() {
        Assertions.assertThatThrownBy(() -> AbsBackupApi.getAllFullBackupsInRange(-1L, 1L))
                .hasMessageContaining("from\" -1 invalid, should not be negative number")
                .isInstanceOf(ApiException.class)
                .extracting(it -> ((ApiException) it).getCode(), InstanceOfAssertFactories.INTEGER)
                .isEqualTo(400);
    }

    @Test
    void getAllFullBackupsNegativeTo() {
        Assertions.assertThatThrownBy(() -> AbsBackupApi.getAllFullBackupsInRange(1L, -1L))
                .hasMessageContaining("\"to\" -1 invalid, should not be negative number")
                .isInstanceOf(ApiException.class)
                .extracting(it -> ((ApiException) it).getCode(), InstanceOfAssertFactories.INTEGER)
                .isEqualTo(400);
    }

    @Test
    void getFullBackupsNegativeFrom() {
        String routineName = "edgeCases";
        Assertions.assertThatThrownBy(() -> AbsBackupApi.getFullBackupsInRange(routineName, -1L, 1L))
                .hasMessageContaining("\"from\" -1 invalid, should not be negative number")
                .isInstanceOf(ApiException.class)
                .extracting(it -> ((ApiException) it).getCode(), InstanceOfAssertFactories.INTEGER)
                .isEqualTo(400);
    }

    @Test
    void getFullBackupsNegativeTo() {
        String routineName = "edgeCases";
        Assertions.assertThatThrownBy(() -> AbsBackupApi.getFullBackupsInRange(routineName, 1L, -1L))
                .hasMessageContaining("\"to\" -1 invalid, should not be negative number")
                .isInstanceOf(ApiException.class)
                .extracting(it -> ((ApiException) it).getCode(), InstanceOfAssertFactories.INTEGER)
                .isEqualTo(400);
    }

    @Test
    void getFullBackupsFromLargerThanTo() {
        String routineName = "edgeCases";
        Assertions.assertThatThrownBy(() -> AbsBackupApi.getFullBackupsInRange(routineName, 2L, 1L))
                .hasMessageContaining("fromTime should be less than toTime")
                .isInstanceOf(ApiException.class)
                .extracting(it -> ((ApiException) it).getCode(), InstanceOfAssertFactories.INTEGER)
                .isEqualTo(400);
    }
}
