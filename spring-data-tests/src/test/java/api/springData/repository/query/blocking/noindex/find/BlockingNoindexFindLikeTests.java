package api.springData.repository.query.blocking.noindex.find;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import api.springData.sample.Person;
import api.springData.utility.TestUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.QueryParam;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.aerospike.query.QueryParam.of;

/**
 * Tests for the "Is like" repository query. Keywords: Like, IsLike.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns20"})
@TestPropertySource(properties = {"personSetName=personSetNameLikeTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexFindLikeTests extends PersonRepositoryQueryTests {

    @Test
    void findBySimplePropertyLike_String() {
        List<Person> persons = repository.findByFirstNameLike("Ca.*er");
        assertThat(persons).contains(carter);

        List<Person> persons0 = repository.findByFirstNameLikeIgnoreCase("CART.*er");
        assertThat(persons0).contains(carter);

        List<Person> persons1 = repository.findByFirstNameLike(".*ve.*");
        assertThat(persons1).contains(dave, oliver);

        List<Person> persons2 = repository.findByFirstNameLike("Carr.*er");
        assertThat(persons2).isEmpty();
    }

    @Test
    // "findByIdLike" uses filter expression (scan operation), only for String ids
    void findByIdLike_String() {
        List<Person> persons = repository.findByIdLike("IDpersonRepositoryQueryTests.*");
        assertThat(persons).containsExactlyInAnyOrderElementsOf(allPersonsWithSimilarIds);

        QueryParam idLike = of("IDpersonRepositoryQueryTests.*");
        QueryParam name = of(carter.getFirstName());
        persons = repository.findByIdLikeAndFirstName(idLike, name);
        assertThat(persons).containsOnly(carter);

        QueryParam ids = of(List.of(carter.getId(), dave.getId()));
        persons = repository.findByIdLikeAndId(idLike, ids);
        assertThat(persons).containsOnly(carter, dave);
    }

    @Test
    void findByNestedSimplePropertyLike_String() {
        oliver.setFriend(dave);
        repository.save(oliver);
        carter.setFriend(stefan);
        repository.save(carter);

        List<Person> result = repository.findByFriendLastNameLike(".*tthe.*");
        assertThat(result).contains(oliver);

        TestUtils.setFriendsToNull(repository, oliver, carter);
    }
}
