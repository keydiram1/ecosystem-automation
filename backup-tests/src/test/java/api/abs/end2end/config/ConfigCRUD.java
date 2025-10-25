package api.abs.end2end.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import utils.abs.AbsRunner;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConfigCRUD extends AbsRunner {
     static final Lock CONFIG_TESTS_LOCK = new ReentrantLock();

    @BeforeAll
    static void beforeAll() {
        CONFIG_TESTS_LOCK.lock();
    }

    @AfterAll
    static void afterAll() {
        CONFIG_TESTS_LOCK.unlock();
    }

}
