package api.springData.repository.query.blocking.indexed.find;

import api.springData.config.NoSecondaryIndexRequired;
import api.springData.repository.query.blocking.indexed.IndexedPersonRepositoryQueryTests;
import api.springData.sample.IndexedPerson;
import api.springData.sample.Person;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.model.Index;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the CrudRepository queries API.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns10"})
@TestPropertySource(properties = {"indexedPersonSetName=personSetNameCrudRepositoryQueryTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingIndexedFindCrudRepositoryQueryTests extends IndexedPersonRepositoryQueryTests {

    @Override
    protected List<Index> newIndexes() {
        return List.of();
    }

    @Test
    @NoSecondaryIndexRequired
    public void findsPersonById() {
        Optional<IndexedPerson> person = repository.findById(john.getId());

        assertThat(person).hasValueSatisfying(actual -> {
            assertThat(actual).isInstanceOf(Person.class);
            assertThat(actual).isEqualTo(john);
        });
    }

    @Test
    @NoSecondaryIndexRequired
    public void findsAllWithGivenIds() {
        List<IndexedPerson> result = (List<IndexedPerson>) repository.findAllById(List.of(john.getId(),
                billy.getId()));

        assertThat(result)
                .contains(john, billy)
                .hasSize(2)
                .doesNotContain(jane, peter, tricia);
    }
}
