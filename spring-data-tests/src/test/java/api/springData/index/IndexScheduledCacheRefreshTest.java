package api.springData.index;

import api.springData.BaseBlockingIntegrationTests;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.model.IndexedField;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static com.aerospike.client.query.IndexType.STRING;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns17"})
@Tag("SPRING-DATA-TESTS-1")
@Slf4j
@ContextConfiguration
@TestPropertySource(properties = {"spring.data.aerospike.index-cache-refresh-seconds=4"})
public class IndexScheduledCacheRefreshTest extends BaseBlockingIntegrationTests {

    String setName = "scheduled";
    String indexName = "index1";
    String binName = "testBin";

    @Test
    public void indexesCacheIsRefreshedOnSchedule() {
        client.createIndex(null, getNameSpace(), setName, indexName, binName, STRING).waitTillComplete();
        log.debug("Test index {} is created", indexName);
        await()
            .timeout(5, SECONDS)
            .pollDelay(4, SECONDS)
            .untilAsserted(() -> Assertions.assertTrue(true));
        log.debug("Checking indexes");

        assertThat(indexesCache.hasIndexFor(new IndexedField(namespace, setName, binName))).isTrue();
    }
}
