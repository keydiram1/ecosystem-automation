package api.springData.repository.query.blocking.noindex;

import api.springData.AsCollections;
import api.springData.BaseBlockingIntegrationTests;
import api.springData.sample.Address;
import api.springData.sample.Person;
import api.springData.sample.PersonNegativeTestsRepository;
import api.springData.sample.PersonRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static api.springData.sample.Person.Gender.FEMALE;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PersonRepositoryQueryTests extends BaseBlockingIntegrationTests {

    protected static final Person dave = Person.builder()
            .id("IDpersonRepositoryQueryTests1")
            .firstName("Dave")
            .lastName("Matthews")
            .age(42)
            .strings(List.of("str0", "str1", "str2"))
            .address(new Address("Foo Street 1", 1, "C0123", "Bar"))
            .build();
    protected static final Person donny = Person.builder()
            .id("IDpersonRepositoryQueryTests2")
            .firstName("Donny")
            .lastName("Macintire")
            .age(39)
            .strings(List.of("str1", "str2", "str3"))
            .stringMap(AsCollections.of("key1", "val1"))
            .build();
    protected static final Person oliver = Person.builder()
            .id("IDpersonRepositoryQueryTests3")
            .firstName("Oliver August")
            .lastName("Matthews")
            .age(14)
            .ints(List.of(425, 550, 990))
            .build();
    protected static final Person alicia = Person.builder()
            .id("IDpersonRepositoryQueryTests4")
            .firstName("Alicia")
            .lastName("Keys")
            .gender(FEMALE)
            .age(30)
            .ints(List.of(550, 600, 990))
            .build();
    protected static final Person carter = Person.builder()
            .id("IDpersonRepositoryQueryTests5")
            .firstName("Carter")
            .lastName("Beauford")
            .age(49)
            .intMap(AsCollections.of("key1", 0, "key2", 1))
            .address(new Address("Foo Street 2", 2, "C0124", "C0123"))
            .build();
    protected static final Person boyd = Person.builder()
            .id("IDpersonRepositoryQueryTests6")
            .firstName("Boyd")
            .lastName("Tinsley")
            .age(45)
            .stringMap(AsCollections.of("key1", "val1", "key2", "val2"))
            .address(new Address(null, null, null, null))
            .build();
    protected static final Person stefan = Person.builder()
            .id("IDpersonRepositoryQueryTests7")
            .firstName("Stefan")
            .lastName("Lessard")
            .age(34)
            .byteArray(new byte[]{1, 0, 1, 1, 0, 0, 0, 1})
            .build();
    protected static final Person leroi = Person.builder()
            .id("IDpersonRepositoryQueryTests8")
            .firstName("Leroi")
            .lastName("Moore")
            .age(44)
            .intArray(new int[]{5, 6, 7, 8, 9, 10})
            .build();
    protected static final Person leroi2 = Person.builder()
            .id("IDpersonRepositoryQueryTests9")
            .firstName("Leroi")
            .lastName("Moore")
            .age(25)
            .build();
    protected static final Person matias = Person.builder()
            .id("IDpersonRepositoryQueryTests10")
            .firstName("Matias")
            .lastName("Craft")
            .age(24)
            .intArray(new int[]{1, 2, 3, 4, 5})
            .build();
    protected static final Person douglas = Person.builder()
            .id("IDpersonRepositoryQueryTests11")
            .firstName("Douglas")
            .lastName("Ford")
            .age(25)
            .build();
    protected final Person david = Person.builder()
            .id("IDpersonRepositoryQueryTests12")
            .firstName(" דוד!@#")
            .lastName("SomeLastName")
            .age(20)
            .gender(Person.Gender.MALE)
            .strings(List.of("!@#$", " ", "AaBbCc", "אבגד", "Aa Bb"))
            .intArray(new int[]{0, -1, -2, -3, 2_147_483_647, -2_147_483_648})
            .stringMap(AsCollections.of("keySpecial", "!@#", "keyHebrew", "ערך"))
            .listOfBoolean(List.of(true, false, true))
            .mapOfBoolean(Map.of("bool1", true, "bool2", false))
            .regDate(LocalDate.now())
            .intSet(Set.of(0, 2_147_483_647, -2_147_483_648))
            .address(new Address("SomeStreet 123", 123, "12345", "SomeCity"))
            .addressesList(List.of(new Address("Street1", 1, "12345", "City1"), new Address("Street2", 2, "54321", "City2")))
            .addressesMap(Map.of("home", new Address("HomeStreet", 10, "12345", "HomeCity"), "work", new Address("WorkStreet", 20, "54321", "WorkCity")))
            .showSize(41)
            .nickname("dood")
            .build();

    protected final List<Person> allPersons = List.of(dave, donny, oliver, alicia, carter, boyd, stefan,
            leroi, leroi2, matias, douglas, david);
    protected final List<Person> allPersonsWithSimilarIds = allPersons.stream()
            .filter(person -> person.getId().startsWith("IDpersonRepositoryQueryTests"))
            .toList();

    @Autowired
    protected PersonRepository<Person> repository;
    @Autowired
    protected PersonNegativeTestsRepository<Person> negativeTestsRepository;

    @BeforeAll
    void beforeAll() {
        //   additionalAerospikeTestOperations.deleteAllAndVerify(Person.class);
        additionalAerospikeTestOperations.saveAll(repository, allPersons);
    }
}
