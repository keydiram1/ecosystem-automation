package api.springData.sample;

import org.springframework.data.aerospike.repository.AerospikeRepository;

public interface DocumentShortIdRepository extends AerospikeRepository<SampleClasses.DocumentWithShortId, Short> {

}
