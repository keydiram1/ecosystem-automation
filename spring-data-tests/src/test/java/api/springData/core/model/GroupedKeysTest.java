package api.springData.core.model;

import api.springData.sample.Person;
import org.junit.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.data.aerospike.core.model.GroupedKeys;
import org.springframework.test.context.TestPropertySource;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns11"})
@Tag("SPRING-DATA-TESTS-1")
public class GroupedKeysTest {

    @Test
    public void shouldGetEntitiesKeys() {
        Set<String> keys = new HashSet<>();
        keys.add("p22");

        GroupedKeys groupedKeys = GroupedKeys.builder()
            .entityKeys(Person.class, keys)
            .build();

        Map<Class<?>, Collection<?>> expectedResult =
            new HashMap<>();
        expectedResult.put(Person.class, keys);

        assertThat(groupedKeys.getEntitiesKeys()).containsAllEntriesOf(expectedResult);
    }
}
