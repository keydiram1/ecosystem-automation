package api.springData.repository.query.reactive.noindex.exists;

import api.springData.repository.query.reactive.noindex.ReactiveCustomerRepositoryQueryTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.QueryParam;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

import java.util.List;

/**
 * Tests for the "Is in" reactive repository query. Keywords: In, IsIn.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns21"})
@TestPropertySource(properties = {"customerSetName=customerExistsReactiveInTests"})
@Tag("SPRING-DATA-TESTS-2")
public class ReactiveNoindexExistsInTests extends ReactiveCustomerRepositoryQueryTests {

    @Test
    public void existsById_AND_SImplePropertyIn() {
        QueryParam ids = QueryParam.of(List.of(fry.getId(), leela.getId()));
        QueryParam firstNames = QueryParam.of(List.of(fry.getFirstName(), leela.getFirstName(), "FirstName"));
        StepVerifier.create(reactiveRepository.existsByIdAndFirstNameIn(ids, firstNames))
            .expectNext(true)
            .verifyComplete();

        firstNames = QueryParam.of(List.of("FirstName"));
        StepVerifier.create(reactiveRepository.existsByIdAndFirstNameIn(ids, firstNames))
            .expectNext(false)
            .verifyComplete();
    }
}
