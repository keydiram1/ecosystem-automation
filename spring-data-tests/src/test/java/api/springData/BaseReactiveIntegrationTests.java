package api.springData;

import api.springData.config.CommonTestConfig;
import api.springData.config.IndexedBinsAnnotationsProcessor;
import api.springData.config.ReactiveTestConfig;
import api.springData.sample.ReactiveIndexedPersonRepository;
import api.springData.utility.QueryUtils;
import com.aerospike.client.exp.Expression;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.Statement;
import com.aerospike.client.reactor.IAerospikeReactorClient;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.aerospike.core.ReactiveAerospikeTemplate;
import org.springframework.data.aerospike.mapping.AerospikePersistentProperty;
import org.springframework.data.aerospike.mapping.BasicAerospikePersistentEntity;
import org.springframework.data.aerospike.query.FilterOperation;
import org.springframework.data.aerospike.query.QueryContext;
import org.springframework.data.aerospike.query.ReactorQueryEngine;
import org.springframework.data.aerospike.query.cache.IndexesCache;
import org.springframework.data.aerospike.query.cache.ReactorIndexRefresher;
import org.springframework.data.aerospike.query.model.IndexedField;
import org.springframework.data.aerospike.query.qualifier.Qualifier;
import org.springframework.data.aerospike.repository.query.Query;
import org.springframework.data.aerospike.server.version.ServerVersionSupport;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.aerospike.core.MappingUtils.getBinNamesFromTargetClass;
import static org.springframework.data.aerospike.repository.query.CriteriaDefinition.AerospikeMetadata.LAST_UPDATE_TIME;

@SpringBootTest(
    classes = {ReactiveTestConfig.class, CommonTestConfig.class},
    properties = {
        "expirationProperty: 1",
        "setSuffix: service1",
        "indexSuffix: index1"
    }
)
public abstract class BaseReactiveIntegrationTests extends BaseIntegrationTests {

    @Autowired
    protected ReactiveAerospikeTemplate reactiveTemplate;
    @Autowired
    protected IAerospikeReactorClient reactorClient;
    @Autowired
    protected ServerVersionSupport serverVersionSupport;
    @Autowired
    protected
    ReactorQueryEngine reactiveQueryEngine;
    @Autowired
    protected ReactorIndexRefresher reactorIndexRefresher;
    @Autowired
    protected IndexesCache indexesCache;
    @Autowired
    protected MappingContext<BasicAerospikePersistentEntity<?>, AerospikePersistentProperty> mappingContext;
    @Autowired
    protected ReactiveBlockingAerospikeTestOperations reactiveBlockingAerospikeTestOperations;

    protected <T> T findById(Serializable id, Class<T> type) {
        return reactiveTemplate.findById(id, type).block();
    }

    protected <T> T findById(Serializable id, Class<T> type, String setName) {
        return reactiveTemplate.findById(id, type, setName).block();
    }

    protected <T> void deleteAll(Iterable<T> iterable) {
        Flux.fromIterable(iterable).flatMap(item -> reactiveTemplate.delete(item)).blockLast();
    }

    protected <T> void deleteAll(Iterable<T> iterable, String setName) {
        Flux.fromIterable(iterable).flatMap(item -> reactiveTemplate.delete(item, setName)).blockLast();
    }

    protected <T> List<T> runLastUpdateTimeQuery(long lastUpdateTimeMillis, FilterOperation operation,
                                                 Class<T> entityClass) {
        Qualifier lastUpdateTimeLtMillis = Qualifier.metadataBuilder()
            .setMetadataField(LAST_UPDATE_TIME)
            .setFilterOperation(operation)
            .setValue(lastUpdateTimeMillis * MILLIS_TO_NANO)
            .build();
        return reactiveTemplate.find(new Query(lastUpdateTimeLtMillis), entityClass).collectList().block();
    }

    protected boolean isIndexedBin(String namespace, String setName, String binName) {
        boolean hasIndex = false;
        if (StringUtils.hasLength(binName)) {
            hasIndex = indexesCache.hasIndexFor(
                    new IndexedField(namespace, setName, binName)
            );
        }
        return hasIndex;
    }

    protected void assertBinIsIndexed(String binName, Class<?> clazz) {
        assertThat(isIndexedBin(getNameSpace(), reactiveTemplate.getSetName(clazz), binName))
                .as(String.format("Expecting bin %s to be indexed", binName)).isTrue();
    }

