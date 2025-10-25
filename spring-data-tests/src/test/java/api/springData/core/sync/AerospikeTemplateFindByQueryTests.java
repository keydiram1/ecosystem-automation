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
import api.springData.sample.Address;
import api.springData.sample.Person;
import api.springData.sample.SampleClasses;
import api.springData.utility.CollectionUtils;
import api.springData.utility.QueryUtils;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.IndexType;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.data.aerospike.query.FilterOperation;
import org.springframework.data.aerospike.query.qualifier.Qualifier;
import org.springframework.data.aerospike.repository.query.CriteriaDefinition;
import org.springframework.data.aerospike.repository.query.Query;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.aerospike.repository.query.CriteriaDefinition.AerospikeQueryCriterion.KEY;
import static org.springframework.data.aerospike.repository.query.CriteriaDefinition.AerospikeQueryCriterion.KEY_VALUE_PAIR;
import static org.springframework.data.domain.Sort.Order.asc;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns9"})
@Tag("SPRING-DATA-TESTS-1")
@TestPropertySource(properties = {"personSetName=personSetNameAerospikeTemplateFindByQueryTests"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AerospikeTemplateFindByQueryTests extends BaseBlockingIntegrationTests {

    public static final String SET_NAME = "setAerospikeTemplateFindByQueryTests";

    final Person jean = Person.builder().id(nextId()).firstName("Jean").lastName("Matthews").age(21)
            .ints(Collections.singletonList(100))
            .strings(Collections.singletonList("str1")).friend(new Person("id21", "TestPerson21", 50)).build();
    final Person ashley = Person.builder().id(nextId()).firstName("Ashley").lastName("Matthews")
            .ints(Collections.singletonList(22))
            .strings(Collections.singletonList("str2")).age(22).friend(new Person("id22", "TestPerson22", 50)).build();
    final Person beatrice = Person.builder().id(nextId()).firstName("Beatrice").lastName("Matthews").age(23)
            .ints(Collections.singletonList(23))
            .friend(new Person("id23", "TestPerson23", 42)).build();
    final Person dave = Person.builder().id(nextId()).firstName("Dave").lastName("Matthews").age(24)
            .stringMap(Collections.singletonMap("key1", "val1"))
            .friend(new Person("id21", "TestPerson24", 54)).build();
    final Person zaipper = Person.builder().id(nextId()).firstName("Zaipper").lastName("Matthews").age(25)
            .stringMap(Collections.singletonMap("key2", "val2")).address(new Address("Street 1", 1, "C0121", "Sun City"))
            .build();
    final Person knowlen = Person.builder().id(nextId()).firstName("knowlen").lastName("Matthews").age(26)
            .intMap(Collections.singletonMap("key1", 11)).address(new Address("Street 2", 2, "C0122", "Sun City")).build();
    final Person xylophone = Person.builder().id(nextId()).firstName("Xylophone").lastName("Matthews").age(27)
            .intMap(Collections.singletonMap("key2", 22)).address(new Address("Street 3", 3, "C0123", "Sun City")).build();
    final Person mitch = Person.builder().id(nextId()).firstName("Mitch").lastName("Matthews").age(28)
            .intMap(Collections.singletonMap("key3", 24)).address(new Address("Street 4", 4, "C0124", "Sun City")).build();
    final Person alister = Person.builder().id(nextId()).firstName("Alister").lastName("Matthews").age(29)
            .stringMap(Collections.singletonMap("key4", "val4")).build();
    final Person aabbot = Person.builder().id(nextId()).firstName("Aabbot").lastName("Matthews").age(30)
            .stringMap(Collections.singletonMap("key4", "val5")).build();
    final List<Person> allPersons = Arrays.asList(jean, ashley, beatrice, dave, zaipper, knowlen, xylophone, mitch,
            alister, aabbot);

    @BeforeAll
    public void beforeAllSetUp() {
        deleteOneByOne(allPersons);

        // batch write operations are supported starting with Server version 6.0+
        template.insertAll(allPersons);
        template.insertAll(allPersons, SET_NAME);

        additionalAerospikeTestOperations.createIndex(Person.class, "person_age_index01", "age",
                IndexType.NUMERIC);
        additionalAerospikeTestOperations.createIndex(Person.class, "person_first_name_index01", "firstName",
                IndexType.STRING);
        additionalAerospikeTestOperations.createIndex(Person.class, "person_last_name_index01", "lastName",
                IndexType.STRING);
        additionalAerospikeTestOperations.createIndex(SET_NAME, "person_set_age_index01", "age",
                IndexType.NUMERIC);
        additionalAerospikeTestOperations.createIndex(SET_NAME, "person_set_first_name_index01", "firstName"
                , IndexType.STRING);
        additionalAerospikeTestOperations.createIndex(SET_NAME, "person_set_last_name_index01", "lastName",
                IndexType.STRING);
    }

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @AfterAll
    public void afterAll() {
        deleteOneByOne(allPersons);
        deleteOneByOne(allPersons, SET_NAME);
        additionalAerospikeTestOperations.dropIndex(Person.class, "person_age_index01");
        additionalAerospikeTestOperations.dropIndex(Person.class, "person_first_name_index01");
        additionalAerospikeTestOperations.dropIndex(Person.class, "person_last_name_index01");
        additionalAerospikeTestOperations.dropIndex(SET_NAME, "person_set_age_index01");
        additionalAerospikeTestOperations.dropIndex(SET_NAME, "person_set_first_name_index01");
        additionalAerospikeTestOperations.dropIndex(SET_NAME, "person_set_last_name_index01");
    }

    @Test
    public void findWithFilterEqual() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findByFirstName", "Dave");
        Stream<Person> result = template.find(query, Person.class);
        assertThat(result).containsOnly(dave);
    }

    @Test
    public void findWithFilterEqualWithSetName() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findByFirstName", "Dave");
        Stream<Person> result = template.find(query, Person.class, SET_NAME);
        assertThat(result).containsOnly(dave);
    }

    @Test
    public void findWithFilterEqual_String_fallbackToFilterExp() {
        additionalAerospikeTestOperations.createIndex(Person.class, "person_first_name_index_numeric", "firstName",
                IndexType.NUMERIC); // incompatible secondary index (should be STRING) causes "index not found" exception
        additionalAerospikeTestOperations.createIndex(Person.class, "person_first_name_index_blob", "firstName",
            IndexType.BLOB); // incompatible secondary index (should be STRING) causes "index not found" exception
        Query query = QueryUtils.createQueryForMethodWithArgs("findByFirstName", "Dave");
        // after getting index exception there is a fallback to filter exp only
        Stream<Person> result = template.find(query, Person.class);
        assertThat(result).containsOnly(dave);
        additionalAerospikeTestOperations.dropIndex(Person.class, "person_first_name_index_numeric");
        additionalAerospikeTestOperations.dropIndex(Person.class, "person_first_name_index_blob");
    }

    @Test
    public void findWithFilterEqualOrderByAsc() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findByLastNameOrderByFirstNameAsc", "Matthews");
        Stream<Person> result = template.find(query, Person.class);

        assertThat(result)
                .hasSize(10)
                .containsExactly(aabbot, alister, ashley, beatrice, dave, jean, knowlen, mitch, xylophone, zaipper);
    }

    @Test
    public void findWithFilterEqualOrderByDesc() {
        Object[] args = {"Matthews"};
        Query query = QueryUtils.createQueryForMethodWithArgs("findByLastNameOrderByFirstNameDesc", args);

        Stream<Person> result = template.find(query, Person.class);

        assertThat(result)
                .hasSize(10)
                .containsExactly(zaipper, xylophone, mitch, knowlen, jean, dave, beatrice, ashley, alister, aabbot);
    }

    @Test
    public void findWithFilterEqualOrderByDescNonExisting() {
        Object[] args = {"NonExistingSurname"};
        Query query = QueryUtils.createQueryForMethodWithArgs("findByLastNameOrderByFirstNameDesc", args);

        Stream<Person> result = template.find(query, Person.class);
        assertThat(result).isEmpty();
    }

    @Test
    public void findWithFilterRange() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findCustomerByAgeBetween", 25, 31);

        Stream<Person> result = template.find(query, Person.class);
        assertThat(result).hasSize(6);
    }

    @Test
    public void findWithFilterRangeWithSetName() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findCustomerByAgeBetween", 25, 31);

        Stream<Person> result = template.find(query, Person.class, SET_NAME);
        assertThat(result).hasSize(6);
    }

    @Test
    public void findWithFilterRangeNonExisting() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findCustomerByAgeBetween", 100, 150);

        Stream<Person> result = template.find(query, Person.class);
        assertThat(result).isEmpty();
    }

    @Test
    public void findWithStatement() {
        Statement aerospikeQuery = new Statement();
        String[] bins = {"firstName", "lastName"}; // fields we want retrieved
        aerospikeQuery.setNamespace(getNameSpace());
        aerospikeQuery.setSetName("personSetNameAerospikeTemplateFindByQueryTests");
        aerospikeQuery.setBinNames(bins);
        aerospikeQuery.setFilter(Filter.equal("firstName", dave.getFirstName()));

        RecordSet rs = client.query(null, aerospikeQuery);
        assertThat(CollectionUtils.toList(rs))
                .singleElement()
                .satisfies(record ->
                        assertThat(record.bins)
                                .containsOnly(entry("firstName", dave.getFirstName()), entry("lastName", dave.getLastName())));
    }

    @Test
    public void findInRange_shouldFindLimitedNumberOfDocuments() {
        int skip = 0;
        int limit = 5;
        Stream<Person> stream = template.findInRange(skip, limit, Sort.unsorted(), Person.class);
        assertThat(stream).hasSize(5);
    }

    @Test
    public void findInRangeWithSetName_shouldFindLimitedNumberOfDocuments() {
        int skip = 0;
        int limit = 5;
        Stream<Person> stream = template.findInRange(skip, limit, Sort.unsorted(), Person.class, SET_NAME);
        assertThat(stream).hasSize(5);
    }

    @Test
    public void findInRange_shouldFailOnUnsortedQueryWithOffsetValue() {
        int skip = 3;
        int limit = 5;
        assertThatThrownBy(() -> template.findInRange(skip, limit, Sort.unsorted(), Person.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsorted query must not have offset value. For retrieving paged results use sorted query.");
    }

    @Test
    public void findInRangeWithSetName_shouldFailOnUnsortedQueryWithOffsetValue() {
        int skip = 3;
        int limit = 5;
        assertThatThrownBy(() -> template.findInRange(skip, limit, Sort.unsorted(), Person.class, SET_NAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsorted query must not have offset value. For retrieving paged results use sorted query.");
    }

    @Test
    public void findInRange_shouldFindLimitedNumberOfDocumentsWithOrderBy() {
        int skip = 0;
        int limit = 5;
        Sort sort = Sort.by(asc("firstName"));
        List<Person> stream = template.findInRange(skip, limit, sort, Person.class)
                .collect(Collectors.toList());

        assertThat(stream)
                .hasSize(5)
                .containsExactly(aabbot, alister, ashley, beatrice, dave);
    }

    @Test
    public void findAll_OrderByFirstName() {
        Sort sort = Sort.by(asc("firstName"));
        List<Person> result = template.findAll(sort, 0, 0, Person.class)
                .collect(Collectors.toList());

        assertThat(result)
                .hasSize(10)
                .containsExactly(aabbot, alister, ashley, beatrice, dave, jean, knowlen, mitch, xylophone, zaipper);
    }

    @Test
    public void findAll_OrderByFirstNameWithSetName() {
        Sort sort = Sort.by(asc("firstName"));
        List<Person> result = template.findAll(sort, 0, 0, Person.class, SET_NAME)
                .collect(Collectors.toList());

        assertThat(result)
                .hasSize(10)
                .containsExactly(aabbot, alister, ashley, beatrice, dave, jean, knowlen, mitch, xylophone, zaipper);
    }

    @Test
    public void findAll_findAllExistingDocuments() {
        Stream<Person> result = template.findAll(Person.class);
        assertThat(result).containsAll(allPersons);
    }

    @Test
    public void findAll_findNothing() {
        additionalAerospikeTestOperations.deleteAllAndVerify(Person.class);

        Stream<Person> result = template.findAll(Person.class);
        assertThat(result).isEmpty();

        // batch write operations are supported starting with Server version 6.0+
        template.insertAll(allPersons);
    }

    @Test
    public void findAll_findIdOnlyRecord() {
        var id = 100;
        var doc = new SampleClasses.DocumentWithPrimitiveIntId(id); // id-only document
        var clazz = SampleClasses.DocumentWithPrimitiveIntId.class;

        var existingDoc = template.findById(id, clazz);
        assertThat(existingDoc).withFailMessage("The same record already exists").isNull();

        template.insert(doc);
        var resultsFindById = template.findById(id, clazz);
        assertThat(resultsFindById).withFailMessage("findById error").isEqualTo(doc);
        var resultsFindAll = template.findAll(clazz);
        // findAll() must correctly find the record that contains id and no bins
        assertThat(resultsFindAll.toList()).size().withFailMessage("findAll error").isEqualTo(1);

        // cleanup
        template.delete(doc);
    }

    @Test
    public void findByListContainingInteger() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findByIntsContaining", 100);
        Stream<Person> result = template.find(query, Person.class);

        assertThat(result)
                .hasSize(1)
                .containsExactlyInAnyOrder(jean);
    }

    @Test
    public void findByListContainingIntegerWithSetName() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findByIntsContaining", 100);
        Stream<Person> result = template.find(query, Person.class, SET_NAME);

        assertThat(result)
                .hasSize(1)
                .containsExactlyInAnyOrder(jean);
    }

    @Test
    public void findByListContainingString() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findByStringsContaining", "str2");
        Stream<Person> result = template.find(query, Person.class);

        assertThat(result)
                .hasSize(1)
                .containsExactlyInAnyOrder(ashley);
    }

    @Test
    public void findByMapKeysContaining() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findByStringMapContaining", KEY, "key1");
        Stream<Person> result = template.find(query, Person.class);

        assertThat(result).containsExactlyInAnyOrder(dave);
    }

    @Test
    public void findByMapValuesContaining() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findByStringMapContaining",
                CriteriaDefinition.AerospikeQueryCriterion.VALUE, "val2");
        Stream<Person> result = template.find(query, Person.class);

        assertThat(result)
                .hasSize(1)
                .containsExactlyInAnyOrder(zaipper);
    }

    @Test
    public void findByMapKeyValueContaining() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findByStringMapContaining",
                KEY_VALUE_PAIR, "key1", "val1");
        Stream<Person> result = template.find(query, Person.class);

        assertThat(result).containsExactlyInAnyOrder(dave);
    }

    @Test
    public void findPersonsByFriendAge() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findByFriendAge", 50);
        Stream<Person> result = template.find(query, Person.class);

        assertThat(result)
                .hasSize(2)
                .containsExactlyInAnyOrder(jean, ashley);
    }

    @Test
    public void findPersonsByFriendAgeNotEqual() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findByFriendAgeIsNot", 50);
        Stream<Person> result = template.find(query, Person.class);

        assertThat(result)
                .contains(beatrice, dave);
    }

    @Test
    public void findPersonsByFriendAgeGreaterThan() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findByFriendAgeGreaterThan", 42);
        Stream<Person> result = template.find(query, Person.class);

        assertThat(result)
                .hasSize(3)
                .containsExactlyInAnyOrder(jean, ashley, dave);
    }

    @Test
    public void findPersonsByFriendAgeLessThanOrEqual() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findByFriendAgeLessThanEqual", 54);
        Stream<Person> result = template.find(query, Person.class);

        assertThat(result)
                .hasSize(4)
                .containsExactlyInAnyOrder(jean, ashley, beatrice, dave);
    }

    @Test
    public void findPersonsByFriendAgeLessThanOrEqualWithSetName() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findByFriendAgeLessThanEqual", 54);
        Stream<Person> result = template.find(query, Person.class, SET_NAME);

        assertThat(result)
                .hasSize(4)
                .containsExactlyInAnyOrder(jean, ashley, beatrice, dave);
    }

    @Test
    public void findPersonsByFriendAgeRange() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findByFriendAgeBetween", 50, 55);
        Stream<Person> result = template.find(query, Person.class);

        assertThat(result)
                .hasSize(3)
                .containsExactlyInAnyOrder(jean, ashley, dave);
    }

    @Test
    public void findPersonsByAddressZipCode() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findByAddressZipCode", "C0123");
        Stream<Person> result = template.find(query, Person.class);

        assertThat(result)
                .hasSize(1)
                .containsExactlyInAnyOrder(xylophone);
    }

    @Test
    public void findByAddressZipCodeContaining() {
        Query query = QueryUtils.createQueryForMethodWithArgs("findByAddressZipCodeContaining", "C012");
        Stream<Person> result = template.find(query, Person.class);

        assertThat(result)
                .hasSize(4)
                .containsExactlyInAnyOrder(zaipper, knowlen, xylophone, mitch);
    }

    @Test
    public void findAllUsingQuery_shouldRunWithDifferentArgumentsCombinations() {
        String fieldName = "data";
        String fieldValue1 = "test";
        String fieldValue2 = "test2";
        SampleClasses.CustomCollectionClass doc1 = new SampleClasses.CustomCollectionClass(id, fieldValue1);
        SampleClasses.CustomCollectionClass doc2 = new SampleClasses.CustomCollectionClass(nextId(), fieldValue2);
        template.save(doc1);
        template.save(doc2);
        additionalAerospikeTestOperations.createIndex(SampleClasses.CustomCollectionClass.class,
                "CustomCollectionClass_field", fieldName, IndexType.STRING);

        // find by query
        Qualifier qualifier = Qualifier.builder()
                .setPath(fieldName)
                .setFilterOperation(FilterOperation.EQ)
                .setValue(fieldValue1)
                .build();
        Stream<SampleClasses.CustomCollectionClass> result1 =
                template.find(new Query(qualifier), SampleClasses.CustomCollectionClass.class);
        assertThat(result1).containsOnly(doc1);

        // find by query with a complex qualifier
        Qualifier dataEqFieldValue1 = Qualifier.builder()
                .setFilterOperation(FilterOperation.EQ)
                .setPath(fieldName)
                .setValue(fieldValue1)
                .build();
        Qualifier dataEqFieldValue2 = Qualifier.builder()
                .setFilterOperation(FilterOperation.EQ)
                .setPath(fieldName)
                .setValue(fieldValue2)
                .build();
        Qualifier qualifierOr = Qualifier.or(dataEqFieldValue1, dataEqFieldValue2);
        Stream<SampleClasses.CustomCollectionClass> result3 =
                template.find(new Query(qualifierOr), SampleClasses.CustomCollectionClass.class);
        assertThat(result3).containsOnly(doc1, doc2);

        additionalAerospikeTestOperations.dropIndex(SampleClasses.CustomCollectionClass.class,
                "CustomCollectionClass_field"); // cleanup
        template.delete(doc1); // cleanup
        template.delete(doc2); // cleanup
    }
}
