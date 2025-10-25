package api.springData.index;

import api.springData.sample.AutoIndexedDocument;
import api.springData.utility.MockObjectProvider;
import com.aerospike.client.query.IndexCollectionType;
import com.aerospike.client.query.IndexType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.core.ReactiveAerospikeTemplate;
import org.springframework.data.aerospike.index.AerospikeIndexDefinition;
import org.springframework.data.aerospike.index.AerospikeIndexResolver;
import org.springframework.data.aerospike.index.ReactiveAerospikePersistenceEntityIndexCreator;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns12"})
@Tag("SPRING-DATA-TESTS-1")
class ReactiveAerospikePersistenceEntityIndexCreatorTest extends ReactiveAerospikePersistenceEntityIndexCreator {

    final boolean createIndexesOnStartup = true;

    public ReactiveAerospikePersistenceEntityIndexCreatorTest() {
        super(null, true, mock(AerospikeIndexResolver.class),
                new MockObjectProvider<>(mock(ReactiveAerospikeTemplate.class)));
    }

    final ReactiveAerospikeTemplate template = mock(ReactiveAerospikeTemplate.class);
    final String name = "someName";
    final String fieldName = "fieldName";
    final Class<?> targetClass = AutoIndexedDocument.class;
    final IndexType type = IndexType.STRING;
    final IndexCollectionType collectionType = IndexCollectionType.DEFAULT;
    final AerospikeIndexDefinition definition = AerospikeIndexDefinition.builder()
            .name(name)
            .bin(fieldName)
            .entityClass(targetClass)
            .type(type)
            .collectionType(collectionType)
            .build();

    @Test
    void shouldFailInstallIndexOnUnhandledException() {
        when(template.createIndex(targetClass, name, fieldName, type, collectionType))
                .thenReturn(Mono.error(new RuntimeException()));

        Set<AerospikeIndexDefinition> indexes = Collections.singleton(definition);

        assertThrows(RuntimeException.class, () -> installIndexes(indexes));
    }
}
