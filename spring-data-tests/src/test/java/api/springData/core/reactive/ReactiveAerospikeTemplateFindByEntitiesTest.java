package api.springData.core.reactive;

import api.springData.BaseReactiveIntegrationTests;
import api.springData.core.sync.AbstractFindByEntitiesTest;
import org.junit.jupiter.api.Tag;
import org.springframework.data.aerospike.core.model.GroupedEntities;
import org.springframework.data.aerospike.core.model.GroupedKeys;
import org.springframework.test.context.TestPropertySource;
import reactor.core.scheduler.Schedulers;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns13"})
@Tag("SPRING-DATA-TESTS-1")
@TestPropertySource(properties = {"personSetName=personSetNameReactiveAerospikeTemplateFindByEntitiesTest"})
public class ReactiveAerospikeTemplateFindByEntitiesTest
    extends BaseReactiveIntegrationTests implements AbstractFindByEntitiesTest {

    @Override
    public <T> void save(T obj) {
        reactiveTemplate.save(obj)
            .subscribeOn(Schedulers.parallel())
            .block();
    }

    @Override
    public <T> void delete(T obj) {
        reactiveTemplate.delete(obj)
            .subscribeOn(Schedulers.parallel())
            .block();
    }

    @Override
    public GroupedEntities findByIds(GroupedKeys groupedKeys) {
        return reactiveTemplate.findByIds(groupedKeys)
            .subscribeOn(Schedulers.parallel())
            .block();
    }
}
