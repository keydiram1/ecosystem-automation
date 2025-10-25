/*
 * Copyright 2012-2020 Aerospike, Inc.
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
package api.springData.query.queryEngine.blocking;

import api.springData.utility.CollectionUtils;
import com.aerospike.client.Value;
import com.aerospike.client.query.IndexType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.FilterOperation;
import org.springframework.data.aerospike.query.KeyRecordIterator;
import org.springframework.data.aerospike.query.qualifier.Qualifier;
import org.springframework.data.aerospike.repository.query.Query;
import org.springframework.test.context.TestPropertySource;
import utils.AutoUtils;

import java.util.Map;
import java.util.stream.Collectors;

import static api.springData.query.queryEngine.QueryEngineTestDataPopulator.*;
import static api.springData.utility.CollectionUtils.countingInt;
import static org.assertj.core.api.Assertions.assertThat;

/*
 * These tests generate qualifiers on indexed bins.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns7"})
@Tag("SPRING-DATA-TESTS-1")
class IndexedQualifierTests extends BaseQueryEngineTests {

    @AfterEach
    public void assertNoScans() {
        try {
            additionalAerospikeTestOperations.assertNoScansForSet(INDEXED_SET_NAME);
        } catch (Exception e) {
            AutoUtils.sleep(2000);
            additionalAerospikeTestOperations.assertNoScansForSet(INDEXED_SET_NAME);
        }
    }

    @Test
    void selectOnIndexedLTQualifier() {
        withIndex(namespace, INDEXED_SET_NAME, "age_index", "age", IndexType.NUMERIC, () -> {
            // Ages range from 25 -> 29. We expected to only get back values with age < 26
            Qualifier qualifier = Qualifier.builder()
                .setPath("age")
                .setFilterOperation(FilterOperation.LT)
                .setValue(26)
                .build();

            KeyRecordIterator iterator = queryEngine.select(namespace, INDEXED_SET_NAME, null,
                new Query(qualifier));

            assertThat(iterator)
                .toIterable()
                .isNotEmpty()
                .allSatisfy(rec -> assertThat(rec.record.getInt("age")).isLessThan(26))
                .hasSize(queryEngineTestDataPopulator.ageCount.get(25));
        });
    }

    @Test
    void selectOnIndexedLTEQQualifier() {
        withIndex(namespace, INDEXED_SET_NAME, "age_index", "age", IndexType.NUMERIC, () -> {
            // Ages range from 25 -> 29. We expected to only get back values with age <= 26
            Qualifier qualifier = Qualifier.builder()
                .setPath("age")
                .setFilterOperation(FilterOperation.LTEQ)
                .setValue(26)
                .build();

            KeyRecordIterator iterator = queryEngine.select(namespace, INDEXED_SET_NAME, null,
                new Query(qualifier));

            Map<Integer, Integer> ageCount = CollectionUtils.toStream(iterator)
                .map(rec -> rec.record.getInt("age"))
                .collect(Collectors.groupingBy(k -> k, countingInt()));
            assertThat(ageCount.keySet())
                .isNotEmpty()
                .allSatisfy(age -> assertThat(age).isLessThanOrEqualTo(26));
            assertThat(ageCount.get(25)).isEqualTo(queryEngineTestDataPopulator.ageCount.get(25));
            assertThat(ageCount.get(26)).isEqualTo(queryEngineTestDataPopulator.ageCount.get(26));
        });
    }

    @Test
    void selectOnIndexedNumericEQQualifier() {
        withIndex(namespace, INDEXED_SET_NAME, "age_index", "age", IndexType.NUMERIC, () -> {
            // Ages range from 25 -> 29. We expected to only get back values with age == 26
            Qualifier qualifier = Qualifier.builder()
                .setPath("age")
                .setFilterOperation(FilterOperation.EQ)
                .setValue(26)
                .build();

            KeyRecordIterator iterator = queryEngine.select(namespace, INDEXED_SET_NAME, null,
                new Query(qualifier));

            assertThat(iterator)
                .toIterable()
                .isNotEmpty()
                .allSatisfy(rec -> assertThat(rec.record.getInt("age")).isEqualTo(26))
                .hasSize(queryEngineTestDataPopulator.ageCount.get(26));
        });
    }

    @Test
    void selectWithBlueColorQuery() {
        withIndex(namespace, INDEXED_SET_NAME, "age_index", "age", IndexType.NUMERIC, () -> {
            Qualifier qual1 = Qualifier.builder()
                .setPath("color")
                .setFilterOperation(FilterOperation.EQ)
                .setValue(BLUE)
                .build();

            KeyRecordIterator it = queryEngine.select(namespace, INDEXED_SET_NAME, new Query(qual1));

            assertThat(it)
                .toIterable()
                .isNotEmpty()
                .allSatisfy(rec -> assertThat(rec.record.getInt("age")).isBetween(25, 29))
                .hasSize(queryEngineTestDataPopulator.colourCounts.get(BLUE));
        });
    }

    @Test
    void selectOnIndexedGTEQQualifier() {
        withIndex(namespace, INDEXED_SET_NAME, "age_index", "age", IndexType.NUMERIC, () -> {
            // Ages range from 25 -> 29. We expected to only get back values with age >= 28
            Qualifier qualifier = Qualifier.builder()
                .setPath("age")
                .setFilterOperation(FilterOperation.GTEQ)
                .setValue(28)
                .build();

            KeyRecordIterator iterator = queryEngine.select(namespace, INDEXED_SET_NAME, null,
                new Query(qualifier));

            Map<Integer, Integer> ageCount = CollectionUtils.toStream(iterator)
                .map(rec -> rec.record.getInt("age"))
                .collect(Collectors.groupingBy(k -> k, countingInt()));
            assertThat(ageCount.keySet())
                .isNotEmpty()
                .allSatisfy(age -> assertThat(age).isGreaterThanOrEqualTo(28));
            assertThat(ageCount.get(28)).isEqualTo(queryEngineTestDataPopulator.ageCount.get(28));
            assertThat(ageCount.get(29)).isEqualTo(queryEngineTestDataPopulator.ageCount.get(29));
        });
    }

    @Test
    void selectOnIndexedGTQualifier() {
        withIndex(namespace, INDEXED_SET_NAME, "age_index", "age", IndexType.NUMERIC, () -> {
            Qualifier qualifier = Qualifier.builder()
                .setPath("age")
                .setFilterOperation(FilterOperation.GT)
                .setValue(28)
                .build();

            KeyRecordIterator iterator = queryEngine.select(namespace, INDEXED_SET_NAME, null,
                new Query(qualifier));

            assertThat(iterator)
                .toIterable()
                .isNotEmpty()
                .allSatisfy(rec -> assertThat(rec.record.getInt("age")).isEqualTo(29))
                .hasSize(queryEngineTestDataPopulator.ageCount.get(29));
        });
    }

    @Test
    void selectOnIndexedStringEQQualifier() {
        withIndex(namespace, INDEXED_SET_NAME, "color_index", "color", IndexType.STRING, () -> {
            Qualifier qualifier = Qualifier.builder()
                .setPath("color")
                .setFilterOperation(FilterOperation.EQ)
                .setValue(ORANGE)
                .build();

            KeyRecordIterator iterator = queryEngine.select(namespace, INDEXED_SET_NAME, null,
                new Query(qualifier));

            assertThat(iterator)
                .toIterable()
                .isNotEmpty()
                .allSatisfy(rec -> assertThat(rec.record.getString("color")).isEqualTo(ORANGE))
                .hasSize(queryEngineTestDataPopulator.colourCounts.get(ORANGE));
        });
    }

    @Test
    void selectWithGeoWithin() {
        if (serverVersionSupport.isDropCreateBehaviorUpdated()) {
            withIndex(namespace, INDEXED_GEO_SET, "geo_index", GEO_BIN_NAME, IndexType.GEO2DSPHERE, () -> {
                double lon = -122.0;
                double lat = 37.5;
                double radius = 50000.0;
                String rgnstr = String.format("{ \"type\": \"AeroCircle\", "
                        + "\"coordinates\": [[%.8f, %.8f], %f] }",
                    lon, lat, radius);

                Qualifier qualifier = Qualifier.builder()
                    .setPath(GEO_BIN_NAME)
                    .setFilterOperation(FilterOperation.GEO_WITHIN)
                    .setValue(Value.getAsGeoJSON(rgnstr))
                    .build();

                KeyRecordIterator iterator = queryEngine.select(namespace, INDEXED_GEO_SET, null,
                    new Query(qualifier));

                assertThat(iterator).toIterable()
                    .isNotEmpty()
                    .allSatisfy(rec -> assertThat(rec.record.generation).isPositive());
                additionalAerospikeTestOperations.assertNoScansForSet(INDEXED_GEO_SET);
            });
        }
    }

    @Test
    void selectWithoutQuery() {
        withIndex(namespace, INDEXED_SET_NAME, "age_index", "age", IndexType.NUMERIC, () -> {
            KeyRecordIterator it = queryEngine.select(namespace, INDEXED_SET_NAME, null);

            Map<Integer, Integer> ageCount = CollectionUtils.toStream(it)
                .map(rec -> rec.record.getInt("age"))
                .collect(Collectors.groupingBy(k -> k, countingInt()));
            assertThat(ageCount.keySet())
                .isNotEmpty()
                .allSatisfy(age -> assertThat(age).isIn((Object[]) AGES));
            assertThat(ageCount.get(28)).isEqualTo(queryEngineTestDataPopulator.ageCount.get(28));
            assertThat(ageCount.get(29)).isEqualTo(queryEngineTestDataPopulator.ageCount.get(29));
        });
    }

    @Test
    void selectWithQualifiersOnly() {
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

        withIndex(namespace, INDEXED_SET_NAME, "age_index", "age", IndexType.NUMERIC, () -> {
            KeyRecordIterator it = queryEngine.select(namespace, INDEXED_SET_NAME, null,
                new Query(Qualifier.and(qual1, qual2)));

            assertThat(it).toIterable()
                .isNotEmpty()
                .allSatisfy(rec -> assertThat(rec.record.getString("color")).isEqualTo(GREEN))
                .allSatisfy(rec -> assertThat(rec.record.getInt("age")).isBetween(28, 29));
        });
    }

    @Test
    void selectWithAndQualifier() {
        Qualifier colorIsGreen = Qualifier.builder()
            .setPath("color")
            .setFilterOperation(FilterOperation.EQ)
            .setValue(GREEN)
            .build();
        Qualifier ageBetween28And29 = Qualifier.builder()
            .setPath("age")
            .setFilterOperation(FilterOperation.BETWEEN)
            .setValue(28)
            .setSecondValue(29)
            .build();

        tryCreateIndex(namespace, INDEXED_SET_NAME, "age_index", "age", IndexType.NUMERIC);
        tryCreateIndex(namespace, INDEXED_SET_NAME, "color_index", "color", IndexType.STRING);
        try {
            Qualifier qualifier = Qualifier.and(colorIsGreen, ageBetween28And29);

            KeyRecordIterator it = queryEngine.select(namespace, INDEXED_SET_NAME, null, new Query(qualifier));

            assertThat(it).toIterable().isNotEmpty()
                .allSatisfy(rec -> {
                    assertThat(rec.record.getInt("age")).isBetween(28, 29);
                    assertThat(rec.record.getString("color")).isEqualTo(GREEN);
                });
        } finally {
            tryDropIndex(INDEXED_SET_NAME, "age_index");
            tryDropIndex(INDEXED_SET_NAME, "color_index");
        }
    }
}
