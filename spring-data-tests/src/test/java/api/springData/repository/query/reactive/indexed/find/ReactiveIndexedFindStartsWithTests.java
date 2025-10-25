package api.springData.repository.query.reactive.indexed.find;

import api.springData.config.AssertBinsAreIndexed;
import api.springData.config.NoSecondaryIndexRequired;
import api.springData.repository.query.reactive.indexed.ReactiveIndexedPersonRepositoryQueryTests;
import api.springData.sample.IndexedPerson;
import api.springData.utility.TestUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.model.Index;
import org.springframework.test.context.TestPropertySource;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

import static com.aerospike.client.query.IndexType.STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the "Starts with" repository query. Keywords: StartingWith, IsStartingWith, StartsWith.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns5"})
@TestPropertySource(properties = {"indexedPersonSetName=personStartsWithReactiveTestsIndexed"})
@Tag("SPRING-DATA-TESTS-2")
public class ReactiveIndexedFindStartsWithTests extends ReactiveIndexedPersonRepositoryQueryTests {

    @Override
    protected List<Index> newIndexes() {
        List<Index> newIndexes = new ArrayList<>();
        String setName = reactiveTemplate.getSetName(IndexedPerson.class);
        String postfix = "r_find_startsWith";
        newIndexes.add(Index.builder()
                .set(setName)
                .name("indexed_person_last_name_" + postfix)
                .bin("lastName")
                .indexType(STRING)
                .build());
        return newIndexes;
    }

  //  @Test
    @AssertBinsAreIndexed(binNames = "lastName", entityClass = IndexedPerson.class)
    void findBySimplePropertyStartingWith_String_Distinct_NoSecondaryIndexFilter() {
        // There is no secondary index filter for "starts with"
        assertThat(queryHasSecIndexFilter("findDistinctByLastNameStartingWith", IndexedPerson.class, "Coutant-Kerbalec")).isFalse();
        List<IndexedPerson> persons = reactiveRepository.findDistinctByLastNameStartingWith("Coutant-Kerbalec")
                .subscribeOn(Schedulers.parallel()).collectList().block();
        assertThat(persons).hasSize(1);

        // There is no secondary index filter for "starts with"
        assertThat(queryHasSecIndexFilter("findByLastNameStartingWith", IndexedPerson.class, "Coutant-Kerbalec")).isFalse();
        List<IndexedPerson> persons2 = reactiveRepository.findByLastNameStartingWith("Coutant-Kerbalec")
                .subscribeOn(Schedulers.parallel()).collectList().block();
        assertThat(persons2).hasSize(2);
    }

    @Test
    @NoSecondaryIndexRequired
    void findByNestedSimplePropertyStartingWith_String_Distinct_NegativeTest() {
        alain.setFriend(luc);
        reactiveRepository.save(alain);
        lilly.setFriend(petra);
        reactiveRepository.save(lilly);
        daniel.setFriend(emilien);
        reactiveRepository.save(daniel);

        assertThatThrownBy(() -> reactiveRepository.findDistinctByFriendLastNameStartingWith("l"))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("DISTINCT queries are currently supported only for the first level objects, got a query for " +
                "friend.lastName");

        TestUtils.setFriendsToNull(reactiveRepository, alain, lilly, daniel);
    }
}
