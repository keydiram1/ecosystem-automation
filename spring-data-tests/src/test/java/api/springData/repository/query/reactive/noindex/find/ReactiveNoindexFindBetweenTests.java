package api.springData.repository.query.reactive.noindex.find;

import api.springData.repository.query.reactive.noindex.ReactiveCustomerRepositoryQueryTests;
import api.springData.sample.Customer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.QueryParam;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.aerospike.query.QueryParam.of;

/**
 * Tests for the "Is between" reactive repository query. Keywords: Between, IsBetween.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns21"})
@TestPropertySource(properties = {"customerSetName=customerReactiveBetweenTests"})
@Tag("SPRING-DATA-TESTS-2")
public class ReactiveNoindexFindBetweenTests extends ReactiveCustomerRepositoryQueryTests {

    @Test
    public void findBySimplePropertyBetween() {
        List<Customer> results = reactiveRepository.findByAgeBetween(10, 40)
            .collectList().block();

        assertThat(results).containsOnly(marge, bart, leela);
    }

    @Test
    public void findBySimplePropertyBetween_AND_SimpleProperty() {
        QueryParam ageBetween = of(30, 70);
        QueryParam lastName = of("Simpson");
        List<Customer> results = reactiveRepository.findByAgeBetweenAndLastName(ageBetween, lastName)
            .collectList().block();

        assertThat(results).containsOnly(homer, marge);
    }

    @Test
    public void findBySimplePropertyBetween_OrderByFirstnameDesc() {
        List<Customer> results = reactiveRepository.findByAgeBetweenOrderByFirstNameDesc(30, 70)
            .collectList().block();

        assertThat(results).containsExactly(matt, marge, homer);
    }
}
