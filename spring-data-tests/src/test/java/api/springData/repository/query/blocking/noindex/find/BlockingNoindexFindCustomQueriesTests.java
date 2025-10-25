package api.springData.repository.query.blocking.noindex.find;

import api.springData.repository.query.blocking.noindex.PersonRepositoryQueryTests;
import api.springData.sample.Person;
import api.springData.sample.PersonId;
import api.springData.sample.PersonSomeFields;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.query.FilterOperation;
import org.springframework.data.aerospike.query.qualifier.Qualifier;
import org.springframework.data.aerospike.repository.query.Query;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.data.aerospike.repository.query.CriteriaDefinition.AerospikeMetadata.SINCE_UPDATE_TIME;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns20"})
@TestPropertySource(properties = {"personSetName=personSetNameCustomQueriesTests"})
@Tag("SPRING-DATA-TESTS-2")
public class BlockingNoindexFindCustomQueriesTests extends PersonRepositoryQueryTests {

    @Test
    void findPersonsByMetadata() {
        // creating an expression "since_update_time metadata value is less than 50 seconds"
        Qualifier sinceUpdateTimeLt50Seconds = Qualifier.metadataBuilder()
                .setMetadataField(SINCE_UPDATE_TIME)
                .setFilterOperation(FilterOperation.LT)
                .setValue(50000L)
                .build();
        assertThat(repository.findUsingQuery(new Query(sinceUpdateTimeLt50Seconds))).containsAll(allPersons);

        // creating an expression "since_update_time metadata value is between 1 millisecond and 50 seconds"
        Qualifier sinceUpdateTimeBetween1And50000 = Qualifier.metadataBuilder()
                .setMetadataField(SINCE_UPDATE_TIME)
                .setFilterOperation(FilterOperation.BETWEEN)
                .setValue(1L)
                .setSecondValue(50000L)
                .build();
        assertThat(repository.findUsingQuery(new Query(sinceUpdateTimeBetween1And50000)))
                .containsAll(repository.findUsingQuery(new Query(sinceUpdateTimeLt50Seconds)));
    }

    @Test
    void findBySimplePropertyEquals_Enum() {
        Qualifier genderEqFemale = Qualifier.builder()
                .setPath("gender")
                .setFilterOperation(FilterOperation.EQ)
                .setValue(Person.Gender.FEMALE)
                .build();
        assertThat(repository.findUsingQuery(new Query(genderEqFemale))).containsOnly(alicia);
    }

    @Test
    void findBySimplePropertyEquals_String() {
        String email = "alicia@test.com";
        alicia.setEmailAddress(email);
        repository.save(alicia);

        Qualifier emailEq = Qualifier.builder()
                // custom bin name has been set to "email" via @Field annotation
                .setPath("email")
                .setFilterOperation(FilterOperation.EQ)
                .setValue(email)
                .build();
        assertThat(repository.findUsingQuery(new Query(emailEq))).containsOnly(alicia);
    }

    @Test
    void findByIdEquals_String_withProjection() {
        String id = oliver.getId();
        Qualifier idEquals = Qualifier.idEquals(id);
        Iterable<PersonId> results = repository.findUsingQuery(new Query(idEquals), PersonId.class);
        assertThat(results).containsOnly(oliver.toPersonId());
    }

    @Test
    void findBySimplePropertyEquals_String_withProjection() {
        String email = "alicia@test.com";
        alicia.setEmailAddress(email);
        repository.save(alicia);

        Qualifier emailEquals = Qualifier.builder()
                // custom bin name has been set to "email" via @Field annotation
                .setPath("email")
                .setFilterOperation(FilterOperation.EQ)
                .setValue(email)
                .build();
        Iterable<PersonSomeFields> results = repository.findUsingQuery(new Query(emailEquals), PersonSomeFields.class);
        assertThat(results).containsOnly(alicia.toPersonSomeFields());
    }

