package api.springData.repository.query.blocking.noindex.find;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import api.springData.sample.Address;
import api.springData.sample.Person;
import api.springData.utility.TestUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the "Is in" repository query. Keywords: In, IsIn.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns8"})
@TestPropertySource(properties = {"personSetName=personSetNameInTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexFindInTests extends PersonRepositoryQueryTests {

    @Test
    void findByIntIn() {
        Stream<Person> result;
        result = repository.findByAgeIn(List.of(1, 2, 10, 33, 51));
        assertThat(result).isEmpty();

        result = repository.findByAgeIn(List.of(1, 2, 10, 33, 51, 25, 44));
        assertThat(result).contains(leroi, leroi2, douglas);
    }

    @Test
    void findByEnumIn() {
        List<Person> result;
        result = repository.findByGenderIn(List.of(Person.Gender.FEMALE));
        assertThat(result).contains(alicia);

        result = repository.findByGenderIn(List.of(Person.Gender.MALE));
        assertThat(result).contains(david);
    }

    @Test
    void findBySimplePropertyIn_String() {
        Stream<Person> result;
        result = repository.findByFirstNameIn(List.of("Anastasiia", "Daniil"));
        assertThat(result).isEmpty();

        result = repository.findByFirstNameIn(List.of("Alicia", "Stefan"));
        assertThat(result).contains(alicia, stefan);
    }

    @Test
    void findByNestedSimplePropertyIn_String() {
        assertThat(carter.getAddress().getZipCode()).isEqualTo("C0124");
        assertThat(dave.getAddress().getZipCode()).isEqualTo("C0123");
        assertThat(repository.findByAddressZipCodeIn(List.of("C0123", "C0124", "C0125")))
                .containsExactlyInAnyOrder(dave, carter);
    }

    @Test
    void findByNestedCollectionIn() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            dave.setInts(List.of(1, 2, 3, 4));
            repository.save(dave);

            carter.setFriend(dave);
            repository.save(carter);

            List<Person> result = repository.findByFriendIntsIn(List.of(List.of(0, 1, 2, 3, 4, 5, 6, 7),
                    List.of(1, 2, 3), List.of(1, 2, 3, 4)));

            assertThat(result).contains(carter);
            TestUtils.setFriendsToNull(repository, carter);
        }
    }

    @Test
    void findByNestedMapIn() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            dave.setIntMap(Map.of("1", 2, "3", 4));
            repository.save(dave);

            carter.setFriend(dave);
            repository.save(carter);

            List<Person> result = repository.findByFriendIntMapIn(List.of(Map.of("0", 1, "2", 3, "4", 5, "6", 7),
                    Map.of("1", 2, "3", 4567), Map.of("1", 2, "3", 4)));

            assertThat(result).contains(carter);
            TestUtils.setFriendsToNull(repository, carter);
        }
    }

    @Test
    void findByNestedPojoIn() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            Address address1 = new Address("Foo Street 1", 1, "C0123", "Bar");
            Address address2 = new Address("Foo Street 2", 2, "C0124", "C0123");
            Address address3 = new Address("Foo Street 1", 23, "C0125", "Bar");
            Address address4 = new Address("Foo Street 1", 456, "C0126", "Bar");
            assertThat(carter.getAddress()).isEqualTo(address2);

            dave.setFriend(carter);
            repository.save(dave);

            List<Person> result = repository.findByFriendAddressIn(List.of(address1, address2, address3, address4));

            assertThat(result).contains(dave);
            TestUtils.setFriendsToNull(repository, dave);
        }
    }
}
