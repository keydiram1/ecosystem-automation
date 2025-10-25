package api.springData.query.queryEngine.reactive;

import com.aerospike.client.query.KeyRecord;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.FilterOperation;
import org.springframework.data.aerospike.query.qualifier.Qualifier;
import org.springframework.data.aerospike.repository.query.Query;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static api.springData.query.queryEngine.QueryEngineTestDataPopulator.USERS_SET;
import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns11"})
@Tag("SPRING-DATA-TESTS-1")
public class ReactiveUsersTests extends BaseReactiveQueryEngineTests {

    @Test
    public void usersInNorthRegion() {
        Qualifier qualifier = Qualifier.builder()
            .setPath("region")
            .setFilterOperation(FilterOperation.EQ)
            .setValue("n")
            .build();

        Flux<KeyRecord> flux = reactiveQueryEngine.select(namespace, USERS_SET, null, new Query(qualifier));

        StepVerifier.create(flux.collectList())
            .expectNextMatches(results -> {
                assertThat(results)
                    .isNotEmpty()
                    .allSatisfy(rec -> assertThat(rec.record.getString("region")).isEqualTo("n"));
                return true;
            })
            .verifyComplete();
    }
}
