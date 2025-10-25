package api.springData.repository.query.blocking.indexed.count;

import api.springData.config.AssertBinsAreIndexed;
import api.springData.repository.query.blocking.indexed.IndexedPersonRepositoryQueryTests;
import api.springData.sample.IndexedPerson;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.model.Index;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;

import static com.aerospike.client.query.IndexType.STRING;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the "Equals" repository query. Keywords: Is, Equals (or no keyword).
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns19"})
@TestPropertySource(properties = {"indexedPersonSetName=personCountSetNameEqualsTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingIndexedCountEqualsTests extends IndexedPersonRepositoryQueryTests {

    @Override
    protected List<Index> newIndexes() {
        List<Index> newIndexes = new ArrayList<>();
        newIndexes.add(Index.builder()
                .set(template.getSetName(IndexedPerson.class))
                .name("indexed_person_last_name_" + "count_equals")
                .bin("lastName")
                .indexType(STRING)
                .build());
        return newIndexes;
    }

    @Test
    @AssertBinsAreIndexed(binNames = "lastName", entityClass = IndexedPerson.class)
    public void countBySimpleProperty_String() {
        assertThat(repository.countByLastName("Lerois")).isZero();
        assertThat(repository.countByLastName("James")).isEqualTo(1);

        assertQueryHasSecIndexFilter("countByLastName", IndexedPerson.class, "James");
        assertThat(repository.countByLastName("James")).isEqualTo(1);
    }
}
