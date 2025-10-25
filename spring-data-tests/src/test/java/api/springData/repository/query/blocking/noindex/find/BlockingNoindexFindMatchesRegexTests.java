package api.springData.repository.query.blocking.noindex.find;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import api.springData.sample.Person;
import api.springData.utility.TestUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the "Matches regex" repository query. Keywords: Regex, MatchesRegex, Matches.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns20"})
@TestPropertySource(properties = {"personSetName=personSetNameMatchesRegexTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexFindMatchesRegexTests extends PersonRepositoryQueryTests {

    @Test
    void findBySimplePropertyMatchingRegex_String() {
        List<Person> persons = repository.findByFirstNameMatchesRegex("Ca.*er");
        assertThat(persons).contains(carter);

        persons = repository.findByFirstNameMatches("Ca.*er");
        assertThat(persons).contains(carter);

        persons = repository.findByFirstNameRegex("Ca.*er");
        assertThat(persons).contains(carter);

        List<Person> persons0 = repository.findByFirstNameMatchesRegexIgnoreCase("CART.*er");
        assertThat(persons0).contains(carter);

        List<Person> persons1 = repository.findByFirstNameMatchesRegex(".*ve.*");
        assertThat(persons1).contains(dave, oliver);

        List<Person> persons2 = repository.findByFirstNameMatchesRegex("Carr.*er");
        assertThat(persons2).isEmpty();
    }

    @Test
    void findByNestedStringSimplePropertyMatchingRegex() {
        oliver.setFriend(dave);
        repository.save(oliver);
        carter.setFriend(stefan);
        repository.save(carter);

        List<Person> result = repository.findByFriendLastNameLike(".*tthe.*");
        assertThat(result).contains(oliver);

        result = repository.findByFriendLastNameMatchesRegex(".*tthe.*");
        assertThat(result).contains(oliver);
        TestUtils.setFriendsToNull(repository, oliver, carter);
    }
}