    @Test
    void findPersonsByQuery() {
        Iterable<Person> result;

        // creating an expression "since_update_time metadata value is greater than 1 millisecond"
        Qualifier sinceUpdateTimeGt1 = Qualifier.metadataBuilder()
                .setMetadataField(SINCE_UPDATE_TIME)
                .setFilterOperation(FilterOperation.GT)
                .setValue(1L)
                .build();

        // creating an expression "since_update_time metadata value is less than 50 seconds"
        Qualifier sinceUpdateTimeLt50Seconds = Qualifier.metadataBuilder()
                .setMetadataField(SINCE_UPDATE_TIME)
                .setFilterOperation(FilterOperation.LT)
                .setValue(50000L)
                .build();
        assertThat(repository.findUsingQuery(new Query(sinceUpdateTimeLt50Seconds))).containsAll(allPersons);

        // creating an expression "since_update_time metadata value is between 1 millisecond and 50 seconds"
        Qualifier sinceUpdateTimeBetween1And50000 = Qualifier.metadataBuilder()
                .setMetadataField(SINCE_UPDATE_TIME)
                .setFilterOperation(FilterOperation.BETWEEN)
                .setValue(1L)
                .setSecondValue(50000L)
                .build();
        assertThat(repository.findUsingQuery(new Query(sinceUpdateTimeBetween1And50000))).containsAll(allPersons);

        // creating an expression "firstName is equal to Carter"
        Qualifier firstNameEqCarter = Qualifier.builder()
                .setPath("firstName")
                .setFilterOperation(FilterOperation.EQ)
                .setValue("Carter")
                .build();

        // creating an expression "age is equal to 49"
        Qualifier ageEq49 = Qualifier.builder()
                .setPath("age")
                .setFilterOperation(FilterOperation.EQ)
                .setValue(49)
                .build();
        result = repository.findUsingQuery(new Query(ageEq49));
        assertThat(result).containsOnly(carter);

        // creating an expression "firstName is equal to Leroi" with sorting by age and limiting by 1 row
        Qualifier firstNameEqLeroi = Qualifier.builder()
                .setPath("firstName")
                .setFilterOperation(FilterOperation.EQ)
                .setValue("Leroi")
                .build();
        Query query = new Query(firstNameEqLeroi);
        query.setSort(Sort.by("age"));
        query.setRows(1);
        result = repository.findUsingQuery(query);
        assertThat(result).containsOnly(leroi2);

        // creating an expression "age is greater than 49"
        Qualifier ageGt49 = Qualifier.builder()
                .setFilterOperation(FilterOperation.GT)
                .setPath("age")
                .setValue(49)
                .build();
        result = repository.findUsingQuery(new Query(ageGt49));
        assertThat(result).doesNotContain(carter);

        // creating an expression "id equals Carter's id"
        Qualifier keyEqCartersId = Qualifier.idEquals(carter.getId());
        result = repository.findUsingQuery(new Query(keyEqCartersId));
        assertThat(result).containsOnly(carter);

        // creating an expression "id equals Boyd's id"
        Qualifier keyEqBoydsId = Qualifier.idEquals(boyd.getId());
        result = repository.findUsingQuery(new Query(keyEqBoydsId));
        assertThat(result).containsOnly(boyd);

        // analogous to {@link SimpleAerospikeRepository#findAllById(Iterable)}
        // creating an expression "id equals Carter's id OR Boyd's id"
        Qualifier keyEqMultipleIds = Qualifier.idIn(carter.getId(), boyd.getId());
        result = repository.findUsingQuery(new Query(keyEqMultipleIds));
        assertThat(result).containsOnly(carter, boyd);

        // metadata and id qualifiers combined with AND
        // not more than one id qualifier is allowed, otherwise the expressions will not overlap because of uniqueness
        result = repository.findUsingQuery(new Query(Qualifier.and(sinceUpdateTimeGt1, keyEqCartersId)));
        // if a query contains id qualifier the results are firstly narrowed down to satisfy the given ids
        // that's why queries with qualifier like Qualifier.or(Qualifier.idEquals(...), ageGt49)) return empty result
        assertThat(result).containsOnly(carter);

        // if a query contains id qualifier the results are firstly narrowed down to satisfy the given ids
        result = repository.findUsingQuery(new Query(Qualifier.and(sinceUpdateTimeGt1, Qualifier.idIn(carter.getId(),
                dave.getId(), boyd.getId()))));
        assertThat(result).containsOnly(carter, dave, boyd);

        // the same qualifiers in different order
        result = repository.findUsingQuery(new Query(Qualifier.and(keyEqCartersId, sinceUpdateTimeGt1)));
        assertThat(result).containsOnly(carter);

        result = repository.findUsingQuery(new Query(Qualifier.and(sinceUpdateTimeGt1, sinceUpdateTimeLt50Seconds,
                ageEq49, firstNameEqCarter, sinceUpdateTimeBetween1And50000, keyEqCartersId)));
        assertThat(result).containsOnly(carter);

        // conditions "age == 49", "firstName is Carter" and "since_update_time metadata value is less than 50 seconds"
        // are combined with OR
        Qualifier orWide = Qualifier.or(ageEq49, firstNameEqCarter, sinceUpdateTimeLt50Seconds);
        result = repository.findUsingQuery(new Query(orWide));
        assertThat(result).containsAll(allPersons);

        // conditions "age == 49" and "firstName is Carter" are combined with OR
        Qualifier orNarrow = Qualifier.or(ageEq49, firstNameEqCarter);
        result = repository.findUsingQuery(new Query(orNarrow));
        assertThat(result).containsOnly(carter);

        // conditions "age == 49" and "age > 49" are not overlapping
        result = repository.findUsingQuery(new Query(Qualifier.and(ageEq49, ageGt49)));
        assertThat(result).isEmpty();

        // conditions "age == 49" and "age > 49" are combined with OR
        Qualifier ageEqOrGt49 = Qualifier.or(ageEq49, ageGt49);

        result = repository.findUsingQuery(new Query(ageEqOrGt49));
        List<Person> personsWithAgeEqOrGt49 = allPersons.stream().filter(person -> person.getAge() >= 49).toList();
        assertThat(result).containsAll(personsWithAgeEqOrGt49);

        // a condition that returns all entities and a condition that returns one entity are combined using AND
        result = repository.findUsingQuery(new Query(Qualifier.and(orWide, orNarrow)));
        assertThat(result).containsOnly(carter);

        // a condition that returns all entities and a condition that returns one entity are combined using AND
        // another way of running the same query
        Qualifier orCombinedWithAnd = Qualifier.and(orWide, orNarrow);
        result = repository.findUsingQuery(new Query(orCombinedWithAnd));
        assertThat(result).containsOnly(carter);
    }

