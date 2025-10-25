package api.springData.query.queryEngine.reactive;

import com.aerospike.client.Value;
import com.aerospike.client.query.KeyRecord;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.qualifier.Qualifier;
import org.springframework.data.aerospike.repository.query.Query;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static api.springData.query.queryEngine.QueryEngineTestDataPopulator.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.aerospike.query.FilterOperation.*;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns1"})
@Tag("SPRING-DATA-TESTS-1")
public class ReactiveSelectorTests extends BaseReactiveQueryEngineTests {

    @Test
    public void selectAll() {
        Flux<KeyRecord> flux = reactiveQueryEngine.select(namespace, SET_NAME, null, null);

        StepVerifier.create(flux)
            .expectNextCount(RECORD_COUNT)
            .verifyComplete();
    }

    @Test
    public void selectEndsWith() {
        Qualifier qual1 = Qualifier.builder()
            .setPath("color")
            .setFilterOperation(ENDS_WITH)
            .setValue("e")
            .build();

        Flux<KeyRecord> flux = reactiveQueryEngine.select(namespace, SET_NAME, null, new Query(qual1));
        StepVerifier.create(flux.collectList())
            .expectNextMatches(results -> {
                assertThat(results)
                    .allSatisfy(rec -> assertThat(rec.record.getString("color")).endsWith("e"))
                    .hasSize(queryEngineTestDataPopulator.colourCounts.get(ORANGE)
                        + queryEngineTestDataPopulator.colourCounts.get(BLUE));
                return true;
            }).verifyComplete();
    }

    @Test
    public void selectStartsWith() {
        Qualifier startsWithQual = Qualifier.builder()
            .setPath("color")
            .setFilterOperation(STARTS_WITH)
            .setValue("bl")
            .build();

        Flux<KeyRecord> flux = reactiveQueryEngine.select(namespace, SET_NAME, null, new Query(startsWithQual));
        StepVerifier.create(flux.collectList())
            .expectNextMatches(results -> {
                assertThat(results)
                    .allSatisfy(rec -> assertThat(rec.record.getString("color")).startsWith("bl"))
                    .hasSize(queryEngineTestDataPopulator.colourCounts.get(BLUE));
                return true;
            })
            .verifyComplete();
    }

    @Test
    public void startWithAndEqualIgnoreCaseReturnsAllItems() {
        boolean ignoreCase = true;
        Qualifier qual1 = Qualifier.builder()
            .setPath("color")
            .setFilterOperation(EQ)
            .setIgnoreCase(ignoreCase)
            .setValue("BLUE")
            .build();

        Qualifier qual2 = Qualifier.builder()
            .setPath("name")
            .setFilterOperation(STARTS_WITH)
            .setIgnoreCase(ignoreCase)
            .setValue("NA")
            .build();

        Flux<KeyRecord> flux = reactiveQueryEngine.select(namespace, SET_NAME, null, new Query(Qualifier.and(qual1, qual2)));
        StepVerifier.create(flux)
            .expectNextCount(queryEngineTestDataPopulator.colourCounts.get("blue"))
            .verifyComplete();
    }

    @Test
    public void equalIgnoreCaseReturnsNoItemsIfNoneMatched() {
        boolean ignoreCase = false;
        Qualifier qual1 = Qualifier.builder()
            .setPath("color")
            .setFilterOperation(EQ)
            .setIgnoreCase(ignoreCase)
            .setValue("BLUE")
            .build();

        Flux<KeyRecord> flux = reactiveQueryEngine.select(namespace, SET_NAME, null, new Query(qual1));
        StepVerifier.create(flux)
            .expectNextCount(0)
            .verifyComplete();
    }

    @Test
    public void startWithIgnoreCaseReturnsNoItemsIfNoneMatched() {
        boolean ignoreCase = false;
        Qualifier qual1 = Qualifier.builder()
            .setPath("name")
            .setFilterOperation(STARTS_WITH)
            .setIgnoreCase(ignoreCase)
            .setValue("NA")
            .build();

        Flux<KeyRecord> flux = reactiveQueryEngine.select(namespace, SET_NAME, null, new Query(qual1));
        StepVerifier.create(flux)
            .expectNextCount(0)
            .verifyComplete();
    }

    @Test
    public void stringEqualIgnoreCaseWorksOnUnindexedBin() {
        boolean ignoreCase = true;
        String expectedColor = "blue";

        Qualifier caseInsensitiveQual = Qualifier.builder()
            .setPath("color")
            .setFilterOperation(EQ)
            .setIgnoreCase(ignoreCase)
            .setValue("BlUe")
            .build();

        Flux<KeyRecord> flux = reactiveQueryEngine.select(namespace, SET_NAME, null, new Query(caseInsensitiveQual));
        StepVerifier.create(flux.collectList())
            .expectNextMatches(results -> {
                assertThat(results)
                    .allSatisfy(rec -> assertThat(rec.record.getString("color")).isEqualTo(expectedColor))
                    .hasSize(queryEngineTestDataPopulator.colourCounts.get(BLUE));
                return true;
            })
            .verifyComplete();
    }

    @Test
    public void stringEqualIgnoreCaseWorksRequiresFullMatch() {
        boolean ignoreCase = true;
        Qualifier caseInsensitiveQual = Qualifier.builder()
            .setPath("color")
            .setFilterOperation(EQ)
            .setIgnoreCase(ignoreCase)
            .setValue("lue")
            .build();

        Flux<KeyRecord> flux = reactiveQueryEngine.select(namespace, SET_NAME, null, new Query(caseInsensitiveQual));

        StepVerifier.create(flux)
            .expectNextCount(0)
            .verifyComplete();
    }

    @Test
    public void selectWithGeoWithin() {
        double lon = -122.0;
        double lat = 37.5;
        double radius = 50000.0;
        String rgnstr = String.format("{ \"type\": \"AeroCircle\", "
                + "\"coordinates\": [[%.8f, %.8f], %f] }",
            lon, lat, radius);
        Qualifier qual1 = Qualifier.builder()
            .setPath(GEO_BIN_NAME)
            .setFilterOperation(GEO_WITHIN)
            .setValue(Value.getAsGeoJSON(rgnstr))
            .build();

        Flux<KeyRecord> flux = reactiveQueryEngine.select(namespace, GEO_SET, null, new Query(qual1));
        StepVerifier.create(flux.collectList())
            .expectNextMatches(results -> {
                assertThat(results)
                    .allSatisfy(rec -> assertThat(rec.record.generation).isPositive())
                    .isNotEmpty();
                return true;
            })
            .verifyComplete();
    }
}
