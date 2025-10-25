package api.springData.repository.query.reactive.noindex.find;

import api.springData.repository.query.reactive.noindex.ReactiveCustomerRepositoryQueryTests;
import api.springData.sample.Customer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the "Is not equal" reactive repository query. Keywords: Not, IsNot.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns21"})
@TestPropertySource(properties = {"customerSetName=customerReactiveNeTests"})
@Tag("SPRING-DATA-TESTS-2")
public class ReactiveNoindexFindNotEqualTests extends ReactiveCustomerRepositoryQueryTests {

    @Test
    public void findBySimplePropertyNotEqual_String() {
        StepVerifier.create(reactiveRepository.findByLastNameNot("Simpson"))
            .recordWith(List::of)
            .consumeRecordedWith(customers -> {
                assertThat(customers).containsExactlyInAnyOrderElementsOf(List.of(matt, leela, fry));
            })
            .expectComplete();

        StepVerifier.create(reactiveRepository.findByFirstNameNotIgnoreCase("SimpSon"))
            // this query returns Mono<Collection>
            .expectNextMatches(customers -> {
                assertThat(customers).containsExactlyInAnyOrderElementsOf(List.of(matt, leela, fry));
                return false;
            })
            .expectComplete();

        StepVerifier.create(reactiveRepository.findOneByLastNameNot("Simpson"))
            // this query returns Mono<Customer>
            .expectNextMatches(customer -> {
                assertThat(customer).isIn(List.of(matt, leela, fry));
                return false;
            })
            .expectComplete();

        Stream<Customer> customersStream = reactiveRepository.findByFirstNameNot("Simpson");
        assertThat(customersStream.toList()).containsExactlyInAnyOrderElementsOf(allCustomers);

        assertThatThrownBy(() -> negativeTestsReactiveRepository.findByLastNameNotIgnoreCase("Simpson"))
            .isInstanceOf(ClassCastException.class)
            .hasMessageContaining("cannot be cast");
    }
}
