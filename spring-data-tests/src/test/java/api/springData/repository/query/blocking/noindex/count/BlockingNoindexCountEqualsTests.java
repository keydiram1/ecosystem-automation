package api.springData.repository.query.blocking.noindex.count;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.QueryParam;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.aerospike.query.QueryParam.of;

/**
 * Tests for the "Equals" repository query. Keywords: Is, Equals (or no keyword).
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns21"})
@TestPropertySource(properties = {"personSetName=personCountSetNameEqualsTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexCountEqualsTests extends PersonRepositoryQueryTests {

    @Test
    void countBySimplePropertyEquals_String() {
        long result = repository.countByFirstName("Leroi");
        assertThat(result).isEqualTo(2);

        long result1 = repository.countByFirstNameIgnoreCase("lEroi");
        assertThat(result1).isEqualTo(2);

        long result2 = repository.countByFirstNameIs("lEroi"); // another way to call the query method
        assertThat(result2).isZero();
    }

    @Test
    void countPersonById_AND_simpleProperty() {
        QueryParam ids = of(dave.getId());
        QueryParam name = of(carter.getFirstName());
        long persons = repository.countByIdAndFirstName(ids, name);
        assertThat(persons).isZero();

        ids = of(dave.getId());
        name = of(dave.getFirstName());
        persons = repository.countByIdAndFirstName(ids, name);
        assertThat(persons).isEqualTo(1);

        ids = of(List.of(leroi.getId(), leroi2.getId(), carter.getId()));
        QueryParam firstName = of(leroi.getFirstName());
        QueryParam age = of(stefan.getAge());
        long persons4 = repository.countByIdAndFirstNameOrAge(ids, firstName, age);
        assertThat(persons4).isEqualTo(2);
    }
}
