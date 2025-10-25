/*
 * Copyright 2012-2019 Aerospike, Inc.
 *
 * Portions may be licensed to Aerospike, Inc. under one or more contributor
 * license agreements WHICH ARE COMPATIBLE WITH THE APACHE LICENSE, VERSION 2.0.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package api.springData.query.queryEngine.reactive;

import com.aerospike.client.Value;
import com.aerospike.client.query.IndexType;
import com.aerospike.client.query.KeyRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.FilterOperation;
import org.springframework.data.aerospike.query.qualifier.Qualifier;
import org.springframework.data.aerospike.repository.query.Query;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;

import static api.springData.query.queryEngine.QueryEngineTestDataPopulator.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.aerospike.query.FilterOperation.*;

/*
 * These tests generate qualifiers on indexed bins.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns3"})
@Tag("SPRING-DATA-TESTS-1")
public class ReactiveIndexedQualifierTests extends BaseReactiveQueryEngineTests {

    @AfterEach
    public void assertNoScans() {
        additionalAerospikeTestOperations.assertNoScansForSet(INDEXED_SET_NAME);
    }

    @Test
    public void selectOnIndexedLTQualifier() {
        // Ages range from 25 -> 29. We expected to only get back values with age < 26
        withIndex(namespace, INDEXED_SET_NAME, "age_index", "age", IndexType.NUMERIC, () -> {
            Qualifier qualifier = Qualifier.builder()
                .setPath("age")
                .setFilterOperation(LT)
                .setValue(26)
                .build();

            Flux<KeyRecord> flux = reactiveQueryEngine.select(namespace, INDEXED_SET_NAME, null, new Query(qualifier));

            StepVerifier.create(flux.collectList())
                .expectNextMatches(results -> {
                    assertThat(results)
                        .filteredOn(keyRecord -> {
                            int age = keyRecord.record.getInt("age");
                            assertThat(age).isLessThan(26);
                            return age == 25;
                        })
                        .hasSize(queryEngineTestDataPopulator.ageCount.get(25));
                    return true;
                })
                .verifyComplete();
        });
    }

    /*
     * If we use a qualifier on an indexed bin, The QueryEngine will generate a Filter. Verify that a LTEQ Filter
     * Operation
     * Generates the correct Filter.Range() filter.
     */
    @Test
    public void selectOnIndexedLTEQQualifier() {
        withIndex(namespace, INDEXED_SET_NAME, "age_index", "age", IndexType.NUMERIC, () -> {
            // Ages range from 25 -> 29. We expected to only get back values with age <= 26
            Qualifier qualifier = Qualifier.builder()
                .setPath("age")
                .setFilterOperation(LTEQ)
                .setValue(26)
                .build();

            Flux<KeyRecord> flux = reactiveQueryEngine.select(namespace, INDEXED_SET_NAME, null, new Query(qualifier));

            StepVerifier.create(flux.collectList())
                .expectNextMatches(results -> {
                    AtomicInteger age25Count = new AtomicInteger();
                    AtomicInteger age26Count = new AtomicInteger();
                    results.forEach(keyRecord -> {
                        int age = keyRecord.record.getInt("age");
                        assertThat(age).isLessThanOrEqualTo(26);

                        if (age == 25) {
                            age25Count.incrementAndGet();
                        } else if (age == 26) {
                            age26Count.incrementAndGet();
                        }
                    });
                    assertThat(age25Count.get()).isEqualTo(queryEngineTestDataPopulator.ageCount.get(25));
                    assertThat(age26Count.get()).isEqualTo(queryEngineTestDataPopulator.ageCount.get(26));
                    return true;
                })
                .verifyComplete();
        });
    }

    @Test
    public void selectOnIndexedNumericEQQualifier() {

        withIndex(namespace, INDEXED_SET_NAME, "age_index", "age", IndexType.NUMERIC, () -> {
            // Ages range from 25 -> 29. We expected to only get back values with age == 26
            Qualifier qualifier = Qualifier.builder()
                .setPath("age")
                .setFilterOperation(EQ)
                .setValue(26)
                .build();

            Flux<KeyRecord> flux = reactiveQueryEngine.select(namespace, INDEXED_SET_NAME, null, new Query(qualifier));
            StepVerifier.create(flux.collectList())
                .expectNextMatches(results -> {
                    assertThat(results)
                        .isNotEmpty()
                        .allSatisfy(rec -> assertThat(rec.record.getInt("age")).isEqualTo(26))
                        .hasSize(queryEngineTestDataPopulator.ageCount.get(26));
                    return true;
                })
                .verifyComplete();
        });
    }

    @Test
    public void selectOnIndexedGTEQQualifier() {

        withIndex(namespace, INDEXED_SET_NAME, "age_index", "age", IndexType.NUMERIC, () -> {
            // Ages range from 25 -> 29. We expected to only get back values with age >= 28
            Qualifier qualifier = Qualifier.builder()
                .setPath("age")
                .setFilterOperation(GTEQ)
                .setValue(28)
                .build();

            Flux<KeyRecord> flux = reactiveQueryEngine.select(namespace, INDEXED_SET_NAME, null, new Query(qualifier));

            StepVerifier.create(flux.collectList())
                .expectNextMatches(results -> {
                    AtomicInteger age28Count = new AtomicInteger();
                    AtomicInteger age29Count = new AtomicInteger();
                    results.forEach(keyRecord -> {
                        int age = keyRecord.record.getInt("age");
                        assertThat(age).isGreaterThanOrEqualTo(28);

                        if (age == 28) {
                            age28Count.incrementAndGet();
                        } else if (age == 29) {
                            age29Count.incrementAndGet();
                        }
                    });
                    assertThat(age28Count.get()).isEqualTo(queryEngineTestDataPopulator.ageCount.get(28));
                    assertThat(age29Count.get()).isEqualTo(queryEngineTestDataPopulator.ageCount.get(29));
                    return true;
                })
                .verifyComplete();
        });
    }

    @Test
    public void selectOnIndexedGTQualifier() {

        withIndex(namespace, INDEXED_SET_NAME, "age_index", "age", IndexType.NUMERIC, () -> {
            // Ages range from 25 -> 29. We expected to only get back values with age > 28 or equivalently == 29
            Qualifier qualifier = Qualifier.builder()
                .setPath("age")
                .setFilterOperation(GT)
                .setValue(28)
                .build();

            Flux<KeyRecord> flux = reactiveQueryEngine.select(namespace, INDEXED_SET_NAME, null, new Query(qualifier));

            StepVerifier.create(flux.collectList())
                .expectNextMatches(results -> {
                    assertThat(results)
                        .isNotEmpty()
                        .allSatisfy(rec -> assertThat(rec.record.getInt("age")).isEqualTo(29))
                        .hasSize(queryEngineTestDataPopulator.ageCount.get(29));
                    return true;
                })
                .verifyComplete();
        });
    }

    @Test
    public void selectOnIndexedStringEQQualifier() {
        withIndex(namespace, INDEXED_SET_NAME, "color_index", "color", IndexType.STRING, () -> {
            Qualifier qualifier = Qualifier.builder()
                .setPath("color")
                .setFilterOperation(EQ)
                .setValue(ORANGE)
                .build();

            Flux<KeyRecord> flux = reactiveQueryEngine.select(namespace, INDEXED_SET_NAME, null, new Query(qualifier));

            StepVerifier.create(flux.collectList())
                .expectNextMatches(results -> {
                    assertThat(results)
                        .isNotEmpty()
                        .allSatisfy(rec -> assertThat(rec.record.getString("color")).isEqualTo(ORANGE))
                        .hasSize(queryEngineTestDataPopulator.colourCounts.get(ORANGE));
                    return true;
                })
                .verifyComplete();
        });
    }

    @Test
    public void selectWithBlueColorQuery() {
        withIndex(namespace, INDEXED_SET_NAME, "age_index", "age", IndexType.NUMERIC, () -> {
            Qualifier qual1 = Qualifier.builder()
                .setPath("color")
                .setFilterOperation(FilterOperation.EQ)
                .setValue(BLUE)
                .build();

            Flux<KeyRecord> flux = reactiveQueryEngine.select(namespace, INDEXED_SET_NAME, new Query(qual1));
            StepVerifier.create(flux.collectList())
                .expectNextMatches(results -> {
                    assertThat(results)
                        .allSatisfy(rec -> {
                            assertThat(rec.record.getString("color")).isEqualTo(BLUE);
                            assertThat(rec.record.getInt("age")).isBetween(25, 29);
                        })
                        .hasSize(queryEngineTestDataPopulator.colourCounts.get(BLUE));
                    return true;
                })
                .verifyComplete();
        });
    }

    @Test
    public void selectWithQualifiersOnly() {
        withIndex(namespace, INDEXED_SET_NAME, "color_index", "color", IndexType.STRING, () -> {
            Qualifier qual1 = Qualifier.builder()
                .setPath("color")
                .setFilterOperation(FilterOperation.EQ)
                .setValue(GREEN)
                .build();
            Qualifier qual2 = Qualifier.builder()
                .setPath("age")
                .setFilterOperation(FilterOperation.BETWEEN)
                .setValue(28)
                .setSecondValue(29)
                .build();

            Flux<KeyRecord> flux = reactiveQueryEngine.select(namespace, INDEXED_SET_NAME, null,
                new Query(Qualifier.and(qual1, qual2)));

            StepVerifier.create(flux.collectList())
                .expectNextMatches(results -> {
                    results.forEach(keyRecord -> assertThat(results)
                        .allSatisfy(rec -> {
                            assertThat(rec.record.getString("color")).isEqualTo(GREEN);
                            assertThat(rec.record.getInt("age")).isBetween(28, 29);
                        }));
                    return true;
                })
                .verifyComplete();
        });
    }

    @Test
    public void selectWithGeoWithin() {
        withIndex(namespace, INDEXED_GEO_SET, "geo_index", GEO_BIN_NAME, IndexType.GEO2DSPHERE, () -> {
            double lon = -122.0;
            double lat = 37.5;
            double radius = 50000.0;
            String rgnstr = String.format("{ \"type\": \"AeroCircle\", "
                    + "\"coordinates\": [[%.8f, %.8f], %f] }",
                lon, lat, radius);
            Qualifier qualifier = Qualifier.builder()
                .setPath(GEO_BIN_NAME)
                .setFilterOperation(GEO_WITHIN)
                .setValue(Value.getAsGeoJSON(rgnstr))
                .build();

            Flux<KeyRecord> flux = reactiveQueryEngine.select(namespace, INDEXED_GEO_SET, null, new Query(qualifier));

            StepVerifier.create(flux.collectList())
                .expectNextMatches(results -> {
                    assertThat(results)
                        .isNotEmpty()
                        .allSatisfy(rec -> assertThat(rec.record.generation).isGreaterThanOrEqualTo(1));
                    return true;
                })
                .verifyComplete();
            additionalAerospikeTestOperations.assertNoScansForSet(INDEXED_GEO_SET);
        });
    }
}
