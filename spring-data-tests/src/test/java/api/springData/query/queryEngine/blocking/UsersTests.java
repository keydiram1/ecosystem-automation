package api.springData.query.queryEngine.blocking;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.FilterOperation;
import org.springframework.data.aerospike.query.KeyRecordIterator;
import org.springframework.data.aerospike.query.qualifier.Qualifier;
import org.springframework.data.aerospike.repository.query.Query;
import org.springframework.test.context.TestPropertySource;

import static api.springData.query.queryEngine.QueryEngineTestDataPopulator.RECORD_COUNT;
import static api.springData.query.queryEngine.QueryEngineTestDataPopulator.USERS_SET;
import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns11"})
@Tag("SPRING-DATA-TESTS-1")
class UsersTests extends BaseQueryEngineTests {

    @Test
    void allUsers() {
        KeyRecordIterator it = queryEngine.select(namespace, USERS_SET, null, null);

        assertThat(it).toIterable().hasSize(RECORD_COUNT);
    }

    @Test
    void usersInterrupted() {
        try (KeyRecordIterator it = queryEngine.select(namespace, USERS_SET, null, null)) {
            int counter = 0;
            while (it.hasNext()) {
                it.next();
                counter++;
                if (counter >= 1000)
                    break;
            }
        }
    }

    @Test
    void usersInNorthRegion() {
        Qualifier qualifier = Qualifier.builder()
            .setPath("region")
            .setFilterOperation(FilterOperation.EQ)
            .setValue("n")
            .build();

        KeyRecordIterator it = queryEngine.select(namespace, USERS_SET, null, new Query(qualifier));

        assertThat(it).toIterable()
            .isNotEmpty()
            .allSatisfy(rec -> assertThat(rec.record.getString("region")).isEqualTo("n"));
    }
}
