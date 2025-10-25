package api.springData.core.reactive;

import api.springData.BaseReactiveIntegrationTests;
import api.springData.sample.Person;
import api.springData.sample.PersonMissingAndRedundantFields;
import api.springData.sample.PersonSomeFields;
import api.springData.sample.PersonTouchOnRead;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns13"})
@Tag("SPRING-DATA-TESTS-1")
@TestPropertySource(properties = {"personSetName=personSetNameReactiveAerospikeTemplateFindByIdProjectionTests"})
public class ReactiveAerospikeTemplateFindByIdProjectionTests extends BaseReactiveIntegrationTests {

    public static final String SET_NAME = "setReactiveAerospikeTemplateFindByIdProjectionTests";

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
        reactiveTemplate.save(firstPerson).subscribeOn(Schedulers.parallel()).block();
        reactiveTemplate.save(secondPerson).subscribeOn(Schedulers.parallel()).block();

        PersonSomeFields result = reactiveTemplate.findById(firstPerson.getId(), Person.class, PersonSomeFields.class)
            .subscribeOn(Schedulers.parallel()).block();

        assert result != null;
        assertThat(result.getFirstName()).isEqualTo("first");
        assertThat(result.getLastName()).isEqualTo("lastName1");
        assertThat(result.getEmailAddress()).isEqualTo("gmail.com");
        reactiveTemplate.delete(firstPerson).block(); // cleanup
        reactiveTemplate.delete(secondPerson).block(); //cleanup
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
        reactiveTemplate.save(firstPerson).subscribeOn(Schedulers.parallel()).block();
        reactiveTemplate.save(secondPerson).subscribeOn(Schedulers.parallel()).block();

        PersonMissingAndRedundantFields result = reactiveTemplate.findById(firstPerson.getId(), Person.class,
            PersonMissingAndRedundantFields.class).subscribeOn(Schedulers.parallel()).block();

        assert result != null;
        assertThat(result.getFirstName()).isEqualTo("first");
        assertThat(result.getLastName()).isEqualTo("lastName1");
        assertThat(result.getMissingField()).isNull();
        assertThat(result.getEmailAddress()).isNull(); // Not annotated with @Field("email").
        reactiveTemplate.delete(firstPerson).block(); // cleanup
        reactiveTemplate.delete(secondPerson).block(); //cleanup
    }

    @Test
    public void findByIdWithProjectionPersonWithMissingFieldsWithSetName() {
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
        reactiveTemplate.save(firstPerson, SET_NAME).subscribeOn(Schedulers.parallel()).block();
        reactiveTemplate.save(secondPerson, SET_NAME).subscribeOn(Schedulers.parallel()).block();

        PersonMissingAndRedundantFields result = reactiveTemplate.findById(firstPerson.getId(), Person.class,
            PersonMissingAndRedundantFields.class, SET_NAME).subscribeOn(Schedulers.parallel()).block();

        assert result != null;
        assertThat(result.getFirstName()).isEqualTo("first");
        assertThat(result.getLastName()).isEqualTo("lastName1");
        assertThat(result.getMissingField()).isNull();
        assertThat(result.getEmailAddress()).isNull(); // Not annotated with @Field("email").
        reactiveTemplate.delete(firstPerson, SET_NAME).block(); // cleanup
        reactiveTemplate.delete(secondPerson, SET_NAME).block(); //cleanup
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
        reactiveTemplate.save(firstPerson).subscribeOn(Schedulers.parallel()).block();
        reactiveTemplate.save(secondPerson).subscribeOn(Schedulers.parallel()).block();

        PersonMissingAndRedundantFields result = reactiveTemplate.findById(firstPerson.getId(), PersonTouchOnRead.class,
            PersonMissingAndRedundantFields.class).subscribeOn(Schedulers.parallel()).block();

        assert result != null;
        assertThat(result.getFirstName()).isEqualTo("first");
        assertThat(result.getLastName()).isEqualTo("lastName1");
        assertThat(result.getMissingField()).isNull();
        assertThat(result.getEmailAddress()).isNull(); // Not annotated with @Field("email").
        reactiveTemplate.delete(firstPerson).block(); // cleanup
        reactiveTemplate.delete(secondPerson).block(); //cleanup
    }

    @Test
    public void findByIdsWithTargetClass_shouldFindExisting() {
        Person firstPerson = Person.builder().id(nextId()).firstName("first").emailAddress("gmail.com").age(40).build();
        Person secondPerson = Person.builder().id(nextId()).firstName("second").emailAddress("gmail.com").age(50)
            .build();
        reactiveTemplate.save(firstPerson).subscribeOn(Schedulers.parallel()).block();
        reactiveTemplate.save(secondPerson).subscribeOn(Schedulers.parallel()).block();

        List<String> ids = Arrays.asList(nextId(), firstPerson.getId(), secondPerson.getId());

        List<PersonSomeFields> actual = reactiveTemplate.findByIds(ids, Person.class, PersonSomeFields.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();

        assertThat(actual).containsExactlyInAnyOrder(
            firstPerson.toPersonSomeFields(),
            secondPerson.toPersonSomeFields());
        reactiveTemplate.delete(firstPerson).block(); // cleanup
        reactiveTemplate.delete(secondPerson).block(); //cleanup
    }

    @Test
    public void findByIdsWithTargetClass_shouldReturnEmptyList() {
        List<PersonSomeFields> actual = reactiveTemplate.findByIds(Collections.emptyList(), Person.class,
                PersonSomeFields.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();

        assertThat(actual).isEmpty();
    }
}