    protected void assertBinsAreIndexed(TestInfo testInfo) {
        Optional<Method> testMethodOptional = testInfo.getTestMethod();
        if (testMethodOptional.isPresent()) {
            Method testMethod = testMethodOptional.get();
            if (!IndexedBinsAnnotationsProcessor.hasNoindexAnnotation(testMethod)) {
                assertThat(IndexedBinsAnnotationsProcessor.hasAssertBinsAreIndexedAnnotation(testMethod))
                        .as(String.format("Expecting the test method %s to have @AssertBinsAreIndexed annotation",
                                testMethod.getName())).
                        isTrue();
                String[] binNames = IndexedBinsAnnotationsProcessor.getBinNames(testMethod);
                Class<?> entityClass = IndexedBinsAnnotationsProcessor.getEntityClass(testMethod);
                assertThat(binNames).as("Expecting bin names to be populated").isNotNull();
                assertThat(binNames).as("Expecting bin names ").isNotEmpty();
                assertThat(entityClass).as("Expecting entityClass to be populated").isNotNull();

                for (String binName : binNames) {
                    assertBinIsIndexed(binName, entityClass);
                }
            }
        }
    }

    /**
     * Assert that the given query statement contains secondary index filter
     *
     * @param methodName        Query method to be performed
     * @param returnEntityClass Class of Query return entity
     * @param methodParams      Query parameters
     */
    protected void assertQueryHasSecIndexFilter(String methodName, Class<?> returnEntityClass,
                                                Object... methodParams) {
        assertThat(queryHasSecIndexFilter(methodName, returnEntityClass, methodParams))
                .as(String.format("Expecting the query %s statement to have secondary index filter", methodName)).isTrue();
    }

    protected boolean queryHasSecIndexFilter(String methodName, Class<?> returnEntityClass,
                                             Object... methodParams) {
        String setName = reactiveTemplate.getSetName(returnEntityClass);
        String[] binNames = getBinNamesFromTargetClass(returnEntityClass, mappingContext);
        Query query = QueryUtils.createQueryForMethodWithArgs(ReactiveIndexedPersonRepository.class, returnEntityClass,
                methodName, methodParams);

        QueryContext queryContext = reactiveQueryEngine.getQueryContextBuilder().build(namespace, setName, query, binNames);
        Statement statement = queryContext.statement();
        // Checking that the statement has secondary index filter (which means it will be used)
        return statement.getFilter() != null;
    }

    protected Filter getQuerySecIndexFilter(String methodName, Class<?> returnEntityClass,
                                            Object... methodParams) {
        String setName = reactiveTemplate.getSetName(returnEntityClass);
        String[] binNames = getBinNamesFromTargetClass(returnEntityClass, mappingContext);
        Query query = QueryUtils.createQueryForMethodWithArgs(methodName, methodParams);

        return getQuerySecIndexFilter(namespace, setName, query, binNames);
    }

    protected Filter getQuerySecIndexFilter(Query query, Class<?> returnEntityClass) {
        String setName = reactiveTemplate.getSetName(returnEntityClass);
        String[] binNames = getBinNamesFromTargetClass(returnEntityClass, mappingContext);

        return getQuerySecIndexFilter(namespace, setName, query, binNames);
    }

    protected Filter getQuerySecIndexFilter(String namespace, String setName, Query query, String[] binNames) {
        QueryContext queryContext = reactiveQueryEngine.getQueryContextBuilder().build(namespace, setName, query, binNames);
        // Checking that the statement has secondary index filter (which means it will be used)
        return queryContext.statement().getFilter();
    }

    protected Expression getQueryExpression(String methodName, Class<?> returnEntityClass,
                                            Object... methodParams) {
        String setName = reactiveTemplate.getSetName(returnEntityClass);
        String[] binNames = getBinNamesFromTargetClass(returnEntityClass, mappingContext);
        Query query = QueryUtils.createQueryForMethodWithArgs(methodName, methodParams);

        return getQueryExpression(namespace, setName, query, binNames);
    }

    protected Expression getQueryExpression(String namespace, String setName, Query query, String[] binNames) {
        QueryContext queryContext = reactiveQueryEngine.getQueryContextBuilder().build(namespace, setName, query, binNames);
        // Checking that the statement has secondary index filter (which means it will be used)
        return reactiveQueryEngine.getFilterExpressionsBuilder().build(queryContext.qualifier());
    }

    protected Expression getQueryExpression(Query query, Class<?> returnEntityClass) {
        String setName = reactiveTemplate.getSetName(returnEntityClass);
        String[] binNames = getBinNamesFromTargetClass(returnEntityClass, mappingContext);

        return getQueryExpression(namespace, setName, query, binNames);
    }
}
