package api.springData.repository.query.reactive.indexed.find;

import api.springData.config.AssertBinsAreIndexed;
import api.springData.repository.query.reactive.indexed.ReactiveIndexedPersonRepositoryQueryTests;
import api.springData.sample.IndexedPerson;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.model.Index;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

import static com.aerospike.client.query.IndexType.NUMERIC;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the "Is less than" repository query. Keywords: LessThan, IsLessThan.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns14"})
@TestPropertySource(properties = {"indexedPersonSetName=personLtReactiveTestsIndexed"})
@Tag("SPRING-DATA-TESTS-2")
public class ReactiveIndexedFindLessThanTests extends ReactiveIndexedPersonRepositoryQueryTests {

    @Override
    protected List<Index> newIndexes() {
        List<Index> newIndexes = new ArrayList<>();
        String setName = reactiveTemplate.getSetName(IndexedPerson.class);
        String postfix = "r_find_lt";
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
    public void findBySimplePropertyLessThan_Integer_Unpaged() {
        assertQueryHasSecIndexFilter("findByAgeLessThan", IndexedPerson.class, 40, Pageable.unpaged());
        Page<IndexedPerson> page = reactiveRepository.findByAgeLessThan(40, Pageable.unpaged())
            .subscribeOn(Schedulers.parallel()).block();
        assertThat(page.hasContent()).isTrue();
        assertThat(page.getNumberOfElements()).isGreaterThan(1);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.getTotalElements()).isEqualTo(page.getSize());
    }
}
