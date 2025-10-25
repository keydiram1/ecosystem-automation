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
import api.springData.sample.SampleClasses;
import api.springData.sample.SampleClasses.DocumentWithTouchOnRead;
import api.springData.sample.SampleClasses.DocumentWithTouchOnReadAndExpirationProperty;
import api.springData.sample.SampleClasses.MapWithDoubleId;
import api.springData.sample.SampleClasses.MapWithIntegerId;
import api.springData.sample.SampleClasses.VersionedClassWithAllArgsConstructor;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.MapOperation;
import com.aerospike.client.cdt.MapPolicy;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns15"})
@Tag("SPRING-DATA-TESTS-1")
@TestPropertySource(properties = {"personSetName=personSetNameAerospikeTemplateFindByIdTests"})
public class AerospikeTemplateFindByIdTests extends BaseBlockingIntegrationTests {

    public static final String SET_NAME = "setAerospikeTemplateFindByIdTests";

    @Test
    public void findById_shouldReadVersionedClassWithAllArgsConstructor() {
        VersionedClassWithAllArgsConstructor inserted = new VersionedClassWithAllArgsConstructor(id, "foobar", 0L);
        template.insert(inserted);
        assertThat(template.findById(id, VersionedClassWithAllArgsConstructor.class).getVersion()).isEqualTo(1L);
        template.update(new VersionedClassWithAllArgsConstructor(id, "foobar1", inserted.getVersion()));
        VersionedClassWithAllArgsConstructor result = template.findById(id, VersionedClassWithAllArgsConstructor.class);
        assertThat(result.getVersion()).isEqualTo(2L);
        template.delete(result); // cleanup
    }

    @Test
    public void findById_shouldReadVersionedClassWithAllArgsConstructorAndSetName() {
        VersionedClassWithAllArgsConstructor inserted = new VersionedClassWithAllArgsConstructor(id, "foobar", 0L);
        template.insert(inserted, SET_NAME);
        assertThat(template.findById(id, VersionedClassWithAllArgsConstructor.class, SET_NAME)
                .getVersion()).isEqualTo(1L);
        template.update(new VersionedClassWithAllArgsConstructor(id, "foobar1", inserted.getVersion()),
                SET_NAME);
        VersionedClassWithAllArgsConstructor result = template.findById(id,
                VersionedClassWithAllArgsConstructor.class, SET_NAME);
        assertThat(result.getVersion()).isEqualTo(2L);
        template.delete(result, SET_NAME); // cleanup
    }

    @Test
    public void findById_shouldReturnNullForNonExistingKey() {
        Person one = template.findById("person-non-existing-key", Person.class);
        assertThat(one).isNull();
    }

    @Test
    public void findById_shouldReturnNullForNonExistingKeyIfTouchOnReadSetToTrue() {
        DocumentWithTouchOnRead one = template.findById("non-existing-key", DocumentWithTouchOnRead.class);
        assertThat(one).isNull();
    }

    @Test
    public void findById_shouldIncreaseVersionIfTouchOnReadSetToTrue() {
        DocumentWithTouchOnRead doc = new DocumentWithTouchOnRead(String.valueOf(id));
        template.save(doc);

        DocumentWithTouchOnRead actual = template.findById(doc.getId(), DocumentWithTouchOnRead.class);
        assertThat(actual.getVersion()).isEqualTo(doc.getVersion() + 1);
        template.delete(actual); // cleanup
    }

    @Test
    public void findByIdFail() {
        Person person = new Person(id, "Oliver");
        person.setAge(25);
        template.insert(person);

        Person person1 = template.findById("Person", Person.class);
        assertThat(person1).isNull();
        template.delete(person); // cleanup
    }

    @Test
    public void findByIds_shouldFindExisting() {
        Person firstPerson = Person.builder().id(nextId()).firstName("first").emailAddress("gmail.com").build();
        Person secondPerson = Person.builder().id(nextId()).firstName("second").emailAddress("gmail.com").build();
        template.save(firstPerson);
        template.save(secondPerson);

        List<String> ids = Arrays.asList(nextId(), firstPerson.getId(), secondPerson.getId());
        List<Person> actual = template.findByIds(ids, Person.class);
        assertThat(actual).containsExactly(firstPerson, secondPerson);
        template.delete(firstPerson); // cleanup
        template.delete(secondPerson); //cleanup
    }

    @Test
    public void findByIdsWithSetName_shouldFindExisting() {
        Person firstPerson = Person.builder().id(nextId()).firstName("first").emailAddress("gmail.com").build();
        Person secondPerson = Person.builder().id(nextId()).firstName("second").emailAddress("gmail.com").build();
        template.save(firstPerson, SET_NAME);
        template.save(secondPerson, SET_NAME);

        List<String> ids = Arrays.asList(nextId(), firstPerson.getId(), secondPerson.getId());
        List<Person> actual = template.findByIds(ids, Person.class, SET_NAME);
        assertThat(actual).containsExactly(firstPerson, secondPerson);
        template.delete(firstPerson, SET_NAME); // cleanup
        template.delete(secondPerson, SET_NAME); //cleanup
    }

