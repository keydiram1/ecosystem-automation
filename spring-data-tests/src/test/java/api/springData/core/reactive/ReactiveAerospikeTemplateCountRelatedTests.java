package api.springData.core.reactive;

import api.springData.BaseReactiveIntegrationTests;
import api.springData.sample.Person;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.core.ReactiveAerospikeTemplate;
import org.springframework.data.aerospike.query.FilterOperation;
import org.springframework.data.aerospike.query.qualifier.Qualifier;
import org.springframework.data.aerospike.query.qualifier.QualifierBuilder;
import org.springframework.data.aerospike.repository.query.Query;
import org.springframework.test.context.TestPropertySource;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for count related methods in {@link ReactiveAerospikeTemplate}.
 *
 * @author Igor Ermolenko
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns13"})
@Tag("SPRING-DATA-TESTS-1")
@TestPropertySource(properties = {"personSetName=personSetNameReactiveAerospikeTemplateCountRelatedTests"})
class ReactiveAerospikeTemplateCountRelatedTests extends BaseReactiveIntegrationTests {

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        reactiveTemplate.deleteAll(Person.class).block();
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .until(() -> isCountExactlyNum(0L));
    }

    @Test
    void count_shouldFindAllItemsByGivenCriteria() {
        String id1 = nextId();
        reactiveTemplate.insert(new Person(id1, "vasili", 50)).block();
        String id2 = nextId();
        reactiveTemplate.insert(new Person(id2, "vasili", 51)).block();
        String id3 = nextId();
        reactiveTemplate.insert(new Person(id3, "vasili", 52)).block();
        String id4 = nextId();
        reactiveTemplate.insert(new Person(id4, "petya", 52)).block();

        QualifierBuilder qbVasya1 = Qualifier.builder()
                .setPath("firstName")
                .setValue("vasili")
                .setFilterOperation(FilterOperation.EQ);

        Query queryVasya1 = new Query(qbVasya1.build());

        Long vasyaCount = reactiveTemplate.count(queryVasya1, Person.class)
                .subscribeOn(Schedulers.parallel())
                .block();
        assertThat(vasyaCount).isEqualTo(3);

        QualifierBuilder qbVasya2 = Qualifier.builder()
                .setPath("age")
                .setValue(51)
                .setFilterOperation(FilterOperation.EQ);

        Query queryVasyaAnd = new Query(Qualifier.and(qbVasya1.build(), qbVasya2.build()));

        Long vasya51Count = reactiveTemplate.count(queryVasyaAnd, Person.class)
                .subscribeOn(Schedulers.parallel())
                .block();
        assertThat(vasya51Count).isEqualTo(1);

        QualifierBuilder qbPetya = Qualifier.builder()
                .setPath("firstName")
                .setValue("petya")
                .setFilterOperation(FilterOperation.EQ);
        Long petyaCount = reactiveTemplate.count(new Query(qbPetya.build()), Person.class)
                .subscribeOn(Schedulers.parallel())
                .block();
        assertThat(petyaCount).isEqualTo(1);

        assertThat(reactiveTemplate.count(null, Person.class).subscribeOn(Schedulers.parallel())
                .block()).isEqualTo(4);
    }

    @Test
    void count_shouldFindAllItemsByGivenCriteriaAndRespectsIgnoreCase() {
        String id1 = nextId();
        reactiveTemplate.insert(new Person(id1, "VaSili", 50)).block();
        String id2 = nextId();
        reactiveTemplate.insert(new Person(id2, "vasILI", 51)).block();
        String id3 = nextId();
        reactiveTemplate.insert(new Person(id3, "vasili", 52)).block();

        QualifierBuilder qbVasya1 = Qualifier.builder()
                .setPath("firstName")
                .setValue("vas")
                .setIgnoreCase(true)
                .setFilterOperation(FilterOperation.STARTS_WITH);

        Query query1 = new Query(qbVasya1.build());
        assertThat(reactiveTemplate.count(query1, Person.class).block()).isEqualTo(3);

        QualifierBuilder qbVasya2 = Qualifier.builder()
                .setPath("firstName")
                .setValue("VaS")
                .setIgnoreCase(false)
                .setFilterOperation(FilterOperation.STARTS_WITH);

        Query query2 = new Query(qbVasya2.build());
        assertThat(reactiveTemplate.count(query2, Person.class)
                .subscribeOn(Schedulers.parallel())
                .block()).isEqualTo(1);
    }

    @Test
    void count_shouldReturnZeroIfNoDocumentsByProvidedCriteriaIsFound() {
        QualifierBuilder qb1 = Qualifier.builder()
                .setPath("firstName")
                .setValue("nastyushka")
                .setFilterOperation(FilterOperation.EQ);

        Long count = reactiveTemplate.count(new Query(qb1.build()), Person.class)
                .subscribeOn(Schedulers.parallel())
                .block();

        assertThat(count).isZero();
    }

    @Test
    void count_shouldRejectNullEntityClass() {
        assertThatThrownBy(() -> reactiveTemplate.count(null, (Class<?>) null).block())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void count_shouldCountAllByPassingEntityClass() {
        String id1 = nextId();
        reactiveTemplate.insert(new Person(id1, "vasili", 50)).subscribeOn(Schedulers.parallel()).block();
        String id2 = nextId();
        reactiveTemplate.insert(new Person(id2, "vasili", 51)).subscribeOn(Schedulers.parallel()).block();
        String id3 = nextId();
        reactiveTemplate.insert(new Person(id3, "vasili", 52)).subscribeOn(Schedulers.parallel()).block();
        String id4 = nextId();
        reactiveTemplate.insert(new Person(id4, "petya", 52)).subscribeOn(Schedulers.parallel()).block();

        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .until(() -> isCountExactlyNum(4L));
    }

    @SuppressWarnings("SameParameterValue")
    private boolean isCountExactlyNum(Long num) {
        return Objects.equals(reactiveTemplate.count(Person.class).block(), num);
    }
}
