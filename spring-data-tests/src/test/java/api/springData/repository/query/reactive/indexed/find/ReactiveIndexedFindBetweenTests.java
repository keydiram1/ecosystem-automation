package api.springData.repository.query.reactive.indexed.find;

import api.springData.config.AssertBinsAreIndexed;
import api.springData.repository.query.reactive.indexed.ReactiveIndexedPersonRepositoryQueryTests;
import api.springData.sample.IndexedPerson;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.model.Index;
import org.springframework.test.context.TestPropertySource;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

import static com.aerospike.client.query.IndexType.NUMERIC;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the "Is between" repository query. Keywords: Between, IsBetween.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns17"})
@TestPropertySource(properties = {"indexedPersonSetName=personBetweenReactiveTestsIndexed"})
@Tag("SPRING-DATA-TESTS-2")
public class ReactiveIndexedFindBetweenTests extends ReactiveIndexedPersonRepositoryQueryTests {

    @Override
    protected List<Index> newIndexes() {
        List<Index> newIndexes = new ArrayList<>();
        newIndexes.add(Index.builder()
                .set(reactiveTemplate.getSetName(IndexedPerson.class))
                .name("indexed_person_age_" + "r_find_between")
                .bin("age")
                .indexType(NUMERIC)
                .build());
        return newIndexes;
    }

    @Test
    @AssertBinsAreIndexed(binNames = "age", entityClass = IndexedPerson.class)
    public void findBySimplePropertyBetween_Integer() {
        assertQueryHasSecIndexFilter("findByAgeBetween", IndexedPerson.class, 39, 45);
        List<IndexedPerson> results = reactiveRepository.findByAgeBetween(39, 45)
                .subscribeOn(Schedulers.parallel()).collectList().block();
        assertThat(results).hasSize(2).contains(alain, luc);
    }
}
