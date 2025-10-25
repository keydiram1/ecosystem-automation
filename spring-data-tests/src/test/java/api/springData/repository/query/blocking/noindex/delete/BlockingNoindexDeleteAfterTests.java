package api.springData.repository.query.blocking.noindex.delete;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Date;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * Tests for the "Is after" repository query. Keywords: After, IsAfter.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns22"})
@TestPropertySource(properties = {"personSetName=personDeleteSetNameAfterTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexDeleteAfterTests extends PersonRepositoryQueryTests {

    @Test
    void deleteByDateSimplePropertyAfter() {
        dave.setDateOfBirth(new Date());
        repository.save(dave);

        Date date = new Date(126230400);
        assertThat(repository.findByDateOfBirthAfter(date)).isNotEmpty();

        repository.deleteByDateOfBirthAfter(date);

        assertThat(repository.findByDateOfBirthAfter(date)).isEmpty();

        dave.setDateOfBirth(null);
        repository.save(dave);
    }

    @Test
    void deleteByDateSimplePropertyAfter_NoMatchingRecords() {
        Date date = new Date();
        assertThat(repository.findByDateOfBirthAfter(date)).isEmpty();

        repository.deleteByDateOfBirthAfter(date);

        assertThat(repository.findByDateOfBirthAfter(date)).isEmpty();
    }
}
