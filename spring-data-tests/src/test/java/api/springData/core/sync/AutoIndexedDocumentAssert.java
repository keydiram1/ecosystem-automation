package api.springData.core.sync;

import com.aerospike.client.cdt.CTX;
import com.aerospike.client.query.IndexCollectionType;
import com.aerospike.client.query.IndexType;
import lombok.experimental.UtilityClass;
import org.springframework.data.aerospike.query.model.Index;
import api.springData.utility.AdditionalAerospikeTestOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@UtilityClass
public class AutoIndexedDocumentAssert {

    public void assertIndexesCreated(AdditionalAerospikeTestOperations operations, String namespace) {
        assertAutoIndexedDocumentIndexesCreated(operations, namespace);
        assertConfigPackageDocumentIndexesCreated(operations, namespace);
    }

    public void assertAutoIndexedDocumentIndexesCreated(AdditionalAerospikeTestOperations operations,
                                                        String namespace) {
        String setName = "auto-indexed-set";
        List<Index> indexes = operations.getIndexes(setName);

        assertThat(indexes).containsExactlyInAnyOrder(
            index(namespace, setName, setName + "_someField_string_default", "someField", IndexType.STRING, null, null),
            index(namespace, setName, setName + "_shortName_string_default", "shortName", IndexType.STRING, null, null),
            index(namespace, setName, "overridden_index_name", "customIndexName", IndexType.NUMERIC, null, null),
            index(namespace, setName, "pre_created_index", "preCreatedIndex", IndexType.NUMERIC, null, null),
            index(namespace, setName, "placeholder_index1", "placeHolderIdx", IndexType.STRING, null, null),
            index(namespace, setName, setName + "_listOfStrings_string_list", "listOfStrings", IndexType.STRING,
                IndexCollectionType.LIST, null),
            index(namespace, setName, setName + "_listOfInts_numeric_list", "listOfInts", IndexType.NUMERIC,
                IndexCollectionType.LIST, null),
            index(namespace, setName, setName + "_mapOfStrKeys_string_mapkeys", "mapOfStrKeys", IndexType.STRING,
                IndexCollectionType.MAPKEYS, null),
            index(namespace, setName, setName + "_mapOfStrVals_string_mapvalues", "mapOfStrVals", IndexType.STRING,
                IndexCollectionType.MAPVALUES, null),
            index(namespace, setName, setName + "_mapOfIntKeys_numeric_mapkeys", "mapOfIntKeys", IndexType.NUMERIC,
                IndexCollectionType.MAPKEYS, null),
            index(namespace, setName, setName + "_mapOfIntVals_numeric_mapvalues", "mapOfIntVals", IndexType.NUMERIC,
                IndexCollectionType.MAPVALUES, null)
        );
    }

    public void assertConfigPackageDocumentIndexesCreated(AdditionalAerospikeTestOperations operations,
                                                          String namespace) {
        String setName = "config-package-document-set";
        List<Index> indexes = operations.getIndexes(setName);

        assertThat(indexes).containsExactlyInAnyOrder(
            index(namespace, setName, "config-package-document-index", "indexedField", IndexType.STRING, null, null)
        );
    }

    private static Index index(String namespace, String setName, String name, String bin, IndexType indexType,
                               IndexCollectionType collectionType) {
        return index(namespace, setName, name, bin, indexType, collectionType, new CTX[0]);
    }

    private static Index index(String namespace, String setName, String name, String bin, IndexType indexType,
                               IndexCollectionType collectionType, CTX[] ctx) {
        return Index.builder()
            .namespace(namespace)
            .set(setName)
            .name(name)
            .bin(bin)
            .indexType(indexType)
            .indexCollectionType(collectionType)
            .ctx(ctx)
            .build();
    }
}