    @Test
    public void findByIds_shouldReturnEmptyList() {
        List<Person> actual = template.findByIds(Collections.emptyList(), Person.class);
        assertThat(actual).isEmpty();
    }

    @Test
    public void findByIdsWithSetName_shouldReturnEmptyList() {
        List<Person> actual = template.findByIds(Collections.emptyList(), Person.class, SET_NAME);
        assertThat(actual).isEmpty();
    }

    @Test
    public void findById_shouldFailOnTouchOnReadWithExpirationProperty() {
        template.insert(new DocumentWithTouchOnReadAndExpirationProperty(id, SampleClasses.EXPIRATION_ONE_MINUTE));
        assertThatThrownBy(() -> template.findById(id, DocumentWithTouchOnReadAndExpirationProperty.class))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void findByKey() { // findByUserKey
        String setName = "Person";
        client.put(null, new Key(getNameSpace(), setName, id),
                new Bin("firstName", "Dave"),
                new Bin("age", 56)
        );

        Person result = template.findById(id, Person.class, setName);
        assertThat(result.getFirstName()).isEqualTo("Dave");
        assertThat(result.getAge()).isEqualTo(56);
        template.delete(result);
    }

    @Test
    public void findById_shouldReadClassWithNumericKeyMapWrittenByTemplate() {
        if (template.getAerospikeConverter().getAerospikeDataSettings().isKeepOriginalKeyTypes()) {
            int intKey = 1;
            double doubleKey = 100.25;
            String value = "String value";
            long id = 10L;
            long id2 = 11L;

            template.save(new MapWithIntegerId(id, Map.of(intKey, value)));
            template.save(new MapWithDoubleId(id2, Map.of(doubleKey, value)));

            MapWithIntegerId resultInt = template.findById(id, MapWithIntegerId.class);
            MapWithDoubleId resultDouble = template.findById(id2, MapWithDoubleId.class);
            assertThat(resultInt.getMapWithIntId()).isEqualTo(Map.of(intKey, value));
            assertThat(resultDouble.getMapWithDoubleId()).isEqualTo(Map.of(doubleKey, value));
            template.delete(resultInt); // cleanup
            template.delete(resultDouble); // cleanup
        }
    }

    @Test
    public void findById_shouldReadClassWithNumericKeyMap() {
        if (template.getAerospikeConverter().getAerospikeDataSettings().isKeepOriginalKeyTypes()) {
            int intKey = 1;
            double doubleKey = 100.25;
            String value = "String value";
            long id1 = 10L;
            long id2 = 11L;

            client.operate(null, new Key(getNameSpace(), "MapWithIntegerId", id1),
                    MapOperation.put(MapPolicy.Default, "mapWithIntId", Value.get(intKey), Value.get(value))
            );
            client.operate(null, new Key(getNameSpace(), "MapWithDoubleId", id2),
                    MapOperation.put(MapPolicy.Default, "mapWithDoubleId", Value.get(doubleKey), Value.get(value))
            );

            MapWithIntegerId resultInt = template.findById(id1, MapWithIntegerId.class);
            assertThat(resultInt.getMapWithIntId()).isEqualTo(Map.of(intKey, value));
            MapWithDoubleId resultDouble = template.findById(id2, MapWithDoubleId.class);
            assertThat(resultDouble.getMapWithDoubleId()).isEqualTo(Map.of(doubleKey, value));
            template.delete(resultInt); // cleanup
            template.delete(resultDouble); // cleanup
        }
    }

    @Test
    public void findById_shouldReadClassWithByteArrayId() {
        if (template.getAerospikeConverter().getAerospikeDataSettings().isKeepOriginalKeyTypes()) {
            long longId = 10L;
            SampleClasses.DocumentWithLongId document = SampleClasses.DocumentWithLongId.builder().id(longId).build();
            template.save(document);
            SampleClasses.DocumentWithLongId result = template.findById(longId, SampleClasses.DocumentWithLongId.class);
            assertThat(result.getId().equals(longId)).isTrue();
            template.delete(result); // cleanup

            byte[] byteArrayId = new byte[]{1, 1, 1, 1};
            SampleClasses.DocumentWithByteArrayId document2 = SampleClasses.DocumentWithByteArrayId.builder()
                    .id(byteArrayId)
                    .build();
            template.save(document2);
            SampleClasses.DocumentWithByteArrayId result2 = template.findById(byteArrayId,
                    SampleClasses.DocumentWithByteArrayId.class);
            assertThat(Arrays.equals(result2.getId(), byteArrayId)).isTrue();
            template.delete(result2); // cleanup
        }
    }
}
