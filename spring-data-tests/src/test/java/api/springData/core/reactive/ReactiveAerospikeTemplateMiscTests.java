package api.springData.core.reactive;

import api.springData.BaseReactiveIntegrationTests;
import api.springData.sample.Person;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.aerospike.core.ReactiveAerospikeTemplate;
import org.springframework.data.aerospike.core.WritePolicyBuilder;
import org.springframework.test.context.TestPropertySource;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

/**
 * Tests for different methods in {@link ReactiveAerospikeTemplate}.
 *
 * @author Igor Ermolenko
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns13"})
@Tag("SPRING-DATA-TESTS-1")
@TestPropertySource(properties = {"personSetName=personSetNameReactiveAerospikeTemplateMiscTests"})
public class ReactiveAerospikeTemplateMiscTests extends BaseReactiveIntegrationTests {

    public static final String SET_NAME = "setReactiveAerospikeTemplateMiscTests";

    @Test
    public void execute_shouldTranslateException() {
        Key key = new Key(getNameSpace(), "shouldTranslateException", "reactiveShouldTranslateException");
        Bin bin = new Bin("bin_name", "bin_value");
        StepVerifier.create(reactorClient.add(null, key, bin))
            .expectNext(key)
            .verifyComplete();

        StepVerifier.create(reactiveTemplate.execute(() -> {
                WritePolicy writePolicy = WritePolicyBuilder.builder(reactorClient.getWritePolicyDefault())
                    .recordExistsAction(RecordExistsAction.CREATE_ONLY)
                    .build();
                return reactorClient.add(writePolicy, key, bin).subscribeOn(Schedulers.parallel()).block();
            }))
            .expectError(DuplicateKeyException.class)
            .verify();
    }

    @Test
    public void exists_shouldReturnTrueIfValueIsPresent() {
        Person one = Person.builder().id(id).firstName("tya").emailAddress("gmail.com").build();
        reactiveTemplate.insert(one).subscribeOn(Schedulers.parallel()).block();

        StepVerifier.create(reactiveTemplate.exists(id, Person.class).subscribeOn(Schedulers.parallel()))
            .expectNext(true)
            .verifyComplete();
        reactiveTemplate.delete(one).block();
    }

    @Test
    public void existsWithSetName_shouldReturnTrueIfValueIsPresent() {
        Person one = Person.builder().id(id).firstName("tya").emailAddress("gmail.com").build();
        reactiveTemplate.insert(one, SET_NAME).subscribeOn(Schedulers.parallel()).block();

        StepVerifier.create(reactiveTemplate.exists(id, SET_NAME).subscribeOn(Schedulers.parallel()))
            .expectNext(true)
            .verifyComplete();
        reactiveTemplate.delete(one, SET_NAME).block();
    }

    @Test
    public void exists_shouldReturnFalseIfValueIsAbsent() {
        StepVerifier.create(reactiveTemplate.exists(id, Person.class).subscribeOn(Schedulers.parallel()))
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    public void existsWithSetName_shouldReturnFalseIfValueIsAbsent() {
        StepVerifier.create(reactiveTemplate.exists(id, SET_NAME).subscribeOn(Schedulers.parallel()))
            .expectNext(false)
            .verifyComplete();
    }
}
