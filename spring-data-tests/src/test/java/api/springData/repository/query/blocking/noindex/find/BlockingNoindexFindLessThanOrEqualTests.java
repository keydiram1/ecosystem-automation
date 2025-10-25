package api.springData.repository.query.blocking.noindex.find;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import api.springData.sample.Address;
import api.springData.sample.Person;
import api.springData.utility.TestUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the "Is less than or equal" repository query. Keywords: LessThanEqual, IsLessThanEqual.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns20"})
@TestPropertySource(properties = {"personSetName=personSetNameLessThanOrEqualTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexFindLessThanOrEqualTests extends PersonRepositoryQueryTests {

    @Test
    void findByStringLessThanEqual() {
        List<Person> result = repository.findByFirstNameLessThanEqual(" דוד!@#");
        assertThat(result).containsOnly(david);
        Assertions.assertThat(repository.findByFirstNameLessThan("Ap")).containsOnly(alicia, david);
    }

    @Test
    void findByIntLessThanEqual() {
        List<Person> result = repository.findByAgeLessThanEqual(14);
        assertThat(result).containsOnly(oliver);
        Assertions.assertThat(repository.findByAgeLessThan(21)).containsOnly(oliver, david);
    }

    @Test
    void findByIntArrayLessThanEqual() {
        int[] intArrayToCompareWith = {0, -1, -2, -3, 2_147_483_647, -2_147_483_648};
        List<Person> persons = repository.findByIntArrayLessThanEqual(intArrayToCompareWith);
        Assertions.assertThat(persons).containsOnly(david);

        intArrayToCompareWith[0] = 10;
        persons = repository.findByIntArrayLessThanEqual(intArrayToCompareWith);
        Assertions.assertThat(persons).containsOnly(leroi, matias, david);
    }

    @Test
    void findByNestedSimplePropertyLessThanOrEqual_Integer() {
        alicia.setFriend(boyd);
        repository.save(alicia);
        dave.setFriend(oliver);
        repository.save(dave);
        carter.setFriend(dave);
        repository.save(carter);
        leroi.setFriend(carter);
        repository.save(leroi);

        List<Person> result = repository.findByFriendAgeLessThanEqual(42);

        assertThat(result)
            .hasSize(2)
            .containsExactlyInAnyOrder(dave, carter);

        TestUtils.setFriendsToNull(repository, alicia, dave, carter, leroi);
    }

    @Test
    void findByNestedSimplePropertyGreaterThan_String() {
        assertThat(carter.getAddress().getZipCode()).isEqualTo("C0124");
        assertThat(dave.getAddress().getZipCode()).isEqualTo("C0123");
        assertThat(repository.findByAddressZipCodeLessThanEqual("C0125")).containsExactlyInAnyOrder(carter, dave, david);
    }

    /**
     * Collections are converted to Lists when saved to AerospikeDB.
     * <p>
     * Argument of type Collection meant to be compared with a List in DB also gets converted to a List.
     * <p>
     * In this test we are providing an unordered Collection (Set) which means that the order of elements in a resulting
     * List cannot be guaranteed.
     * <p>
     * Comparing with an unordered Collection works only for equality (EQ/NOTEQ) operations and not for
     * LT/LTEQ/GT/GTEQ/BETWEEN.
     */
    @Test
    void findByCollectionLessThanOrEqual_UnorderedSet_Equals_List() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            Set<Integer> setToCompareWith = Set.of(0, 1, 2, 3, 4);
            dave.setIntSet(setToCompareWith);
            repository.save(dave);
            assertThat(dave.getIntSet()).isEqualTo(setToCompareWith);

            List<Person> persons = repository.findByIntSetLessThanEqual(setToCompareWith);
            assertThat(persons).contains(dave);
        }
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
    void findByCollectionLessThanOrEqual_SortedSet() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            Set<Integer> davesIntSet = Set.of(1);
            dave.setIntSet(davesIntSet);
            repository.save(dave);
            assertThat(dave.getIntSet()).isEqualTo(davesIntSet);

            Set<Integer> setToCompareWith = new TreeSet<>(Set.of(3, 1, 2, 4, 0)); // gets sorted using natural order
            List<Person> persons = repository.findByIntSetLessThanEqual(setToCompareWith);
            assertThat(persons).doesNotContain(dave);

            Set<Integer> setToCompareWith2 = new TreeSet<>(Comparator.reverseOrder());
            setToCompareWith2.addAll(Set.of(3, 1, 2, 4, 0)); // gets sorted using Comparator: 4, 3, 2, 1, 0
            List<Person> persons2 = repository.findByIntSetLessThanEqual(setToCompareWith2);
            assertThat(persons2).contains(dave);

            List<Integer> listToCompareWith = List.of(0, 4, 3, 1, 2); // the insertion order is preserved
            List<Person> persons3 = repository.findByIntSetLessThanEqual(listToCompareWith);
            assertThat(persons3).doesNotContain(dave);

            // gets sorted using natural order
            PriorityQueue<Integer> queueToCompareWith = new PriorityQueue<>(Set.of(3, 1, 2, 4, 0));
            List<Person> persons4 = repository.findByIntSetLessThanEqual(queueToCompareWith);
            assertThat(persons4).doesNotContain(dave);

            PriorityQueue<Integer> queueToCompareWith2 = new PriorityQueue<>(Comparator.reverseOrder());
            queueToCompareWith2.addAll(Set.of(3, 1, 2, 4, 0)); // gets sorted using Comparator: 4, 3, 2, 1, 0
            List<Person> persons5 = repository.findByIntSetLessThanEqual(queueToCompareWith2);
            assertThat(persons5).contains(dave);
        }
    }

    @Test
    void findByNestedCollectionLessThanOrEqual() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            dave.setInts(List.of(1, 2, 3, 4));
            repository.save(dave);

            carter.setFriend(dave);
            repository.save(carter);

            List<Person> result = repository.findByFriendIntsLessThanEqual(List.of(1, 2, 3, 4, 5));

            assertThat(result).contains(carter);
            TestUtils.setFriendsToNull(repository, carter);
        }
    }

    @Test
    void findByNestedMapLessThanOrEqual() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            dave.setIntMap(Map.of("1", 2, "3", 4));
            repository.save(dave);

            carter.setFriend(dave);
            repository.save(carter);

            List<Person> result = repository.findByFriendIntMapLessThanEqual(Map.of("1", 2, "3", 4, "5", 6));

            assertThat(result).contains(carter);
            TestUtils.setFriendsToNull(repository, carter);
        }
    }

    @Test
    void findByNestedPojoLessThanOrEqual() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            Address address = new Address("Foo Street 1", 2, "C0124", "Bar");
            assertThat(dave.getAddress()).isNotNull();

            carter.setFriend(dave);
            repository.save(carter);

            List<Person> result = repository.findByFriendAddressLessThanEqual(address);

            assertThat(result).contains(carter);
            TestUtils.setFriendsToNull(repository, carter);
        }
    }
}
