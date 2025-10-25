package api.springData.repository.query.blocking.noindex.find;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import api.springData.sample.Person;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the CrudRepository queries API.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns20"})
@TestPropertySource(properties = {"personSetName=personSetNameCrudRepositoryQueryTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexFindCrudRepositoryQueryTests extends PersonRepositoryQueryTests {

    @Test
    void findPersonById() {
        Optional<Person> person = repository.findById(dave.getId());

        assertThat(person).hasValueSatisfying(actual -> {
            assertThat(actual).isInstanceOf(Person.class);
            assertThat(actual).isEqualTo(dave);
        });
    }

    @Test
    void findAll() {
        List<Person> result = (List<Person>) repository.findAll();
        assertThat(result).containsExactlyInAnyOrderElementsOf(allPersons);
    }

    @Test
    void findAll_Paginated() {
        Page<Person> result = repository.findAll(PageRequest.of(1, 2, Sort.Direction.ASC, "lastname", "firstname"));
        assertThat(result.isFirst()).isFalse();
        assertThat(result.isLast()).isFalse();
    }

    @Test
    void findAll_doesNotFindDeletedPersonByEntity() {
        try {
            repository.delete(dave);
            List<Person> result = (List<Person>) repository.findAll();
            assertThat(result)
                .doesNotContain(dave)
                .containsExactlyInAnyOrderElementsOf(
                    allPersons.stream().filter(person -> !person.equals(dave)).collect(Collectors.toList())
                );
        } finally {
            repository.save(dave);
        }
    }

    @Test
    void findAll_doesNotFindPersonDeletedById() {
        try {
            repository.deleteById(dave.getId());
            List<Person> result = (List<Person>) repository.findAll();
            assertThat(result)
                .doesNotContain(dave)
                .hasSize(allPersons.size() - 1);
        } finally {
            repository.save(dave);
        }
    }

    @Test
    void createDeletePersonById() {
        assertThat(repository.findById("tempID")).isEmpty();

        Person tempPerson = Person.builder()
                .id("tempID")
                .firstName("tempName")
                .build();
        List<Person> allTempPersons = List.of(tempPerson);
        additionalAerospikeTestOperations.saveAll(repository, allTempPersons);

        assertThat(repository.findById("tempID")).contains(tempPerson);

        repository.deleteById("tempID");

        assertThat(repository.findById("tempID")).isEmpty();
    }

    @Test
    void deleteAllPersonsFromList() {
        // batch delete requires server ver. >= 6.0.0
        repository.deleteAll(List.of(dave, carter));
        assertThat(repository.findAllById(List.of(dave.getId(), carter.getId()))).isEmpty();
        repository.save(dave); // cleanup
        repository.save(carter); // cleanup
    }
}
