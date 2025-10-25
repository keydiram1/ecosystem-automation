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

/**
 * Tests for the "Is null" repository query. Keywords: Null, IsNull.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns20"})
@TestPropertySource(properties = {"personSetName=personSetNameNullTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexFindNullTests extends PersonRepositoryQueryTests {

    @Test
    void findByArrayIsNull() {
        List<Person> result = repository.findByIntArrayIsNull();
        assertThat(result).containsOnly(dave, donny, oliver, alicia, carter, boyd, stefan, leroi2, douglas);
    }

    @Test
    void findByNestedSimplePropertyIsNull() {
        Assertions.assertThat(stefan.getAddress()).isNull();
        Assertions.assertThat(carter.getAddress().getZipCode()).isNotNull();
        Assertions.assertThat(dave.getAddress().getZipCode()).isNotNull();

        stefan.setAddress(new Address(null, null, null, null));
        repository.save(stefan);
        Assertions.assertThat(repository.findByAddressZipCodeIsNull())
            .contains(stefan)
            .doesNotContain(carter, dave);

        dave.setBestFriend(stefan);
        repository.save(dave);
        carter.setFriend(dave);
        repository.save(carter);
        Assertions.assertThat(repository.findByFriendBestFriendAddressZipCodeIsNull()).contains(carter);

        stefan.setAddress(null); // cleanup
        repository.save(stefan);
        TestUtils.setFriendsToNull(repository, carter, dave);
    }

    @Test
    void findByPOJOIsNull() {
        Assertions.assertThat(stefan.getAddress()).isNull();
        Assertions.assertThat(carter.getAddress()).isNotNull();
        Assertions.assertThat(dave.getAddress()).isNotNull();
        Assertions.assertThat(repository.findByAddressIsNull())
            .contains(stefan)
            .doesNotContain(carter, dave);

        stefan.setAddress(new Address(null, null, null, null));
        repository.save(stefan);
        Assertions.assertThat(repository.findByAddressIsNull()).doesNotContain(stefan);

        stefan.setAddress(null); // cleanup
        repository.save(stefan);
    }

    @Test
    void findByNestedCollectionIsNull() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            dave.setInts(List.of(1, 2, 3, 4));
            repository.save(dave);

            carter.setFriend(dave);
            repository.save(carter);
            assertThat(carter.getFriend().getInts()).isNotNull();
            assertThat(dave.getFriend()).isNull();

            List<Person> result = repository.findByFriendIntsIsNull();

            assertThat(result)
                    .contains(dave)
                    .doesNotContain(carter);
            TestUtils.setFriendsToNull(repository, carter);
        }
    }

    @Test
    void findByNestedMapIsNull() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            dave.setIntMap(Map.of("1", 2, "3", 4));
            repository.save(dave);

            carter.setFriend(dave);
            repository.save(carter);
            assertThat(carter.getFriend().getIntMap()).isNotNull();
            assertThat(dave.getFriend()).isNull();

            List<Person> result = repository.findByFriendIntMapIsNull();

            assertThat(result)
                    .contains(dave)
                    .doesNotContain(carter);
            TestUtils.setFriendsToNull(repository, carter);
        }
    }

    @Test
    void findByNestedPojoIsNull() {
        if (serverVersionSupport.isFindByCDTSupported()) {
            assertThat(dave.getAddress()).isNotNull();
            assertThat(donny.getAddress()).isNull();

            carter.setFriend(dave);
            repository.save(carter);
            stefan.setFriend(donny);
            repository.save(stefan);

            List<Person> result = repository.findByFriendAddressIsNull();

            assertThat(result)
                    .contains(stefan)
                    .doesNotContain(carter);
            TestUtils.setFriendsToNull(repository, carter, stefan);
        }
    }
}
