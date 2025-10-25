package api.springData.core.sync;

import api.springData.BaseBlockingIntegrationTests;
import api.springData.sample.Person;
import api.springData.sample.PersonMissingAndRedundantFields;
import api.springData.sample.PersonSomeFields;
import api.springData.sample.PersonTouchOnRead;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns15"})
@Tag("SPRING-DATA-TESTS-1")
@TestPropertySource(properties = {"personSetName=personSetNameAerospikeTemplateFindByIdProjectionTests"})
public class AerospikeTemplateFindByIdProjectionTests extends BaseBlockingIntegrationTests {

    public static final String SET_NAME = "setAerospikeTemplateFindByIdProjectionTests";

    @Test
    public void findByIdWithProjection() {
        Person firstPerson = Person.builder()
            .id(nextId())
            .firstName("first")
            .lastName("lastName1")
            .emailAddress("gmail.com")
            .age(40)
            .build();
        Person secondPerson = Person.builder()
            .id(nextId())
            .firstName("second")
            .lastName("lastName2")
            .emailAddress("gmail.com")
            .age(50)
            .build();
        template.save(firstPerson);
        template.save(secondPerson);

        PersonSomeFields result = template.findById(firstPerson.getId(), Person.class, PersonSomeFields.class);
        assertThat(result.getFirstName()).isEqualTo("first");
        assertThat(result.getLastName()).isEqualTo("lastName1");
        assertThat(result.getEmailAddress()).isEqualTo("gmail.com");
        template.delete(firstPerson); // cleanup
        template.delete(secondPerson); //cleanup
    }

    @Test
    public void findByIdWithProjectionPersonWithMissingFields() {
        Person firstPerson = Person.builder()
            .id(nextId())
            .firstName("first")
            .lastName("lastName1")
            .emailAddress("gmail.com")
            .build();
        Person secondPerson = Person.builder()
            .id(nextId())
            .firstName("second")
            .lastName("lastName2")
            .emailAddress("gmail.com")
            .build();
        template.save(firstPerson);
        template.save(secondPerson);

        PersonMissingAndRedundantFields result = template.findById(firstPerson.getId(), Person.class,
            PersonMissingAndRedundantFields.class);

        assertThat(result.getFirstName()).isEqualTo("first");
        assertThat(result.getLastName()).isEqualTo("lastName1");
        assertThat(result.getMissingField()).isNull();
        assertThat(result.getEmailAddress()).isNull(); // Not annotated with @Field("email").
        template.delete(firstPerson); // cleanup
        template.delete(secondPerson); //cleanup
    }

    @Test
    public void findByIdWithProjectionPersonWithMissingFieldsIncludingTouchOnRead() {
        PersonTouchOnRead firstPerson = PersonTouchOnRead.builder()
            .id(nextId())
            .firstName("first")
            .lastName("lastName1")
            .emailAddress("gmail.com")
            .build();
        PersonTouchOnRead secondPerson = PersonTouchOnRead.builder()
            .id(nextId())
            .firstName("second")
            .lastName("lastName2")
            .emailAddress("gmail.com")
            .build();
        template.save(firstPerson);
        template.save(secondPerson);

        PersonMissingAndRedundantFields result = template.findById(firstPerson.getId(), PersonTouchOnRead.class,
            PersonMissingAndRedundantFields.class);

        assertThat(result.getFirstName()).isEqualTo("first");
        assertThat(result.getLastName()).isEqualTo("lastName1");
        assertThat(result.getMissingField()).isNull();
        assertThat(result.getEmailAddress()).isNull(); // Not annotated with @Field("email").
        template.delete(firstPerson); // cleanup
        template.delete(secondPerson); //cleanup
    }

    @Test
    public void findByIdWithProjectionPersonWithMissingFieldsIncludingTouchOnReadAndSetName() {
        PersonTouchOnRead firstPerson = PersonTouchOnRead.builder()
            .id(nextId())
            .firstName("first")
            .lastName("lastName1")
            .emailAddress("gmail.com")
            .build();
        PersonTouchOnRead secondPerson = PersonTouchOnRead.builder()
            .id(nextId())
            .firstName("second")
            .lastName("lastName2")
            .emailAddress("gmail.com")
            .build();
        template.save(firstPerson, SET_NAME);
        template.save(secondPerson, SET_NAME);

        PersonMissingAndRedundantFields result = template.findById(firstPerson.getId(), PersonTouchOnRead.class,
            PersonMissingAndRedundantFields.class, SET_NAME);

        assertThat(result.getFirstName()).isEqualTo("first");
        assertThat(result.getLastName()).isEqualTo("lastName1");
        assertThat(result.getMissingField()).isNull();
        assertThat(result.getEmailAddress()).isNull(); // Not annotated with @Field("email").
        template.delete(firstPerson, SET_NAME); // cleanup
        template.delete(secondPerson, SET_NAME); //cleanup
    }

    @Test
    public void findByIdsWithTargetClass_shouldFindExisting() {
        Person firstPerson = Person.builder().id(nextId()).firstName("first").emailAddress("gmail.com").age(40).build();
        Person secondPerson = Person.builder().id(nextId()).firstName("second").emailAddress("gmail.com").age(50)
            .build();
        template.save(firstPerson);
        template.save(secondPerson);

        List<String> ids = Arrays.asList(nextId(), firstPerson.getId(), secondPerson.getId());
        List<PersonSomeFields> actual = template.findByIds(ids, Person.class, PersonSomeFields.class);

        assertThat(actual).containsExactly(firstPerson.toPersonSomeFields(), secondPerson.toPersonSomeFields());
        template.delete(firstPerson); // cleanup
        template.delete(secondPerson); //cleanup
    }

    @Test
    public void findByIdsWithTargetClassAndSetName_shouldFindExisting() {
        Person firstPerson = Person.builder().id(nextId()).firstName("first").emailAddress("gmail.com").age(40).build();
        Person secondPerson = Person.builder().id(nextId()).firstName("second").emailAddress("gmail.com").age(50)
            .build();
        template.save(firstPerson, SET_NAME);
        template.save(secondPerson, SET_NAME);

        List<String> ids = Arrays.asList(nextId(), firstPerson.getId(), secondPerson.getId());
        List<PersonSomeFields> actual = template.findByIds(ids, Person.class, PersonSomeFields.class, SET_NAME);

        assertThat(actual).containsExactly(firstPerson.toPersonSomeFields(), secondPerson.toPersonSomeFields());
        template.delete(firstPerson, SET_NAME); // cleanup
        template.delete(secondPerson, SET_NAME); //cleanup
    }

    @Test
    public void findByIdsWithTargetClass_shouldReturnEmptyList() {
        List<PersonSomeFields> actual = template.findByIds(Collections.emptyList(), Person.class,
            PersonSomeFields.class);
        assertThat(actual).isEmpty();
    }

    @Test
    public void findByIdsWithTargetClassAndSetName_shouldReturnEmptyList() {
        List<PersonSomeFields> actual = template.findByIds(Collections.emptyList(), Person.class,
            PersonSomeFields.class, SET_NAME);
        assertThat(actual).isEmpty();
    }
}
