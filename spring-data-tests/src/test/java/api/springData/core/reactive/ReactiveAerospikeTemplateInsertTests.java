package api.springData.core.reactive;

import api.springData.BaseReactiveIntegrationTests;
import api.springData.sample.Person;
import api.springData.sample.SampleClasses.CustomCollectionClass;
import api.springData.sample.SampleClasses.DocumentWithByteArray;
import api.springData.sample.SampleClasses.VersionedClass;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.policy.Policy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.TestPropertySource;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns10"})
@Tag("SPRING-DATA-TESTS-1")
@TestPropertySource(properties = {"personSetName=personSetNameReactiveAerospikeTemplateInsertTests"})
@TestInstance(Lifecycle.PER_CLASS)
public class ReactiveAerospikeTemplateInsertTests extends BaseReactiveIntegrationTests {

    public static final String SET_NAME = "setReactiveAerospikeTemplateInsertTests";

    @BeforeEach
    public void beforeEach() {
        reactiveTemplate.deleteAll(Person.class).block();
        reactiveTemplate.deleteAll(SET_NAME);
        reactiveTemplate.deleteAll(VersionedClass.class).block();
        reactiveTemplate.deleteAll(DocumentWithByteArray.class).block();
        reactiveTemplate.deleteAll(CustomCollectionClass.class);
    }

    @AfterAll
    public void afterAll() {
        reactiveTemplate.deleteAll(Person.class).block();
        reactiveTemplate.deleteAll(SET_NAME);
        reactiveTemplate.deleteAll(VersionedClass.class).block();
        reactiveTemplate.deleteAll(DocumentWithByteArray.class).block();
        reactiveTemplate.deleteAll(CustomCollectionClass.class);
    }

    @Test
    public void insertsAndFindsWithCustomCollectionSet() {
        CustomCollectionClass initial = new CustomCollectionClass(id, "data0");
        reactiveTemplate.insert(initial).block();

        StepVerifier.create(reactorClient.get(new Policy(), new Key(getNameSpace(), "custom-set", id)))
                .assertNext(keyRecord -> assertThat(keyRecord.record.getString("data")).isEqualTo("data0"))
                .verifyComplete();
        CustomCollectionClass result = findById(id, CustomCollectionClass.class);
        assertThat(findById(id, CustomCollectionClass.class)).isEqualTo(initial);
    }

    @Test
    public void insertsDocumentWithListMapDateStringLongValues() {
        Person customer = Person.builder()
                .id(id)
                .firstName("Dave")
                .lastName("Grohl")
                .age(45)
                .waist(90)
                .emailAddress("dave@gmail.com")
                .stringMap(Collections.singletonMap("k", "v"))
                .strings(Arrays.asList("a", "b", "c"))
                .friend(new Person(null, "Anna", 43))
                .isActive(true)
                .gender(Person.Gender.MALE)
                .dateOfBirth(new Date())
                .build();

        StepVerifier.create(reactiveTemplate.insert(customer))
                .expectNext(customer)
                .verifyComplete();

        Person actual = findById(id, Person.class);
        assertThat(actual).isEqualTo(customer);
    }

    @Test
    public void insertsDocumentWithListMapDateStringLongValuesAndSetName() {
        Person customer = Person.builder()
                .id(id)
                .firstName("Dave")
                .lastName("Grohl")
                .age(45)
                .waist(90)
                .emailAddress("dave@gmail.com")
                .stringMap(Collections.singletonMap("k", "v"))
                .strings(Arrays.asList("a", "b", "c"))
                .friend(new Person(null, "Anna", 43))
                .isActive(true)
                .gender(Person.Gender.MALE)
                .dateOfBirth(new Date())
                .build();

        StepVerifier.create(reactiveTemplate.insert(customer, SET_NAME))
                .expectNext(customer)
                .verifyComplete();

        Person actual = findById(id, Person.class, SET_NAME);
        assertThat(actual).isEqualTo(customer);
    }

    @Test
    public void insertsAndFindsDocumentWithByteArrayField() {
        DocumentWithByteArray document = new DocumentWithByteArray(id, new byte[]{1, 0, 0, 1, 1, 1, 0, 0});

        reactiveTemplate.insert(document).subscribeOn(Schedulers.parallel()).block();

        DocumentWithByteArray result = findById(id, DocumentWithByteArray.class);
        assertThat(result).isEqualTo(document);
    }

    @Test
    public void insertsDocumentWithNullFields() {
        VersionedClass document = new VersionedClass(id, null);
        reactiveTemplate.insert(document).subscribeOn(Schedulers.parallel()).block();

        assertThat(document.getField()).isNull();
    }

