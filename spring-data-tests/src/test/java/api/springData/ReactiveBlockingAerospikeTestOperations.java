package api.springData;

import api.springData.sample.Customer;
import api.springData.sample.Person;
import api.springData.utility.AdditionalAerospikeTestOperations;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.IAerospikeClient;
import org.springframework.data.aerospike.core.ReactiveAerospikeTemplate;
import org.springframework.data.aerospike.query.cache.IndexInfoParser;
import org.springframework.data.aerospike.repository.ReactiveAerospikeRepository;
import org.springframework.data.aerospike.server.version.ServerVersionSupport;
import org.testcontainers.containers.GenericContainer;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static api.springData.utility.AerospikeUniqueId.nextId;

public class ReactiveBlockingAerospikeTestOperations extends AdditionalAerospikeTestOperations {

    private final ReactiveAerospikeTemplate template;
    private final ServerVersionSupport serverVersionSupport;

    public ReactiveBlockingAerospikeTestOperations(IndexInfoParser indexInfoParser,
                                                   IAerospikeClient client, GenericContainer<?> aerospike,
                                                   ReactiveAerospikeTemplate reactiveAerospikeTemplate,
                                                   ServerVersionSupport serverVersionSupport) {
        super(indexInfoParser, client, serverVersionSupport, reactiveAerospikeTemplate, aerospike);
        this.template = reactiveAerospikeTemplate;
        this.serverVersionSupport = serverVersionSupport;
    }

    @Override
    protected boolean isEntityClassSetEmpty(Class<?> clazz) {
        return Boolean.FALSE.equals(template.findAll(clazz).hasElements().block());
    }

    @Override
    protected void truncateSetOfEntityClass(Class<?> clazz) {
        template.deleteAll(clazz).block();
    }

    @Override
    protected boolean isSetEmpty(Class<?> clazz, String setName) {
        return Boolean.FALSE.equals(template.findAll(clazz, setName).hasElements().block());
    }

    @Override
    protected void truncateSet(String setName) {
        template.deleteAll(setName).block();
    }

    @Override
    protected String getNamespace() {
        return template.getNamespace();
    }

    @Override
    protected String getSetName(Class<?> clazz) {
        return template.getSetName(clazz);
    }

    public List<Customer> saveGeneratedCustomers(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> Customer.builder().id(nextId())
                .firstName("firstName" + i)
                .lastName("lastName")
                .build())
            .peek(document -> template.save(document).block())
            .collect(Collectors.toList());
    }

    public List<Person> saveGeneratedPersons(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> Person.builder().id(nextId())
                .firstName("firstName" + i)
                .emailAddress("mail.com")
                .build())
            .peek(document -> template.save(document).block())
            .collect(Collectors.toList());
    }

    public <T> void deleteAll(ReactiveAerospikeRepository<T, ?> repository, Collection<T> entities) {
        // batch write operations are supported starting with Server version 6.0+
        try {
            repository.deleteAll(entities).block();
        } catch (AerospikeException.BatchRecordArray ignored) {
            // KEY_NOT_FOUND ResultCode causes exception if there are no entities
        }
    }

    public <T> void saveAll(ReactiveAerospikeRepository<T, ?> repository, Collection<T> entities) {
        // batch write operations are supported starting with Server version 6.0+
        repository.saveAll(entities).blockLast();
    }
}
