package api.petstore;

import api.petstore.dto.Pet;
import api.petstore.utils.PetStoreRunner;
import io.restassured.response.Response;

import java.util.concurrent.ThreadLocalRandom;

public class PetApi {
    private final RestClient restClient;

    public PetApi() {
        this.restClient = PetStoreRunner.CLIENT;
    }

    public Pet createPet(Pet pet) {
        return restClient.post("/pet", pet, Pet.class);
    }

    public Pet createPet(String name) {
        Pet p = Pet.builder()
                .name(name)
                .photoUrl("https://example.com/1")
                .build();
        return createPet(p);
    }

    public Pet getPetById(long petId) {
        return restClient.get("/pet/" + petId, Pet.class);
    }

    public Pet updatePet(Pet pet) {
        return restClient.put("/pet", pet, Pet.class);
    }

    public boolean deletePet(long petId) {
        // treat 200/204/404 as ok for the public demo
        return restClient.deleteOk("/pet/" + petId, 200, 204, 404);
    }

    public boolean petExists(long petId) {
        Response r = restClient.getRaw("/pet/" + petId);
        if (r.statusCode() != 200) return false;
        String body = r.asString();
        if (body.isBlank() || "{}".equals(body)) return false;
        if (body.contains("\"name\":\"doggie\"")) return false;
        if (body.contains("\"photoUrls\":[\"string\"]")) return false;
        return true;
    }

    public long getRandomPetId() {
        return Math.abs(ThreadLocalRandom.current().nextLong(1_000_000_000L));
    }
}
