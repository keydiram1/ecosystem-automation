package api.springData.repository.query.blocking.noindex.delete;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import api.springData.sample.Person;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the "Is greater than" repository query. Keywords: GreaterThan, IsGreaterThan.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns22"})
@TestPropertySource(properties = {"personSetName=personDeleteSetNameGtTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexDeleteGreaterThanTests extends PersonRepositoryQueryTests {

    @Test
    void deleteBySimpleProperty_Integer_Paginated() {
        List<Person> findQueryResults = repository.findByAgeGreaterThan(40);
        assertThat(findQueryResults).isNotEmpty();

        repository.deleteByAgeGreaterThan(40, PageRequest.of(0, 1));
        List<Person> findQueryResultsAfterDelete = repository.findByAgeGreaterThan(40);
        assertThat(findQueryResultsAfterDelete.size()).isNotZero().isLessThan(findQueryResults.size());

        // Query to delete results from a non-existing page, no records must be deleted
        repository.deleteByAgeGreaterThan(40, PageRequest.of(10, 1, Sort.by("firstName")));
        List<Person> findQueryResultsAfterDelete2 = repository.findByAgeGreaterThan(40);
        assertThat(findQueryResultsAfterDelete2).containsExactlyInAnyOrderElementsOf(findQueryResultsAfterDelete);
    }
}
