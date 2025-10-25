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
import api.springData.sample.SampleClasses.CustomCollectionClass;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.aerospike.core.WritePolicyBuilder;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns14"})
@Tag("SPRING-DATA-TESTS-1")
public class AerospikeTemplatePersistTests extends BaseBlockingIntegrationTests {

    public static final String SET_NAME = "setAerospikeTemplatePersistTests";

    @Test
    public void shouldPersistWithCustomWritePolicy() {
        CustomCollectionClass initial = new CustomCollectionClass(id, "data");

        WritePolicy writePolicy = WritePolicyBuilder.builder(client.getWritePolicyDefault())
            .recordExistsAction(RecordExistsAction.CREATE_ONLY)
            .build();

        template.persist(initial, writePolicy);

        CustomCollectionClass actual = template.findById(id, CustomCollectionClass.class);
        assertThat(actual).isEqualTo(initial);
    }

    @Test
    public void shouldPersistWithCustomWritePolicyWithSetName() {
        CustomCollectionClass initial = new CustomCollectionClass(id, "data");

        WritePolicy writePolicy = WritePolicyBuilder.builder(client.getWritePolicyDefault())
            .recordExistsAction(RecordExistsAction.CREATE_ONLY)
            .build();

        template.persist(initial, writePolicy, SET_NAME);

        CustomCollectionClass actual = template.findById(id, CustomCollectionClass.class, SET_NAME);
        assertThat(actual).isEqualTo(initial);
    }

    @Test
    public void shouldNotPersistWithCustomWritePolicy() {
        CustomCollectionClass initial = new CustomCollectionClass(id, "data");

        WritePolicy writePolicy = WritePolicyBuilder.builder(client.getWritePolicyDefault())
            .recordExistsAction(RecordExistsAction.UPDATE_ONLY)
            .build();

        assertThatThrownBy(() -> template.persist(initial, writePolicy))
            .isInstanceOf(DataRetrievalFailureException.class);
    }

    @Test
    public void shouldNotPersistWithCustomWritePolicyWithSetName() {
        CustomCollectionClass initial = new CustomCollectionClass(id, "data");

        WritePolicy writePolicy = WritePolicyBuilder.builder(client.getWritePolicyDefault())
            .recordExistsAction(RecordExistsAction.UPDATE_ONLY)
            .build();

        assertThatThrownBy(() -> template.persist(initial, writePolicy, SET_NAME))
            .isInstanceOf(DataRetrievalFailureException.class);
    }
}
