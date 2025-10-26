package api.petstore;

import api.petstore.dto.ApiResponse;
import api.petstore.dto.User;
import api.petstore.utils.PetStoreRunner;
import io.restassured.response.Response;
import org.apache.commons.lang3.RandomStringUtils;

public class UserApi {
    private final RestClient restClient;

    public UserApi() {
        this.restClient = PetStoreRunner.CLIENT;
    }

    public ApiResponse createUser(User user) {
        return restClient.post("/user", user, ApiResponse.class);
    }

    public User createUser(String username) {
        User u = new User();
        u.setUsername(username);
        restClient.post("/user", u, ApiResponse.class);   // POST
        return restClient.get("/user/" + username, User.class); // GET -> return created
    }

    public User getUserByUsername(String username) {
        return restClient.get("/user/" + username, User.class);
    }

    public User updateUser(String username, User user) {
        return restClient.put("/user/" + username, user, User.class);
    }

    public boolean isUserExists(String username) {
        Response r = restClient.getRaw("/user/" + username);
        return r.statusCode() == 200;
    }

    public boolean deleteUser(String username) {
        return restClient.deleteOk("/user/" + username, 200, 204, 404);
    }

    public String getRandomUsername() {
        return "username" + RandomStringUtils.randomAlphanumeric(8);
    }
}
