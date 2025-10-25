package api.springData.repository.query.blocking.noindex.find;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import api.springData.sample.Address;
import api.springData.sample.Person;
import api.springData.utility.TestUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the "Is less than" repository query. Keywords: LessThan, IsLessThan.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns20"})
@TestPropertySource(properties = {"personSetName=personSetNameLessThanTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexFindLessThanTests extends PersonRepositoryQueryTests {

    @Test
    void findByStringLessThan() {
        List<Person> result = repository.findByFirstNameLessThan(" דוד!@#");
        assertThat(result).isEmpty();
        Assertions.assertThat(repository.findByFirstNameLessThan("Ap")).containsOnly(alicia, david);
    }

    @Test
    void findByIntLessThan() {
        List<Person> result = repository.findByAgeLessThan(14);
        assertThat(result).isEmpty();
        Assertions.assertThat(repository.findByAgeLessThan(21)).containsOnly(oliver, david);
    }

    @Test
    void findByIntArrayLessThan() {
        int[] intArrayToCompareWith = {6, -1, -2, -3, 2_147_483_647, -2_147_483_648};
        List<Person> persons = repository.findByIntArrayLessThan(intArrayToCompareWith);
        Assertions.assertThat(persons).containsOnly(leroi, matias, david);

        intArrayToCompareWith[0] = 1;
        persons = repository.findByIntArrayLessThan(intArrayToCompareWith);
        Assertions.assertThat(persons).containsOnly(david);

        intArrayToCompareWith[0] = 0;
        persons = repository.findByIntArrayLessThan(intArrayToCompareWith);
        Assertions.assertThat(persons).isEmpty();
    }

    @Test
    void findBySimpleProperty_Integer_Unpaged() {
        Page<Person> page = repository.findByAgeLessThan(40, Pageable.unpaged());
        assertThat(page.hasContent()).isTrue();
        assertThat(page.getNumberOfElements()).isGreaterThan(1);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.getTotalElements()).isEqualTo(page.getSize());
    }

    /**
     * Collections are converted to Lists when saved to AerospikeDB.
     * <p>
     * Argument of type Collection meant to be compared with a List in DB also gets converted to a List.
     * <p>
     * In this test we are providing a SortedSet and a PriorityQueue which preserve the order of elements, such
     * Collections can be consistently compared to a List saved in DB.
     */
    @Test
    void findByCollectionLessThan_SortedSet() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            Set<Integer> davesIntSet = Set.of(1);
            dave.setIntSet(davesIntSet);
            repository.save(dave);
            assertThat(dave.getIntSet()).isEqualTo(davesIntSet);

            Set<Integer> setToCompareWith = new TreeSet<>(Set.of(3, 1, 2, 4, 0)); // gets sorted using natural order
            List<Person> persons = repository.findByIntSetLessThan(setToCompareWith);
            assertThat(persons).doesNotContain(dave);

            Set<Integer> setToCompareWith2 = new TreeSet<>(Comparator.reverseOrder());
            setToCompareWith2.addAll(Set.of(3, 1, 2, 4, 0)); // gets sorted using Comparator: 4, 3, 2, 1, 0
            List<Person> persons2 = repository.findByIntSetLessThan(setToCompareWith2);
            assertThat(persons2).contains(dave);

            List<Integer> listToCompareWith = List.of(0, 4, 3, 1, 2); // the insertion order is preserved
            List<Person> persons3 = repository.findByIntSetLessThan(listToCompareWith);
            assertThat(persons3).doesNotContain(dave);

            // gets sorted using natural order
            PriorityQueue<Integer> queueToCompareWith = new PriorityQueue<>(Set.of(3, 1, 2, 4, 0));
            List<Person> persons4 = repository.findByIntSetLessThan(queueToCompareWith);
            assertThat(persons4).doesNotContain(dave);

            PriorityQueue<Integer> queueToCompareWith2 = new PriorityQueue<>(Comparator.reverseOrder());
            queueToCompareWith2.addAll(Set.of(3, 1, 2, 4, 0)); // gets sorted using Comparator: 4, 3, 2, 1, 0
            List<Person> persons5 = repository.findByIntSetLessThan(queueToCompareWith2);
            assertThat(persons5).contains(dave);
        }
    }

    @Test
    void findByCollectionLessThan() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            List<String> davesStrings = dave.getStrings();
            List<String> listToCompareWith = List.of("str1", "str2", "str3");
            List<String> listWithFewerElements = List.of("str1", "str2");

            dave.setStrings(listWithFewerElements);
            repository.save(dave);
            assertThat(donny.getStrings()).isEqualTo(listToCompareWith);
            assertThat(dave.getStrings()).isEqualTo(listWithFewerElements);

            List<Person> persons = repository.findByStringsLessThan(listToCompareWith);
            assertThat(persons).contains(dave);

            dave.setStrings(davesStrings);
            repository.save(dave);
        }
    }

    @Test
    void findPersonsByCollectionLessThan_NegativeTest() {
        assertThatThrownBy(() -> negativeTestsRepository.findByStringsLessThan())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Person.strings LT: invalid number of arguments, expecting one");

        assertThatThrownBy(() -> negativeTestsRepository.findByStringsLessThan(List.of("string1"), List.of(
                "String2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Person.strings LT: invalid number of arguments, expecting one");

        assertThatThrownBy(() -> negativeTestsRepository.findByStringsLessThan("string1", "string2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Person.strings LT: invalid number of arguments, expecting one");
    }

    @Test
    void findByMapLessThanNegativeTest() {
        assertThatThrownBy(() -> negativeTestsRepository.findByIntMapLessThan(100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Person.intMap LT: invalid argument type, expecting Map");

        assertThatThrownBy(() -> negativeTestsRepository.findByIntMapLessThan(100, 200, 300))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Person.intMap LT: invalid number of arguments, expecting one");

        assertThatThrownBy(() -> negativeTestsRepository.findByIntMapLessThan(new Person("id1", "name1"), 400))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Person.intMap LT: invalid number of arguments, expecting one");
    }

    @Test
    void findByPOJOLessThan() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            assertThat(dave.getAddress().getStreet()).isEqualTo("Foo Street 1");
            assertThat(dave.getAddress().getApartment()).isEqualTo(1);
            assertThat(boyd.getAddress().getStreet()).isEqualTo(null);
            assertThat(boyd.getAddress().getApartment()).isEqualTo(null);

            Address address = new Address("Foo Street 2", 2, "C0124", "C0123");
            assertThat(dave.getAddress()).isNotEqualTo(address);
            assertThat(boyd.getAddress()).isNotEqualTo(address);
            assertThat(carter.getAddress()).isEqualTo(address);

            List<Person> persons = repository.findByAddressLessThan(address);
            assertThat(persons).containsExactlyInAnyOrder(dave, boyd);
        }
    }

    @Test
    void findByNestedCollectionLessThan() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            dave.setInts(List.of(1, 2, 3, 4));
            repository.save(dave);

            carter.setFriend(dave);
            repository.save(carter);

            List<Person> result = repository.findByFriendIntsLessThan(List.of(1, 2, 3, 4, 5));

            assertThat(result).contains(carter);
            TestUtils.setFriendsToNull(repository, carter);
        }
    }

    @Test
    void findByNestedMapLessThan() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            dave.setIntMap(Map.of("1", 2, "3", 4));
            repository.save(dave);

            carter.setFriend(dave);
            repository.save(carter);

            List<Person> result = repository.findByFriendIntMapLessThan(Map.of("1", 2, "3", 4, "5", 6));

            assertThat(result).contains(carter);
            TestUtils.setFriendsToNull(repository, carter);
        }
    }

    @Test
    void findByNestedPojoLessThan() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            Address address = new Address("Foo Street 1", 2, "C0124", "Bar");
            assertThat(dave.getAddress()).isNotNull();

            carter.setFriend(dave);
            repository.save(carter);

            List<Person> result = repository.findByFriendAddressLessThan(address);

            assertThat(result).contains(carter);
            TestUtils.setFriendsToNull(repository, carter);
        }
    }
}
