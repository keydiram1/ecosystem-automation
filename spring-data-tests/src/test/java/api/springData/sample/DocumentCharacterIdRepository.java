package api.springData.sample;

import org.springframework.data.aerospike.repository.AerospikeRepository;

public interface DocumentCharacterIdRepository extends AerospikeRepository<SampleClasses.DocumentWithCharacterId, Character> {

}
