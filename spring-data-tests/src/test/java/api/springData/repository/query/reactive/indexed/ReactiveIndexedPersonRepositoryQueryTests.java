package api.springData.repository.query.reactive.indexed;

import api.springData.AsCollections;
import api.springData.BaseReactiveIntegrationTests;
import api.springData.ReactiveBlockingAerospikeTestOperations;
import api.springData.sample.Address;
import api.springData.sample.IndexedPerson;
import api.springData.sample.Person;
import api.springData.sample.ReactiveIndexedPersonRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.aerospike.query.model.Index;
import org.springframework.test.context.TestPropertySource;
import utils.AerospikeLogger;

import java.util.Arrays;
import java.util.List;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns13"})
@Tag("SPRING-DATA-TESTS-1")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ReactiveIndexedPersonRepositoryQueryTests extends BaseReactiveIntegrationTests {

    protected static final IndexedPerson alain = IndexedPerson.builder()
            .id("IDReactiveIndexedPersonRepositoryQueryTests1")
            .firstName("Alain")
            .lastName("Sebastian")
            .age(42)
            .gender(Person.Gender.MALE)
            .strings(Arrays.asList("str1", "str2"))
            .address(new Address("Foo Street 1", 1, "C0123", "Bar"))
            .build();
    protected static final IndexedPerson luc = IndexedPerson.builder()
            .id("IDReactiveIndexedPersonRepositoryQueryTests2")
            .firstName("Luc")
            .lastName("Besson")
            .age(39)
            .gender(Person.Gender.MALE)
            .stringMap(AsCollections.of("key1", "val1", "key2", "val2"))
            .address(new Address(null, null, null, null))
            .build();
    protected static final IndexedPerson lilly = IndexedPerson.builder()
            .id("IDReactiveIndexedPersonRepositoryQueryTests3")
            .firstName("Lilly")
            .lastName("Bertineau")
            .age(28)
            .gender(Person.Gender.FEMALE)
            .intMap(AsCollections.of("key1", 1, "key2", 2))
            .address(new Address("Foo Street 2", 2, "C0124", "C0123"))
            .build();
    protected static final IndexedPerson daniel = IndexedPerson.builder()
            .id("IDReactiveIndexedPersonRepositoryQueryTests4")
            .firstName("Daniel")
            .lastName("Morales")
            .age(29)
            .gender(Person.Gender.MALE)
            .ints(Arrays.asList(500, 550, 990))
            .build();
    protected static final IndexedPerson petra = IndexedPerson.builder()
            .id("IDReactiveIndexedPersonRepositoryQueryTests5")
            .firstName("Petra")
            .lastName("Coutant-Kerbalec")
            .age(34)
            .gender(Person.Gender.FEMALE)
            .stringMap(AsCollections.of("key1", "val1", "key2", "val2", "key3", "val3"))
            .build();
    protected static final IndexedPerson emilien = IndexedPerson.builder()
            .id("IDReactiveIndexedPersonRepositoryQueryTests6")
            .firstName("Emilien")
            .lastName("Coutant-Kerbalec")
            .age(30)
            .gender(Person.Gender.MALE)
            .intMap(AsCollections.of("key1", 0, "key2", 1))
            .ints(Arrays.asList(450, 550, 990))
            .build();
    protected static final List<IndexedPerson> allIndexedPersons = Arrays.asList(alain, luc, lilly, daniel, petra,
            emilien);
    @Autowired
    protected ReactiveIndexedPersonRepository reactiveRepository;
    @Autowired
    protected ReactiveBlockingAerospikeTestOperations reactiveBlockingAerospikeTestOperations;

    protected abstract List<Index> newIndexes();

    @BeforeAll
    public void beforeAll() {
        // reactiveBlockingAerospikeTestOperations.deleteAll(reactiveRepository, allIndexedPersons);
        reactiveBlockingAerospikeTestOperations.saveAll(reactiveRepository, allIndexedPersons);

        try {
            additionalAerospikeTestOperations.createIndexesAsyncBlocking(reactorClient, reactiveTemplate, newIndexes());
        } catch (Exception e) {
            AerospikeLogger.info("Creating indexes failed due to: " + e.getMessage());
        }
    }

    @AfterAll
    public void afterAll() {
        try {
            additionalAerospikeTestOperations.dropIndexes(newIndexes());
        } catch (Exception e) {
            AerospikeLogger.info("Dropping indexes failed due to: " + e.getMessage());
        }
    }
}
