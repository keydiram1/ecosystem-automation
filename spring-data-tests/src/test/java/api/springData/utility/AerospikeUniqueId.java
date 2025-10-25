package api.springData.utility;

import java.util.concurrent.atomic.AtomicLong;

public class AerospikeUniqueId {

    private static final AtomicLong counter = new AtomicLong();

    public static String nextId() {
        return "as-" + counter.incrementAndGet();
    }
}
