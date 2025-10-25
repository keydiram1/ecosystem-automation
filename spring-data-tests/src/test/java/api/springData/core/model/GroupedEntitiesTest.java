package api.springData.core.model;

import api.springData.sample.Customer;
import api.springData.sample.Person;
import org.junit.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.data.aerospike.core.model.GroupedEntities;
import org.springframework.test.context.TestPropertySource;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns11"})
@Tag("SPRING-DATA-TESTS-1")
public class GroupedEntitiesTest {

    private static final Map<Class<?>, Collection<?>> entitiesMap = Map.of(
        Person.class, List.of(Person.builder().id("22").build()),
        Customer.class, List.of(Customer.builder().id("33").build())
    );
    private static final GroupedEntities TEST_GROUPED_ENTITIES = GroupedEntities.builder()
        .entities(entitiesMap)
        .build();

    @Test
    public void shouldGetEntitiesByClass() {
        Person expectedResult = Person.builder().id("22").build();
        assertThat(TEST_GROUPED_ENTITIES.getEntitiesByClass(Person.class))
            .containsExactlyInAnyOrder(expectedResult);
    }

    @Test
    public void shouldReturnAnEmptyResultIfGroupedEntitiesDoesNotContainResult() {
        assertThat(TEST_GROUPED_ENTITIES.getEntitiesByClass(String.class)).isEmpty();
    }

    @Test
    public void shouldContainEntities() {
        assertThat(TEST_GROUPED_ENTITIES.containsEntities()).isTrue();
    }

    @Test
    public void shouldNotContainEntities() {
        GroupedEntities groupedEntities = GroupedEntities.builder().build();
        assertThat(groupedEntities.containsEntities()).isFalse();
    }
}
