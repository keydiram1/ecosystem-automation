package api.springData.repository.query.blocking.noindex.exists;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.QueryParam;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.aerospike.query.QueryParam.of;

/**
 * Tests for the "Equals" repository query. Keywords: Is, Equals (or no keyword).
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns21"})
@TestPropertySource(properties = {"personSetName=personExistsSetNameEqualsTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexExistsEqualsTests extends PersonRepositoryQueryTests {

    @Test
    void existsBySimplePropertyEquals_String() {
        boolean result = repository.existsByFirstName("Leroi");
        assertThat(result).isTrue();

        boolean result1 = repository.existsByFirstNameIgnoreCase("lEroi");
        assertThat(result1).isTrue();

        boolean result2 = repository.existsByFirstNameIs("lEroi"); // another way to call the query method
        assertThat(result2).isFalse();
    }

    @Test
    void existsPersonById_AND_simpleProperty() {
        QueryParam ids = of(dave.getId());
        QueryParam name = of(carter.getFirstName());
        boolean result = repository.existsByIdAndFirstName(ids, name);
        assertThat(result).isFalse();

        ids = of(dave.getId());
        name = of(dave.getFirstName());
        result = repository.existsByIdAndFirstName(ids, name);
        assertThat(result).isTrue();

        ids = of(List.of(leroi.getId(), leroi2.getId(), carter.getId()));
        QueryParam firstName = of(leroi.getFirstName());
        QueryParam age = of(stefan.getAge());
        boolean result2 = repository.existsByIdAndFirstNameOrAge(ids, firstName, age);
        assertThat(result2).isTrue();
    }
}
