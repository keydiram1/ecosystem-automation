package api.springData.repository.query.blocking.noindex.exists;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the "Is after" repository query. Keywords: After, IsAfter.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns21"})
@TestPropertySource(properties = {"personSetName=personExistsSetNameAfterTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexExistsAfterTests extends PersonRepositoryQueryTests {

    @Test
    void existsByDateSimplePropertyAfter() {
        dave.setDateOfBirth(new Date());
        repository.save(dave);

        boolean result = repository.existsByDateOfBirthAfter(new Date(126230400));
        assertThat(result).isTrue();

        dave.setDateOfBirth(null);
        repository.save(dave);
    }

    @Test
    void existsByDateSimplePropertyAfter_NoMatchingRecords() {
        boolean result = repository.existsByDateOfBirthAfter(new Date());
        assertThat(result).isFalse();
    }
}
