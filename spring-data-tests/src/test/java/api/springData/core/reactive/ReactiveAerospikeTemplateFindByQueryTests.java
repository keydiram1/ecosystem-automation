package api.springData.core.reactive;

import api.springData.BaseReactiveIntegrationTests;
import api.springData.sample.Address;
import api.springData.sample.Person;
import api.springData.sample.SampleClasses;
import api.springData.utility.QueryUtils;
import com.aerospike.client.query.IndexType;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.data.aerospike.repository.query.CriteriaDefinition.AerospikeQueryCriterion.*;
import static org.springframework.data.domain.Sort.Order.asc;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns8"})
@Tag("SPRING-DATA-TESTS-1")
@TestPropertySource(properties = {"personSetName=personSetNameReactiveAerospikeTemplateFindByQueryTests"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReactiveAerospikeTemplateFindByQueryTests extends BaseReactiveIntegrationTests {

    public static final String SET_NAME = "setReactiveAerospikeTemplateFindByQueryTests";

    @BeforeAll
    public void beforeAllSetUp() {
        additionalAerospikeTestOperations.deleteAllAndVerify(Person.class);
        additionalAerospikeTestOperations.deleteAllAndVerify(Person.class, SET_NAME);
        additionalAerospikeTestOperations.createIndex(Person.class, "person_age_index",
            "age", IndexType.NUMERIC);
        additionalAerospikeTestOperations.createIndex(Person.class, "person_last_name_index",
            "lastName", IndexType.STRING);
        additionalAerospikeTestOperations.createIndex(Person.class, "person_first_name_index",
            "firstName", IndexType.STRING);
    }

    @Override
    @BeforeEach
    public void setUp() {
        additionalAerospikeTestOperations.deleteAllAndVerify(Person.class);
        additionalAerospikeTestOperations.deleteAllAndVerify(Person.class, SET_NAME);
        super.setUp();
    }

    //@AfterAll
    public void afterAll() {
        additionalAerospikeTestOperations.dropIndex(Person.class, "person_age_index");
        additionalAerospikeTestOperations.dropIndex(Person.class, "person_last_name_index");
        additionalAerospikeTestOperations.dropIndex(Person.class, "person_first_name_index");
        additionalAerospikeTestOperations.deleteAllAndVerify(Person.class);
        additionalAerospikeTestOperations.deleteAllAndVerify(Person.class, SET_NAME);
    }

    @Test
    public void findWithFilterEqual_String_fallbackToFilterExp() {
        reactiveTemplate.createIndex(Person.class, "person_first_name_index_numeric", "firstName",
                IndexType.NUMERIC).block(); // incompatible secondary index (should be STRING) causes "index not found" exception
        Query query = QueryUtils.createQueryForMethodWithArgs("findByFirstName", "Dave");
        reactiveTemplate.insert(new Person(nextId(), "Dave", "Matthews")).block();
        // after getting index exception there is a fallback to filter exp only
        List<Person> result = reactiveTemplate.find(query, Person.class).collectList().block();
        assertThat(Objects.requireNonNull(result).stream().map(Person::getFirstName).collect(Collectors.toList()))
                .containsExactly("Dave");
        reactiveTemplate.deleteIndex(Person.class, "person_first_name_index_numeric").block();
    }

    @Test
    public void findAll_findAllExistingDocuments() {
        List<Person> persons = IntStream.rangeClosed(1, 10)
            .mapToObj(age -> Person.builder().id(nextId()).firstName("Dave").lastName("Matthews").age(age).build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(persons).blockLast();

        List<Person> result = reactiveTemplate.findAll(Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(result).hasSameElementsAs(persons);

        deleteAll(persons); // cleanup
    }

    @Test
    public void findAllWithSetName_findAllExistingDocuments() {
        List<Person> persons = IntStream.rangeClosed(1, 10)
            .mapToObj(age -> Person.builder().id(nextId()).firstName("Dave").lastName("Matthews").age(age).build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(persons, SET_NAME).blockLast();

        List<Person> result = reactiveTemplate.findAll(Person.class, SET_NAME)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(result).hasSameElementsAs(persons);

        deleteAll(persons, SET_NAME); // cleanup
    }

    @Test
    public void findAll_findNothing() {
        List<Person> actual = reactiveTemplate.findAll(Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();

        assertThat(actual).isEmpty();
    }

    @Test
    public void findAll_findIdOnlyRecord() {
        var id = 100;
        var doc = new SampleClasses.DocumentWithPrimitiveIntId(id); // id-only document
        var clazz = SampleClasses.DocumentWithPrimitiveIntId.class;

        var existingDoc = reactiveTemplate.findById(id, clazz).block();
        assertThat(existingDoc).withFailMessage("The same record already exists").isNull();

        reactiveTemplate.insert(doc).block();
        var resultsFindById = reactiveTemplate.findById(id, clazz).block();
        assertThat(resultsFindById).withFailMessage("findById error").isEqualTo(doc);
        var resultsFindAll = reactiveTemplate.findAll(clazz).collectList().block();
        // findAll() must correctly find the record that contains id and no bins
        assertThat(resultsFindAll).size().withFailMessage("findAll error").isEqualTo(1);

        // cleanup
        reactiveTemplate.delete(doc);
    }

    @Test
    public void findInRange_shouldFindLimitedNumberOfDocuments() {
        List<Person> allUsers = IntStream.range(20, 27)
            .mapToObj(id -> new Person(nextId(), "Firstname", "Lastname")).collect(Collectors.toList());
        reactiveTemplate.insertAll(allUsers).blockLast();

        List<Person> actual = reactiveTemplate.findInRange(0, 5, Sort.unsorted(), Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(actual)
            .hasSize(5)
            .containsAnyElementsOf(allUsers);

        deleteAll(allUsers); // cleanup
    }

    @Test
    public void findInRange_shouldFindLimitedNumberOfDocumentsAndSkip() {
        List<Person> allUsers = IntStream.range(20, 27)
            .mapToObj(id -> new Person(nextId(), "Firstname", "Lastname")).collect(Collectors.toList());
        reactiveTemplate.insertAll(allUsers).blockLast();

        List<Person> actual = reactiveTemplate.findInRange(0, 5, Sort.unsorted(), Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();

        assertThat(actual)
            .hasSize(5)
            .containsAnyElementsOf(allUsers);

        deleteAll(allUsers); // cleanup
    }

    @Test
    public void findInRangeWithSetName_shouldFindLimitedNumberOfDocumentsAndSkip() {
        List<Person> allUsers = IntStream.range(20, 27)
            .mapToObj(id -> new Person(nextId(), "Firstname", "Lastname")).collect(Collectors.toList());
        reactiveTemplate.insertAll(allUsers, SET_NAME).blockLast();

        List<Person> actual = reactiveTemplate.findInRange(0, 5, Sort.unsorted(), Person.class, SET_NAME)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();

        assertThat(actual)
            .hasSize(5)
            .containsAnyElementsOf(allUsers);

        deleteAll(allUsers, SET_NAME); // cleanup
    }

    @Test
    public void findInRange_shouldFindLimitedNumberOfDocumentsWithOrderBy() {
        List<Person> persons = new ArrayList<>();
        persons.add(new Person(nextId(), "Dave", "Matthews"));
        persons.add(new Person(nextId(), "Josh", "Matthews"));
        persons.add(new Person(nextId(), "Chris", "Yes"));
        persons.add(new Person(nextId(), "Kate", "New"));
        persons.add(new Person(nextId(), "Nicole", "Joshua"));
        reactiveTemplate.insertAll(persons).blockLast();

        int skip = 0;
        int limit = 3;
        Sort sort = Sort.by(asc("firstName"));

        List<Person> result = reactiveTemplate.findInRange(skip, limit, sort, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();

        assertThat(Objects.requireNonNull(result).stream().map(Person::getFirstName).collect(Collectors.toList()))
            .hasSize(3)
            .containsExactly("Chris", "Dave", "Josh");

        deleteAll(persons); // cleanup
    }

    @Test
    public void findAll_OrderByFirstName() {
        List<Person> persons = new ArrayList<>();
        persons.add(new Person(nextId(), "Dave", "Matthews"));
        persons.add(new Person(nextId(), "Josh", "Matthews"));
        persons.add(new Person(nextId(), "Chris", "Yes"));
        persons.add(new Person(nextId(), "Kate", "New"));
        persons.add(new Person(nextId(), "Nicole", "Joshua"));
        reactiveTemplate.insertAll(persons).blockLast();

        Sort sort = Sort.by(asc("firstName"));
        List<Person> result = reactiveTemplate.findAll(sort, 0, 0, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();

        assertThat(Objects.requireNonNull(result).stream().map(Person::getFirstName).collect(Collectors.toList()))
            .hasSize(5)
            .containsExactly("Chris", "Dave", "Josh", "Kate", "Nicole");

        deleteAll(persons); // cleanup
    }

    @Test
    public void find_throwsExceptionForUnsortedQueryWithSpecifiedOffsetValue() {
        Query query = new Query((Qualifier) null);
        query.setOffset(1);

        assertThatThrownBy(() -> reactiveTemplate.find(query, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsorted query must not have offset value. For retrieving paged results use sorted query.");
    }

    @Test
    public void findByFilterEqual() {
        List<Person> allUsers = IntStream.rangeClosed(1, 10)
            .mapToObj(id -> new Person(nextId(), "Dave", "Matthews")).collect(Collectors.toList());
        reactiveTemplate.insertAll(allUsers).blockLast();

        Query query = QueryUtils.createQueryForMethodWithArgs("findByFirstName", "Dave");

        List<Person> actual = reactiveTemplate.find(query, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(actual)
            .hasSize(10)
            .containsExactlyInAnyOrderElementsOf(allUsers);

        deleteAll(allUsers); // cleanup
    }

    @Test
    public void findByFilterEqualOrderBy() {
        List<Person> allUsers = IntStream.rangeClosed(1, 10)
            .mapToObj(id -> new Person(nextId(), "Dave" + id, "Matthews")).collect(Collectors.toList());
        Collections.shuffle(allUsers); // Shuffle user list
        reactiveTemplate.insertAll(allUsers).blockLast();
        allUsers.sort(Comparator.comparing(Person::getFirstName)); // Order user list by firstname ascending

        Query query = QueryUtils.createQueryForMethodWithArgs("findByLastNameOrderByFirstNameAsc", "Matthews");

        List<Person> actual = reactiveTemplate.find(query, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(actual)
            .hasSize(10)
            .containsExactlyElementsOf(allUsers);

        deleteAll(allUsers); // cleanup
    }

    @Test
    public void findByFilterEqualOrderByDesc() {
        List<Person> allUsers = IntStream.rangeClosed(1, 10)
            .mapToObj(id -> new Person(nextId(), "Dave" + id, "Matthews")).collect(Collectors.toList());
        Collections.shuffle(allUsers); // Shuffle user list
        reactiveTemplate.insertAll(allUsers).blockLast();
        allUsers.sort((o1, o2) -> o2.getFirstName()
            .compareTo(o1.getFirstName())); // Order user list by firstname descending

        Query query = QueryUtils.createQueryForMethodWithArgs("findByLastNameOrderByFirstNameDesc", "Matthews");

        List<Person> actual = reactiveTemplate.find(query, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(actual)
            .hasSize(10)
            .containsExactlyElementsOf(allUsers);

        deleteAll(allUsers); // cleanup
    }

    @Test
    public void findByFilterEqualOrderByDescWithSetName() {
        List<Person> allUsers = IntStream.rangeClosed(1, 10)
            .mapToObj(id -> new Person(nextId(), "Dave" + id, "Matthews")).collect(Collectors.toList());
        Collections.shuffle(allUsers); // Shuffle user list
        reactiveTemplate.insertAll(allUsers, SET_NAME).blockLast();
        allUsers.sort((o1, o2) -> o2.getFirstName()
            .compareTo(o1.getFirstName())); // Order user list by firstname descending

        Query query = QueryUtils.createQueryForMethodWithArgs("findByLastNameOrderByFirstNameDesc", "Matthews");

        List<Person> actual = reactiveTemplate.find(query, Person.class, SET_NAME)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(actual)
            .hasSize(10)
            .containsExactlyElementsOf(allUsers);

        deleteAll(allUsers, SET_NAME); // cleanup
    }

    @Test
    public void findByFilterRange() {
        List<Person> allUsers = IntStream.rangeClosed(21, 30)
            .mapToObj(age -> Person.builder().id(nextId()).firstName("Dave" + age).lastName("Matthews").age(age)
                .build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(allUsers).blockLast();

        // upper limit is exclusive
        Query query = QueryUtils.createQueryForMethodWithArgs("findCustomerByAgeBetween", 25, 31);

        List<Person> actual = reactiveTemplate.find(query, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();

        assertThat(actual)
            .hasSize(6)
            .containsExactlyInAnyOrderElementsOf(allUsers.subList(4, 10));

        deleteAll(allUsers); // cleanup
    }

    @Test
    public void findByFilterRangeNonExisting() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findCustomerByAgeBetween", 100, 150);

        List<Person> actual = reactiveTemplate.find(query, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();

        assertThat(actual).isEmpty();
    }

    @Test
    public void findWithFilterEqualOrderByDescNonExisting() {
        Object[] args = {"NonExistingSurname"};
        Query query = QueryUtils.createQueryForMethodWithArgs("findByLastNameOrderByFirstNameDesc", args);

        List<Person> result = reactiveTemplate.find(query, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();

        assertThat(result).isEmpty();
    }

    @Test
    public void findByListContainingInteger() {
        List<Person> persons = IntStream.rangeClosed(1, 7)
            .mapToObj(id -> Person.builder().id(nextId()).firstName("Dave" + id).lastName("Matthews")
                .ints(Collections.singletonList(100 * id)).build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(persons).blockLast();

        Query query = QueryUtils.createQueryForMethodWithArgs("findByIntsContaining", 100);

        List<Person> result = reactiveTemplate.find(query, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(result)
            .hasSize(1)
            .containsExactlyInAnyOrderElementsOf(persons.subList(0, 1));

        deleteAll(persons); // cleanup
    }

    @Test
    public void findByListContainingString() {
        List<Person> persons = IntStream.rangeClosed(1, 7)
            .mapToObj(id -> Person.builder().id(nextId()).firstName("Dave" + id).lastName("Matthews")
                .strings(Collections.singletonList("str" + id)).build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(persons).blockLast();

        Query query = QueryUtils.createQueryForMethodWithArgs("findByStringsContaining", "str2");

        List<Person> result = reactiveTemplate.find(query, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(result)
            .hasSize(1)
            .containsExactlyInAnyOrderElementsOf(persons.subList(1, 2));

        deleteAll(persons); // cleanup
    }

    @Test
    public void findByMapKeysContaining() {
        List<Person> persons = IntStream.rangeClosed(1, 2)
            .mapToObj(id -> Person.builder().id(nextId()).firstName("Dave" + id).lastName("Matthews")
                .stringMap(Collections.singletonMap("key" + id, "val" + id)).build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(persons).blockLast();

        Query query = QueryUtils.createQueryForMethodWithArgs("findByStringMapContaining", KEY, "key1");

        List<Person> result = reactiveTemplate.find(query, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(result)
            .hasSize(1)
            .containsExactlyInAnyOrderElementsOf(persons.subList(0, 1));

        deleteAll(persons); // cleanup
    }

    @Test
    public void findByMapValuesContaining() {
        List<Person> persons = IntStream.rangeClosed(1, 2)
            .mapToObj(id -> Person.builder().id(nextId()).firstName("Dave" + id).lastName("Matthews")
                .stringMap(Collections.singletonMap("key" + id, "val" + id)).build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(persons).blockLast();

        Query query = QueryUtils.createQueryForMethodWithArgs("findByStringMapContaining", VALUE, "val1");

        List<Person> result = reactiveTemplate.find(query, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(result)
            .hasSize(1)
            .containsExactlyInAnyOrderElementsOf(persons.subList(0, 1));

        deleteAll(persons); // cleanup
    }

    @Test
    public void findByMapKeyValueContaining() {
        List<Person> persons = IntStream.rangeClosed(1, 5)
            .mapToObj(id -> Person.builder().id(nextId()).firstName("Dave" + id).lastName("Matthews")
                .stringMap(Collections.singletonMap("key" + id, "val" + id)).build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(persons).blockLast();

        Query query = QueryUtils.createQueryForMethodWithArgs("findByStringMapContaining", KEY_VALUE_PAIR, "key1",
            "val1");

        List<Person> result = reactiveTemplate.find(query, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(result)
            .hasSize(1)
            .containsExactlyInAnyOrderElementsOf(persons.subList(0, 1));

        deleteAll(persons); // cleanup
    }

    @Test
    public void findByMapKeyValue() {
        List<Person> persons = IntStream.rangeClosed(1, 5)
            .mapToObj(id -> Person.builder().id(nextId()).firstName("Dave" + id).lastName("Matthews")
                .stringMap(Collections.singletonMap("key" + id, "val" + id)).build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(persons).blockLast();

        Query query = QueryUtils.createQueryForMethodWithArgs("findByStringMapContaining", KEY_VALUE_PAIR, "key3",
            "val3");

        List<Person> result = reactiveTemplate.find(query, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(result)
            .hasSize(1)
            .containsExactlyInAnyOrderElementsOf(persons.subList(2, 3));

        deleteAll(persons); // cleanup
    }

    @Test
    public void findPersonsByFriendAge() {
        List<Person> persons = IntStream.rangeClosed(1, 5)
            .mapToObj(id -> Person.builder().id(nextId()).firstName("Dave" + id).lastName("Matthews")
                .friend(new Person("person" + id, "Leroi" + id, 10 * id)).build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(persons).blockLast();

        Query query = QueryUtils.createQueryForMethodWithArgs("findByFriendAge", 50);

        List<Person> result = reactiveTemplate.find(query, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(result)
            .hasSize(1)
            .containsExactlyInAnyOrderElementsOf(persons.subList(4, 5));

        deleteAll(persons); // cleanup
    }

    @Test
    public void findPersonsByFriendAgeNotEqual() {
        List<Person> persons = IntStream.rangeClosed(1, 5)
            .mapToObj(id -> Person.builder().id(nextId()).firstName("Dave" + id).lastName("Matthews")
                .friend(new Person("person" + id, "Leroi" + id, 10 * id)).build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(persons).blockLast();

        Query query = QueryUtils.createQueryForMethodWithArgs("findByFriendAgeIsNot", 50);

        List<Person> result = reactiveTemplate.find(query, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(result)
            .hasSize(4)
            .containsExactlyInAnyOrderElementsOf(persons.subList(0, 4));

        deleteAll(persons); // cleanup
    }

    @Test
    public void findPersonsByFriendAgeGreaterThan() {
        List<Person> persons = IntStream.rangeClosed(1, 5)
            .mapToObj(id -> Person.builder().id(nextId()).firstName("Dave" + id).lastName("Matthews")
                .friend(new Person("person" + id, "Leroi" + id, 10 * id)).build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(persons).blockLast();

        Query query = QueryUtils.createQueryForMethodWithArgs("findByFriendAgeGreaterThan", 42);

        List<Person> result = reactiveTemplate.find(query, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(result)
            .hasSize(1)
            .containsExactlyInAnyOrderElementsOf(persons.subList(4, 5));

        deleteAll(persons); // cleanup
    }

    @Test
    public void findPersonsByFriendAgeLessThanOrEqual() {
        List<Person> persons = IntStream.rangeClosed(1, 5)
            .mapToObj(id -> Person.builder().id(nextId()).firstName("Dave" + id).lastName("Matthews")
                .friend(new Person("person" + id, "Leroi" + id, 10 * id)).build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(persons).blockLast();

        Query query = QueryUtils.createQueryForMethodWithArgs("findByFriendAgeLessThanEqual", 42);

        List<Person> result = reactiveTemplate.find(query, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(result)
            .hasSize(4)
            .containsExactlyInAnyOrderElementsOf(persons.subList(0, 4));

        deleteAll(persons); // cleanup
    }

    @Test
    public void findPersonsByFriendAgeRange() {
        List<Person> persons = IntStream.rangeClosed(1, 5)
            .mapToObj(id -> Person.builder().id(nextId()).firstName("Dave" + id).lastName("Matthews")
                .friend(new Person("person" + id, "Leroi" + id, 10 * id)).build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(persons).blockLast();

        Query query = QueryUtils.createQueryForMethodWithArgs("findByFriendAgeBetween", 42, 51);

        List<Person> result = reactiveTemplate.find(query, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(result)
            .hasSize(1)
            .containsExactlyInAnyOrderElementsOf(persons.subList(4, 5));

        deleteAll(persons); // cleanup
    }

    @Test
    public void findPersonsByAddressZipCode() {
        List<Person> persons = IntStream.rangeClosed(1, 5)
            .mapToObj(id -> Person.builder().id(nextId()).firstName("Dave" + id).lastName("Matthews")
                .address(new Address("Foo Street " + id, id, "C0123" + id, "City" + id)).build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(persons).blockLast();

        Query query = QueryUtils.createQueryForMethodWithArgs("findByAddressZipCode", "C01233");

        List<Person> result = reactiveTemplate.find(query, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(result)
            .hasSize(1)
            .containsExactlyInAnyOrderElementsOf(persons.subList(2, 3));

        deleteAll(persons); // cleanup
    }

    @Test
    public void findByAddressZipCodeContaining() {
        List<Person> persons = IntStream.rangeClosed(1, 5)
            .mapToObj(id -> Person.builder().id(nextId()).firstName("Dave" + id).lastName("Matthews")
                .address(new Address("Foo Street " + id, id, "C012" + id, "City" + id)).build())
            .collect(Collectors.toList());
        reactiveTemplate.insertAll(persons).blockLast();

        Query query = QueryUtils.createQueryForMethodWithArgs("findByAddressZipCodeContaining", "123");

        List<Person> result = reactiveTemplate.find(query, Person.class)
            .subscribeOn(Schedulers.parallel())
            .collectList().block();
        assertThat(result)
            .hasSize(1)
            .containsExactlyInAnyOrderElementsOf(persons.subList(2, 3));

        deleteAll(persons); // cleanup
    }
}
