package api.springData.repository.query.blocking.noindex.find;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import api.springData.sample.Address;
import api.springData.sample.Person;
import api.springData.utility.TestUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the "Exists" repository query. Keywords: Exists.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns20"})
@TestPropertySource(properties = {"personSetName=personSetNameExistsTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexFindExistsTests extends PersonRepositoryQueryTests {

    @Test
    void findByNestedSimplePropertyExists() {
        Assertions.assertThat(stefan.getAddress()).isNull();
        Assertions.assertThat(carter.getAddress().getZipCode()).isNotNull();
        Assertions.assertThat(dave.getAddress().getZipCode()).isNotNull();

        Assertions.assertThat(repository.findByAddressZipCodeExists())
                .contains(carter, dave)
                .doesNotContain(stefan);

        stefan.setAddress(new Address(null, null, null, null));
        repository.save(stefan);
        // when set to null a bin/field becomes non-existing
        Assertions.assertThat(repository.findByAddressZipCodeExists())
                .contains(carter, dave)
                .doesNotContain(stefan);

        stefan.setAddress(new Address(null, null, "zipCode", null));
        repository.save(stefan);
        Assertions.assertThat(repository.findByAddressZipCodeExists())
                .contains(carter, dave, stefan);

        stefan.setAddress(null); // cleanup
        stefan.setStringMap(null);
        repository.save(stefan);
    }

    @Test
    void findByPOJOExists() {
        Assertions.assertThat(stefan.getAddress()).isNull();
        Assertions.assertThat(carter.getAddress()).isNotNull();
        Assertions.assertThat(dave.getAddress()).isNotNull();

        Assertions.assertThat(repository.findByAddressExists())
                .contains(carter, dave)
                .doesNotContain(stefan);
    }

    @Test
    void findByPOJOExistsNegativeTest() {
        assertThatThrownBy(() -> negativeTestsRepository.findByAddressExists(new Address(null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Person.address IS_NOT_NULL: expecting no arguments");
    }

    @Test
    void findByNestedCollectionExists() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            dave.setInts(List.of(1, 2, 3, 4));
            repository.save(dave);

            carter.setFriend(dave);
            repository.save(carter);

            List<Person> result = repository.findByFriendIntsExists();

            assertThat(result).contains(carter);
            TestUtils.setFriendsToNull(repository, carter);
        }
    }

    @Test
    void findByNestedMapExists() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            dave.setIntMap(Map.of("1", 2, "3", 4));
            repository.save(dave);

            carter.setFriend(dave);
            repository.save(carter);

            List<Person> result = repository.findByFriendIntMapExists();

            assertThat(result).contains(carter);
            TestUtils.setFriendsToNull(repository, carter);
        }
    }

    @Test
    void findByNestedPojoExists() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            assertThat(dave.getAddress()).isNotNull();

            carter.setFriend(dave);
            repository.save(carter);

            List<Person> result = repository.findByFriendAddressExists();

            assertThat(result).contains(carter);
            TestUtils.setFriendsToNull(repository, carter);
        }
    }

    @Test
    void findByStringExists() {
        List<Person> result = repository.findByFirstNameExists();
        assertThat(result).containsExactlyInAnyOrderElementsOf(allPersons);
        Assertions.assertThat(repository.findByNicknameExists()).containsOnly(david);
    }

    @Test
    void findByIntExists() {
        List<Person> result = repository.findByAgeExists();
        assertThat(result).containsExactlyInAnyOrderElementsOf(allPersons);
        Assertions.assertThat(repository.findByShowSizeExists()).containsExactlyInAnyOrderElementsOf(allPersons);
    }

    @Test
    void findByMapExists() {
        Assertions.assertThat(repository.findByIntMapExists()).containsOnly(carter);
    }


    @Test
    void findByListExists() {
        Assertions.assertThat(repository.findByIntsExists()).containsOnly(alicia, oliver);
    }

    @Test
    void findByArrayExists() {
        Assertions.assertThat(repository.findByIntArrayExists()).containsOnly(leroi, matias, david);
    }

    @Test
    void findByBooleanExists() {
        List<Person> result = repository.findByIsActiveExists();
        assertThat(result).containsExactlyInAnyOrderElementsOf(allPersons);
    }

    @Test
    void findBySetExists() {
        Assertions.assertThat(repository.findByIntSetExists()).containsOnly(david);
    }
}
