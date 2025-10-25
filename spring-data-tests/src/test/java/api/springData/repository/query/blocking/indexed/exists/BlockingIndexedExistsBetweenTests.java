package api.springData.repository.query.blocking.indexed.exists;

import api.springData.config.AssertBinsAreIndexed;
import api.springData.repository.query.blocking.indexed.IndexedPersonRepositoryQueryTests;
import api.springData.sample.IndexedPerson;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.model.Index;
import org.springframework.data.aerospike.query.model.IndexedField;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;

import static com.aerospike.client.query.IndexCollectionType.MAPVALUES;
import static com.aerospike.client.query.IndexType.NUMERIC;
import static com.aerospike.client.query.IndexType.STRING;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the "Is between" repository query. Keywords: Between, IsBetween.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns21"})
@TestPropertySource(properties = {"indexedPersonSetName=personExistsSetNameBetweenTestsIndexed"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingIndexedExistsBetweenTests extends IndexedPersonRepositoryQueryTests {

    @Override
    protected List<Index> newIndexes() {
        List<Index> newIndexes = new ArrayList<>();
        String setName = template.getSetName(IndexedPerson.class);
        String postfix = "exists_between";
        newIndexes.add(Index.builder()
                .set(setName)
                .name("indexed_person_firstName_" + postfix)
                .bin("firstName")
                .indexType(STRING)
                .build());
        newIndexes.add(Index.builder()
                .set(setName)
                .name("indexed_person_age_" + postfix)
                .bin("age")
                .indexType(NUMERIC)
                .build());
        newIndexes.add(Index.builder()
                .set(setName)
                .name("indexed_person_address_values_num_" + postfix)
                .bin("address")
                .indexType(NUMERIC)
                .indexCollectionType(MAPVALUES)
                .build());
        return newIndexes;
    }

    @Test
    @AssertBinsAreIndexed(binNames = "age", entityClass = IndexedPerson.class)
    void existsBySimplePropertyBetween_Integer() {
        assertThat(john.getAge()).isBetween(40, 46);
        assertThat(peter.getAge()).isBetween(40, 46);
        assertQueryHasSecIndexFilter("existsByAgeBetween", IndexedPerson.class, 40, 46);

        long result = repository.countByAgeBetween(40, 46);
        assertThat(result).isGreaterThan(0);
    }

    @Test
    @AssertBinsAreIndexed(binNames = "firstName", entityClass = IndexedPerson.class)
    void existsBySimplePropertyBetween_String() {
        assertThat(indexesCache.hasIndexFor(
                new IndexedField("source-ns21", "personExistsSetNameBetweenTestsIndexed", "firstName"))
        ).isTrue();
        assertQueryHasNoSecIndexFilter("existsByFirstNameBetween", IndexedPerson.class, "Jane", "John");
        long result = repository.countByFirstNameBetween("Jane", "John");
        assertThat(result).isEqualTo(1);
    }

    @Test
    @AssertBinsAreIndexed(binNames = "address", entityClass = IndexedPerson.class)
    void existsByNestedSimplePropertyBetween_Integer() {
        assertThat(jane.getAddress().getApartment()).isEqualTo(2);
        assertThat(john.getAddress().getApartment()).isEqualTo(1);
        assertQueryHasSecIndexFilter("existsByAddressApartmentBetween", IndexedPerson.class, 1, 3);
        assertThat(repository.countByAddressApartmentBetween(1, 3)).isEqualTo(2);
    }
}
