package api.abs;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import utils.ConfigParametersHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class PrometheusClient {
    public static PrometheusClient prometheusClient
            = new PrometheusClient(ConfigParametersHandler.getParameter("BACKUP_SERVICE_URL"));

    private final String baseUrl;
    private final OkHttpClient client = new OkHttpClient();

    @SneakyThrows
    public Metrics fetch() {
        String metricsData = fetchMetricsData();
        return new Metrics(
                extractMetricValue(metricsData, "aerospike_backup_service_duration_millis"),
                extractMetricValue(metricsData, "aerospike_backup_service_failure_total"),
                extractMetricValue(metricsData, "aerospike_backup_service_incremental_duration_millis"),
                extractMetricValue(metricsData, "aerospike_backup_service_incremental_failure_total"),
                extractMetricValue(metricsData, "aerospike_backup_service_incremental_runs_total"),
                extractMetricValue(metricsData, "aerospike_backup_service_incremental_skip_total"),
                extractMetricValue(metricsData, "aerospike_backup_service_runs_total"),
                extractMetricValue(metricsData, "aerospike_backup_service_skip_total"),
                extractMetricValue(metricsData, "go_goroutines"),
                extractMetricValue(metricsData, "go_memstats_heap_alloc_bytes"),
                extractMetricValue(metricsData, "go_threads"),
                extractMetricValue(metricsData, "promhttp_metric_handler_requests_in_flight"),
                extractMetricValue(metricsData, "promhttp_metric_handler_requests_total"),
                extractBackupProgressMetrics(metricsData),
                extractMetricValue(metricsData, "aerospike_backup_service_restore_in_progress")
        );
    }

    @SneakyThrows
    private String fetchMetricsData() {
        Request request = new Request.Builder()
                .url(baseUrl + "/metrics")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            return response.body().string();
        }
    }

    private long extractMetricValue(String metrics, String metricName) {
        Pattern pattern = Pattern.compile(metricName + "\\s+(\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)");
        Matcher matcher = pattern.matcher(metrics);
        if (matcher.find()) {
            String value = matcher.group(1);
            if (value.contains("e") || value.contains("E")) {
                // Handle scientific notation
                return Math.round(Double.parseDouble(value));
            } else {
                // Handle regular integer
                return Long.parseLong(value.split("\\.")[0]);
            }
        }
        return 0;
    }

    private Map<String, BackupProgress> extractBackupProgressMetrics(String metricsData) {
        Pattern pattern = Pattern.compile(
                "aerospike_backup_service_backup_progress_pct\\{routine=\"(.*?)\",type=\"(.*?)\"\\}\\s+(\\d+(?:\\.\\d+)?)"
        );
        Matcher matcher = pattern.matcher(metricsData);
        Map<String, BackupProgress> backupProgressMetrics = new HashMap<>();

        while (matcher.find()) {
            String routine = matcher.group(1);
            BackupType type = BackupType.fromString(matcher.group(2));
            long value = Math.round(Double.parseDouble(matcher.group(3)));

            BackupProgress currentProgress = backupProgressMetrics.getOrDefault(routine, new BackupProgress(0, 0));

            BackupProgress updatedProgress = switch (type) {
                case FULL -> new BackupProgress(value, currentProgress.incremental());
                case INCREMENTAL -> new BackupProgress(currentProgress.full(), value);
            };

            backupProgressMetrics.put(routine, updatedProgress);
        }

        return backupProgressMetrics;
    }
}
