package api.springData.utility;

import org.assertj.core.data.Offset;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.util.TimeUtils;
import org.springframework.test.context.TestPropertySource;

import static api.springData.sample.SampleClasses.EXPIRATION_ONE_SECOND;
import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns12"})
@Tag("SPRING-DATA-TESTS-1")
public class TimeUtilsTests {

    @Test
    public void shouldConvertOffsetInSecondsToUnixTime() {
        long expected = DateTime.now().plusSeconds(EXPIRATION_ONE_SECOND).getMillis();
        long actual = TimeUtils.offsetInSecondsToUnixTime(EXPIRATION_ONE_SECOND);

        assertThat(actual).isCloseTo(expected, Offset.offset(100L));
    }

    @Test
    public void shouldConvertUnixTimeToOffsetInSeconds() {
        long nowPlusOneSecond = DateTime.now().plusSeconds(EXPIRATION_ONE_SECOND).getMillis();
        int actual = TimeUtils.unixTimeToOffsetInSeconds(nowPlusOneSecond);

        assertThat(actual).isEqualTo(EXPIRATION_ONE_SECOND);
    }
}
