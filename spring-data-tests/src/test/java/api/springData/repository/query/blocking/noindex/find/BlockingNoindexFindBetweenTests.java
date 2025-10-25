package api.springData.repository.query.blocking.noindex.find;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import api.springData.sample.Address;
import api.springData.sample.Person;
import api.springData.utility.TestUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the "Is between" repository query. Keywords: Between, IsBetween.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns20"})
@TestPropertySource(properties = {"personSetName=personSetNameBetweenTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexFindBetweenTests extends PersonRepositoryQueryTests {

    @Test
    void betweenSpecificDates() {
        Date dateOfBirth = new Date();
        donny.setDateOfBirth(new Date(dateOfBirth.getTime() - 150));
        repository.save((donny));

        // Bug? 150 is not between 150 and 148?
        List<Person> persons = repository.findByDateOfBirthBetween(new Date(dateOfBirth.getTime() - 149),new Date(dateOfBirth.getTime() - 147));
        assertThat(persons).isEmpty();

        persons = repository.findByDateOfBirthBetween(new Date(dateOfBirth.getTime() - 150),new Date(dateOfBirth.getTime() - 148));
        assertThat(persons).contains(donny);

        donny.setDateOfBirth(null);
        repository.save(donny);
    }

    @Test
    void findBySimplePropertyBetween_Integer() {
        assertThat(dave.getAge()).isBetween(40, 46);
        Iterable<Person> persons = repository.findByAgeBetween(40, 46);
        assertThat(persons).contains(dave);
    }

    @Test
    void findBySimplePropertyBetween_String() {
        Iterable<Person> persons = repository.findByFirstNameBetween("Dave", "David");
        assertThat(persons).containsExactly(dave);
    }

    @Test
    void findByNestedSimplePropertyBetween_Integer() {
        oliver.setFriend(alicia);
        repository.save(oliver);
        dave.setFriend(oliver);
        repository.save(dave);
        carter.setFriend(dave);
        repository.save(carter);

        List<Person> result = repository.findByFriendAgeBetween(40, 45);
        assertThat(result)
            .hasSize(1)
            .containsExactly(carter);

        TestUtils.setFriendsToNull(repository, oliver, dave, carter);
    }

    /**
     * Collections are converted to Lists when saved to AerospikeDB.
     * <p>
     * Argument of type Collection meant to be compared with a List in DB also gets converted to a List.
     * <p>
     * In this test we are providing a SortedSet and a PriorityQueue which preserve the order of elements,
     * such Collections can be consistently compared to a List saved in DB.
     */
    @Test
    void findByCollectionBetween_SortedSet() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            Set<Integer> davesIntSet = Set.of(1);
            dave.setIntSet(davesIntSet);
            repository.save(dave);
            assertThat(dave.getIntSet()).isEqualTo(davesIntSet);

            Set<Integer> setToCompareWith = new TreeSet<>(Set.of(3, 1, 2, 4, 0)); // gets sorted using natural order
            List<Person> persons = repository.findByIntSetBetween(Set.of(0), setToCompareWith);
            assertThat(persons).doesNotContain(dave);

            Set<Integer> setToCompareWith2 = new TreeSet<>(Comparator.reverseOrder());
            setToCompareWith2.addAll(Set.of(3, 1, 2, 4, 0)); // gets sorted using Comparator: 4, 3, 2, 1, 0
            List<Person> persons2 = repository.findByIntSetBetween(Set.of(0), setToCompareWith2);
            assertThat(persons2).contains(dave);

            List<Integer> listToCompareWith = List.of(0, 4, 3, 1, 2); // the insertion order is preserved
            List<Person> persons3 = repository.findByIntSetBetween(Set.of(0), listToCompareWith);
            assertThat(persons3).doesNotContain(dave);

            // gets sorted using natural order
            PriorityQueue<Integer> queueToCompareWith = new PriorityQueue<>(Set.of(3, 1, 2, 4, 0));
            List<Person> persons4 = repository.findByIntSetBetween(Set.of(0), queueToCompareWith);
            assertThat(persons4).doesNotContain(dave);

            PriorityQueue<Integer> queueToCompareWith2 = new PriorityQueue<>(Comparator.reverseOrder());
            queueToCompareWith2.addAll(Set.of(3, 1, 2, 4, 0)); // gets sorted using Comparator: 4, 3, 2, 1, 0
            List<Person> persons5 = repository.findByIntSetBetween(Set.of(0), queueToCompareWith2);
            assertThat(persons5).contains(dave);
        }
    }

    @Test
    void findByCollectionBetween_IntegerList() {
        List<Integer> list1 = List.of(100, 200, 300);
        List<Integer> list2 = List.of(1000, 2000, 3000);

        List<Person> persons = repository.findByIntsBetween(list1, list2);
        assertThat(persons).containsExactlyInAnyOrder(oliver, alicia);
    }

    @Test
    void findByCollectionBetween_StringList() {
        List<String> list1 = List.of("str", "str1");
        List<String> list2 = List.of("str55", "str65");

        List<Person> persons = repository.findByStringsBetween(list1, list2);
        assertThat(persons).containsExactlyInAnyOrder(dave, donny);
    }

    @Test
    void findByCollectionBetween_NegativeTest() {
        assertThatThrownBy(() -> negativeTestsRepository.findByIntsBetween())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Person.ints BETWEEN: invalid number of arguments, expecting two");

        assertThatThrownBy(() -> negativeTestsRepository.findByIntsBetween(100))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Person.ints BETWEEN: invalid number of arguments, expecting two");

        assertThatThrownBy(() -> negativeTestsRepository.findByIntsBetween(Map.of(100, 200), Map.of(300, 400)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Person.ints BETWEEN: invalid argument type, expecting Collection");
    }

    @Test
    void findByMapBetween() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            assertThat(carter.getIntMap()).isEqualTo(Map.of("key1", 0, "key2", 1));

            Map<String, Integer> map1 = Map.of("key1", -1, "key2", 0);
            Map<String, Integer> map2 = Map.of("key1", 2, "key2", 3);

            List<Person> persons;
            persons = repository.findByIntMapBetween(map1, map2);
            assertThat(persons).contains(carter);
        }
    }

    @Test
    void findByMapOfListsBetween() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            Map<String, List<Integer>> mapOfLists1 = Map.of("0", List.of(100), "1", List.of(200));
            Map<String, List<Integer>> mapOfLists2 = Map.of("2", List.of(301), "3", List.of(401));
            Map<String, List<Integer>> mapOfLists3 = Map.of("1", List.of(102), "2", List.of(202));
            Map<String, List<Integer>> mapOfLists4 = Map.of("3", List.of(3000), "4", List.of(4000));
            stefan.setMapOfIntLists(mapOfLists1);
            repository.save(stefan);
            douglas.setMapOfIntLists(mapOfLists2);
            repository.save(douglas);
            matias.setMapOfIntLists(mapOfLists3);
            repository.save(matias);
            leroi2.setMapOfIntLists(mapOfLists4);
            repository.save(leroi2);

            List<Person> persons;
            var map1 = Map.of("0", List.of(100), "1", List.of(200));
            var map2 = Map.of("3", List.of(3000), "4", List.of(4001));
            persons = repository.findByMapOfIntListsBetween(map1, map2);
            assertThat(persons).contains(stefan, douglas, matias, leroi2);

            var map3 = Map.of("0", List.of(100), "1", List.of(200));
            var map4 = Map.of("3", List.of(3000), "4", List.of(4000));
            persons = repository.findByMapOfIntListsBetween(map3, map4);
            assertThat(persons).contains(stefan, douglas, matias);

            var map5 = Map.of("5", List.of(4001));
            var map6 = Map.of("910", List.of(10000));
            persons = repository.findByMapOfIntListsBetween(map5, map6);
            assertThat(persons).isEmpty();
        }
    }

    @Test
    void findByMapBetween_NegativeTest() {
        assertThatThrownBy(() -> negativeTestsRepository.findByIntMapBetween())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Person.intMap BETWEEN: invalid number of arguments, expecting two");

        assertThatThrownBy(() -> negativeTestsRepository.findByIntMapBetween(100))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Person.intMap BETWEEN: invalid number of arguments, expecting two");

        assertThatThrownBy(() -> negativeTestsRepository.findByIntMapBetween(100, Map.of(200, 300)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Person.intMap BETWEEN: invalid argument type, expecting Map");

        assertThatThrownBy(() -> negativeTestsRepository.findByIntMapBetween(100, 200))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Person.intMap BETWEEN: invalid argument type, expecting Map");

        assertThatThrownBy(() -> negativeTestsRepository.findByIntMapBetween(100, 200, 300, 400))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Person.intMap BETWEEN: invalid number of arguments, expecting two");
    }

    @Test
    void findByPOJOBetween() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            assertThat(dave.getAddress()).isEqualTo(new Address("Foo Street 1", 1, "C0123", "Bar"));
            Address address1 = new Address("Foo Street 1", 0, "C0123", "Bar");
            Address address2 = new Address("Foo Street 2", 2, "C0124", "Bar");
            List<Person> persons1 = repository.findByAddressBetween(address1, address2);
            assertThat(persons1).containsExactly(dave);

            address1 = new Address("Foo Street 0", 0, "C0122", "Bar");
            address2 = new Address("Foo Street 0", 0, "C0123", "Bar");
            List<Person> persons2 = repository.findByAddressBetween(address1, address2);
            assertThat(persons2).isEmpty();
        }
    }

    @Test
    void findByPOJOBetween_NegativeTest() {
        assertThatThrownBy(() -> negativeTestsRepository.findByAddressBetween())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Person.address BETWEEN: invalid number of arguments, expecting two POJOs");

        assertThatThrownBy(() -> negativeTestsRepository.findByAddressBetween(100))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Person.address BETWEEN: invalid number of arguments, expecting two POJOs");

        assertThatThrownBy(() -> negativeTestsRepository.findByAddressBetween(100, 200, 300))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Person.address BETWEEN: invalid number of arguments, expecting two POJOs");

        assertThatThrownBy(() -> negativeTestsRepository.findByAddressBetween(100, 200))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Person.address BETWEEN: Type mismatch, expecting Address");
    }

    @Test
    void findByNestedSimplePropertyBetween_String() {
        assertThat(carter.getAddress().getZipCode()).isEqualTo("C0124");
        assertThat(dave.getAddress().getZipCode()).isEqualTo("C0123");
        assertThat(repository.findByAddressZipCodeBetween("C0123", "C0124"))
                .containsExactly(dave);
    }

    @Test
    void findByNestedCollectionBetween() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            dave.setInts(List.of(1, 2, 3, 4));
            repository.save(dave);

            carter.setFriend(dave);
            repository.save(carter);

            List<Person> result = repository.findByFriendIntsBetween(List.of(1, 2, 3, 4), List.of(1, 2, 3, 4, 5));

            assertThat(result).contains(carter);
            TestUtils.setFriendsToNull(repository, carter);
        }
    }

    @Test
    void findByNestedMapBetween() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            dave.setIntMap(Map.of("1", 2, "3", 4));
            repository.save(dave);

            carter.setFriend(dave);
            repository.save(carter);

            List<Person> result = repository.findByFriendIntMapBetween(Map.of("1", 2, "3", 4), Map.of("1", 2, "3", 4, "5", 6));

            assertThat(result).contains(carter);
            TestUtils.setFriendsToNull(repository, carter);
        }
    }

    @Test
    void findByNestedPojoBetween() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            Address address1 = new Address("Foo Street 1", 1, "C0123", "Bar");
            Address address2 = new Address("Foo Street 1", 2, "C0124", "Bar");
            assertThat(dave.getAddress()).isNotNull();

            carter.setFriend(dave);
            repository.save(carter);

            List<Person> result = repository.findByFriendAddressBetween(address1, address2);

            assertThat(result).contains(carter);
            TestUtils.setFriendsToNull(repository, carter);
        }
    }
}
