package api.springData.core.sync;

import api.springData.BaseBlockingIntegrationTests;
import org.junit.jupiter.api.Tag;
import org.springframework.data.aerospike.core.model.GroupedEntities;
import org.springframework.data.aerospike.core.model.GroupedKeys;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns11"})
@TestPropertySource(properties = {"personSetName=personSetNameAerospikeTemplateFindByEntitiesTests"})
@Tag("SPRING-DATA-TESTS-1")
public class AerospikeTemplateFindByEntitiesTests
    extends BaseBlockingIntegrationTests implements AbstractFindByEntitiesTest {

    @Override
    public <T> void save(T obj) {
        template.save(obj);
    }

    @Override
    public <T> void delete(T obj) {
        template.delete(obj);
    }

    @Override
    public GroupedEntities findByIds(GroupedKeys groupedKeys) {
        return template.findByIds(groupedKeys);
    }
}
