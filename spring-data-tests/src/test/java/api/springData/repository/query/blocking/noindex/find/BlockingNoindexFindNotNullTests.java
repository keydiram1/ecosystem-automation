package api.springData.repository.query.blocking.noindex.find;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import api.springData.sample.Address;
import api.springData.sample.Person;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the "Is not null" repository query. Keywords: NotNull, IsNotNull.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns20"})
@TestPropertySource(properties = {"personSetName=personSetNameNotNullTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexFindNotNullTests extends PersonRepositoryQueryTests {

    @Test
    void findByArrayIsNotNull() {
        List<Person> result = repository.findByIntArrayIsNotNull();
        assertThat(result).containsOnly(david, leroi, matias);
    }

    @Test
    void findByListIsNotNull() {
        List<Person> result = repository.findByNicknameIsNotNull();
        assertThat(result).containsOnly(david);
    }

    @Test
    void findByStringIsNotNull() {
        List<Person> result = repository.findByAddressesListIsNotNull();
        assertThat(result).containsOnly(david);
    }

    @Test
    void findByNestedSimpleValueIsNotNull() {
        Assertions.assertThat(stefan.getAddress())
                .isNull();
        Assertions.assertThat(carter.getAddress()
                .getZipCode()).isNotNull();
        Assertions.assertThat(dave.getAddress()
                .getZipCode()).isNotNull();

        stefan.setAddress(new Address(null, null, "zipCode", null));
        repository.save(stefan);
        Assertions.assertThat(repository.findByAddressZipCodeIsNotNull())
                .contains(stefan); // zipCode is not null

        stefan.setAddress(new Address(null, null, null, null));
        repository.save(stefan);
        Assertions.assertThat(repository.findByAddressZipCodeIsNotNull())
                .doesNotContain(stefan); // zipCode is null

        stefan.setAddress(null); // cleanup
        repository.save(stefan);
    }

    @Test
    void findByPOJOIsNotNull() {
        Assertions.assertThat(stefan.getAddress())
                .isNull();
        Assertions.assertThat(carter.getAddress())
                .isNotNull();
        Assertions.assertThat(dave.getAddress())
                .isNotNull();
        Assertions.assertThat(repository.findByAddressIsNotNull())
                .contains(carter, dave)
                .doesNotContain(stefan);

        stefan.setAddress(new Address(null, null, "zipCode", null));
        repository.save(stefan);
        Assertions.assertThat(repository.findByAddressIsNotNull())
                .contains(stefan); // Address is not null

        stefan.setAddress(new Address(null, null, null, null));
        repository.save(stefan);
        Assertions.assertThat(repository.findByAddressIsNotNull())
                .contains(stefan); // Address is not null

        stefan.setAddress(null); // cleanup
        repository.save(stefan);
    }
}
