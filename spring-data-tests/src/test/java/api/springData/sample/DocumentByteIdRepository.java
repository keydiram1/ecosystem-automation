package api.springData.sample;

import org.springframework.data.aerospike.repository.AerospikeRepository;

public interface DocumentByteIdRepository extends AerospikeRepository<SampleClasses.DocumentWithByteId, Byte> {

}
