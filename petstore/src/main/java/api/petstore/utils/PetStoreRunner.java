package api.petstore.utils;

import api.petstore.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class PetStoreRunner {
    public static final String PET_STORE_BASE_URL;
    public static final RestClient CLIENT;

    // Load .env -> system props once per JVM
    static {
        PropertiesHandler.addSystemProperties("../devops/install/petstore/.env");
        PET_STORE_BASE_URL = PropertiesHandler.getParameter("PETSTORE_BASE_URL");
        CLIENT = new RestClient(PET_STORE_BASE_URL);
    }

    @BeforeAll
    void startClassDriver() {
        System.out.println("before all tests");
    }

    @AfterAll
    void stopClassDriver() {
        System.out.println("After all tests");
    }
}
