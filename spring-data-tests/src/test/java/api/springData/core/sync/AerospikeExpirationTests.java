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
import api.springData.sample.SampleClasses.DocumentWithDefaultConstructor;
import api.springData.sample.SampleClasses.DocumentWithExpiration;
import api.springData.sample.SampleClasses.DocumentWithExpirationAnnotation;
import api.springData.sample.SampleClasses.DocumentWithExpirationAnnotationAndPersistenceConstructor;
import api.springData.sample.SampleClasses.DocumentWithExpirationOneDay;
import api.springData.sample.SampleClasses.DocumentWithUnixTimeExpiration;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.assertj.core.data.Offset;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static api.springData.utility.AerospikeExpirationPolicy.DO_NOT_UPDATE_EXPIRATION;
import static api.springData.utility.AerospikeExpirationPolicy.NEVER_EXPIRE;
import static api.springData.utility.AwaitilityUtils.awaitTenSecondsUntil;
import static api.springData.utility.AwaitilityUtils.awaitTwoSecondsUntil;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns16"})
@Tag("SPRING-DATA-TESTS-1")
public class AerospikeExpirationTests extends BaseBlockingIntegrationTests {

    private static final String NAMESPACE = "source-ns16";

    @Test
    public void shouldAddValuesMapAndExpire() {
        DocumentWithDefaultConstructor document = new DocumentWithDefaultConstructor();
        document.setId(id);
        document.setExpiration(DateTime.now().plusSeconds(1));

        template.add(document, singletonMap("intField", 10L));

        awaitTwoSecondsUntil(() -> {
            DocumentWithDefaultConstructor shouldNotExpire = template.findById(id,
                DocumentWithDefaultConstructor.class);
            assertThat(shouldNotExpire).isNotNull();
            assertThat(shouldNotExpire.getIntField()).isEqualTo(10);
        });
        awaitTenSecondsUntil(() ->
            assertThat(template.findById(id, DocumentWithDefaultConstructor.class)).isNull()
        );
    }

    @Test
    public void shouldAddValueAndExpire() {
        DocumentWithDefaultConstructor document = new DocumentWithDefaultConstructor();
        document.setId(id);
        document.setExpiration(DateTime.now().plusSeconds(1));

        template.add(document, "intField", 10L);

        awaitTwoSecondsUntil(() -> {
            DocumentWithDefaultConstructor shouldNotExpire = template.findById(id,
                DocumentWithDefaultConstructor.class);
            assertThat(shouldNotExpire).isNotNull();
            assertThat(shouldNotExpire.getIntField()).isEqualTo(10);
        });
        awaitTenSecondsUntil(() ->
            assertThat(template.findById(id, DocumentWithDefaultConstructor.class)).isNull()
        );
    }

    @Test
    public void shouldExpireBasedOnUnixTimeValue() {
        template.insert(new DocumentWithUnixTimeExpiration(id, DateTime.now().plusSeconds(1)));

        awaitTwoSecondsUntil(() ->
            assertThat(template.findById(id, DocumentWithUnixTimeExpiration.class)).isNotNull()
        );
        awaitTenSecondsUntil(() ->
            assertThat(template.findById(id, DocumentWithUnixTimeExpiration.class)).isNull()
        );
    }

    @Test
    public void shouldExpireBasedOnFieldValue() {
        template.insert(new DocumentWithExpirationAnnotation(id, 1));

        awaitTwoSecondsUntil(() ->
            assertThat(template.findById(id, DocumentWithExpirationAnnotation.class)).isNotNull()
        );

        awaitTenSecondsUntil(() ->
            assertThat(template.findById(id, DocumentWithExpirationAnnotation.class)).isNull()
        );
    }

    @Test
    public void shouldReturnExpirationValue() {
        template.insert(new DocumentWithExpirationAnnotation(id, 5));

        awaitTenSecondsUntil(() -> {
            DocumentWithExpirationAnnotation document = template.findById(id, DocumentWithExpirationAnnotation.class);
            assertThat(document.getExpiration()).isGreaterThan(0).isLessThan(5);
        });
    }

    @Test
    public void shouldUpdateExpirationOnTouchOnRead() {
        template.insert(new DocumentWithExpirationOneDay(id));

        awaitTenSecondsUntil(() -> {
            Key key = new Key(template.getNamespace(), template.getSetName(DocumentWithExpirationOneDay.class), id);
            Record record = template.getAerospikeClient().get(null, key);
            int initialExpiration = record.expiration;

            template.findById(id, DocumentWithExpirationOneDay.class);

            record = template.getAerospikeClient().get(null, key);

            assertThat(record.expiration - initialExpiration).isCloseTo(2, offset(1));
        });
    }

    @Test
    public void shouldExpire() {
        template.insert(new DocumentWithExpiration(id));

        awaitTwoSecondsUntil(() ->
            assertThat(template.findById(id, DocumentWithExpiration.class)).isNotNull()
        );
        awaitTenSecondsUntil(() ->
            assertThat(template.findById(id, DocumentWithExpiration.class)).isNull()
        );
    }

    @Test
    public void shouldSaveAndGetDocumentWithImmutableExpiration() {
        template.insert(new DocumentWithExpirationAnnotationAndPersistenceConstructor(id, 60L));

        DocumentWithExpirationAnnotationAndPersistenceConstructor doc = template.findById(id,
            DocumentWithExpirationAnnotationAndPersistenceConstructor.class);
        assertThat(doc).isNotNull();
        assertThat(doc.getExpiration()).isCloseTo(60L, Offset.offset(10L));
    }

    @Test
    public void save_expiresDocumentWithVersion() {
        template.save(new DocumentWithExpirationOneDay(id));

        additionalAerospikeTestOperations.addDuration(Duration.ofHours(24).plus(Duration.ofMinutes(1)), NAMESPACE);

        DocumentWithExpirationOneDay document = template.findById(id, DocumentWithExpirationOneDay.class);
        assertThat(document).isNull();
    }

    @Test
    void shouldNeverExpire() {
        DocumentWithExpirationAnnotation document = new DocumentWithExpirationAnnotation(id, NEVER_EXPIRE);

        template.insert(document);

        DocumentWithExpirationAnnotation byId = template.findById(id, DocumentWithExpirationAnnotation.class);
        assertThat(byId).isNotNull();
        Record record = client.get(null, new Key(namespace, "expiration-set", id));
        assertThat(record.getTimeToLive()).isEqualTo(NEVER_EXPIRE);
    }

    @Test
    void shouldNotUpdateExpirationWhenRecordIsUpdatedForNotUpdateExpirationPolicy() {
        DocumentWithExpirationAnnotation document = new DocumentWithExpirationAnnotation(id, 10);

        template.insert(document);
        Record record = client.get(null, new Key(namespace, "expiration-set", document.getId()));

        template.update(new DocumentWithExpirationAnnotation(id, DO_NOT_UPDATE_EXPIRATION));

        Record recordAfterUpdate = client.get(null, new Key(namespace, "expiration-set", document.getId()));
        assertThat(record.getTimeToLive()).isEqualTo(recordAfterUpdate.getTimeToLive());
    }

    @Test
    void invalidExpirationValueThrowsException() {
        DocumentWithExpirationAnnotation document = new DocumentWithExpirationAnnotation(id, -500);

        assertThatThrownBy(() -> template.insert(document))
            .isInstanceOf(InvalidDataAccessApiUsageException.class)
            .hasCauseInstanceOf(AerospikeException.class)
            .hasStackTraceContaining("Parameter error");
    }
}
