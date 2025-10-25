package api.springData.core.reactive;

import api.springData.BaseReactiveIntegrationTests;
import api.springData.sample.Person;
import api.springData.sample.PersonSomeFields;
import api.springData.utility.QueryUtils;
import com.aerospike.client.query.IndexType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.data.aerospike.query.qualifier.Qualifier;
import org.springframework.data.aerospike.repository.query.Query;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns13"})
@Tag("SPRING-DATA-TESTS-1")
@TestPropertySource(properties = {"personSetName=personSetNameReactiveAerospikeTemplateFindByQueryProjectionTest"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReactiveAerospikeTemplateFindByQueryProjectionTest extends BaseReactiveIntegrationTests {

    public static final String SET_NAME = "setName1";

    @BeforeAll
    public void beforeAllSetUp() {
        reactiveTemplate.deleteAll(Person.class).block();
        reactiveTemplate.deleteAll(SET_NAME).block();
        additionalAerospikeTestOperations.createIndex(
            Person.class, "person_age_index_FindByQueryProjectionTest", "age", IndexType.NUMERIC);
        additionalAerospikeTestOperations.createIndex(
            Person.class, "person_last_name_index2", "lastName", IndexType.STRING);
        additionalAerospikeTestOperations.createIndex(
            Person.class, "person_first_name_index2", "firstName", IndexType.STRING);
        additionalAerospikeTestOperations.createIndex(
            SET_NAME, "person_age_index_FindByQueryProjectionTest" + SET_NAME, "age", IndexType.NUMERIC);
        additionalAerospikeTestOperations.createIndex(
            SET_NAME, "person_last_name_index2" + SET_NAME, "lastName", IndexType.STRING);
        additionalAerospikeTestOperations.createIndex(
            SET_NAME, "person_first_name_index2" + SET_NAME, "firstName", IndexType.STRING);
    }

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        reactiveTemplate.deleteAll(Person.class).block();
        reactiveTemplate.deleteAll(SET_NAME).block();
    }

    @AfterAll
    public void afterAll() {
        additionalAerospikeTestOperations.dropIndex(Person.class, "person_age_index_FindByQueryProjectionTest");
        additionalAerospikeTestOperations.dropIndex(Person.class, "person_last_name_index2");
        additionalAerospikeTestOperations.dropIndex(Person.class, "person_first_name_index2");
        additionalAerospikeTestOperations.dropIndex(SET_NAME, "person_age_index_FindByQueryProjectionTest" + SET_NAME);
        additionalAerospikeTestOperations.dropIndex(SET_NAME, "person_last_name_index2" + SET_NAME);
        additionalAerospikeTestOperations.dropIndex(SET_NAME, "person_first_name_index2" + SET_NAME);
        reactiveTemplate.deleteAll(Person.class).block();
        reactiveTemplate.deleteAll(SET_NAME).block();
    }

    @Test
    public void findAll_findsAllExistingDocumentsProjection() {
        List<Person> persons = IntStream.rangeClosed(1, 10)
            .mapToObj(age -> Person.builder().id(nextId()).firstName("Dave").lastName("Matthews").age(age).build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(persons).blockLast();

        List<PersonSomeFields> result = reactiveTemplate.findAll(Person.class, PersonSomeFields.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(result)
            .hasSameElementsAs(persons.stream().map(Person::toPersonSomeFields).collect(Collectors.toList()));
        deleteAll(persons); // cleanup
    }

    @Test
    public void findAllWithSetName_findsAllExistingDocumentsProjection() {
        List<Person> persons = IntStream.rangeClosed(1, 10)
            .mapToObj(age -> Person.builder().id(nextId()).firstName("Dave").lastName("Matthews").age(age).build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(persons, SET_NAME).blockLast();

        List<PersonSomeFields> result = reactiveTemplate.findAll(PersonSomeFields.class, SET_NAME)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(result)
            .hasSameElementsAs(persons.stream().map(Person::toPersonSomeFields).collect(Collectors.toList()));
        deleteAll(persons, SET_NAME); // cleanup
    }

    @Test
    public void findInRange_shouldFindLimitedNumberOfDocumentsProjection() {
        List<Person> allUsers = IntStream.range(20, 27)
            .mapToObj(id ->
                Person.builder().id(nextId()).firstName("Firstname").lastName("Lastname").build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(allUsers).blockLast();

        List<PersonSomeFields> actual = reactiveTemplate.findInRange(0, 5, Sort.unsorted(), Person.class,
                PersonSomeFields.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(actual)
            .hasSize(5)
            .containsAnyElementsOf(allUsers.stream().map(Person::toPersonSomeFields).collect(Collectors.toList()));
        deleteAll(allUsers); // cleanup
    }

    @Test
    public void find_throwsExceptionForUnsortedQueryWithSpecifiedOffsetValueProjection() {
        Query query = new Query((Qualifier) null);
        query.setOffset(1);

        assertThatThrownBy(() -> reactiveTemplate.find(query, Person.class, PersonSomeFields.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsorted query must not have offset value. For retrieving paged results use sorted query.");
    }

    @Test
    public void findByFilterEqualProjection() {
        List<Person> allUsers = IntStream.rangeClosed(1, 10)
            .mapToObj(id ->
                Person.builder().id(nextId()).firstName("Dave").lastName("Matthews").build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(allUsers).blockLast();

        Query query = QueryUtils.createQueryForMethodWithArgs("findByFirstName", "Dave");

        List<PersonSomeFields> actual = reactiveTemplate.find(query, Person.class, PersonSomeFields.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(actual)
            .hasSize(10)
            .containsExactlyInAnyOrderElementsOf(allUsers.stream().map(Person::toPersonSomeFields)
                .collect(Collectors.toList()));
        deleteAll(allUsers); // cleanup
    }

    @Test
    public void findByFilterRangeProjection() {
        List<Person> allUsers = IntStream.rangeClosed(21, 30)
            .mapToObj(age -> Person.builder().id(nextId()).firstName("Dave" + age).lastName("Matthews").age(age)
                .build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(allUsers).blockLast();

        Query query = QueryUtils.createQueryForMethodWithArgs("findCustomerByAgeBetween", 25, 31);

        List<PersonSomeFields> actual = reactiveTemplate.find(query, Person.class, PersonSomeFields.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();

        assertThat(actual)
            .hasSize(6)
            .containsExactlyInAnyOrderElementsOf(
                allUsers.stream().map(Person::toPersonSomeFields).collect(Collectors.toList()).subList(4, 10));
        deleteAll(allUsers); // cleanup
    }

    @Test
    public void findByFilterRangeProjectionWithSetName() {
        List<Person> allUsers = IntStream.rangeClosed(21, 30)
            .mapToObj(age -> Person.builder().id(nextId()).firstName("Dave" + age).lastName("Matthews").age(age)
                .build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(allUsers, SET_NAME).blockLast();

        Query query = QueryUtils.createQueryForMethodWithArgs("findCustomerByAgeBetween", 25, 31);

        List<PersonSomeFields> actual = reactiveTemplate.find(query, PersonSomeFields.class, SET_NAME)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();

        assertThat(actual)
            .hasSize(6)
            .containsExactlyInAnyOrderElementsOf(
                allUsers.stream().map(Person::toPersonSomeFields).collect(Collectors.toList()).subList(4, 10));
        deleteAll(allUsers, SET_NAME); // cleanup
    }

    @Test
    public void findByFilterRangeNonExistingProjection() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findCustomerByAgeBetween", 100, 150);

        List<PersonSomeFields> actual = reactiveTemplate.find(query, Person.class, PersonSomeFields.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(actual).isEmpty();
    }
}
