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
 * Tests for the "Equals" reactive repository query. Keywords: Is, Equals (or no keyword).
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns21"})
@TestPropertySource(properties = {"customerSetName=customerReactiveEqualsTests"})
@Tag("SPRING-DATA-TESTS-2")
public class ReactiveNoindexFindLikeTests extends ReactiveCustomerRepositoryQueryTests {

    @Test
    void findBySimplePropertyLike_String() {
        List<Customer> results = reactiveRepository.findByFirstNameLike("Mat.*")
                .collectList().block();
        assertThat(results).containsOnly(matt);
    }

    @Test
    void findByIdLike_String() {
        List<Customer> results = reactiveRepository.findByIdLike("as-.*")
                .collectList().block();
        assertThat(results).contains(matt);

        QueryParam idLike = of("as-.*");
        QueryParam name = of(marge.getFirstName());
        results = reactiveRepository.findByIdLikeAndFirstName(idLike, name)
                .collectList().block();
        assertThat(results).contains(marge);

        QueryParam ids = of(List.of(marge.getId(), homer.getId()));
        results = reactiveRepository.findByIdLikeAndId(idLike, ids)
                .collectList().block();
        assertThat(results).contains(marge, homer);
    }
}
