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

import api.springData.BaseBlockingIntegrationTests;
import api.springData.sample.SampleClasses;
import api.springData.sample.SampleClasses.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.aerospike.mapping.AerospikeMappingContext;
import org.springframework.data.aerospike.mapping.AerospikePersistentProperty;
import org.springframework.data.aerospike.mapping.BasicAerospikePersistentEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns17"})
@Tag("SPRING-DATA-TESTS-1")
public class AerospikePersistentEntityTest extends BaseBlockingIntegrationTests {

    static final int DEFAULT_EXPIRATION = 0;

    @Autowired
    protected AerospikeMappingContext context;

    @Test
    public void shouldReturnExpirationForDocumentWithExpiration() {
        BasicAerospikePersistentEntity<?> persistentEntity =
            context.getRequiredPersistentEntity(DocumentWithExpiration.class);

        assertThat(persistentEntity.getExpiration()).isEqualTo(SampleClasses.EXPIRATION_ONE_SECOND);
    }

    @Test
    public void shouldReturnExpirationForDocumentWithExpirationExpression() {
        BasicAerospikePersistentEntity<?> persistentEntity =
            context.getRequiredPersistentEntity(DocumentWithExpirationExpression.class);

        assertThat(persistentEntity.getExpiration()).isEqualTo(SampleClasses.EXPIRATION_ONE_SECOND);
    }

    @Test
    public void shouldReturnExpirationForDocumentWithExpirationUnit() {
        BasicAerospikePersistentEntity<?> persistentEntity =
            context.getRequiredPersistentEntity(DocumentWithExpirationUnit.class);

        assertThat(persistentEntity.getExpiration()).isEqualTo((int) TimeUnit.MINUTES.toSeconds(1));
    }

    @Test
    public void shouldReturnZeroForDocumentWithoutExpiration() {
        BasicAerospikePersistentEntity<?> persistentEntity =
            context.getRequiredPersistentEntity(DocumentWithoutExpiration.class);

        assertThat(persistentEntity.getExpiration()).isEqualTo(DEFAULT_EXPIRATION);
    }

    @Test
    public void shouldReturnNeverExpireForDocumentWithNeverExpireAndWithoutExpirationUnit() {
        BasicAerospikePersistentEntity<?> persistentEntity =
            context.getRequiredPersistentEntity(DocumentWithNeverExpireAndWithoutExpirationUnit.class);

        assertThat(persistentEntity.getExpiration()).isEqualTo(SampleClasses.EXPIRATION_NEVER_EXPIRE);
    }

    @Test
    public void shouldReturnNeverExpireForDocumentWithNeverExpireAndExpirationUnit() {
        BasicAerospikePersistentEntity<?> persistentEntity =
            context.getRequiredPersistentEntity(DocumentWithNeverExpireAndExpirationUnit.class);

        assertThat(persistentEntity.getExpiration()).isEqualTo(SampleClasses.EXPIRATION_NEVER_EXPIRE);
    }

    @Test
    public void shouldReturnZeroForDocumentWithoutAnnotation() {
        BasicAerospikePersistentEntity<?> persistentEntity =
            context.getRequiredPersistentEntity(DocumentWithoutAnnotation.class);

        assertThat(persistentEntity.getExpiration()).isEqualTo(DEFAULT_EXPIRATION);
    }

    @Test
    public void shouldFailForDocumentWithExpirationAndExpression() {
        BasicAerospikePersistentEntity<?> persistentEntity =
            context.getRequiredPersistentEntity(DocumentWithExpirationAndExpression.class);

        assertThatThrownBy(persistentEntity::getExpiration)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Both 'expiration' and 'expirationExpression' are set");
    }

    @Test
    public void shouldGetExpirationProperty() {
        BasicAerospikePersistentEntity<?> persistentEntity =
            context.getRequiredPersistentEntity(DocumentWithExpirationAnnotation.class);
        AerospikePersistentProperty expirationProperty = persistentEntity.getExpirationProperty();

        assertThat(expirationProperty).isNotNull();
        assertThat(expirationProperty.isExpirationProperty()).isTrue();
        assertThat(expirationProperty.isExpirationSpecifiedAsUnixTime()).isFalse();
    }

    @Test
    public void shouldGetExpirationPropertySpecifiedAsUnixTime() {
        BasicAerospikePersistentEntity<?> persistentEntity =
            context.getRequiredPersistentEntity(DocumentWithUnixTimeExpiration.class);
        AerospikePersistentProperty expirationProperty = persistentEntity.getExpirationProperty();

        assertThat(expirationProperty).isNotNull();
        assertThat(expirationProperty.isExpirationProperty()).isTrue();
        assertThat(expirationProperty.isExpirationSpecifiedAsUnixTime()).isTrue();
    }

    @Test
    public void shouldFailForNonExpirationProperty() {
        BasicAerospikePersistentEntity<?> persistentEntity =
            context.getRequiredPersistentEntity(DocumentWithUnixTimeExpiration.class);
        AerospikePersistentProperty expirationProperty = persistentEntity.getIdProperty();

        assertThatThrownBy(expirationProperty::isExpirationSpecifiedAsUnixTime)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Property id is not expiration property");
    }

    @Test
    public void shouldResolvePlaceholdersInCollection() {
        BasicAerospikePersistentEntity<?> persistentEntity =
            context.getRequiredPersistentEntity(DocumentWithExpressionInCollection.class);

        assertThat(persistentEntity.getSetName()).isEqualTo(DocumentWithExpressionInCollection.COLLECTION_PREFIX +
            "service1");
    }
}
