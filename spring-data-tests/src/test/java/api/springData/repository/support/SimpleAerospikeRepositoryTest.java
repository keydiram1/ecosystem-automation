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
package api.springData.repository.support;

import api.springData.sample.Person;
import api.springData.utility.AdditionalAerospikeTestOperations;
import com.aerospike.client.query.IndexType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.aerospike.core.AerospikeOperations;
import org.springframework.data.aerospike.repository.support.SimpleAerospikeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.data.aerospike.util.Utils.iterableToList;

/**
 * @author Peter Milne
 * @author Jean Mercier
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns12"})
@Tag("SPRING-DATA-TESTS-1")
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class SimpleAerospikeRepositoryTest {

    @Mock
    EntityInformation<Person, String> metadata;
    @Mock
    AerospikeOperations operations;
    @InjectMocks
    SimpleAerospikeRepository<Person, String> aerospikeRepository;
    @Autowired
    AdditionalAerospikeTestOperations additionalAerospikeTestOperations;

    Person testPerson;
    List<Person> testPersons;

    @BeforeEach
    public void setUp() {
        when(metadata.getJavaType()).thenReturn(Person.class);

        testPerson = new Person("21", "Jean");
        testPersons = new ArrayList<>();
        testPersons.add(new Person("one", "Jean", 21));
        testPersons.add(new Person("two", "Jean2", 22));
        testPersons.add(new Person("three", "Jean3", 23));
    }

    @Test
    public void findOne() {
        when(operations.findById("21", Person.class)).thenReturn(testPerson);

        Optional<Person> person = aerospikeRepository.findById("21");
        assertThat(person)
            .hasValueSatisfying(actual -> assertThat(actual.getFirstName()).isEqualTo("Jean"));
    }

    @Test
    public void save() {
        Person myPerson = aerospikeRepository.save(testPerson);

        assertThat(testPerson).isEqualTo(myPerson);
        verify(operations).save(testPerson);
    }

    @Test
    public void saveIterableOfS() {
        // batch write operations are supported starting with Server version 6.0+
        Iterable<Person> result = aerospikeRepository.saveAll(testPersons);

        assertThat(result).isEqualTo(testPersons);
        verify(operations).saveAll(testPersons);
    }

    @Test
    public void delete() {
        aerospikeRepository.delete(testPerson);
        verify(operations).delete(testPerson);
    }

    @Test
    public void findAllSort() {
        when(operations.findAll(Sort.by(Sort.Direction.ASC, "firstName"), 0, 0, Person.class))
            .thenReturn(testPersons.stream());

        Iterable<Person> fetchList = aerospikeRepository.findAll(Sort.by(Sort.Direction.ASC, "firstName"));
        List<Person> results = new ArrayList<>();
        fetchList.forEach(results::add);
        assertThat(results).isEqualTo(testPersons);
    }

    @Test
    public void findAllPageable() {
        Page<Person> page = new PageImpl<>(iterableToList(testPersons), PageRequest.of(0, 2), 5);

        doReturn(testPersons.stream()).when(operations).findInRange(0, 2, Sort.unsorted(), Person.class);
        doReturn("set").when(operations).getSetName(Person.class);
        doReturn(5L).when(operations).count("set");

        Page<Person> result = aerospikeRepository.findAll(PageRequest.of(0, 2));

        verify(operations).findInRange(0, 2, Sort.unsorted(), Person.class);
        assertThat(result).isEqualTo(page);
    }

    @Test
    public void exists() {
        when(operations.exists(testPerson.getId(), Person.class)).thenReturn(true);

        boolean exists = aerospikeRepository.existsById(testPerson.getId());
        assertThat(exists).isTrue();
    }

    @Test
    public void findAll() {
        when(operations.findAll(Person.class)).thenReturn(testPersons.stream());

        Iterable<Person> fetchList = aerospikeRepository.findAll();
        assertThat(fetchList).hasSameElementsAs(testPersons);
    }

    @Test
    public void findAllIterableOfID() {
        List<String> ids = testPersons.stream().map(Person::getId).collect(toList());
        when(aerospikeRepository.findAllById(ids)).thenReturn(testPersons);

        List<Person> fetchList = (List<Person>) aerospikeRepository.findAllById(ids);
        assertThat(fetchList).isEqualTo(testPersons);
    }

    @Test
    public void deleteID() {
        aerospikeRepository.deleteById("one");
        verify(operations).deleteById("one", Person.class);
    }

    @Test
    public void deleteAllById() {
        List<String> personIds = testPersons.stream()
            .map(Person::getId)
            .collect(toList());
        aerospikeRepository.deleteAllById(personIds);
        verify(operations).deleteByIds(personIds, Person.class);
    }

    @Test
    public void deleteAllIterable() {
        aerospikeRepository.deleteAll(List.of(testPerson));
        verify(operations).deleteAll(List.of(testPerson));
    }

    @Test
    public void deleteAll() {
        aerospikeRepository.deleteAll();
        verify(operations).deleteAll(Person.class);
    }

    @Test
    public void createIndex() {
        aerospikeRepository.createIndex(Person.class, "index_first_name", "firstName", IndexType.STRING);
        verify(operations).createIndex(Person.class, "index_first_name", "firstName", IndexType.STRING);
    }

    @Test
    public void deleteIndex() {
        aerospikeRepository.deleteIndex(Person.class, "index_first_name");
        verify(operations).deleteIndex(Person.class, "index_first_name");
    }

    @Test
    public void indexExists() {
        when(operations.indexExists(anyString())).thenReturn(true);
        boolean exists = aerospikeRepository.indexExists("index_first_name");

        assertThat(exists).isTrue();
        verify(operations).indexExists("index_first_name");
    }
}
