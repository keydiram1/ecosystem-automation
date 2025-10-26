package api.petstore.utils;

import api.petstore.PetApi;
import api.petstore.StoreApi;
import api.petstore.UserApi;

public final class PetUtils {
    private PetUtils() {}

    public static void tryDeleteData(long petId, long orderId, String username) {
        var pets   = new PetApi();
        var store  = new StoreApi();
        var users  = new UserApi();

        if (username != null && !username.isBlank()) {
            try { users.deleteUser(username); } catch (Exception ignored) {}
        }
        if (petId > 0) {
            try { pets.deletePet(petId); } catch (Exception ignored) {}
        }
        if (orderId > 0) {
            try { store.deleteOrder(orderId); } catch (Exception ignored) {}
        }
    }
}