    @Test
    void findPersonsByQueryMustBeValid() {
        assertThatThrownBy(() -> repository.findUsingQuery(new Query(Qualifier.metadataBuilder()
                .setMetadataField(SINCE_UPDATE_TIME)
                .setFilterOperation(FilterOperation.BETWEEN)
                .setValue(1L)
                .build())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BETWEEN: expecting secondValue to be provided");

        assertThatThrownBy(() -> repository.findUsingQuery(new Query(Qualifier.metadataBuilder()
                .setMetadataField(SINCE_UPDATE_TIME)
                .setFilterOperation(FilterOperation.BETWEEN)
                .setValue(1L)
                .setSecondValue(null)
                .build())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BETWEEN: secondValue is expected to be set as Long");

        assertThatThrownBy(() -> repository.findUsingQuery(new Query(Qualifier.metadataBuilder()
                .setMetadataField(SINCE_UPDATE_TIME)
                .setFilterOperation(FilterOperation.BETWEEN)
                .setValue("value")
                .setSecondValue(1L)
                .build())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BETWEEN: value is expected to be set as Long");

        assertThatThrownBy(() -> repository.findUsingQuery(new Query(Qualifier.metadataBuilder()
                .setMetadataField(SINCE_UPDATE_TIME)
                .setFilterOperation(FilterOperation.GT)
                .setValue(1)
                .build())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("GT: value is expected to be set as Long");

        assertThatThrownBy(() -> repository.findUsingQuery(new Query(Qualifier.metadataBuilder()
                .setMetadataField(SINCE_UPDATE_TIME)
                .setFilterOperation(FilterOperation.LT)
                .setValue(1)
                .build())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LT: value is expected to be set as Long");

        assertThatThrownBy(() -> repository.findUsingQuery(new Query(Qualifier.metadataBuilder()
                .setMetadataField(SINCE_UPDATE_TIME)
                .setFilterOperation(FilterOperation.LTEQ)
                .setValue(1)
                .build())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LTEQ: value is expected to be set as Long");

        assertThatThrownBy(() -> repository.findUsingQuery(new Query(Qualifier.metadataBuilder()
                .setMetadataField(SINCE_UPDATE_TIME)
                .setFilterOperation(FilterOperation.STARTS_WITH)
                .setValue(1L)
                .build())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Operation STARTS_WITH cannot be applied to metadataField");

        Qualifier keyEqCartersId = Qualifier.idEquals(carter.getId());
        Qualifier keyEqBoydsId = Qualifier.idEquals(boyd.getId());

        // not more than one id qualifier is allowed
        assertThatThrownBy(() -> repository.findUsingQuery(new Query(Qualifier.and(keyEqCartersId,
                keyEqBoydsId))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expecting not more than one id qualifier in qualifiers array, got 2");

        // not more than one id qualifier is allowed
        assertThatThrownBy(() -> repository.findUsingQuery(new Query(Qualifier.and(keyEqCartersId,
                keyEqBoydsId))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expecting not more than one id qualifier in qualifiers array, got 2");

        // not more than one id qualifier is allowed
        assertThatThrownBy(() -> repository.findUsingQuery(new Query(Qualifier.or(keyEqCartersId, keyEqBoydsId))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expecting not more than one id qualifier in qualifiers array, got 2");
    }

    @Test
    void mapValuesTest() {
        String keyExactMatch = "key2";
        String valueToSearch = "val1";
        assertThat(donny.getStringMap().get("key1")).isEqualTo(valueToSearch);
        assertThat(boyd.getStringMap().get("key1")).isEqualTo(valueToSearch);

        Qualifier stringMapValuesContainString = Qualifier.builder()
                .setPath("stringMap")
                .setFilterOperation(FilterOperation.MAP_VALUES_CONTAIN)
                .setValue(valueToSearch)
                .build();
        assertThat(repository.findUsingQuery(new Query(stringMapValuesContainString))).containsOnly(donny, boyd);

        int valueToSearchLessThan = 100;
        assertThat(carter.getIntMap().get(keyExactMatch)).isLessThan(valueToSearchLessThan);

        // it cannot be easily combined using boolean logic
        // because in fact it is a "less than" Exp that uses the result of another Exp "MapExp.getByKey"
        Qualifier intMapWithExactKeyAndValueLt100 = Qualifier.builder()
                .setPath("intMap." + keyExactMatch) // Map bin name
                .setFilterOperation(FilterOperation.MAP_VAL_LT_BY_KEY)
                .setValue(valueToSearchLessThan) // Map value to compare with
                .build();
        assertThat(repository.findUsingQuery(new Query(intMapWithExactKeyAndValueLt100))).containsOnly(carter);
    }
}

