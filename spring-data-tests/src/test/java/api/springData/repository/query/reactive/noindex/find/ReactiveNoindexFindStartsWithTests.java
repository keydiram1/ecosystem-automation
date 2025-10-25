package api.springData.repository.query.reactive.noindex.find;

import api.springData.repository.query.reactive.noindex.ReactiveCustomerRepositoryQueryTests;
import api.springData.sample.Customer;
import api.springData.sample.CustomerSomeFields;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the "Starts with" reactive repository query. Keywords: StartingWith, IsStartingWith, StartsWith.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns21"})
@TestPropertySource(properties = {"customerSetName=customerReactiveStartsWithTests"})
@Tag("SPRING-DATA-TESTS-2")
public class ReactiveNoindexFindStartsWithTests extends ReactiveCustomerRepositoryQueryTests {

    @Test
    public void findByFirstnameStartsWithOrderByAgeAsc_ShouldWorkProperly() {
        List<Customer> results = reactiveRepository.findByFirstNameStartsWithOrderByAgeAsc("Ma")
            .collectList().block();

        assertThat(results).containsExactly(maggie, marge, matt);
    }

    @Test
    public void findCustomerSomeFieldsByFirstnameStartsWithOrderByAgeAsc_ShouldWorkProperly() {
        List<CustomerSomeFields> results =
            reactiveRepository.findCustomerSomeFieldsByFirstNameStartsWithOrderByFirstNameAsc("Ma")
                .collectList().block();

        assertThat(results).containsExactly( maggie.toCustomerSomeFields(), marge.toCustomerSomeFields(),
            matt.toCustomerSomeFields());
    }
}
