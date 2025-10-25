package api.springData.repository.query.blocking.indexed.count;

import api.springData.config.NoSecondaryIndexRequired;
import api.springData.repository.query.blocking.indexed.IndexedPersonRepositoryQueryTests;
import api.springData.sample.IndexedPerson;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.QueryParam;
import org.springframework.data.aerospike.query.model.Index;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the "Is in" reactive repository query. Keywords: In, IsIn.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns21"})
@TestPropertySource(properties = {"indexedPersonSetName=personCountSetNameInTestsIndexed"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingIndexedCountInTests extends IndexedPersonRepositoryQueryTests {

    @Override
    protected List<Index> newIndexes() {
        return List.of();
    }

    @Test
    @NoSecondaryIndexRequired
    public void countById_AND_SimplePropertyIn() {
        QueryParam ids = QueryParam.of(List.of(billy.getId(), tricia.getId()));
        QueryParam firstNames = QueryParam.of(List.of(billy.getFirstName(), tricia.getFirstName(), "FirstName"));
        // SIndex Filter cannot be used so far because in such combined query we use client.get()
        assertQueryHasNoSecIndexFilter("countByIdAndFirstNameIn", IndexedPerson.class, ids, firstNames);
        assertThat(repository.countByIdAndFirstNameIn(ids, firstNames)).isEqualTo(2);

        firstNames = QueryParam.of(List.of("FirstName"));
        assertThat(repository.countByIdAndFirstNameIn(ids, firstNames)).isEqualTo(0);
    }
}
