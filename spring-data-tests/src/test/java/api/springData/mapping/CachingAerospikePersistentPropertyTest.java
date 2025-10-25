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
package api.springData.mapping;

import api.springData.sample.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.data.aerospike.mapping.AerospikeMappingContext;
import org.springframework.data.aerospike.mapping.AerospikePersistentEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Peter Milne
 * @author Jean Mercier
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns11"})
@Tag("SPRING-DATA-TESTS-1")
public class CachingAerospikePersistentPropertyTest {

    AerospikeMappingContext context;

    @BeforeEach
    public void setUp() {
        context = new AerospikeMappingContext();
        context.setApplicationContext(mock(ApplicationContext.class));
    }

    @Test
    public void isTransient() {
        AerospikePersistentEntity<?> entity = context.getRequiredPersistentEntity(Person.class);

        assertThat(entity.getIdProperty().isTransient()).isFalse();
    }

    @Test
    public void isAssociation() {
        AerospikePersistentEntity<?> entity = context.getRequiredPersistentEntity(Person.class);

        assertThat(entity.getIdProperty().isAssociation()).isFalse();
    }

    @Test
    public void usePropertyAccess() {
        AerospikePersistentEntity<?> entity = context.getRequiredPersistentEntity(Person.class);

        assertThat(entity.getIdProperty().usePropertyAccess()).isFalse();
    }

    @Test
    public void isIdProperty() {
        AerospikePersistentEntity<?> entity = context.getRequiredPersistentEntity(Person.class);

        assertThat(entity.getIdProperty().isIdProperty()).isTrue();
    }

    @Test
    public void getFieldName() {
        AerospikePersistentEntity<?> entity = context.getRequiredPersistentEntity(Person.class);

        assertThat(entity.getIdProperty().getName()).isEqualTo("id");
    }
}
