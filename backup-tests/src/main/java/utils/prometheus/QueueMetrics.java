package utils.prometheus;

import java.util.Map;

public class QueueMetrics {
    public final long backup_records_total;
    public final long backup_timer_seconds_duration_sum;
    public final long recover_records_total;
    public final long backup_run_total;
    public final long recover_run_total;

    QueueMetrics(Map<String, Double> data) {
        backup_records_total = data.getOrDefault("backup_records_total", -1.0).longValue();
        backup_timer_seconds_duration_sum = data.getOrDefault("backup_timer_seconds_duration_sum", -1.0).longValue();
        recover_records_total = data.getOrDefault("recover_records_total", -1.0).longValue();
        backup_run_total = data.getOrDefault("backup_run_total", -1.0).longValue();
        recover_run_total = data.getOrDefault("recover_run_total", -1.0).longValue();
    }
}
