package api.abs;

import api.abs.generated.ApiClient;
import api.abs.generated.api.BackupApi;
import api.abs.generated.api.ConfigurationApi;
import api.abs.generated.api.RestoreApi;
import api.abs.generated.api.SystemApi;
import lombok.experimental.UtilityClass;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import utils.AerospikeLogger;
import utils.ConfigParametersHandler;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@UtilityClass
public class API {
    static final OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                StringBuilder log = new StringBuilder();
                Request request = chain.request();
                log.append("Request: ").append(request);
                if (request.body() != null && request.body().contentLength() > 0) {
                    Buffer buffer = new Buffer();
                    request.body().writeTo(buffer);
                    log.append("\nRequest body: ").append(buffer.readUtf8());
                }
                Instant before = Instant.now();
                try (Response response = chain.proceed(request)) {
                    ResponseBody responseBody = response.body();
                    BufferedSource source = responseBody.source();
                    source.request(Long.MAX_VALUE); // Buffer the entire body.
                    Buffer buffer = source.getBuffer();
                    byte[] responseBytes = buffer.clone().readByteArray();

                    log.append("\nResponse header: ").append(response.headers());
                    if (responseBytes.length > 0) {
                        String mediaType = responseBody.contentType().toString();
                        if (mediaType.contains("application/json")) {
                            // It's JSON, print the full body
                            String responseBodyString = new String(responseBytes, StandardCharsets.UTF_8);
                            log.append("Response body: ").append(responseBodyString).append("\n");
                        }
                    }
                    return response.newBuilder()
                            .body(ResponseBody.create(responseBody.contentType(), responseBytes))
                            .headers(response.headers())
                            .build();
                } catch (Exception e) {
                    log.append("\nRequest error: ").append(e.getMessage());
                    throw new RuntimeException("Network request failed", e);
                } finally {
                    log.append("Duration: ").append(Duration.between(before, Instant.now()));
                    AerospikeLogger.info(log.toString(), true);
                }
            })
            .build();

    static final int ABS_CLIENT_TIMEOUT =
            "LOCAL".equalsIgnoreCase(ConfigParametersHandler.getParameter("STORAGE_PROVIDER"))
                    ? 30_000
                    : 180_000;

    static final ApiClient apiClient = new ApiClient(client).setBasePath(ConfigParametersHandler.getParameter("BACKUP_SERVICE_URL")).setReadTimeout(ABS_CLIENT_TIMEOUT)
            .setWriteTimeout(ABS_CLIENT_TIMEOUT);
    static final ConfigurationApi configurationApi = new ConfigurationApi(apiClient);
    static final SystemApi systemApi = new SystemApi(apiClient);
    static final BackupApi backupApi = new BackupApi(apiClient);
    static final RestoreApi restoreApi = new RestoreApi(apiClient);
}