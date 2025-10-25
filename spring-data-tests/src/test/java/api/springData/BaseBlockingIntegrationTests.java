package api.springData;

import api.springData.config.BlockingTestConfig;
import api.springData.config.CommonTestConfig;
import api.springData.config.IndexedBinsAnnotationsProcessor;
import api.springData.utility.QueryUtils;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.exp.Expression;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.Statement;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.data.aerospike.cache.AerospikeCacheKeyProcessor;
import org.springframework.data.aerospike.core.AerospikeTemplate;
import org.springframework.data.aerospike.mapping.AerospikePersistentProperty;
import org.springframework.data.aerospike.mapping.BasicAerospikePersistentEntity;
import org.springframework.data.aerospike.query.FilterOperation;
import org.springframework.data.aerospike.query.QueryContext;
import org.springframework.data.aerospike.query.QueryEngine;
import org.springframework.data.aerospike.query.cache.IndexRefresher;
import org.springframework.data.aerospike.query.cache.IndexesCache;
import org.springframework.data.aerospike.query.model.IndexedField;
import org.springframework.data.aerospike.query.qualifier.Qualifier;
import org.springframework.data.aerospike.repository.query.Query;
import org.springframework.data.aerospike.server.version.ServerVersionSupport;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.StringUtils;
import utils.AerospikeLogger;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.aerospike.core.MappingUtils.getBinNamesFromTargetClass;
import static org.springframework.data.aerospike.repository.query.CriteriaDefinition.AerospikeMetadata.LAST_UPDATE_TIME;

@SpringBootTest(
        classes = {BlockingTestConfig.class, CommonTestConfig.class},
        properties = {
                "expirationProperty: 1",
                "setSuffix: service1",
                "indexSuffix: index1"
        }
)
public abstract class BaseBlockingIntegrationTests extends BaseIntegrationTests {

    @Autowired
    protected AerospikeTemplate template;
    @Autowired
    protected IAerospikeClient client;
    @Autowired
    protected QueryEngine queryEngine;
    @Autowired
    protected ServerVersionSupport serverVersionSupport;
    @Autowired
    protected IndexesCache indexesCache;
    @Autowired
    protected IndexRefresher indexRefresher;
    @Autowired
    protected Environment env;
    @Autowired
    protected MappingContext<BasicAerospikePersistentEntity<?>, AerospikePersistentProperty> mappingContext;
    @Autowired
    protected AerospikeCacheKeyProcessor cacheKeyProcessor;

    protected <T> void deleteOneByOne(Collection<T> collection) {
        collection.forEach(item -> template.delete(item));
    }

    protected <T> void deleteOneByOne(Collection<T> collection, String setName) {
        collection.forEach(item -> template.delete(item, setName));
    }

    protected <T> List<T> runLastUpdateTimeQuery(long lastUpdateTimeMillis, FilterOperation operation,
                                                 Class<T> entityClass) {
        Qualifier lastUpdateTimeLtMillis = Qualifier.metadataBuilder()
                .setMetadataField(LAST_UPDATE_TIME)
                .setFilterOperation(operation)
                .setValue(lastUpdateTimeMillis * MILLIS_TO_NANO)
                .build();
        return template.find(new Query(lastUpdateTimeLtMillis), entityClass).toList();
    }

    public <T> T findById(Object id, Class<T> entityClass) {
        AerospikeLogger.info("template findById with id " + id.toString() + " and entityClass " + entityClass.toString());
        System.out.println("d");
        return template.findById(id, entityClass);
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
        assertThat(isIndexedBin(getNameSpace(), template.getSetName(clazz), binName))
                .as(String.format("Expecting bin %s to be indexed", binName)).isTrue();
    }

    protected void assertBinsAreIndexed(TestInfo testInfo) {
        testInfo.getTestMethod().stream()
                .filter(not(IndexedBinsAnnotationsProcessor::hasNoindexAnnotation))
                .forEach(testMethod -> {
                    assertThat(IndexedBinsAnnotationsProcessor.hasAssertBinsAreIndexedAnnotation(testMethod))
                            .as(String.format("Expecting the test method %s to have @AssertBinsAreIndexed annotation",
                                    testMethod.getName()))
                            .isTrue();
                    String[] binNames = IndexedBinsAnnotationsProcessor.getBinNames(testMethod);
                    Class<?> entityClass = IndexedBinsAnnotationsProcessor.getEntityClass(testMethod);
                    assertThat(binNames).as("Expecting bin names to be populated").isNotNull();
                    assertThat(binNames).as("Expecting bin names ").isNotEmpty();
                    assertThat(entityClass).as("Expecting entityClass to be populated").isNotNull();

                    for (String binName : binNames) {
                        assertBinIsIndexed(binName, entityClass);
                    }
                });
    }


    /**
     * Assert that the given query's statement contains secondary index filter
     *
     * @param query             Query to be performed
     * @param returnEntityClass Class of Query return entity
     */
    protected void assertQueryHasSecIndexFilter(Query query, Class<?> returnEntityClass) {
        String setName = template.getSetName(returnEntityClass);
        String[] binNames = getBinNamesFromTargetClass(returnEntityClass, mappingContext);

        assertThat(queryHasSecIndexFilter(namespace, setName, query, binNames))
                .as(String.format("Expecting the query '%s' statement to have secondary index filter",
                        query.getCriteriaObject())).isTrue();
    }

