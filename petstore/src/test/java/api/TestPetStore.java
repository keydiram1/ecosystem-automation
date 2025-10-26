package api;

import api.petstore.PetApi;
import api.petstore.StoreApi;
import api.petstore.UserApi;
import api.petstore.dto.ApiResponse;
import api.petstore.dto.Order;
import api.petstore.dto.Pet;
import api.petstore.dto.User;
import api.petstore.utils.PetStoreRunner;
import api.petstore.utils.PetUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestPetStore extends PetStoreRunner {

    private PetApi petApi;
    private StoreApi storeApi;
    private UserApi userApi;

    // Test data
    private long petId;
    private long orderId;
    private String username;

    @BeforeAll
    void beforeAll() {
        petApi = new PetApi();
        storeApi = new StoreApi();
        userApi = new UserApi();

        petId = petApi.getRandomPetId();
        orderId = petApi.getRandomPetId();
        username = userApi.getRandomUsername();
    }

    @AfterAll
    void afterAll() {
        PetUtils.tryDeleteData(petId, orderId, username);
    }

    @Test
    void createUser() {
        var newUser = User.builder()
                .username(username)
                .firstName("Ed")
                .lastName("Ram")
                .email("ed@example.com")
                .password("secret123")
                .phone("0500000000")
                .userStatus(1)
                .build();

        ApiResponse createResp = userApi.createUser(newUser);
        assertEquals(200, createResp.getCode());

        var fetched = userApi.getUserByUsername(username);
        assertEquals(username, fetched.getUsername());
        assertEquals("Ed", fetched.getFirstName());
        assertEquals("Ram", fetched.getLastName());
    }

    @Test
    void createPet() {
        // name + photoUrls are required by Petstore
        Pet pet = Pet.builder()
                .id(petId)
                .name("Rex")
                .photoUrls(List.of("https://example.com/1"))
                .status("available")
                .build();

        Pet created = petApi.createPet(pet);
        assertEquals(petId, created.getId());
        assertEquals("Rex", created.getName());

        Pet fetched = petApi.getPetById(petId);
        assertEquals("available", fetched.getStatus());
    }

    @Test
    void updatePet() {
        Pet pet = Pet.builder()
                .id(petId)
                .name("Rex")
                .photoUrls(List.of("https://example.com/1"))
                .status("pending")
                .build();

        Pet updated = petApi.updatePet(pet);
        assertEquals("pending", updated.getStatus());

        Pet fetched = petApi.getPetById(petId);
        assertEquals("pending", fetched.getStatus());
    }

    @Test
    void placeAndGetOrder() {
        var order = Order.builder()
                .id(orderId)
                .petId(petId)
                .quantity(1)
                .status("placed")
                .shipDate(Instant.now())
                .complete(true)
                .build();

        var placed = storeApi.placeOrder(order);
        assertEquals(orderId, placed.getId());
        assertEquals(petId, placed.getPetId());

        var fetched = storeApi.getOrderById(orderId);
        assertEquals(1, fetched.getQuantity());
        assertEquals("placed", fetched.getStatus());
    }

    @Test
    void deleteUser() {
        User user = userApi.createUser("deleteUser" + RandomStringUtils.randomAlphanumeric(8));

        assertTrue(userApi.deleteUser(user.getUsername()));
        assertFalse(userApi.isUserExists(user.getUsername()));
    }

    @Test
    void deletePet() throws InterruptedException {
        Pet pet = petApi.createPet("Rex" + RandomStringUtils.randomAlphanumeric(8));
        assertTrue(petApi.deletePet(pet.getId()));
        assertFalse(petApi.petExists(pet.getId()));
    }
}