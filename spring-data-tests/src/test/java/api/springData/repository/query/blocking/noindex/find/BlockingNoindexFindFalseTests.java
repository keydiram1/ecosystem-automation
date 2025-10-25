package api.springData.repository.query.blocking.noindex.find;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import api.springData.sample.Person;
import com.aerospike.client.Value;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Tests for the "Is false" repository query. Keywords: False, IsFalse.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns20"})
@TestPropertySource(properties = {"personSetName=personSetNameFalseTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexFindFalseTests extends PersonRepositoryQueryTests {

    @Test
    void findByBooleanIntSimplePropertyIsFalse() {
        boolean initialValue = Value.UseBoolBin;
        Value.UseBoolBin = false; // save boolean as int
        Person intBoolBinPerson = Person.builder().id(nextId()).isActive(true).firstName("Test")
            .build();
        repository.save(intBoolBinPerson);

        Assertions.assertThat(repository.findByIsActiveFalse()).doesNotContain(intBoolBinPerson);

        Value.UseBoolBin = initialValue; // set back to the default value
        repository.delete(intBoolBinPerson);
    }

    @Test
    void findByBooleanSimplePropertyIsFalse() {
        boolean initialValue = Value.UseBoolBin;
        Value.UseBoolBin = true; // save boolean as bool, available in Server 5.6+
        Person intBoolBinPerson = Person.builder().id(nextId()).isActive(true).firstName("Test")
            .build();
        repository.save(intBoolBinPerson);

        Assertions.assertThat(repository.findByIsActiveFalse()).doesNotContain(intBoolBinPerson);

        Value.UseBoolBin = initialValue; // set back to the default value
        repository.delete(intBoolBinPerson);
    }
}
