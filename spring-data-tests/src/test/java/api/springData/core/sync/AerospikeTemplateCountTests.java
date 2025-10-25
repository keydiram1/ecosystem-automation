/*
 * Copyright 2019 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package api.springData.core.sync;

import api.springData.BaseBlockingIntegrationTests;
import api.springData.sample.Person;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.data.aerospike.query.FilterOperation;
import org.springframework.data.aerospike.query.qualifier.Qualifier;
import org.springframework.data.aerospike.repository.query.Query;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns7"})
@Tag("SPRING-DATA-TESTS-1")
@TestPropertySource(properties = {"personSetName=personSetNameAerospikeTemplateCountTests"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AerospikeTemplateCountTests extends BaseBlockingIntegrationTests {

    public static final String SET_NAME = "set";

    @BeforeAll
    public void beforeAll() {
        template.refreshIndexesCache();
    }

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        additionalAerospikeTestOperations.deleteAllAndVerify(Person.class);
        additionalAerospikeTestOperations.deleteAllAndVerify(Person.class, SET_NAME);
    }

    @Test
    public void countFindsAllItemsByGivenCriteria() {
        template.insert(new Person(id, "vasili", 50));
        String id2 = nextId();
        template.insert(new Person(id2, "vasili", 51));
        String id3 = nextId();
        template.insert(new Person(id3, "vasili", 52));
        String id4 = nextId();
        template.insert(new Person(id4, "petya", 52));

        long vasyaCount = template.count(
            new Query(Qualifier.builder()
                .setFilterOperation(FilterOperation.EQ)
                .setPath("firstName")
                .setValue("vasili")
                .build()
            ),
            Person.class
        );

        assertThat(vasyaCount).isEqualTo(3);

        Qualifier qbIs1 = Qualifier.builder()
            .setFilterOperation(FilterOperation.EQ)
            .setPath("firstName")
            .setValue("vasili")
            .build();

        Qualifier qbIs2 = Qualifier.builder()
            .setFilterOperation(FilterOperation.EQ)
            .setPath("age")
            .setValue(51)
            .build();

        long vasya51Count = template.count(
            new Query(Qualifier.and(qbIs1, qbIs2)), Person.class
        );

        assertThat(vasya51Count).isEqualTo(1);

        long petyaCount = template.count(
            new Query(Qualifier.builder()
                .setFilterOperation(FilterOperation.EQ)
                .setPath("firstName")
                .setValue("petya")
                .build()
            ),
            Person.class
        );

        assertThat(petyaCount).isEqualTo(1);

        template.delete(template.findById(id, Person.class));
        template.delete(template.findById(id2, Person.class));
        template.delete(template.findById(id3, Person.class));
        template.delete(template.findById(id4, Person.class));
    }

    @Test
    public void countFindsAllItemsByGivenCriteriaAndRespectsIgnoreCase() {
        template.insert(new Person(id, "VaSili", 50));
        String id2 = nextId();
        template.insert(new Person(id2, "vasILI", 51));
        String id3 = nextId();
        template.insert(new Person(id3, "vasili", 52));

        Query query1 = new Query(Qualifier.builder()
            .setPath("firstName")
            .setValue("vas")
            .setFilterOperation(FilterOperation.STARTS_WITH)
            .setIgnoreCase(true)
            .build()
        );
        assertThat(template.count(query1, Person.class)).isEqualTo(3);

        Query query2 = new Query(Qualifier.builder()
            .setPath("firstName")
            .setValue("VaS")
            .setFilterOperation(FilterOperation.STARTS_WITH)
            .setIgnoreCase(false)
            .build()
        );

        assertThat(template.count(query2, Person.class)).isEqualTo(1);

        assertThat(template.count(null, Person.class)).isEqualTo(3);

        template.delete(template.findById(id, Person.class));
        template.delete(template.findById(id2, Person.class));
        template.delete(template.findById(id3, Person.class));
    }

    @Test
    public void countReturnsZeroIfNoDocumentsByProvidedCriteriaIsFound() {
        Query query1 = new Query
            (Qualifier.builder()
                .setPath("firstName")
                .setValue("nastyushka")
                .setFilterOperation(FilterOperation.STARTS_WITH)
                .build()
            );

        long count = template.count(query1, Person.class);

        assertThat(count).isZero();
    }

    @Test
    public void countRejectsNullEntityClass() {
        assertThatThrownBy(() -> template.count(null, (Class<?>) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Class must not be null!");
    }

    @Test
    void countForNotExistingSetIsZero() {
        long count = template.count("not-existing-set-name");

        assertThat(count).isZero();
    }

    @Test
    void countForObjects() {
        template.insert(new Person(id, "vasili", 50));
        String id2 = nextId();
        template.insert(new Person(id2, "vasili", 51));
        String id3 = nextId();
        template.insert(new Person(id3, "vasili", 52));
        String id4 = nextId();
        template.insert(new Person(id4, "petya", 52));

        Awaitility.await()
            .atMost(Duration.ofSeconds(15))
            .until(() -> isCountExactlyNum(4L));

        template.delete(template.findById(id, Person.class));
        template.delete(template.findById(id2, Person.class));
        template.delete(template.findById(id3, Person.class));
        template.delete(template.findById(id4, Person.class));
    }

    @Test
    void countForObjectsWithSetName() {
        template.insert(new Person(id, "vasili", 50), SET_NAME);
        String id2 = nextId();
        template.insert(new Person(id2, "vasili", 51), SET_NAME);
        String id3 = nextId();
        template.insert(new Person(id3, "vasili", 52), SET_NAME);
        String id4 = nextId();
        template.insert(new Person(id4, "petya", 52), SET_NAME);

        Awaitility.await()
            .atMost(Duration.ofSeconds(20))
            .until(() -> isCountExactlyNumWithSetName(4L, SET_NAME));

        template.delete(template.findById(id, Person.class, SET_NAME), SET_NAME);
        template.delete(template.findById(id2, Person.class, SET_NAME), SET_NAME);
        template.delete(template.findById(id3, Person.class, SET_NAME), SET_NAME);
        template.delete(template.findById(id4, Person.class, SET_NAME), SET_NAME);
    }

    @SuppressWarnings("SameParameterValue")
    private boolean isCountExactlyNum(Long num) {
        return Objects.equals(template.count(Person.class), num);
    }

    @SuppressWarnings("SameParameterValue")
    private boolean isCountExactlyNumWithSetName(Long num, String setName) {
        return Objects.equals(template.count(setName), num);
    }
}
