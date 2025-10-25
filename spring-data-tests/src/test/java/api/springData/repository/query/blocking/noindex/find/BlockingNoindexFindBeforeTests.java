package api.springData.repository.query.blocking.noindex.find;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import api.springData.sample.Person;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the "Is before" repository query. Keywords: Before, IsBefore.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns20"})
@TestPropertySource(properties = {"personSetName=personSetNameBeforeTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexFindBeforeTests extends PersonRepositoryQueryTests {

    @Test
    void findByLocalDateSimplePropertyBefore() {
        dave.setRegDate(LocalDate.of(1980, 3, 10));
        repository.save(dave);

        List<Person> persons = repository.findByRegDateBefore(LocalDate.of(1981, 3, 10));
        assertThat(persons).contains(dave);

        dave.setDateOfBirth(null);
        repository.save(dave);
    }

    @Test
    void beforeSpecificDate() {
        Date dateOfBirth = new Date();
        donny.setDateOfBirth(new Date(dateOfBirth.getTime() - 86400000));
        repository.saveAll(List.of(dave, donny));

        List<Person> persons = repository.findByDateOfBirthBefore(new Date(dateOfBirth.getTime() - 86400000));
        assertThat(persons).isEmpty();

        persons = repository.findByDateOfBirthBefore(new Date(dateOfBirth.getTime() - 8639999));
        assertThat(persons).contains(donny);

        donny.setDateOfBirth(null);
        repository.save(donny);
    }

    @Test
    void dontFindNullValue() {
        donny.setDateOfBirth(null);
        repository.save(donny);

        List<Person> persons = repository.findByDateOfBirthBefore(new Date());
        assertThat(persons).doesNotContain(donny);
    }
}
