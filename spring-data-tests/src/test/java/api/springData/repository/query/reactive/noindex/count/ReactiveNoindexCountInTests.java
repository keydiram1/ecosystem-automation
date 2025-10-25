package api.springData.repository.query.reactive.noindex.count;

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
@TestPropertySource(properties = {"customerSetName=customerCountReactiveInTests"})
@Tag("SPRING-DATA-TESTS-2")
public class ReactiveNoindexCountInTests extends ReactiveCustomerRepositoryQueryTests {

    @Test
    public void countById_AND_SimplePropertyIn() {
        QueryParam ids = QueryParam.of(List.of(marge.getId(), homer.getId()));
        QueryParam firstNames = QueryParam.of(List.of(homer.getFirstName(), marge.getFirstName(), "FirstName"));
        StepVerifier.create(reactiveRepository.countByIdAndFirstNameIn(ids, firstNames))
            .expectNext(2L)
            .verifyComplete();

        firstNames = QueryParam.of(List.of("FirstName"));
        StepVerifier.create(reactiveRepository.countByIdAndFirstNameIn(ids, firstNames))
            .expectNext(0L)
            .verifyComplete();
    }
}
