package api.springData.core.reactive;

import api.springData.BaseReactiveIntegrationTests;
import api.springData.sample.SampleClasses.CompositeKey;
import api.springData.sample.SampleClasses.DocumentWithCompositeKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns13"})
@Tag("SPRING-DATA-TESTS-1")
public class ReactiveAerospikeTemplateCompositeKeyTests extends BaseReactiveIntegrationTests {

    private DocumentWithCompositeKey document;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        document = new DocumentWithCompositeKey(new CompositeKey(nextId(), 77));
        reactiveTemplate.save(document).block();
    }

    @Test
    public void findById() {
        StepVerifier.create(reactiveTemplate.findById(document.getId(), DocumentWithCompositeKey.class))
            .expectNext(document)
            .verifyComplete();
    }

    @Test
    public void findByIds() {
        DocumentWithCompositeKey document2 = new DocumentWithCompositeKey(new CompositeKey(nextId(), 999));
        reactiveTemplate.save(document2).block();

        List<DocumentWithCompositeKey> actual = reactiveTemplate.findByIds(asList(document.getId(),
                document2.getId()), DocumentWithCompositeKey.class)
            .collectList().block();

        assertThat(actual).containsOnly(document, document2);
    }

    @Test
    public void delete() {
        StepVerifier.create(reactiveTemplate.deleteById(document.getId(), DocumentWithCompositeKey.class))
            .expectNext(true)
            .verifyComplete();
    }

    @Test
    public void exists() {
        StepVerifier.create(reactiveTemplate.exists(document.getId(), DocumentWithCompositeKey.class))
            .expectNext(true)
            .verifyComplete();
    }
}
