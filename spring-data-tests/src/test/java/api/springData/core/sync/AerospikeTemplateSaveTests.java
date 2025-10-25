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
import api.springData.sample.SampleClasses.CustomCollectionClass;
import api.springData.sample.SampleClasses.DocumentWithByteArray;
import api.springData.sample.SampleClasses.DocumentWithTouchOnRead;
import api.springData.sample.SampleClasses.VersionedClass;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.Policy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.TestPropertySource;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns14"})
@TestPropertySource(properties = {"personSetName=personSetNameAerospikeTemplateSaveTests"})
@Tag("SPRING-DATA-TESTS-1")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AerospikeTemplateSaveTests extends BaseBlockingIntegrationTests {

    public static final String SET_NAME = "setAerospikeTemplateSaveTests";

    @AfterAll
    public void afterAll() {
        template.deleteAll(VersionedClass.class);
        template.deleteAll(SampleClasses.DocumentWithIntArray.class);
        template.deleteAll(SampleClasses.DocumentWithBigIntegerAndNestedArray.class);
        template.deleteAll(Person.class);
        template.deleteAll(CustomCollectionClass.class);
        template.deleteAll(DocumentWithTouchOnRead.class);
        template.deleteAll(SET_NAME);
    }

    // test for RecordExistsAction.REPLACE_ONLY policy
    @Test
    public void shouldReplaceAllBinsPresentInAerospikeWhenSavingDocument() {
        VersionedClass first = new VersionedClass(id, "foo");
        template.save(first);
        Key key = new Key(getNameSpace(), template.getSetName(VersionedClass.class), id);
        additionalAerospikeTestOperations.addNewFieldToSavedDataInAerospike(key);

        template.save(new VersionedClass(id, "foo2", 2L));

        Record aeroRecord = client.get(new Policy(), key);
        assertThat(aeroRecord.bins.get("notPresent")).isNull();
        assertThat(aeroRecord.bins.get("field")).isEqualTo("foo2");
    }

    @Test
    public void shouldSaveDocumentWithArray() {
        int[] array = new int[]{0, 1, 2, 3, 4, 5};
        SampleClasses.DocumentWithIntArray doc = new SampleClasses.DocumentWithIntArray(id, array);
        template.save(doc);

        Key key = new Key(getNameSpace(), template.getSetName(SampleClasses.DocumentWithIntArray.class), id);
        Record aeroRecord = client.get(new Policy(), key);
        assertThat(aeroRecord.bins.get("array")).isNotNull();
        SampleClasses.DocumentWithIntArray result = template.findById(id, SampleClasses.DocumentWithIntArray.class);
        assertThat(result.getArray()).isEqualTo(array);
    }

    @Test
    public void shouldSaveDocumentWithNestedArrayAndBigInteger() {
        Integer[] array = new Integer[]{0, 1, 2, 3, 4};
        SampleClasses.ObjectWithIntegerArray objectWithArray = new SampleClasses.ObjectWithIntegerArray(array);
        BigInteger bigInteger = new BigInteger("100");
        SampleClasses.DocumentWithBigIntegerAndNestedArray doc =
                new SampleClasses.DocumentWithBigIntegerAndNestedArray(id, bigInteger, objectWithArray);
        template.save(doc);

        SampleClasses.DocumentWithBigIntegerAndNestedArray result =
                template.findById(id, SampleClasses.DocumentWithBigIntegerAndNestedArray.class);
        assertThat(result.getBigInteger()).isEqualTo(bigInteger);
        assertThat(result.getObjectWithArray().getArray()).isEqualTo(array);
    }

    @Test
    public void shouldSaveAndSetVersion() {
        VersionedClass first = new VersionedClass(id, "foo");
        template.save(first);

        assertThat(first.getVersion()).isEqualTo(1);
        assertThat(template.findById(id, VersionedClass.class).getVersion()).isEqualTo(1);
    }

    @Test
    public void shouldNotSaveVersionedDocumentIfItAlreadyExists() {
        template.save(new VersionedClass(id, "foo"));

        assertThatThrownBy(() -> template.save(new VersionedClass(id, "foo")))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    public void shouldUpdateNotVersionedDocumentIfItAlreadyExists() {
        Person person = new Person(id, "Amol");
        person.setAge(28);
        template.save(person);

        assertThatNoException().isThrownBy(() -> template.save(person));
    }

    @Test
    public void shouldSaveDocumentWithEqualVersion() {
        // if an object has version property, GenerationPolicy.EXPECT_GEN_EQUAL is used
        VersionedClass first = new VersionedClass(id, "foo", 0L);
        VersionedClass second = new VersionedClass(id, "foo", 1L);
        VersionedClass third = new VersionedClass(id, "foo", 2L);

        template.save(first);
        template.save(second);
        template.save(third);

        assertThat(first.getVersion() == 1).isTrue();
        assertThat(second.getVersion() == 2).isTrue();
        assertThat(third.getVersion() == 3).isTrue();
    }

    @Test
    public void shouldFailSaveNewDocumentWithVersionGreaterThanZero() {
        assertThatThrownBy(() -> template.save(new VersionedClass(nextId(), "foo", 5L)))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    public void shouldUpdateNullField() {
        VersionedClass versionedClass = new VersionedClass(id, null);
        template.save(versionedClass);
        assertThat(versionedClass.getVersion() == 1).isTrue();

        VersionedClass saved = template.findById(id, VersionedClass.class);
        template.save(saved);
        assertThat(saved.getVersion() == 2).isTrue();
    }

    @Test
    public void shouldUpdateNullFieldForClassWithVersionField() {
        VersionedClass versionedClass = new VersionedClass(id, "field");
        template.save(versionedClass);

        VersionedClass byId = template.findById(id, VersionedClass.class);
        assertThat(byId.getField())
                .isEqualTo("field");

        template.save(new VersionedClass(id, null, byId.getVersion()));

        assertThat(template.findById(id, VersionedClass.class).getField())
                .isNull();
    }

    @Test
    public void shouldUpdateNullFieldForClassWithoutVersionField() {
        Person person = new Person(id, "Oliver");
        person.setFirstName("First name");
        template.insert(person);

        assertThat(template.findById(id, Person.class)).isEqualTo(person);

        person.setFirstName(null);
        template.save(person);

        Person result = template.findById(id, Person.class);
        assertThat(result.getFirstName()).isNull();
        template.delete(result); // cleanup
    }

    @Test
    public void shouldUpdateExistingDocument() {
        VersionedClass one = new VersionedClass(id, "foo");
        template.save(one);

        template.save(new VersionedClass(id, "foo1", one.getVersion()));

        VersionedClass value = template.findById(id, VersionedClass.class);
        assertThat(value.getVersion()).isEqualTo(2);
        assertThat(value.getField()).isEqualTo("foo1");
    }

    @Test
    public void shouldSetVersionWhenSavingTheSameDocument() {
        VersionedClass one = new VersionedClass(id, "foo");
        template.save(one);
        template.save(one);
        template.save(one);

        assertThat(one.getVersion()).isEqualTo(3);
    }

    @Test
    public void shouldSaveMultipleTimeDocumentWithoutVersion() {
        CustomCollectionClass one = new CustomCollectionClass(id, "numbers");

        template.save(one);
        template.save(one);

        assertThat(template.findById(id, CustomCollectionClass.class)).isEqualTo(one);
    }

    @Test
    public void shouldUpdateDocumentDataWithoutVersion() {
        CustomCollectionClass first = new CustomCollectionClass(id, "numbers");
        CustomCollectionClass second = new CustomCollectionClass(id, "hot dog");

        template.save(first);
        template.save(second);

        assertThat(template.findById(id, CustomCollectionClass.class)).isEqualTo(second);
    }

    @Test
    public void rejectsNullObjectToBeSaved() {
        assertThatThrownBy(() -> template.save(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldSaveAndFindDocumentWithByteArrayField() {
        DocumentWithByteArray document = new DocumentWithByteArray(id, new byte[]{1, 0, 0, 1, 1, 1, 0, 0});

        template.save(document);

        DocumentWithByteArray result = template.findById(id, DocumentWithByteArray.class);

        assertThat(result).isEqualTo(document);
    }

    @Test
    public void shouldSaveAllAndSetVersion() {
        VersionedClass first = new VersionedClass(id, "foo");
        VersionedClass second = new VersionedClass(nextId(), "bar");

        template.save(second);
        assertThat(second.getVersion()).isEqualTo(1);
        second.setVersion(second.getVersion());

        // batch write operations are supported starting with Server version 6.0+
        template.saveAll(List.of(first, second));

        assertThat(first.getVersion()).isEqualTo(1);
        assertThat(second.getVersion()).isEqualTo(2);
        assertThat(template.findById(id, VersionedClass.class).getVersion()).
                isEqualTo(1);
        template.delete(first); // cleanup
        template.delete(second); // cleanup
    }

    @Test
    public void shouldSaveAllAndSetVersionWithSetName() {
        VersionedClass first = new VersionedClass(id, "foo");
        VersionedClass second = new VersionedClass(nextId(), "foo");
        // batch write operations are supported starting with Server version 6.0+
        template.saveAll(List.of(first, second), SET_NAME);

        assertThat(first.getVersion()).isEqualTo(1);
        assertThat(second.getVersion()).isEqualTo(1);
        assertThat(template.findById(id, VersionedClass.class, SET_NAME).getVersion()).
                isEqualTo(1);
        template.delete(first, SET_NAME); // cleanup
        template.delete(second, SET_NAME); // cleanup
    }

    @Test
    public void shouldSaveAllVersionedDocumentsAndSetVersionAndThrowExceptionIfDuplicatesWithinOneBatch() {
        // batch write operations are supported starting with Server version 6.0+
        VersionedClass first = new VersionedClass("newId100", "foo");
        VersionedClass second = new VersionedClass("newId200", "foo");

        // The documents’ versions are equal to zero, meaning the documents have not been saved to the database yet
        assertThat(first.getVersion() == 0).isTrue();
        assertThat(second.getVersion() == 0).isTrue();

        // An attempt to save the same versioned documents in one batch results in getting an exception
        assertThatThrownBy(() -> template.saveAll(List.of(first, first, second, second)))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessageFindingMatch("Failed to save the record with ID .* due to versions mismatch");

        // The documents' versions get updated after they are read from the corresponding database records
        assertThat(first.getVersion() == 1).isTrue();
        assertThat(second.getVersion() == 1).isTrue();

        template.delete(first); // cleanup
        template.delete(second); // cleanup

        // The same versioned documents can be saved if they are not in the same batch.
        // This way, the generation counts of the corresponding database records can be used
        // to update the documents’ versions each time.
        VersionedClass newFirst = new VersionedClass("newId300", "foo");
        VersionedClass newSecond = new VersionedClass("newId400", "bar");

        assertThat(newFirst.getVersion() == 0).isTrue();
        assertThat(newSecond.getVersion() == 0).isTrue();

        template.saveAll(List.of(newFirst, newSecond));
        assertThat(newFirst.getVersion() == 1).isTrue();
        assertThat(newSecond.getVersion() == 1).isTrue();

        template.saveAll(List.of(newFirst, newSecond));
        assertThat(newFirst.getVersion() == 2).isTrue();
        assertThat(newSecond.getVersion() == 2).isTrue();
    }

    @Test
    public void shouldSaveAllNotVersionedDocumentsIfAlreadyExist() {
        // batch write operations are supported starting with Server version 6.0+
        Person john = new Person("id1", "John");
        Person jack = new Person("id2", "Jack");
        template.save(jack); // saving non-versioned document to create a new DB record
        // If an object has no version property, RecordExistsAction.UPDATE is used
        // If a corresponding record does not exist it will be created, otherwise updated (an "upsert")
        template.saveAll(List.of(john, jack));
        assertThat(template.findById("id1", Person.class)).isEqualTo(john); // DB record is created
        assertThat(template.findById("id2", Person.class)).isEqualTo(jack);
        template.delete(john); // cleanup
        template.delete(jack); // cleanup

        Person person = new Person(id, "Amol");
        person.setAge(28);
        template.save(person);
        // If an object has no version property, RecordExistsAction.UPDATE is used
        assertThatNoException().isThrownBy(() -> template.saveAll(List.of(person, person)));
        template.delete(person); // cleanup
    }
}