    @Test
    public void insertsDocumentWithZeroVersionIfThereIsNoDocumentWithSameKey() {
        VersionedClass document = new VersionedClass(id, "any");
        reactiveTemplate.insert(document).subscribeOn(Schedulers.parallel()).block();

        assertThat(document.getVersion()).isEqualTo(1);
    }

    @Test
    public void insertsDocumentWithVersionGreaterThanZeroIfThereIsNoDocumentWithSameKey() {
        VersionedClass document = new VersionedClass(id, "any", 5L);
        reactiveTemplate.insert(document).subscribeOn(Schedulers.parallel()).block();

        assertThat(document.getVersion()).isEqualTo(1);
    }

    @Test
    public void insertsDocumentWithVersionGreaterThanZeroIfThereIsNoDocumentWithSameKeyAndSetName() {
        VersionedClass document = new VersionedClass(id, "any", 5L);
        reactiveTemplate.insert(document, SET_NAME).subscribeOn(Schedulers.parallel()).block();

        assertThat(document.getVersion()).isEqualTo(1);
    }


    @Test
    public void throwsExceptionForDuplicateId() {
        Person person = new Person(id, "Amol", 28);

        reactiveTemplate.insert(person).subscribeOn(Schedulers.parallel()).block();
        StepVerifier.create(reactiveTemplate.insert(person).subscribeOn(Schedulers.parallel()))
                .expectError(DuplicateKeyException.class)
                .verify();
    }

    @Test
    public void throwsExceptionForDuplicateIdAndSetName() {
        Person person = new Person(id, "Amol", 28);

        reactiveTemplate.insert(person, SET_NAME).subscribeOn(Schedulers.parallel()).block();
        StepVerifier.create(reactiveTemplate.insert(person, SET_NAME).subscribeOn(Schedulers.parallel()))
                .expectError(DuplicateKeyException.class)
                .verify();
    }

    @Test
    public void throwsExceptionForDuplicateIdForVersionedDocument() {
        VersionedClass document = new VersionedClass(id, "any", 5L);

        reactiveTemplate.insert(document).subscribeOn(Schedulers.parallel()).block();
        StepVerifier.create(reactiveTemplate.insert(document).subscribeOn(Schedulers.parallel()))
                .expectError(DuplicateKeyException.class)
                .verify();
    }

    @Test
    public void insertAll_shouldInsertAllDocuments() {
        Person customer1 = new Person(nextId(), "Dave");
        Person customer2 = new Person(nextId(), "James");
        reactiveTemplate.insertAll(List.of(customer1, customer2)).blockLast();

        Person result1 = findById(customer1.getId(), Person.class);
        Person result2 = findById(customer2.getId(), Person.class);
        assertThat(result1).isEqualTo(customer1);
        assertThat(result2).isEqualTo(customer2);
        reactiveTemplate.delete(result1).block(); // cleanup
        reactiveTemplate.delete(result2).block(); // cleanup

        Iterable<Person> personsToInsert = IntStream.range(0, 101)
                .mapToObj(age -> Person.builder().id(nextId())
                        .firstName("Gregor")
                        .age(age).build())
                .collect(Collectors.toList());
        reactiveTemplate.insertAll(personsToInsert).blockLast();

        @SuppressWarnings("CastCanBeRemovedNarrowingVariableType")
        List<String> ids = ((List<Person>) personsToInsert).stream().map(Person::getId).toList();
        List<Person> result = reactiveTemplate.findByIds(ids, Person.class).collectList().block();
        assertThat(result).hasSameElementsAs(personsToInsert);
    }

    @Test
    public void insertAllWithSetName_shouldInsertAllDocuments() {
        Person customer1 = new Person(nextId(), "Dave");
        Person customer2 = new Person(nextId(), "James");
        reactiveTemplate.insertAll(List.of(customer1, customer2), SET_NAME).blockLast();

        Person result1 = findById(customer1.getId(), Person.class, SET_NAME);
        Person result2 = findById(customer2.getId(), Person.class, SET_NAME);
        assertThat(result1).isEqualTo(customer1);
        assertThat(result2).isEqualTo(customer2);
    }

    @Test
    public void insertAll_rejectsDuplicateId() {
        Person person = new Person(id, "Amol");
        person.setAge(28);

        StepVerifier.create(reactiveTemplate.insertAll(List.of(person, person)))
                .expectError(AerospikeException.BatchRecordArray.class)
                .verify();
    }

    @Test
    public void insertAllWithSetName_rejectsDuplicateId() {
        Person person = new Person(id, "Amol");
        person.setAge(28);

        StepVerifier.create(reactiveTemplate.insertAll(List.of(person, person), SET_NAME))
                .expectError(AerospikeException.BatchRecordArray.class)
                .verify();
    }
}
