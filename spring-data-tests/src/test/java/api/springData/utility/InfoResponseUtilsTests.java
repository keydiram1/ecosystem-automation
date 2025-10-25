package api.springData.utility;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.util.InfoResponseUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns12"})
@Tag("SPRING-DATA-TESTS-1")
class InfoResponseUtilsTests {

    @Test
    void propertyInStartOfResponseSemicolonSeparated() {
        String response = "replication-factor=2;memory-size=1073741824;default-ttl=2592000";

        Integer property = InfoResponseUtils.getPropertyFromConfigResponse(response, "replication-factor",
                Integer::parseInt);

        assertThat(property).isEqualTo(2);
    }

    @Test
    void propertiesColonSeparated() {
        String response = "objects=780000:tombstones=10:memory_data_bytes=655:truncate_lut=343408318198:stop-writes" +
                "-count=0:set-enable-xdr=use-default:disable-eviction=false;";

        Integer objects = InfoResponseUtils.getPropertyFromInfoResponse(response, "objects", Integer::parseInt);
        assertThat(objects).isEqualTo(780000);
        Integer tombstones = InfoResponseUtils.getPropertyFromInfoResponse(response, "tombstones", Integer::parseInt);
        assertThat(tombstones).isEqualTo(10);
        Integer memoryDataBytes = InfoResponseUtils.getPropertyFromInfoResponse(response, "memory_data_bytes",
                Integer::parseInt);
        assertThat(memoryDataBytes).isEqualTo(655);
        Long truncateLut = InfoResponseUtils.getPropertyFromInfoResponse(response, "truncate_lut", Long::parseLong);
        assertThat(truncateLut).isEqualTo(343408318198L);
        Integer stopWritesCount = InfoResponseUtils.getPropertyFromInfoResponse(response, "stop-writes-count",
                Integer::parseInt);
        assertThat(stopWritesCount).isEqualTo(0);
        assertThat(InfoResponseUtils.getPropertyFromInfoResponse(response, "set-enable-xdr", Function.identity())).isEqualTo("use-default");
        assertThat(InfoResponseUtils.getPropertyFromInfoResponse(response, "disable-eviction", Boolean::parseBoolean)).isFalse();
    }

    @Test
    void propertyInTheMiddleOfResponse() {
        String response = ";memory-size=1073741824;default-ttl=2592000;replication-factor=2;";

        Integer property = InfoResponseUtils.getPropertyFromConfigResponse(response, "replication-factor",
                Integer::parseInt);

        assertThat(property).isEqualTo(2);
    }

    @Test
    void propertyInvalidTypeInResponse() {
        String response = "memory-size=1073741824;default-ttl=2592000;replication-factor=factor;";

        assertThatThrownBy(() -> InfoResponseUtils.getPropertyFromConfigResponse(response, "replication-factor",
                Integer::parseInt))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Failed to parse value 'factor' for property 'replication-factor' " +
                        "in response");
    }

    @Test
    void propertyInvalidFormatInResponse() {
        String response = "memory-size=1073741824;default-ttl=2592000;replication-factor;";

        assertThatThrownBy(() -> InfoResponseUtils.getPropertyFromConfigResponse(response, "replication-factor",
                Integer::parseInt))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Failed to parse server response. Expected property " +
                        "'replication-factor' to have length 2 in response");
    }

    @Test
    void missingPropertyInResponse() {
        String response = "memory-size=1073741824;default-ttl=2592000;";

        assertThatThrownBy(() -> InfoResponseUtils.getPropertyFromConfigResponse(response, "replication-factor",
                Integer::parseInt))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Failed to parse server response. Cannot find property " +
                        "'replication-factor' in response");
    }

    @Test
    void emptyResponse() {
        String response = "";

        assertThatThrownBy(() -> InfoResponseUtils.getPropertyFromConfigResponse(response, "replication-factor",
                Integer::parseInt))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Failed to parse server response. Cannot find property " +
                        "'replication-factor' in response");
    }
}
