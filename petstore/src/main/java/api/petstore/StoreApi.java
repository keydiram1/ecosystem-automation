package api.petstore;

import api.petstore.dto.Order;
import api.petstore.utils.PetStoreRunner;

import java.util.concurrent.ThreadLocalRandom;

public class StoreApi {
    private final RestClient restClient;

    public StoreApi() {
        this.restClient = PetStoreRunner.CLIENT;
    }

    public Order placeOrder(Order order) {
        return restClient.post("/store/order", order, Order.class);
    }

    public Order getOrderById(long orderId) {
        return restClient.get("/store/order/" + orderId, Order.class);
    }

    public boolean deleteOrder(long orderId) {
        return restClient.deleteOk("/store/order/" + orderId, 200, 204, 404);
    }

    public long getRandomOrderId() {
        return Math.abs(ThreadLocalRandom.current().nextLong(1_000_000_000L));
    }
}
