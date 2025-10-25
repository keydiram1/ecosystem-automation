package utils.prometheus;

import api.backup.BackupBaseRequests;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class PrometheusReader {

    public QueueMetrics readQueueHandlerMetrics() {
        Response response = RestAssured.given(BackupBaseRequests.getQueueHandlerBaseRequest("/prometheus")).get();
        return new QueueMetrics(responseToMap(response));
    }

    public BackendMetrics readBackendMetrics() {
        Response response = RestAssured.given(BackupBaseRequests.getBaseRequestSpec("/actuator/prometheus")).get();
        return new BackendMetrics(responseToMap(response));
    }

    private Map<String, Double> responseToMap(Response response) {
        String content = response.asString();
        return Arrays.stream(content.split("\n"))
                .filter(it -> !it.startsWith("#"))
                .map(line -> line.split(" "))
                .filter(it -> it.length == 2)
                .collect(Collectors.toMap(s -> s[0], s -> Double.valueOf(s[1])));
    }
}
