package api.springData.index;

import api.springData.sample.AutoIndexedDocument;
import api.springData.utility.MockObjectProvider;
import com.aerospike.client.query.IndexCollectionType;
import com.aerospike.client.query.IndexType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.aerospike.core.AerospikeTemplate;
import org.springframework.data.aerospike.index.AerospikeIndexDefinition;
import org.springframework.data.aerospike.index.AerospikeIndexResolver;
import org.springframework.data.aerospike.mapping.AerospikeMappingContext;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns11"})
@Tag("SPRING-DATA-TESTS-1")
class AerospikePersistenceEntityIndexCreatorTest {

    final boolean createIndexesOnStartup = true;
    final AerospikeIndexResolver aerospikeIndexResolver = mock(AerospikeIndexResolver.class);
    final AerospikeTemplate template = mock(AerospikeTemplate.class);
    final ObjectProvider<AerospikeTemplate> templateProvider = new MockObjectProvider<>(template);
    final AerospikeMappingContext mappingContext = mock(AerospikeMappingContext.class);

    final String name = "someName";
    final String fieldName = "fieldName";
    final Class<?> targetClass = AutoIndexedDocument.class;
    final IndexType type = IndexType.STRING;
    final IndexCollectionType collectionType = IndexCollectionType.LIST;
    final AerospikeIndexDefinition definition = AerospikeIndexDefinition.builder()
            .name(name)
            .bin(fieldName)
            .entityClass(targetClass)
            .type(type)
            .collectionType(collectionType)
            .ctx(null)
            .build();

    @Test
    void shouldInstallIndex() {
        Set<AerospikeIndexDefinition> indexes = Collections.singleton(definition);

        installIndexes(indexes);

        verify(template).createIndex(targetClass, name, fieldName, type, collectionType);
    }

    private void installIndexes(Set<AerospikeIndexDefinition> indexes) {
        indexes.forEach(this::installIndex);
    }

    private void installIndex(AerospikeIndexDefinition index) {
        System.out.printf("Installing aerospike index: %s%n", index);
        try {
            if (index.getCtx() == null) {
                templateProvider.getIfUnique().createIndex(index.getEntityClass(), index.getName(),
                        index.getBin(), index.getType(), index.getCollectionType());
            } else {
                templateProvider.getIfUnique().createIndex(index.getEntityClass(), index.getName(),
                        index.getBin(), index.getType(), index.getCollectionType(), index.getCtx());
            }
            System.out.printf("Installed aerospike index: %s successfully.%n", index);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to install aerospike index: " + index, e);
        }
    }
}
