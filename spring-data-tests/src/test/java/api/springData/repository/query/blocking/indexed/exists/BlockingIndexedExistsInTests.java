package api.springData.repository.query.blocking.indexed.exists;

import api.springData.config.AssertBinsAreIndexed;
import api.springData.config.NoSecondaryIndexRequired;
import api.springData.repository.query.blocking.indexed.IndexedPersonRepositoryQueryTests;
import api.springData.sample.IndexedPerson;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.QueryParam;
import org.springframework.data.aerospike.query.model.Index;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;

import static com.aerospike.client.query.IndexType.STRING;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the "Is in" reactive repository query. Keywords: In, IsIn.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns21"})
@TestPropertySource(properties = {"indexedPersonSetName=personExistsSetNameInTestsIndexed"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingIndexedExistsInTests extends IndexedPersonRepositoryQueryTests {

    @Override
    protected List<Index> newIndexes() {
        List<Index> newIndexes = new ArrayList<>();
        newIndexes.add(Index.builder()
                .set(template.getSetName(IndexedPerson.class))
                .name("indexed_person_first_name_" + "exists_in")
                .bin("firstName")
                .indexType(STRING)
                .build());
        return newIndexes;
    }

    @Test
    @AssertBinsAreIndexed(binNames = "firstName", entityClass = IndexedPerson.class)
    public void existsBySimplePropertyIn() {
        var firstNames = List.of("Billy", "Tricia");
        // No Filter for IN query
        assertQueryHasNoSecIndexFilter("existsByFirstNameIn", IndexedPerson.class, firstNames);
        assertThat(repository.existsByFirstNameIn(firstNames)).isTrue();

        firstNames = List.of("FirstName");
        assertThat(repository.existsByFirstNameIn(firstNames)).isFalse();
    }

    @Test
    @NoSecondaryIndexRequired
    public void existsById_AND_SimplePropertyIn() {
        QueryParam ids = QueryParam.of(List.of(billy.getId(), tricia.getId()));
        QueryParam firstNames = QueryParam.of(List.of(billy.getFirstName(), tricia.getFirstName(), "FirstName"));
        // SIndex Filter cannot be used so far because in such combined query we use client.get()
        assertThat(repository.existsByIdAndFirstNameIn(ids, firstNames)).isTrue();

        firstNames = QueryParam.of(List.of("FirstName"));
        assertThat(repository.existsByIdAndFirstNameIn(ids, firstNames)).isFalse();
    }
}
