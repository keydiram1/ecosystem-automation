package api.springData.config;

import api.springData.BlockingAerospikeTestOperations;
import api.springData.sample.ContactRepository;
import api.springData.sample.CustomerRepository;
import api.springData.sample.SampleClasses;
import api.springData.utility.AdditionalAerospikeTestOperations;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.policy.ClientPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.aerospike.config.AbstractAerospikeDataConfiguration;
import org.springframework.data.aerospike.core.AerospikeTemplate;
import org.springframework.data.aerospike.query.cache.IndexInfoParser;
import org.springframework.data.aerospike.repository.config.EnableAerospikeRepositories;
import org.springframework.data.aerospike.server.version.ServerVersionSupport;
import org.testcontainers.containers.GenericContainer;

import java.util.Arrays;
import java.util.List;

/**
 * @author Peter Milne
 * @author Jean Mercier
 */
@EnableAerospikeRepositories(basePackageClasses = {ContactRepository.class, CustomerRepository.class})
public class BlockingTestConfig extends AbstractAerospikeDataConfiguration {

    @Autowired
    Environment env;

    @Override
    protected List<Object> customConverters() {
        return Arrays.asList(
                SampleClasses.CompositeKey.CompositeKeyToStringConverter.INSTANCE,
                SampleClasses.CompositeKey.StringToCompositeKeyConverter.INSTANCE
        );
    }

    @Override
    protected ClientPolicy getClientPolicy() {
        ClientPolicy clientPolicy = super.getClientPolicy(); // applying default values first
        clientPolicy.readPolicyDefault.maxRetries = 3;
        clientPolicy.writePolicyDefault.totalTimeout = 1000;
        clientPolicy.infoPolicyDefault.timeout = 1000;
        clientPolicy.user = "tester";
        clientPolicy.password = "psw";
        return clientPolicy;
    }

    @Bean
    public AdditionalAerospikeTestOperations aerospikeOperations(AerospikeTemplate template, IAerospikeClient client,
                                                                 GenericContainer<?> aerospike,
                                                                 ServerVersionSupport serverVersionSupport) {
        return new BlockingAerospikeTestOperations(new IndexInfoParser(), template, client, aerospike,
                serverVersionSupport);
    }
}
