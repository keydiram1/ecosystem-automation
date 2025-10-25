package utils.prometheus;

import java.util.Map;

public class BackendMetrics {
    public final long exception_aerospike_total;
    public final long exception_500_total;
    public final long queue_read_key_not_found_total;
    public final long queue_new;
    public final long queue_all;
    public final long queue_in_process;

    BackendMetrics(Map<String, Double> data) {
        exception_aerospike_total = data.getOrDefault("exception_aerospike_total", -1.0).longValue();
        exception_500_total = data.getOrDefault("exception_500_total", -1.0).longValue();
        queue_read_key_not_found_total = data.getOrDefault("queue_read_key_not_found_total", -1.0).longValue();
        queue_new = data.getOrDefault("queue_new", -1.0).longValue();
        queue_all = data.getOrDefault("queue_all", -1.0).longValue();
        queue_in_process = data.getOrDefault("queue_in_process", -1.0).longValue();
    }
}
