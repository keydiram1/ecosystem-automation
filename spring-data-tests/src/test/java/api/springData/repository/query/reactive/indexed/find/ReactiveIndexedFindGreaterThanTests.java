package api.springData.repository.query.reactive.indexed.find;

import api.springData.config.AssertBinsAreIndexed;
import api.springData.repository.query.reactive.indexed.ReactiveIndexedPersonRepositoryQueryTests;
import api.springData.sample.IndexedPerson;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.model.Index;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

import static com.aerospike.client.query.IndexType.NUMERIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the "Is greater than" repository query. Keywords: GreaterThan, IsGreaterThan.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns15"})
@TestPropertySource(properties = {"indexedPersonSetName=personGtReactiveTestsIndexed"})
@Tag("SPRING-DATA-TESTS-2")
public class ReactiveIndexedFindGreaterThanTests extends ReactiveIndexedPersonRepositoryQueryTests {

    @Override
    protected List<Index> newIndexes() {
        List<Index> newIndexes = new ArrayList<>();
        String setName = reactiveTemplate.getSetName(IndexedPerson.class);
        String postfix = "r_find_gt";
        newIndexes.add(Index.builder()
                .set(setName)
                .name("indexed_person_age_" + postfix)
                .bin("age")
                .indexType(NUMERIC)
                .build());
        return newIndexes;
    }

    @Test
    @AssertBinsAreIndexed(binNames = "age", entityClass = IndexedPerson.class)
    public void findBySimplePropertyGreaterThan_Integer_Paginated() {
        assertQueryHasSecIndexFilter("findByAgeGreaterThan", IndexedPerson.class, 1, PageRequest.of(0, 1));
        Page<IndexedPerson> page = reactiveRepository.findByAgeGreaterThan(1, PageRequest.of(0, 1))
            .subscribeOn(Schedulers.parallel()).block();
        assertThat(page).containsAnyElementsOf(allIndexedPersons);

        Slice<IndexedPerson> slice = reactiveRepository.findByAgeGreaterThan(1, PageRequest.of(0, 2))
            .subscribeOn(Schedulers.parallel()).block();
        assertThat(slice).hasSize(2).containsAnyElementsOf(allIndexedPersons);

        Slice<IndexedPerson> sliceSorted = reactiveRepository.findByAgeGreaterThan(1, PageRequest.of(1, 2, Sort.by(
                "age")))
            .subscribeOn(Schedulers.parallel()).block();
        assertThat(sliceSorted).hasSize(2).containsAnyElementsOf(allIndexedPersons);

        assertThatThrownBy(() -> reactiveRepository.findByAgeGreaterThan(1, PageRequest.of(1, 2)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsorted query must not have offset value. For retrieving paged results use sorted query.");
    }

    @Test
    @AssertBinsAreIndexed(binNames = "age", entityClass = IndexedPerson.class)
    public void findBySimplePropertyGreaterThan_Integer_Unpaged() {
        assertQueryHasSecIndexFilter("findByAgeGreaterThan", IndexedPerson.class, 40, Pageable.unpaged());
        Slice<IndexedPerson> slice = reactiveRepository.findByAgeGreaterThan(40, Pageable.unpaged())
            .subscribeOn(Schedulers.parallel()).block();
        assertThat(slice.hasContent()).isTrue();
        assertThat(slice.getNumberOfElements()).isGreaterThan(0);
        assertThat(slice.hasNext()).isFalse();
        assertThat(slice.isLast()).isTrue();
    }
}
