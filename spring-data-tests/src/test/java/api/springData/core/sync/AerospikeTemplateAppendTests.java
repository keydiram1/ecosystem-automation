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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns16"})
@Tag("SPRING-DATA-TESTS-1")
@TestPropertySource(properties = {"personSetName=personSetNameAerospikeTemplateAppendTests"})
public class AerospikeTemplateAppendTests extends BaseBlockingIntegrationTests {

    public static final String SET_NAME = "setAerospikeTemplateAppendTests";

    @Test
    public void shouldAppend() {
        Person one = Person.builder().id(id).firstName("Nas").build();
        template.insert(one);

        Person appended = template.append(one, "firstName", "tya");

        assertThat(appended).isEqualTo(Person.builder().id(id).firstName("Nastya").build());
        assertThat(appended.getFirstName()).isEqualTo("Nastya");
        Person result = template.findById(id, Person.class);
        assertThat(result.getFirstName()).isEqualTo("Nastya");
        template.delete(result); // cleanup
    }

    @Test
    public void shouldAppendMultipleFields() {
        Person one = Person.builder().id(id).firstName("Nas").emailAddress("nastya@").build();
        template.insert(one);

        Map<String, String> toBeUpdated = new HashMap<>();
        toBeUpdated.put("firstName", "tya");
        toBeUpdated.put("email", "gmail.com");
        Person appended = template.append(one, toBeUpdated);

        assertThat(appended.getFirstName()).isEqualTo("Nastya");
        assertThat(appended.getEmailAddress()).isEqualTo("nastya@gmail.com");
        Person actual = template.findById(id, Person.class);
        assertThat(actual.getFirstName()).isEqualTo("Nastya");
        assertThat(actual.getEmailAddress()).isEqualTo("nastya@gmail.com");
        template.delete(actual); // cleanup
    }

    @Test
    public void shouldAppendWithSetName() {
        Person one = Person.builder().id(id).firstName("Nas").build();
        template.insert(one, SET_NAME);

        Person appended = template.append(one, SET_NAME, "firstName", "tya");

        assertThat(appended).isEqualTo(Person.builder().id(id).firstName("Nastya").build());
        assertThat(appended.getFirstName()).isEqualTo("Nastya");
        Person result = template.findById(id, Person.class, SET_NAME);
        assertThat(result.getFirstName()).isEqualTo("Nastya");
        template.delete(result, SET_NAME); // cleanup
    }
}
