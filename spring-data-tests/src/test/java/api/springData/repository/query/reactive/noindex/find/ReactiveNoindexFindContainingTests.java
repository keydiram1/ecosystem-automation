package api.springData.repository.query.reactive.noindex.find;

import api.springData.repository.query.reactive.noindex.ReactiveCustomerRepositoryQueryTests;
import api.springData.sample.Customer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the "Contains" reactive repository query. Keywords: Containing, IsContaining, Contains.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns21"})
@TestPropertySource(properties = {"customerSetName=customerReactiveContainingTests"})
@Tag("SPRING-DATA-TESTS-2")
public class ReactiveNoindexFindContainingTests extends ReactiveCustomerRepositoryQueryTests {

    @Test
    public void findBySimplePropertyContaining() {
        List<Customer> results = reactiveRepository.findByFirstNameContains("ar")
            .collectList().block();

        assertThat(results).containsOnly(marge, bart);
    }

    @Test
    public void findBySimplePropertyContaining_IgnoreCase() {
        List<Customer> results = reactiveRepository.findByFirstNameContainingIgnoreCase("m")
            .collectList().block();

        assertThat(results).containsOnly(homer, marge, matt, maggie);
    }
}
