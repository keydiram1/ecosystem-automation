package api.springData;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns16"})
@Tag("SPRING-DATA-TESTS-1")
public class PoliciesVerificationTests extends BaseBlockingIntegrationTests {

    @Test
    public void sendKeyShouldBeTrueByDefault() {
        assertThat(client.getWritePolicyDefault().sendKey).isTrue();
    }
}
