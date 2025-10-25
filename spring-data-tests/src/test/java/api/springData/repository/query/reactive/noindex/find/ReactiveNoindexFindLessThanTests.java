package api.springData.repository.query.reactive.noindex.find;

import api.springData.repository.query.reactive.noindex.ReactiveCustomerRepositoryQueryTests;
import api.springData.sample.Customer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.domain.Sort.Order.asc;

/**
 * Tests for the "Is less than" reactive repository query. Keywords: LessThan, IsLessThan.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns21"})
@TestPropertySource(properties = {"customerSetName=customerReactiveLtTests"})
@Tag("SPRING-DATA-TESTS-2")
public class ReactiveNoindexFindLessThanTests extends ReactiveCustomerRepositoryQueryTests {

    @Test
    public void findByAgeLessThan_ShouldWorkProperly() {
        List<Customer> results = reactiveRepository.findByAgeLessThan(40, Sort.by(asc("firstName")))
            .collectList().block();

        assertThat(results).containsExactly(bart, leela, lisa, maggie, marge);
    }
}
