package api.springData.repository.query.blocking.noindex.find;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import api.springData.sample.Address;
import api.springData.sample.Person;
import api.springData.utility.TestUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the "Is not in" repository query. Keywords: NotIn, IsNotIn.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns1"})
@TestPropertySource(properties = {"personSetName=personSetNameNotInTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexFindNotInTests extends PersonRepositoryQueryTests {


    @Test
    void findByIntNotIn() {
        Stream<Person> result;
        result = repository.findByAgeNotIn(List.of(1, 2, 10, 33, 51));
        assertThat(result).containsExactlyInAnyOrderElementsOf(allPersons);

        result = repository.findByAgeNotIn(List.of(42, 39, 14, 30, 49, 45, 34, 44, 25));
        assertThat(result).contains(matias, david);
    }

    @Test
    void findByEnumNotIn() {
        List<Person> result;
        result = repository.findByGenderNotIn(List.of(Person.Gender.FEMALE));
        assertThat(result).contains(dave, donny, oliver, carter, boyd, stefan, leroi, leroi2, matias, douglas, david);

        result = repository.findByGenderNotIn(List.of(Person.Gender.MALE));
        assertThat(result).contains(dave, donny, oliver, carter, boyd, stefan, leroi, leroi2, matias, douglas, alicia);
    }

    @Test
    void findByStringNotIn() {
        Stream<Person> result;
        result = repository.findByFirstNameNotIn(List.of("Anastasiia", "Daniil"));
        assertThat(result).containsExactlyInAnyOrderElementsOf(allPersons);

        result = repository.findByFirstNameNotIn(List.of("Alicia", "Stefan", "Douglas", "Leroi", "Boyd", "Carter", "Oliver August", "Donny", "Dave", ""));
        assertThat(result).contains(matias, david);
    }

    @Test
    void findBySimplePropertyNotIn_String() {
        Collection<String> firstNames;
        firstNames = allPersons.stream().map(Person::getFirstName).collect(Collectors.toSet());
        assertThat(repository.findByFirstNameNotIn(firstNames)).isEmpty();

        firstNames = List.of("Dave", "Donny", "Carter", "Boyd", "Leroi", "Stefan", "Matias", "Douglas");
        assertThat(repository.findByFirstNameNotIn(firstNames)).containsExactlyInAnyOrder(oliver, alicia, david);
    }

    @Test
    void findByNestedSimplePropertyNotIn_String() {
        assertThat(carter.getAddress().getZipCode()).isEqualTo("C0124");
        assertThat(dave.getAddress().getZipCode()).isEqualTo("C0123");
        assertThat(repository.findByAddressZipCodeNotIn(List.of("C0123", "C0125"))).containsOnly(donny, oliver, alicia, boyd, stefan,
                leroi, leroi2, matias, douglas, david, carter);
    }

    @Test
    void findByNestedCollectionNotIn() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            dave.setInts(List.of(1, 2, 3, 4));
            repository.save(dave);

            carter.setFriend(dave);
            repository.save(carter);

            List<Person> result = repository.findByFriendIntsNotIn(List.of(List.of(0, 1, 2, 3, 4, 5, 6, 7),
                    List.of(1, 2, 3), List.of(0, 1, 2, 3, 4, 5)));

            assertThat(result).contains(carter);
            TestUtils.setFriendsToNull(repository, carter);
        }
    }

    @Test
    void findByNestedMapNotIn() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            dave.setIntMap(Map.of("1", 2, "3", 4));
            repository.save(dave);

            carter.setFriend(dave);
            repository.save(carter);

            List<Person> result = repository.findByFriendIntMapNotIn(List.of(Map.of("0", 1, "2", 3, "4", 5, "6", 7),
                    Map.of("1", 2, "3", 4567), Map.of("0", 1, "2", 3, "4", 5)));

            assertThat(result).contains(carter);
            TestUtils.setFriendsToNull(repository, carter);
        }
    }

    @Test
    void findByNestedPojoNotIn() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            Address address1 = new Address("Foo Street 1", 1, "C0123", "Bar");
            Address address2 = new Address("Foo Street 1", 2, "C0124", "C0123");
            Address address3 = new Address("Foo Street 1", 23, "C0125", "Bar");
            Address address4 = new Address("Foo Street 1", 456, "C0126", "Bar");
            assertThat(carter.getAddress())
                    .isNotEqualTo(address1)
                    .isNotEqualTo(address2)
                    .isNotEqualTo(address3)
                    .isNotEqualTo(address4);

            dave.setFriend(carter);
            repository.save(dave);

            List<Person> result = repository.findByFriendAddressNotIn(List.of(address1, address2, address3, address4));

            assertThat(result).contains(dave);
            TestUtils.setFriendsToNull(repository, dave);
        }
    }
}
