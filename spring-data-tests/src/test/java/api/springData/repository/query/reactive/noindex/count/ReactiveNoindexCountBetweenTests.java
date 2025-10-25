package api.springData.repository.query.reactive.noindex.count;

import api.springData.repository.query.reactive.noindex.ReactiveCustomerRepositoryQueryTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

/**
 * Tests for the "Is between" reactive repository query. Keywords: Between, IsBetween.
 */
@TestPropertySource(properties = "spring.data.aerospike.namespace=source-ns21")
@TestPropertySource(properties = {"customerSetName=customerCountReactiveBetweenTestsIndexed"})
@Tag("SPRING-DATA-TESTS-2")
public class ReactiveNoindexCountBetweenTests extends ReactiveCustomerRepositoryQueryTests {

    @Test
    public void countBySimplePropertyBetween_Integer() {
        StepVerifier.create(reactiveRepository.countByAgeBetween(20, 1100))
            .expectNextMatches(result -> result >= 2)
            .verifyComplete();
    }
}
