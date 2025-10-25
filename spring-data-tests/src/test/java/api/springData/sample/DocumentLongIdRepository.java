package api.springData.sample;

import org.springframework.data.aerospike.repository.AerospikeRepository;

public interface DocumentLongIdRepository extends AerospikeRepository<SampleClasses.DocumentWithLongId, Long> {

}
