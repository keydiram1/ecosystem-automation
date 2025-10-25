package api.springData.convert;

import api.springData.BaseBlockingIntegrationTests;
import api.springData.sample.Address;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns16"})
@Tag("SPRING-DATA-TESTS-1")
class AerospikeReadDataIntegrationTests extends BaseBlockingIntegrationTests {

    long longId = 10L;
    String name = "John";
    int age = 74;
    Map<Integer, String> map = Map.of(10, "100");
    Address address = new Address("Street", 20, "ZipCode", "City");
    Map<String, Object> addressMap = Map.of(
        "street", address.getStreet(),
        "apartment", address.getApartment(),
        "zipCode", address.getZipCode(),
        "city", address.getCity());

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    static class User {

        @Id
        long id;
        String name;
        int age;
        Map<Integer, String> map;
        Address address;
    }

    @Test
    void readDocumentWithLongId() {
        template.getAerospikeClient().put(null,
                new Key(namespace, template.getSetName(User.class), longId),
                new Bin("name", name),
                new Bin("age", 74),
                new Bin("map", map),
                new Bin("address", addressMap)
        );
        // we can read the record into a User document because its class is given
        List<User> users = template.findAll(User.class).toList();
        User user;
        if (template.getAerospikeConverter().getAerospikeDataSettings().isKeepOriginalKeyTypes()) {
            // we need isKeepOriginalKeyTypes == true because id is of type long, otherwise findById() returns null
            // isKeepOriginalKeyTypes parameter would be unimportant if id were of type String
            user = template.findById(longId, User.class);
            assertThat(users.get(0).getId()).isEqualTo(user.getId());
            assertThat(users.get(0).getName()).isEqualTo(user.getName());
        } else {
            user = users.get(0);
        }
        assertThat(user.getId()).isEqualTo(longId);
        assertThat(user.getName()).isEqualTo(name);
        assertThat(user.getAge()).isEqualTo(age);
        assertThat(user.getMap()).isEqualTo(map);
        assertThat(user.getAddress()).isEqualTo(address);
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    static class Document {

        @Id
        String id;
        int number;
    }

    @Test
    void readLongIdAsString() {
        template.getAerospikeClient().put(null,
                new Key(namespace, template.getSetName(Document.class), longId),
                new Bin("number", age)
        );
        // we can read the record into a Document because its class is given
        List<Document> users = template.findAll(Document.class).toList();
        Document document;
        if (template.getAerospikeConverter().getAerospikeDataSettings().isKeepOriginalKeyTypes()) {
            // original id has type long
            document = template.findById(longId, Document.class);
            assertThat(users.get(0).getId()).isEqualTo(document.getId());
            assertThat(users.get(0).getNumber()).isEqualTo(document.getNumber());
        } else {
            document = users.get(0);
        }
        assertThat(document.getId()).isEqualTo(String.valueOf(longId));
        assertThat(document.getNumber()).isEqualTo(age);
    }
}
