package api.springData.repository.query.blocking.noindex.find;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import api.springData.sample.Address;
import api.springData.sample.Person;
import api.springData.utility.TestUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.data.aerospike.repository.query.CriteriaDefinition.AerospikeNullQueryCriterion.NULL_PARAM;
import static org.springframework.data.aerospike.repository.query.CriteriaDefinition.AerospikeQueryCriterion.*;

/**
 * Tests for the "Does not contain" repository query. Keywords: IsNotContaining, NotContaining, NotContains.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns20"})
@TestPropertySource(properties = {"personSetName=personSetNameNotContainingTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexFindNotContainingTests extends PersonRepositoryQueryTests {

    @Test
    void findByArrayNotContaining() {
        assertThat(repository.findByIntArrayNotContaining(1)).containsOnly(dave, donny, oliver, alicia, carter, boyd, stefan, leroi, leroi2, david, douglas);
        assertThat(repository.findByIntArrayNotContaining(5)).containsOnly(dave, donny, oliver, alicia, carter, boyd, stefan, leroi2, david, douglas);
        assertThat(repository.findByIntArrayNotContaining(10)).containsOnly(dave, donny, oliver, alicia, carter, boyd, stefan, leroi2, david, douglas, matias);
        assertThat(repository.findByIntArrayNotContaining(0)).containsOnly(dave, donny, oliver, alicia, carter, boyd, stefan, leroi, leroi2, douglas, matias);
        assertThat(repository.findByIntArrayNotContaining(-2)).containsOnly(dave, donny, oliver, alicia, carter, boyd, stefan, leroi, leroi2, douglas, matias);
        assertThat(repository.findByIntArrayNotContaining(2_147_483_647)).containsOnly(dave, donny, oliver, alicia, carter, boyd, stefan, leroi, leroi2, douglas, matias);
        assertThat(repository.findByIntArrayNotContaining(-2_147_483_648)).containsOnly(dave, donny, oliver, alicia, carter, boyd, stefan, leroi, leroi2, douglas, matias);
    }

    @Test
    void findBySimplePropertyNotContaining_String() {
        List<Person> persons = repository.findByFirstNameNotContaining("er");
        assertThat(persons).containsExactlyInAnyOrder(dave, donny, alicia, boyd, stefan, matias, douglas, david);
    }

    @Test
    void findByNestedSimplePropertyNotContaining() {
        Address cartersAddress = carter.getAddress();
        Address davesAddress = dave.getAddress();
        Address boydsAddress = boyd.getAddress();

        carter.setAddress(new Address("Foo Street 2", 2, "C10124", "C0123"));
        repository.save(carter);
        dave.setAddress(new Address("Foo Street 1", 1, "C10123", "Bar"));
        repository.save(dave);
        boyd.setAddress(new Address("Foo Street 3", 3, "C112344123", "Bar"));
        repository.save(boyd);

        List<Person> persons = repository.findByAddressZipCodeNotContaining("C10");
        assertThat(persons).containsExactlyInAnyOrder(donny, boyd, oliver, alicia, stefan, leroi, leroi2, matias,
                douglas, david);

        carter.setAddress(cartersAddress);
        repository.save(carter);
        dave.setAddress(davesAddress);
        repository.save(dave);
        boyd.setAddress(boydsAddress);
        repository.save(boyd);
    }

    @Test
    void findByCollectionNotContainingString() {
        List<Person> persons = repository.findByStringsNotContaining("str5");
        assertThat(persons).containsExactlyInAnyOrderElementsOf(allPersons);
    }

    @Test
    void findByCollectionNotContainingPOJO() {
        Address address1 = new Address("Foo Street 1", 1, "C0123", "Bar");

        List<Address> listOfAddresses = List.of(address1);
        stefan.setAddressesList(listOfAddresses);
        repository.save(stefan);

        List<Person> persons;
        persons = repository.findByAddressesListNotContaining(address1);
        assertThat(persons).containsExactlyInAnyOrderElementsOf(
                allPersons.stream().filter(person -> !person.getFirstName().equals("Stefan")).collect(Collectors.toList()));

        stefan.setAddressesList(null);
        repository.save(stefan);
    }

    @Test
    void findByCollectionContainingNull() {
        List<String> strings = new ArrayList<>();
        strings.add("ing");
        stefan.setStrings(strings);
        repository.save(stefan);
        assertThat(repository.findByStringsNotContaining(NULL_PARAM)).contains(stefan);

        strings.add(null);
        stefan.setStrings(strings);
        repository.save(stefan);
        assertThat(repository.findByStringsNotContaining(NULL_PARAM)).doesNotContain(stefan);

        stefan.setStrings(null); // cleanup
        repository.save(stefan);
    }

    @Test
    void findByCollection_NegativeTest() {
        assertThatThrownBy(() -> negativeTestsRepository.findByIntsNotContaining())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Person.ints NOT_CONTAINING: invalid number of arguments, expecting one");
    }

    @Test
    void findByMapNotContainingKey_String() {
        assertThat(donny.getStringMap()).containsKey("key1");
        assertThat(boyd.getStringMap()).containsKey("key1");

        List<Person> persons = repository.findByStringMapNotContaining(KEY, "key1");
        assertThat(persons).contains(dave, oliver, alicia, carter, stefan, leroi, leroi2, matias, douglas);
    }

    @Test
    void findByMapNotContainingValue_String() {
        assertThat(donny.getStringMap()).containsValue("val1");
        assertThat(boyd.getStringMap()).containsValue("val1");

        List<Person> persons = repository.findByStringMapNotContaining(VALUE, "val1");
        assertThat(persons).contains(dave, oliver, alicia, carter, stefan, leroi, leroi2, matias, douglas);
    }

    @Test
    void findByMapNotContainingKeyValuePair_String() {
        assertThat(donny.getStringMap()).containsKey("key1");
        assertThat(donny.getStringMap()).containsValue("val1");
        assertThat(boyd.getStringMap()).containsKey("key1");
        assertThat(boyd.getStringMap()).containsValue("val1");

        Map<String, String> cartersOriginalStringMap = carter.getStringMap();
        // setting a Map to check the case with existing key and a different value
        carter.setStringMap(Map.of("key1", "val2"));
        repository.save(carter);
        assertThat(carter.getStringMap()).containsKey("key1");
        assertThat(carter.getStringMap()).doesNotContainValue("val1");

        List<Person> personsNotContainingKeyValue = repository.findByStringMapNotContaining(KEY_VALUE_PAIR, "key1",
                "val1");
        assertThat(personsNotContainingKeyValue)
                .contains(carter)
                .doesNotContain(donny, boyd);

        // cleanup
        carter.setStringMap(cartersOriginalStringMap);
        repository.save(carter);
    }

    @Test
    void findByMapNotContainingNullValue() {
        Map<String, String> stringMap = new HashMap<>();
        stringMap.put("key", "str");
        stefan.setStringMap(stringMap);
        repository.save(stefan);

        // find Persons with stringMap not containing null value (regardless of a key)
        assertThat(repository.findByStringMapNotContaining(VALUE, NULL_PARAM)).contains(stefan);

        // Currently getting key-specific results for a Map requires 2 steps:
        // firstly query for all entities with existing map key
        List<Person> personsWithMapKeyExists = repository.findByStringMapContaining(KEY, "key");
        // and then process the results programmatically - leave only the records that have the key's value != null
        List<Person> personsWithMapValueNotNull = personsWithMapKeyExists.stream()
                .filter(person -> person.getStringMap().get("key") != null).toList();
        assertThat(personsWithMapValueNotNull).contains(stefan);

        stringMap.put("key", null);
        stefan.setStringMap(stringMap);
        repository.save(stefan);
        assertThat(repository.findByStringMapNotContaining(VALUE, NULL_PARAM)).doesNotContain(stefan);

        personsWithMapKeyExists = repository.findByStringMapContaining(KEY, "key");
        personsWithMapValueNotNull = personsWithMapKeyExists.stream()
                .filter(person -> person.getStringMap().get("key") != null).toList();
        assertThat(personsWithMapValueNotNull).doesNotContain(stefan);

        stefan.setStringMap(null); // cleanup
        repository.save(stefan);
    }

    @Test
    void findByMapNotContaining_NegativeTest() {
        assertThatThrownBy(() -> negativeTestsRepository.findByStringMapNotContaining())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Person.stringMap NOT_CONTAINING: invalid number of arguments, at least two arguments are " +
                        "required");

        assertThatThrownBy(() -> negativeTestsRepository.findByStringMapNotContaining("key1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Person.stringMap NOT_CONTAINING: invalid number of arguments, at least two arguments are " +
                        "required");

        assertThatThrownBy(() -> negativeTestsRepository.findByStringMapNotContaining(1100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Person.stringMap NOT_CONTAINING: invalid number of arguments, at least two arguments are " +
                        "required");

        assertThatThrownBy(() -> negativeTestsRepository.findByStringMapNotContaining(KEY, VALUE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Person.stringMap NOT_CONTAINING: invalid map key type at position 2");

        assertThatThrownBy(() -> negativeTestsRepository.findByStringMapNotContaining("key", "value",
                new Person("id", "firstName"), "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Person.stringMap NOT_CONTAINING: invalid first argument type, required " +
                        "AerospikeQueryCriterion");
    }

    @Test
    void findByNestedMapNotContainingNullValue() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            Map<String, String> stringMap = new HashMap<>();
            stringMap.put("key", "str");
            stefan.setStringMap(stringMap);
            repository.save(stefan);

            dave.setFriend(stefan);
            repository.save(dave);

            // find Persons with stringMap containing null value (regardless of key)
            assertThat(repository.findByFriendStringMapNotContaining(VALUE, NULL_PARAM)).contains(dave);

            // Currently getting key-specific results for a Map requires 2 steps:
            // firstly query for all entities with existing map key
            List<Person> personsWithMapKeyExists = repository.findByFriendStringMapContaining(KEY, "key");
            // and then process the results programmatically - leave only the records that have the key's value != null
            List<Person> personsWithMapValueNotNull = personsWithMapKeyExists.stream()
                    .filter(person -> person.getFriend().getStringMap().get("key") != null).toList();
            assertThat(personsWithMapValueNotNull).contains(dave);

            // Checking that the query results change if there is null value
            stringMap.put("key", null);
            stefan.setStringMap(stringMap);
            repository.save(stefan);
            assertThat(repository.findByFriendStringMapNotContaining(VALUE, NULL_PARAM)).doesNotContain(dave);

            personsWithMapKeyExists = repository.findByFriendStringMapContaining(KEY, "key");
            personsWithMapValueNotNull = personsWithMapKeyExists.stream()
                    .filter(person -> person.getFriend().getStringMap().get("key") != null).toList();
            assertThat(personsWithMapValueNotNull).doesNotContain(dave);

            TestUtils.setFriendsToNull(repository, dave); // cleanup
            stefan.setStringMap(null);
            repository.save(stefan);
        }
    }
}
