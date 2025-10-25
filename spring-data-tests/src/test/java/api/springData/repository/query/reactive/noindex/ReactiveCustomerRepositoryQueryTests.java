package api.springData.repository.query.reactive.noindex;

import api.springData.BaseReactiveIntegrationTests;
import api.springData.sample.Customer;
import api.springData.sample.ReactiveCustomerNegativeTestsRepository;
import api.springData.sample.ReactiveCustomerRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static api.springData.utility.AerospikeUniqueId.nextId;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReactiveCustomerRepositoryQueryTests extends BaseReactiveIntegrationTests {

    protected static final Customer homer = Customer.builder()
        .id(nextId()).firstName("Homer").lastName("Simpson").age(42).group('a').build();
    protected static final Customer marge = Customer.builder()
        .id(nextId()).firstName("Marge").lastName("Simpson").age(39).group('b').build();
    protected static final Customer bart = Customer.builder()
        .id(nextId()).firstName("Bart").lastName("Simpson").age(10).group('b').build();
    protected static final Customer lisa = Customer.builder()
        .id(nextId()).firstName("Lisa").lastName("Simpson").age(8).build();
    protected static final Customer maggie = Customer.builder()
        .id(nextId()).firstName("Maggie").lastName("Simpson").age(1).build();
    protected static final Customer matt = Customer.builder()
        .id(nextId()).firstName("Matt").lastName("Groening").age(65).group('c').build();
    protected static final Customer fry = Customer.builder()
        .id(nextId()).firstName("Philip J.").lastName("Fry").age(1029).build();
    protected static final Customer leela = Customer.builder().
        id(nextId()).firstName("Leela").lastName("Turanga").age(29).build();

    protected static final List<Customer> allCustomers = List.of(homer, marge, bart, lisa, maggie, matt, fry, leela);

    @Autowired
    protected ReactiveCustomerRepository reactiveRepository;
    @Autowired
    protected ReactiveCustomerNegativeTestsRepository negativeTestsReactiveRepository;

    @BeforeAll
    void beforeEach() {
        reactiveBlockingAerospikeTestOperations.deleteAllAndVerify(Customer.class);
        reactiveBlockingAerospikeTestOperations.saveAll(reactiveRepository, allCustomers);
    }

    @AfterAll
    void afterAll() {
        reactiveBlockingAerospikeTestOperations.deleteAll(reactiveRepository, allCustomers);
    }
}
