package api.springData.core.sync;

import api.springData.sample.Customer;
import api.springData.sample.Person;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.data.aerospike.core.model.GroupedEntities;
import org.springframework.data.aerospike.core.model.GroupedKeys;
import org.springframework.data.mapping.MappingException;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static api.springData.utility.AerospikeUniqueId.nextId;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public interface AbstractFindByEntitiesTest {

    @Test
    default void shouldFindAllRequestedEntities() {
        List<Person> persons = saveGeneratedPersons(5);
        List<Customer> customers = saveGeneratedCustomers(3);

        GroupedKeys groupedKeys = getGroupedKeys(persons, customers);
        GroupedEntities byIds = findByIds(groupedKeys);

        assertThat(byIds.getEntitiesByClass(Person.class)).containsExactlyInAnyOrderElementsOf(persons);
        assertThat(byIds.getEntitiesByClass(Customer.class)).containsExactlyInAnyOrderElementsOf(customers);
        deleteEntities(persons); // cleanup
    }

    @Test
    default void shouldReturnAnEmptyResultIfKeysWhereSetToWrongEntities() {
        List<Person> persons = saveGeneratedPersons(5);
        List<Customer> customers = saveGeneratedCustomers(3);

        Set<String> requestedPersonsIds = persons.stream()
            .map(Person::getId)
            .collect(Collectors.toSet());
        Set<String> requestedCustomerIds = customers.stream().map(Customer::getId)
            .collect(Collectors.toSet());

        GroupedKeys groupedKeys = GroupedKeys.builder()
            .entityKeys(Person.class, requestedCustomerIds)
            .entityKeys(Customer.class, requestedPersonsIds)
            .build();

        GroupedEntities byIds = findByIds(groupedKeys);
        assertThat(byIds.containsEntities()).isFalse();
        deleteEntities(persons); // cleanup
    }

    @Test
    default void shouldFindSomeOfIdsOfRequestedEntities() {
        List<Person> persons = saveGeneratedPersons(2);
        List<Customer> customers = saveGeneratedCustomers(3);

        GroupedKeys requestMapWithRandomExtraIds = getGroupedEntitiesKeysWithRandomExtraIds(persons, customers);
        GroupedEntities results = findByIds(requestMapWithRandomExtraIds);

        assertThat(results.getEntitiesByClass(Person.class)).containsExactlyInAnyOrderElementsOf(persons);
        assertThat(results.getEntitiesByClass(Customer.class)).containsExactlyInAnyOrderElementsOf(customers);
        deleteEntities(persons); // cleanup
    }

    @Test
    default void shouldFindResultsOfOneOfRequestedEntity() {
        List<Person> persons = saveGeneratedPersons(3);

        GroupedKeys groupedKeysWithRandomExtraIds = getGroupedEntitiesKeysWithRandomExtraIds(persons, emptyList());
        GroupedEntities results = findByIds(groupedKeysWithRandomExtraIds);

        assertThat(results.getEntitiesByClass(Person.class)).containsExactlyInAnyOrderElementsOf(persons);
        assertThat(results.getEntitiesByClass(Customer.class)).containsExactlyInAnyOrderElementsOf(emptyList());
        deleteEntities(persons); // cleanup
    }

    @Test
    default void shouldFindForOneEntityIfAnotherContainsEmptyRequestList() {
        List<Person> persons = saveGeneratedPersons(3);

        GroupedKeys groupedKeys = getGroupedKeys(persons, emptyList());
        GroupedEntities batchGroupedEntities = findByIds(groupedKeys);

        assertThat(batchGroupedEntities.getEntitiesByClass(Person.class)).containsExactlyInAnyOrderElementsOf(persons);
        assertThat(batchGroupedEntities.getEntitiesByClass(Customer.class)).containsExactlyInAnyOrderElementsOf(emptyList());
        deleteEntities(persons); // cleanup
    }

    @Test
    default void shouldReturnMapWithEmptyResultsOnEmptyRequest() {
        GroupedKeys groupedKeys = GroupedKeys.builder()
            .entityKeys(Person.class, emptyList())
            .entityKeys(Customer.class, emptyList())
            .build();

        GroupedEntities batchGroupedEntities = findByIds(groupedKeys);

        assertThat(batchGroupedEntities.getEntitiesByClass(Person.class))
            .containsExactlyInAnyOrderElementsOf(emptyList());
        assertThat(batchGroupedEntities.getEntitiesByClass(Customer.class))
            .containsExactlyInAnyOrderElementsOf(emptyList());
    }

    @Test
    default void shouldReturnMapWithEmptyResultsIfNoEntitiesWhereFound() {
        GroupedKeys groupedKeys = GroupedKeys.builder()
            .entityKeys(Person.class, singletonList(nextId()))
            .entityKeys(Customer.class, singletonList(nextId()))
            .build();

        GroupedEntities batchGroupedEntities = findByIds(groupedKeys);

        assertThat(batchGroupedEntities.getEntitiesByClass(Person.class))
            .containsExactlyInAnyOrderElementsOf(emptyList());
        assertThat(batchGroupedEntities.getEntitiesByClass(Customer.class))
            .containsExactlyInAnyOrderElementsOf(emptyList());
    }

    @Test
    default void shouldThrowMappingExceptionOnNonAerospikeEntityClass() {
        List<Person> persons = saveGeneratedPersons(2);
        Set<String> personIds = persons.stream()
            .map(Person::getId)
            .collect(Collectors.toSet());

        GroupedKeys groupedKeys = GroupedKeys.builder()
            .entityKeys(Person.class, personIds)
            .entityKeys(String.class, singletonList(1L))
            .build();

        assertThatThrownBy(() -> findByIds(groupedKeys))
            .isInstanceOf(MappingException.class)
            .hasMessage("Couldn't find PersistentEntity for type class java.lang.String");
        deleteEntities(persons); // cleanup
    }

    @Test
    default void shouldReturnAnEmptyResultOnEmptyRequestMap() {
        GroupedKeys groupedKeys = GroupedKeys.builder().build();
        GroupedEntities byIds = findByIds(groupedKeys);
        assertThat(byIds.getEntitiesByClass(Person.class)).isEmpty();
    }

    @Test
    default void shouldThrowConverterNotFoundExceptionOnClassWithoutConverter() {
        GroupedKeys groupedKeys = GroupedKeys.builder()
            .entityKeys(Person.class, singletonList(Person.builder().id("id").build()))
            .build();

        assertThatThrownBy(() -> findByIds(groupedKeys))
            .isInstanceOf(ConverterNotFoundException.class)
            .hasMessageContaining("No converter found capable of converting from type");
    }

    default GroupedKeys getGroupedKeys(Collection<Person> persons, Collection<Customer> customers) {
        Set<String> requestedPersonsIds = persons.stream()
            .map(Person::getId)
            .collect(Collectors.toSet());
        Set<String> requestedCustomerIds = customers.stream().map(Customer::getId)
            .collect(Collectors.toSet());

        return GroupedKeys.builder()
            .entityKeys(Person.class, requestedPersonsIds)
            .entityKeys(Customer.class, requestedCustomerIds)
            .build();
    }

    default GroupedKeys getGroupedEntitiesKeysWithRandomExtraIds(Collection<Person> persons,
                                                                 Collection<Customer> customers) {
        Set<String> requestedPersonsIds = Stream.concat(persons.stream()
                .map(Person::getId), Stream.of(nextId(), nextId()))
            .collect(Collectors.toSet());
        Set<String> requestedCustomerIds = Stream.concat(customers.stream()
                .map(Customer::getId), Stream.of(nextId(), nextId()))
            .collect(Collectors.toSet());

        return GroupedKeys.builder()
            .entityKeys(Person.class, requestedPersonsIds)
            .entityKeys(Customer.class, requestedCustomerIds)
            .build();
    }

    default List<Customer> saveGeneratedCustomers(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> Customer.builder().id(nextId())
                .firstName("firstName" + i)
                .lastName("Smith")
                .build())
            .peek(this::save)
            .collect(Collectors.toList());
    }

    default List<Person> saveGeneratedPersons(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> Person.builder().id(nextId())
                .firstName("firstName" + i)
                .emailAddress("gmail.com")
                .build())
            .peek(this::save)
            .collect(Collectors.toList());
    }

    default <T> void deleteEntities(List<T> entities) {
        for (T entity : entities) {
            delete(entity);
        }
    }

    <T> void save(T obj);

    <T> void delete(T obj);

    GroupedEntities findByIds(GroupedKeys groupedKeys);
}
