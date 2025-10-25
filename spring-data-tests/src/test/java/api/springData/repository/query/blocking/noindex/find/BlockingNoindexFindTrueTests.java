package api.springData.repository.query.blocking.noindex.find;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import api.springData.sample.Person;
import com.aerospike.client.Value;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the "Is true" repository query. Keywords: True, IsTrue.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns2"})
@TestPropertySource(properties = {"personSetName=personSetNameTrueTests"})
@Execution(ExecutionMode.SAME_THREAD)
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexFindTrueTests extends PersonRepositoryQueryTests {

    @Test
    void findByBooleanIntSimplePropertyIsTrue() {
        boolean initialValue = Value.UseBoolBin;
        Value.UseBoolBin = false; // save boolean as int
        Person intBoolBinPerson = Person.builder().id(nextId()).isActive(true).firstName("TestBoolBinFalse")
            .build();
        repository.save(intBoolBinPerson);

        List<Person> persons1 = repository.findByIsActiveTrue();
        assertThat(persons1).contains(intBoolBinPerson);

        List<Person> persons2 = repository.findByIsActiveIsTrue(); // another way to call the query method
        assertThat(persons2).containsExactlyElementsOf(persons1);

        Value.UseBoolBin = initialValue; // set back to the default value
        repository.delete(intBoolBinPerson);
    }

    @Test
    void findByBooleanSimplePropertyIsTrue() {
        boolean initialValue = Value.UseBoolBin;
        Value.UseBoolBin = true; // save boolean as bool, available in Server 5.6+
        Person intBoolBinPerson = Person.builder().id(nextId()).isActive(true).firstName("TestBoolBinTrue")
            .build();
        repository.save(intBoolBinPerson);

        List<Person> persons1 = repository.findByIsActiveTrue();
        assertThat(persons1).contains(intBoolBinPerson);

        List<Person> persons2 = repository.findByIsActiveIsTrue(); // another way to call the query method
        assertThat(persons2).containsExactlyElementsOf(persons1);

        Value.UseBoolBin = initialValue; // set back to the default value
        repository.delete(intBoolBinPerson);
    }
}
