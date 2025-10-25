package api.springData.repository.query.reactive.noindex.delete;

import api.springData.repository.query.reactive.noindex.ReactiveCustomerRepositoryQueryTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.QueryParam;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

/**
 * Tests for the "Is in" reactive repository query. Keywords: In, IsIn.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns22"})
@TestPropertySource(properties = {"customerSetName=customerDeleteReactiveInTests"})
@Tag("SPRING-DATA-TESTS-2")
public class ReactiveNoindexDeleteInTests extends ReactiveCustomerRepositoryQueryTests {

    @Test
    public void deleteById_AND_SimplePropertyIn() {
        StepVerifier.create(reactiveRepository.findAllById(List.of(bart.getId(), lisa.getId())).collectList())
            .expectNextMatches(list -> list.size() == 2 && list.contains(bart) && list.contains(lisa))
            .verifyComplete();

        QueryParam ids = QueryParam.of(List.of(bart.getId(), lisa.getId()));
        QueryParam firstNames = QueryParam.of(List.of("FirstName"));
        // no records satisfying the condition
        StepVerifier.create(reactiveRepository.deleteByIdAndFirstNameIn(ids, firstNames))
            .expectComplete()
            .verify();

        // no records get deleted
        StepVerifier.create(reactiveRepository.findAllById(List.of(bart.getId(), lisa.getId())).collectList())
            .expectNextMatches(list -> list.size() == 2 && list.contains(bart) && list.contains(lisa))
            .verifyComplete();

        // 2 records satisfying the condition
        firstNames = QueryParam.of(List.of(bart.getFirstName(), lisa.getFirstName(), "FirstName"));
        StepVerifier.create(reactiveRepository.deleteByIdAndFirstNameIn(ids, firstNames))
            .expectComplete()
            .verify();

        // 2 records get deleted
        StepVerifier.create(reactiveRepository.findAllById(List.of(bart.getId(), lisa.getId())).collectList())
            .expectNextMatches(List::isEmpty)
            .verifyComplete();

        // cleanup
        StepVerifier.create(reactiveRepository.saveAll(Flux.just(bart, lisa)))
            .expectNextCount(2)
            .verifyComplete();
    }
}
