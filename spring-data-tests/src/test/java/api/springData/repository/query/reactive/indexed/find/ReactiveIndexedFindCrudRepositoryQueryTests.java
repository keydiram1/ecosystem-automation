package api.springData.repository.query.reactive.indexed.find;

import api.springData.repository.query.reactive.indexed.ReactiveIndexedPersonRepositoryQueryTests;
import org.springframework.data.aerospike.query.model.Index;

import java.util.List;

/**
 * Tests for the CrudRepository queries API.
 */
public class ReactiveIndexedFindCrudRepositoryQueryTests extends ReactiveIndexedPersonRepositoryQueryTests {

    @Override
    protected List<Index> newIndexes() {
        return List.of();
    }

}
