package api.springData.repository.query.blocking.indexed.find;

import api.springData.config.AssertBinsAreIndexed;
import api.springData.repository.query.blocking.indexed.IndexedPersonRepositoryQueryTests;
import api.springData.sample.IndexedPerson;
import api.springData.utility.TestUtils;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.CTX;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.FilterOperation;
import org.springframework.data.aerospike.query.QueryParam;
import org.springframework.data.aerospike.query.model.Index;
import org.springframework.data.aerospike.query.model.IndexedField;
import org.springframework.data.aerospike.query.qualifier.Qualifier;
import org.springframework.data.aerospike.repository.query.Query;
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
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns6"})
@TestPropertySource(properties = {"indexedPersonSetName=personSetNameBetweenTestsIndexed"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingIndexedFindBetweenTests extends IndexedPersonRepositoryQueryTests {

    @Override
    protected List<Index> newIndexes() {
        List<Index> newIndexes = new ArrayList<>();
        String setName = template.getSetName(IndexedPerson.class);
        String postfix = "find_between";
        newIndexes.add(Index.builder()
                .set(setName)
                .name("indexed_person_age_" + postfix)
                .bin("age")
                .indexType(NUMERIC)
                .build());
        newIndexes.add(Index.builder()
                .set(setName)
                .name("indexed_person_bestFriend_friend_address_values_")
                .bin("bestFriend")
                .indexType(STRING)
                .indexCollectionType(MAPVALUES)
                .ctx(new CTX[]{CTX.mapKey(Value.get("friend")), CTX.mapKey(Value.get("address"))})
                .build());
        newIndexes.add(Index.builder()
                .set(setName)
                .name("indexed_person_bestFriend_friend_address_values_num_index")
                .bin("bestFriend")
                .indexType(NUMERIC)
                .indexCollectionType(MAPVALUES)
                .ctx(new CTX[]{CTX.mapKey(Value.get("friend")), CTX.mapKey(Value.get("address"))})
                .build());
        return newIndexes;
    }

    @Test
    @AssertBinsAreIndexed(binNames = "age", entityClass = IndexedPerson.class)
    public void findBySimplePropertyBetween_Integer() {
        assertQueryHasSecIndexFilter_Debug("findByAgeBetween", IndexedPerson.class, 40, 45);
        assertThat(indexesCache.hasIndexFor(
                new IndexedField("source-ns6", "personSetNameBetweenTestsIndexed", "age"))
        ).withFailMessage("No index for 'age' bin in source-ns6.personSetNameBetweenTestsIndexed").isTrue();
        Iterable<IndexedPerson> it = repository.findByAgeBetween(40, 45);
        assertThat(it).hasSize(2).contains(john, peter);
    }

    @Test
    @AssertBinsAreIndexed(binNames = "age", entityClass = IndexedPerson.class)
    public void findBySimplePropertyBetween_Integer_OrderBySimpleProperty() {
        assertQueryHasSecIndexFilter("findByAgeBetweenOrderByLastName", IndexedPerson.class, 30, 45);
        Iterable<IndexedPerson> it = repository.findByAgeBetweenOrderByLastName(30, 45);
        assertThat(it).hasSize(3);
    }

    @Test
    @AssertBinsAreIndexed(binNames = "age", entityClass = IndexedPerson.class)
    public void findBySimplePropertyBetween_Integer_AND_SimplePropertyEquals_String() {
        QueryParam ageBetween = QueryParam.of(40, 45);
        QueryParam lastName = QueryParam.of("Matthews");
        assertQueryHasSecIndexFilter("findByAgeBetweenAndLastName", IndexedPerson.class, ageBetween, lastName);
        Iterable<IndexedPerson> it = repository.findByAgeBetweenAndLastName(ageBetween, lastName);
        assertThat(it).hasSize(0);

        ageBetween = QueryParam.of(20, 26);
        lastName = QueryParam.of("Smith");
        assertQueryHasSecIndexFilter("findByAgeBetweenAndLastName", IndexedPerson.class, ageBetween, lastName);
        Iterable<IndexedPerson> result = repository.findByAgeBetweenAndLastName(ageBetween, lastName);
        assertThat(result).hasSize(1).contains(billy);
    }

    @Test
    @AssertBinsAreIndexed(binNames = "age", entityClass = IndexedPerson.class)
    public void findBySimplePropertyBetween_Integer_OR_SimplePropertyEquals_String() {
        QueryParam ageBetween = QueryParam.of(40, 45);
        QueryParam lastName = QueryParam.of("James");
        assertThat(queryHasSecIndexFilter("findByAgeBetween", IndexedPerson.class, 40, 45)
                || queryHasSecIndexFilter("findByLastName", IndexedPerson.class, "James")).isTrue();
        Iterable<IndexedPerson> it = repository.findByAgeBetweenOrLastName(ageBetween, lastName);
        assertThat(it).containsExactlyInAnyOrder(john, peter, tricia);

        ageBetween = QueryParam.of(20, 26);
        lastName = QueryParam.of("Macintosh");
        assertThat(queryHasSecIndexFilter("findByAgeBetween", IndexedPerson.class, 20, 26)
                || queryHasSecIndexFilter("findByLastName", IndexedPerson.class, "Macintosh")).isTrue();
        Iterable<IndexedPerson> result = repository.findByAgeBetweenOrLastName(ageBetween, lastName);
        assertThat(result).containsExactlyInAnyOrder(billy, peter);
    }

    @Test
    @AssertBinsAreIndexed(binNames = "bestFriend", entityClass = IndexedPerson.class)
    void findByNestedSimplePropertyBetween_Integer_3_levels() {
        int apartment = 2;
        assertThat(jane.getAddress().getApartment()).isEqualTo(apartment);
        tricia.setFriend(jane);
        repository.save(tricia);
        billy.setBestFriend(tricia);
        repository.save(billy);
        assertThat(billy.getBestFriend().getFriend().getAddress().getApartment()).isEqualTo(apartment);

        assertQueryHasSecIndexFilter("findByBestFriendFriendAddressApartmentBetween", IndexedPerson.class, 1, 3);
        List<IndexedPerson> persons = repository.findByBestFriendFriendAddressApartmentBetween(1, 3);
        assertThat(persons).contains(billy);

        // An alternative way to perform the same using a custom query
        Qualifier nestedApartmentBetween = Qualifier.builder()
                // find records having a map with a key between given values
                // POJOs are saved as Maps
                .setFilterOperation(FilterOperation.MAP_VAL_BETWEEN_BY_KEY) // POJOs are saved as Maps
                .setPath("bestFriend.friend.address.apartment") // path includes bin name, context and the required map key
                .setValue(1) // lower limit for the value of the nested key
                .setSecondValue(3) // lower limit for the value of the nested key
                .build();

        assertQueryHasSecIndexFilter(new Query(nestedApartmentBetween), IndexedPerson.class);
        Iterable<IndexedPerson> persons2 = repository.findUsingQuery(new Query(nestedApartmentBetween));
        assertThat(persons).isEqualTo(persons2);
        TestUtils.setFriendsToNull(repository, tricia, billy);
    }
}
