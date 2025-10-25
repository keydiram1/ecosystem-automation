/*
 * Copyright 2012-2018 the original author or authors
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
package api.springData.repository;

import api.springData.BaseBlockingIntegrationTests;
import api.springData.sample.CompositeObject;
import api.springData.sample.CompositeObjectRepository;
import api.springData.sample.SimpleObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns17"})
@Tag("SPRING-DATA-TESTS-1")
public class RepositoriesIntegrationTests extends BaseBlockingIntegrationTests {

    @Autowired
    CompositeObjectRepository repository;

    @Test
    public void findOne_shouldReturnNullForNonExistingKey() {
        Optional<CompositeObject> one = repository.findById("non-existing-id");

        assertThat(one).isNotPresent();
    }

    @Test
    public void shouldSaveObjectWithPersistenceConstructorThatHasAllFields() {
        CompositeObject expected = CompositeObject.builder()
            .id("composite-object-1")
            .intValue(15)
            .simpleObject(SimpleObject.builder().property1("prop1").property2(555).build())
            .build();
        repository.save(expected);

        Optional<CompositeObject> actual = repository.findById(expected.getId());
        assertThat(actual).hasValue(expected);
        //noinspection OptionalGetWithoutIsPresent
        repository.delete(actual.get());
    }

    @Test
    public void shouldDeleteObjectWithPersistenceConstructor() {
        String id = nextId();
        CompositeObject expected = CompositeObject.builder()
            .id(id)
            .build();
        repository.save(expected);
        assertThat(repository.findById(id)).isPresent();

        repository.delete(expected);
        assertThat(repository.findById(id)).isNotPresent();
    }
}
