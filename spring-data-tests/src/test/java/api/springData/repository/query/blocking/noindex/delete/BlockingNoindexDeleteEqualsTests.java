package api.springData.repository.query.blocking.noindex.delete;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.QueryParam;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.aerospike.query.QueryParam.of;

/**
 * Tests for the "Equals" repository query. Keywords: Is, Equals (or no keyword).
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns22"})
@TestPropertySource(properties = {"personSetName=personDeleteSetNameEqualsTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexDeleteEqualsTests extends PersonRepositoryQueryTests {

    @Test
    void deleteBySimplePropertyEquals_String() {
        assertThat(repository.findByFirstName("Leroi")).isNotEmpty();
        repository.deleteByFirstName("Leroi");
        assertThat(repository.findByFirstName("Leroi")).isEmpty();

        repository.save(leroi);
        assertThat(repository.findByFirstNameIgnoreCase("lEroi")).isNotEmpty();
        repository.deleteByFirstNameIgnoreCase("lEroi");
        assertThat(repository.findByFirstNameIgnoreCase("lEroi")).isEmpty();
    }

    @Test
    void deletePersonById_AND_simpleProperty() {
        QueryParam ids = of(dave.getId());
        QueryParam name = of(carter.getFirstName());
        repository.deleteByIdAndFirstName(ids, name);
        assertThat(repository.findByIdAndFirstName(ids, name)).isEmpty();

        ids = of(dave.getId());
        name = of(dave.getFirstName());
        assertThat(repository.findByIdAndFirstName(ids, name)).isNotEmpty();
        repository.deleteByIdAndFirstName(ids, name);
        assertThat(repository.findByIdAndFirstName(ids, name)).isEmpty();
    }
}
