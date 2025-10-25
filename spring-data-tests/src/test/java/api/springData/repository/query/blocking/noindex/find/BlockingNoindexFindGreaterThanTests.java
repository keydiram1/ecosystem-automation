package api.springData.repository.query.blocking.noindex.find;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import api.springData.sample.Address;
import api.springData.sample.Person;
import api.springData.sample.PersonSomeFields;
import api.springData.utility.TestUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the "Is greater than" repository query. Keywords: GreaterThan, IsGreaterThan.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns20"})
@TestPropertySource(properties = {"personSetName=personSetNameGreaterThanTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexFindGreaterThanTests extends PersonRepositoryQueryTests {

    @Test
    void findByStringGreaterThan() {
        List<Person> result = repository.findByFirstNameGreaterThan("z");
        assertThat(result).isEmpty();
        result = repository.findByFirstNameGreaterThan("Stefan");
        assertThat(result).isEmpty();
        Assertions.assertThat(repository.findByFirstNameGreaterThan("S")).containsOnly(stefan);
    }

    @Test
    void findByIntGreaterThan() {
        List<Person> result = repository.findByAgeGreaterThan(50);
        assertThat(result).isEmpty();
        Assertions.assertThat(repository.findByAgeGreaterThan(44)).containsOnly(boyd, carter);
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
    void findByCollectionGreaterThan_SortedSet() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            Set<Integer> davesIntSet = Set.of(1);
            dave.setIntSet(davesIntSet);
            repository.save(dave);
            assertThat(dave.getIntSet()).isEqualTo(davesIntSet);

            Set<Integer> setToCompareWith = new TreeSet<>(Set.of(0, 1, 2, 3, 4)); // natural order
            List<Person> persons = repository.findByIntSetGreaterThan(setToCompareWith);
            assertThat(persons).contains(dave);

            Set<Integer> setToCompareWith2 = new TreeSet<>(Comparator.comparingInt(Integer::intValue));
            setToCompareWith2.addAll(Set.of(3, 1, 2, 4, 0)); // gets sorted using Comparator
            List<Person> persons2 = repository.findByIntSetGreaterThan(setToCompareWith2);
            assertThat(persons2).contains(dave);

            List<Integer> listToCompareWith = List.of(3, 1, 2, 0, 4); // the insertion order is preserved
            List<Person> persons3 = repository.findByIntSetGreaterThan(listToCompareWith);
            assertThat(persons3).doesNotContain(dave);

            // gets sorted using natural order
            PriorityQueue<Integer> queueToCompareWith = new PriorityQueue<>(Set.of(3, 1, 2, 4, 0));
            List<Person> persons4 = repository.findByIntSetGreaterThan(queueToCompareWith);
            assertThat(persons4).contains(dave);
        }
    }

    @Test
    void findByIntArrayGreaterThan() {
        int[] intArrayToCompareWith = {6, -1, -2, -3, 2_147_483_647, -2_147_483_648};
        List<Person> persons = repository.findByIntArrayGreaterThan(intArrayToCompareWith);
        Assertions.assertThat(persons).isEmpty();

        intArrayToCompareWith[0] = 0;
        persons = repository.findByIntArrayGreaterThan(intArrayToCompareWith);
        Assertions.assertThat(persons).containsOnly(leroi, matias);

        intArrayToCompareWith[0] = -1;
        persons = repository.findByIntArrayGreaterThan(intArrayToCompareWith);
        Assertions.assertThat(persons).containsOnly(leroi, matias, david);
    }


    @Test
    void findBySimpleProperty_Integer_Paginated() {
        Slice<Person> slice = repository.findByAgeGreaterThan(40, PageRequest.of(0, 10));
        assertThat(slice.hasContent()).isTrue();
        assertThat(slice.hasNext()).isFalse();
        assertThat(slice.getContent()).hasSize(4).contains(dave, carter, boyd, leroi);

        Slice<Person> slice2 = repository.findByAgeGreaterThan(40, PageRequest.of(0, 1));
        assertThat(slice2.hasContent()).isTrue();
        assertThat(slice2.hasNext()).isTrue();
        assertThat(slice2.getContent()).containsAnyOf(dave, carter, boyd, leroi).hasSize(1);

        Slice<Person> slice3 = repository.findByAgeGreaterThan(100, PageRequest.of(0, 10));
        assertThat(slice3.hasContent()).isFalse();
        assertThat(slice3.hasNext()).isFalse();
        assertThat(slice3.getContent()).isEmpty();
    }

    @Test
    void findBySimpleProperty_Integer_PaginatedHasPrevHasNext() {
        Slice<Person> first = repository.findByAgeGreaterThan(40, PageRequest.of(0, 1, Sort.by("age")));

        assertThat(first.hasContent()).isTrue();
        assertThat(first.getNumberOfElements()).isEqualTo(1);
        assertThat(first.hasNext()).isTrue();
        assertThat(first.isFirst()).isTrue();
        assertThat(first.isLast()).isFalse();

        Slice<Person> last = repository.findByAgeGreaterThan(20, PageRequest.of(4, 2, Sort.by("age")));
        assertThat(last.hasContent()).isTrue();
        assertThat(last.getNumberOfElements()).isEqualTo(2);
        assertThat(last.hasNext()).isFalse();
        assertThat(last.isLast()).isTrue();
    }

    @Test
    void findBySimpleProperty_Integer_SortedWithOffset() {
        List<Person> result = IntStream.range(0, 4)
                .mapToObj(index -> repository.findByAgeGreaterThan(40, PageRequest.of(
                        index, 1, Sort.by("age")
                )))
                .flatMap(slice -> slice.getContent().stream())
                .collect(Collectors.toList());

        assertThat(result)
                .hasSize(4)
                .containsSequence(dave, leroi, boyd, carter);
    }

    @Test
    void findBySimpleProperty_Integer_UnsortedWithOffset_NegativeTest() {
        assertThatThrownBy(() -> repository.findByAgeGreaterThan(1, PageRequest.of(1, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsorted query must not have offset value. For retrieving paged results use sorted query.");
    }

    @Test
    void findBySimpleProperty_Integer_Unpaged() {
        Slice<Person> slice = repository.findByAgeGreaterThan(40, Pageable.unpaged());
        assertThat(slice.hasContent()).isTrue();
        assertThat(slice.getNumberOfElements()).isGreaterThan(0);
        assertThat(slice.hasNext()).isFalse();
        assertThat(slice.isLast()).isTrue();
    }

    @Test
    void findByNestedSimplePropertyGreaterThan_Integer() {
        alicia.setFriend(boyd);
        repository.save(alicia);
        dave.setFriend(oliver);
        repository.save(dave);
        carter.setFriend(dave);
        repository.save(carter);
        leroi.setFriend(carter);
        repository.save(leroi);

        assertThat(alicia.getFriend().getAge()).isGreaterThan(42);
        assertThat(leroi.getFriend().getAge()).isGreaterThan(42);

        List<Person> result = repository.findByFriendAgeGreaterThan(42);

        assertThat(result)
                .hasSize(2)
                .containsExactlyInAnyOrder(alicia, leroi);

        TestUtils.setFriendsToNull(repository, alicia, dave, carter, leroi);
    }

    @Test
    void findBySimplePropertyGreaterThan_Integer_projection() {
        Slice<PersonSomeFields> slice = repository.findPersonSomeFieldsByAgeGreaterThan(40, PageRequest.of(0, 10));

        assertThat(slice.hasContent()).isTrue();
        assertThat(slice.hasNext()).isFalse();
        assertThat(slice.getContent()).hasSize(4).contains(dave.toPersonSomeFields(),
                carter.toPersonSomeFields(), boyd.toPersonSomeFields(), leroi.toPersonSomeFields());
    }

    @Test
    void findBySimplePropertyGreaterThan_String() {
        List<Person> result = repository.findByFirstNameGreaterThan("Leroa");
        assertThat(result).contains(leroi, leroi2);
    }

    @Test
    void findByCollectionGreaterThan() {
        List<Integer> listToCompare1 = List.of(100, 200, 300, 400);
        List<Integer> listToCompare2 = List.of(425, 550);
        List<Integer> listToCompare3 = List.of(426, 551, 991);
        List<Integer> listToCompare4 = List.of(1000, 2000, 3000, 4000);
        List<Integer> listToCompare5 = List.of(551, 601, 991);
        List<Integer> listToCompare6 = List.of(550, 600, 990);

        List<Person> persons;
        persons = repository.findByIntsGreaterThan(listToCompare1);
        assertThat(persons).containsOnly(oliver, alicia);

        persons = repository.findByIntsGreaterThan(listToCompare2);
        assertThat(persons).containsOnly(oliver, alicia);

        persons = repository.findByIntsGreaterThan(listToCompare3);
        assertThat(persons).containsOnly(alicia);

        persons = repository.findByIntsGreaterThan(listToCompare4);
        assertThat(persons).isEmpty();

        persons = repository.findByIntsGreaterThan(listToCompare5);
        assertThat(persons).isEmpty();

        persons = repository.findByIntsGreaterThan(listToCompare6);
        assertThat(persons).isEmpty();
    }

    /*
        Note:
        only the upper level ListOfLists will be compared even if the parameter has different number of levels
        So findByListOfListsGreaterThan(List.of(1)) and findByListOfListsGreaterThan(List.of(List.of(List.of(1))))
        will compare with the given parameter only the upper level ListOfLists itself
     */
    @Test
    void findByCollectionOfListsGreaterThan() {
        List<List<Integer>> listOfLists1 = List.of(List.of(100));
        List<List<Integer>> listOfLists2 = List.of(List.of(101));
        List<List<Integer>> listOfLists3 = List.of(List.of(102));
        List<List<Integer>> listOfLists4 = List.of(List.of(1000));
        stefan.setListOfIntLists(listOfLists1);
        repository.save(stefan);
        douglas.setListOfIntLists(listOfLists2);
        repository.save(douglas);
        matias.setListOfIntLists(listOfLists3);
        repository.save(matias);
        leroi2.setListOfIntLists(listOfLists4);
        repository.save(leroi2);

        List<Person> persons;
        persons = repository.findByListOfIntListsGreaterThan(List.of(List.of(99)));
        assertThat(persons).containsOnly(stefan, douglas, matias, leroi2);

        persons = repository.findByListOfIntListsGreaterThan(List.of(List.of(100)));
        assertThat(persons).containsOnly(douglas, matias, leroi2);

        persons = repository.findByListOfIntListsGreaterThan(List.of(List.of(102)));
        assertThat(persons).containsOnly(leroi2);

        persons = repository.findByListOfIntListsGreaterThan(List.of(List.of(401)));
        assertThat(persons).containsOnly(leroi2);

        persons = repository.findByListOfIntListsGreaterThan(List.of(List.of(4000)));
        assertThat(persons).isEmpty();
    }

    @Test
    void findByCollection_NegativeTest() {
        assertThatThrownBy(() -> negativeTestsRepository.findByIntsGreaterThan(100, 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Person.ints GT: invalid number of arguments, expecting one");

        assertThatThrownBy(() -> negativeTestsRepository.findByIntsGreaterThan(100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Person.ints GT: invalid argument type, expecting Collection");
    }

    @Test
    void findByMapGreaterThan() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            assertThat(boyd.getStringMap()).isNotEmpty();
            assertThat(donny.getStringMap()).isNotEmpty();

            Map<String, String> mapToCompare = Map.of("Key", "Val", "Key2", "Val2");
            List<Person> persons = repository.findByStringMapGreaterThan(mapToCompare);
            assertThat(persons).containsExactlyInAnyOrder(boyd, david);
        }
    }

    @Test
    void findByNestedSimplePropertyGreaterThan_String() {
        assertThat(carter.getAddress().getZipCode()).isEqualTo("C0124");
        assertThat(repository.findByAddressZipCodeGreaterThan("C0123")).containsExactly(carter);
    }

    @Test
    void findByNestedCollectionGreaterThan() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            dave.setInts(List.of(1, 2, 3, 4));
            repository.save(dave);

            carter.setFriend(dave);
            repository.save(carter);

            List<Person> result = repository.findByFriendIntsGreaterThan(List.of(1, 2, 3, 3));

            assertThat(result).contains(carter);
            TestUtils.setFriendsToNull(repository, carter);
        }
    }

    @Test
    void findByNestedMapGreaterThan() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            Address address = new Address("Foo Street 1", 1, "C0123", "Bar");
            assertThat(carter.getAddress()).isNotNull();

            dave.setFriend(carter);
            repository.save(dave);

            List<Person> result = repository.findByFriendAddressGreaterThan(address);

            assertThat(result).contains(dave);
            TestUtils.setFriendsToNull(repository, dave);
        }
    }

    @Test
    void findByNestedPojoGreaterThan() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            dave.setIntMap(Map.of("1", 2, "3", 4));
            repository.save(dave);

            carter.setFriend(dave);
            repository.save(carter);

            List<Person> result = repository.findByFriendIntMapGreaterThan(Map.of("1", 2, "3", 3));

            assertThat(result).contains(carter);
            TestUtils.setFriendsToNull(repository, carter);
        }
    }
}