    /**
     * Assert that the given method's query statement contains secondary index filter
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

    protected void assertQueryHasSecIndexFilter_Debug(String methodName, Class<?> returnEntityClass,
                                                      Object... methodParams) {
        assertThat(queryHasSecIndexFilter_Debug(methodName, returnEntityClass, methodParams))
                .as(String.format("Expecting the query %s statement to have secondary index filter", methodName)).isTrue();
    }

    /**
     * Get the given method's secondary index filter
     *
     * @param methodName        Query method to be performed
     * @param returnEntityClass Class of Query return entity
     * @param methodParams      Query parameters
     */
    protected Filter getQueryHasSecIndexFilter(String methodName, Class<?> returnEntityClass,
                                               Object... methodParams) {
        return getQuerySecIndexFilter(methodName, returnEntityClass, methodParams);
    }

    /**
     * Assert that the given method's query statement does not contain secondary index filter
     *
     * @param methodName        Query method to be performed
     * @param returnEntityClass Class of Query return entity
     * @param methodParams      Query parameters
     */
    protected void assertQueryHasNoSecIndexFilter(String methodName, Class<?> returnEntityClass,
                                                  Object... methodParams) {
        assertThat(queryHasSecIndexFilter(methodName, returnEntityClass, methodParams)).isFalse();
    }

    protected boolean queryHasSecIndexFilter(String methodName, Class<?> returnEntityClass,
                                             Object... methodParams) {
        String setName = template.getSetName(returnEntityClass);
        String[] binNames = getBinNamesFromTargetClass(returnEntityClass, mappingContext);
        Query query = QueryUtils.createQueryForMethodWithArgs(methodName, methodParams);

        return queryHasSecIndexFilter(namespace, setName, query, binNames);
    }

    protected boolean queryHasSecIndexFilter_Debug(String methodName, Class<?> returnEntityClass,
                                                   Object... methodParams) {
        String setName = template.getSetName(returnEntityClass);
        String[] binNames = getBinNamesFromTargetClass(returnEntityClass, mappingContext);
        Query query = QueryUtils.createQueryForMethodWithArgs(methodName, methodParams);
        AerospikeLogger.info("Query method: " + methodName);

        return queryHasSecIndexFilter_Debug(namespace, setName, query, binNames);
    }

    protected boolean queryHasSecIndexFilter(String namespace, String setName, Query query, String[] binNames) {
        QueryContext queryContext = queryEngine.getQueryContextBuilder().build(namespace, setName, query, binNames);
        Statement statement = queryContext.statement();
        // Checking that the statement has secondary index filter (which means it will be used)
        return statement.getFilter() != null;
    }

    protected boolean queryHasSecIndexFilter_Debug(String namespace, String setName, Query query, String[] binNames) {
        QueryContext queryContext = queryEngine.getQueryContextBuilder().build(namespace, setName, query, binNames);
        Statement statement = queryContext.statement();
        // Checking that the statement has secondary index filter (which means it will be used)
        AerospikeLogger.info("Statement filter name: " + statement.getFilter().getName());
        return statement.getFilter() != null;
    }

    protected Filter getQuerySecIndexFilter(String methodName, Class<?> returnEntityClass, Object... methodParams) {
        String setName = template.getSetName(returnEntityClass);
        String[] binNames = getBinNamesFromTargetClass(returnEntityClass, mappingContext);
        Query query = QueryUtils.createQueryForMethodWithArgs(methodName, methodParams);

        return getQuerySecIndexFilter(namespace, setName, query, binNames);
    }

    protected Filter getQuerySecIndexFilter(Query query, Class<?> returnEntityClass) {
        String setName = template.getSetName(returnEntityClass);
        String[] binNames = getBinNamesFromTargetClass(returnEntityClass, mappingContext);

        return getQuerySecIndexFilter(namespace, setName, query, binNames);
    }

    protected Filter getQuerySecIndexFilter(String namespace, String setName, Query query, String[] binNames) {
        QueryContext queryContext = queryEngine.getQueryContextBuilder().build(namespace, setName, query, binNames);
        // Checking that the statement has secondary index filter (which means it will be used)
        return queryContext.statement().getFilter();
    }

    protected Expression getQueryExpression(String methodName, Class<?> returnEntityClass, Object... methodParams) {
        String setName = template.getSetName(returnEntityClass);
        String[] binNames = getBinNamesFromTargetClass(returnEntityClass, mappingContext);
        Query query = QueryUtils.createQueryForMethodWithArgs(methodName, methodParams);

        return getQueryExpression(namespace, setName, query, binNames);
    }

    protected Expression getQueryExpression(Query query, Class<?> returnEntityClass) {
        String setName = template.getSetName(returnEntityClass);
        String[] binNames = getBinNamesFromTargetClass(returnEntityClass, mappingContext);

        return getQueryExpression(namespace, setName, query, binNames);
    }

    protected Expression getQueryExpression(String namespace, String setName, Query query, String[] binNames) {
        QueryContext queryContext = queryEngine.getQueryContextBuilder().build(namespace, setName, query, binNames);
        // Checking that the statement has secondary index filter (which means it will be used)
        return queryEngine.getFilterExpressionsBuilder().build(queryContext.qualifier());
    }

    protected Map<?, ?> pojoToMap(Object pojo) {
        Object result = template.getAerospikeConverter().toWritableValue(pojo, TypeInformation.of(pojo.getClass()));
        if (result instanceof Map<?, ?>) {
            return (Map<?, ?>) result;
        }

        throw new IllegalArgumentException("The result of conversion is not a Map, expecting only a POJO argument");
    }
}
