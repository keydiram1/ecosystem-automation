package utils;

import lombok.SneakyThrows;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PhaserWaitGroup {

    private static final Map<String, PhaserWaitGroup> pool = new ConcurrentHashMap<>();

    public final AtomicInteger numberOfArrivedParties = new AtomicInteger(0);

    private final Phaser phaser;
    private final String testGroup;

    public PhaserWaitGroup(String testGroupName) {
        phaser = new Phaser();
        testGroup = testGroupName;
    }

    public static PhaserWaitGroup singleton(String testGroupName) {
        return pool.computeIfAbsent(testGroupName, PhaserWaitGroup::new);
    }

    public void register() {
        AerospikeLogger.info("Registering a %s test class in the phaser.".formatted(testGroup));
        phaser.register();
        numberOfArrivedParties.incrementAndGet();
    }

    public void arrive() {
        numberOfArrivedParties.decrementAndGet();
    }

    @SneakyThrows
    public void wait(int timeoutMinutes) {
        AerospikeLogger.info("Waiting for all %s test classes.".formatted(testGroup));
        AerospikeLogger.info("Number of registered %s classes: %d".formatted(testGroup, phaser.getRegisteredParties()));
        AerospikeLogger.info("Number of arrived %s classes: %d".formatted(testGroup, phaser.getArrivedParties()));
        phaser.awaitAdvanceInterruptibly(phaser.arrive(), timeoutMinutes, TimeUnit.MINUTES);
        AerospikeLogger.info("All %s tests have reached the barrier.".formatted(testGroup));
    }
}
