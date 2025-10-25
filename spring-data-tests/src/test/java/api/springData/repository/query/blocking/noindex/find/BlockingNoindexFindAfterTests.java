package api.springData.repository.query.blocking.noindex.find;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import api.springData.sample.Person;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the "Is after" repository query. Keywords: After, IsAfter.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns20"})
@TestPropertySource(properties = {"personSetName=personSetNameAfterTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexFindAfterTests extends PersonRepositoryQueryTests {

    @Test
    void findByDateSimplePropertyAfter() {
        dave.setDateOfBirth(new Date());
        repository.save(dave);

        List<Person> persons = repository.findByDateOfBirthAfter(new Date(126230400));
        assertThat(persons).contains(dave);

        dave.setDateOfBirth(null);
        repository.save(dave);
    }

    @Test
    void afterSpecificDate() {
        Date dateOfBirth = new Date();
        donny.setDateOfBirth(new Date(dateOfBirth.getTime() - 86400000));
        repository.save(donny);

        List<Person> persons = repository.findByDateOfBirthAfter(new Date(dateOfBirth.getTime() - 86400000));
        assertThat(persons).isEmpty();

        persons = repository.findByDateOfBirthAfter(new Date(dateOfBirth.getTime() - 86400001));
        assertThat(persons).contains(donny);

        donny.setDateOfBirth(null);
        repository.save(donny);
    }

    @Test
    void dontFindNullValue() {
        donny.setDateOfBirth(null);
        repository.save(donny);

        List<Person> persons = repository.findByDateOfBirthAfter(new Date(0));
        assertThat(persons).doesNotContain(donny);
    }
}
