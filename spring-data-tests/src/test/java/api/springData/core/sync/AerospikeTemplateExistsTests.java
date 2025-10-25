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

import static org.assertj.core.api.Assertions.assertThat;
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns15"})
@Tag("SPRING-DATA-TESTS-1")
@TestPropertySource(properties = {"personSetName=personSetNameAerospikeTemplateExistsTests"})
public class AerospikeTemplateExistsTests extends BaseBlockingIntegrationTests {

    public static final String SET_NAME = "setAerospikeTemplateExistsTests";

    @Test
    public void exists_shouldReturnTrueIfValueIsPresent() {
        Person one = Person.builder().id(id).firstName("tya").emailAddress("gmail.com").build();
        template.insert(one);

        assertThat(template.exists(id, Person.class)).isTrue();
        template.delete(one);
    }

    @Test
    public void existsWithSetName_shouldReturnTrueIfValueIsPresent() {
        Person one = Person.builder().id(id).firstName("tya").emailAddress("gmail.com").build();
        template.insert(one, SET_NAME);

        assertThat(template.exists(id, SET_NAME)).isTrue();
        template.delete(one, SET_NAME);
    }

    @Test
    public void exists_shouldReturnFalseIfValueIsAbsent() {
        assertThat(template.exists(id, Person.class)).isFalse();
    }

    @Test
    public void existsWithSetName_shouldReturnFalseIfValueIsAbsent() {
        assertThat(template.exists(id, SET_NAME)).isFalse();
    }
}
