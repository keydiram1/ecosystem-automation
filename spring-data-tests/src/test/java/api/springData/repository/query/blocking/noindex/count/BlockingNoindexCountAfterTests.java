package api.springData.repository.query.blocking.noindex.count;

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
@TestPropertySource(properties = {"personSetName=personCountSetNameAfterTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexCountAfterTests extends PersonRepositoryQueryTests {

    @Test
    void countByDateSimplePropertyAfter() {
        dave.setDateOfBirth(new Date());
        repository.save(dave);

        long result = repository.countByDateOfBirthAfter(new Date(126230400));
        assertThat(result).isEqualTo(1);

        dave.setDateOfBirth(null);
        repository.save(dave);
    }

    @Test
    void countByDateSimplePropertyAfter_NoMatchingRecords() {
        long result = repository.countByDateOfBirthAfter(new Date());
        assertThat(result).isZero();
    }
}
