/**
 *
 */
package api.springData.convert;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.convert.AerospikeReadData;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns9"})
@Tag("SPRING-DATA-TESTS-1")
public class AerospikeReadDataTest {

    @Test
    public void shouldThrowExceptionIfRecordIsNull() {
        assertThatThrownBy(() -> AerospikeReadData.forRead(new Key("namespace", "set", 867), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Record must not be null");
    }

    @Test
    public void shouldNotThrowExceptionIfRecordBinsAreNull() {
        AerospikeReadData data = AerospikeReadData.forRead(
                new Key("namespace", "set", 867),
                new Record(null, 0, 0));
        // If null bins are given, an empty Map is used instead
        assertThat(data.getAeroRecord()).isEqualTo(Map.of());
        assertThat(data.getValue("867")).isNull();
    }

    @Test
    public void shouldThrowExceptionIfKeyIsNull() {
        assertThatThrownBy(() -> AerospikeReadData.forRead(null, new Record(Collections.emptyMap(), 0, 0)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Key must not be null");
    }
}
