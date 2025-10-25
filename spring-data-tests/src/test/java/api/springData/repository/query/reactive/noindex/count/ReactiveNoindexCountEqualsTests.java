package api.springData.repository.query.reactive.noindex.count;

import api.springData.repository.query.reactive.noindex.ReactiveCustomerRepositoryQueryTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.QueryParam;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

/**
 * Tests for the "Equals" reactive repository query. Keywords: Is, Equals (or no keyword).
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns21"})
@TestPropertySource(properties = {"customerSetName=customerCountReactiveEqualsTests"})
@Tag("SPRING-DATA-TESTS-2")
public class ReactiveNoindexCountEqualsTests extends ReactiveCustomerRepositoryQueryTests {

    @Test
    public void countById_AND_SimpleProperty() {
        QueryParam id = QueryParam.of(marge.getId());
        QueryParam firstName = QueryParam.of(marge.getFirstName());
        StepVerifier.create(reactiveRepository.countByIdAndFirstName(id, firstName))
            .expectNext(1L)
            .verifyComplete();

        firstName = QueryParam.of(marge.getFirstName() + "_");
        StepVerifier.create(reactiveRepository.countByIdAndFirstName(id, firstName))
            .expectNext(0L)
            .verifyComplete();
    }
}
