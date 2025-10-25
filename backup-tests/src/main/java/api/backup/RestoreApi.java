package api.backup;

import api.RestUtils;
import api.backup.dto.BackgroundJob;
import api.backup.dto.RestoreRecordsRequest;
import api.backup.dto.RestoreSetRequest;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.experimental.UtilityClass;
import org.awaitility.Awaitility;
import utils.AerospikeLogger;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import static api.backup.dto.BackgroundJob.BackgroundJobStatus.*;

@UtilityClass
public class RestoreApi {

    public static BackgroundJob restoreSet(RestoreSetRequest request, long minutesToWait) {
        Response response = restore("/v1/restore/set").body(request).post();
        String restoreId = response.body().asString();
        Callable<BackgroundJob> checkJobStatus = () -> JobAPI.getJob(restoreId);
        Predicate<BackgroundJob> isDone = job -> job.getStatus() == DONE;

        return Awaitility.waitAtMost(Duration.ofMinutes(minutesToWait))
                .pollInterval(Duration.ofSeconds(1))
                .until(checkJobStatus, isDone);
    }

    public static BackgroundJob restoreSet(RestoreSetRequest request) {
        return restoreSet(request, 2);
    }

    public static Response restoreSetResponse(RestoreSetRequest request) {
        Response response = restore("/v1/restore/set").body(request).post();
        RestUtils.printResponse(response, "restore");
        return response;
    }

    public static Response restoreRecordResponse(long initialTimeStamp, String digest, String setName, String srcClusterName,
                                                 String trgClusterName, String srcNS, String trgNS) {
        Response response = restore("/v1/restore/records").body(
                        RestoreRecordsRequest.builder()
                                .fromTime(0L)
                                .toTime(initialTimeStamp)
                                .srcClusterName(srcClusterName)
                                .trgClusterName(trgClusterName)
                                .srcNS(srcNS)
                                .trgNS(trgNS)
                                .set(setName)
                                .srcDigests(new String[]{digest})
                                .build())
                .post();
        RestUtils.printResponse(response, "restoreRecord");
        return response;
    }

    public static int restoreRecord(long initialTimeStamp, String digest, String setName, String srcClusterName, String trgClusterName,
                                    String srcNS, String trgNS) {
        Map<String, Object> bodyAsMap = getBodyAsMap(
                restoreRecordResponse(initialTimeStamp, digest, setName, srcClusterName, trgClusterName, srcNS, trgNS));
        return ((Number) bodyAsMap.get("success")).intValue();
    }

    public static Response restoreNamespaceResponse(long fromTime, long toTime, String srcClusterName, String trgClusterName,
                                                    String srcNS, String trgNS) {
        Response response = restore("/v1/restore/namespace").body(
                RestoreSetRequest.builder()
                        .fromTime(fromTime)
                        .toTime(toTime)
                        .srcClusterName(srcClusterName)
                        .trgClusterName(trgClusterName)
                        .srcNS(srcNS)
                        .trgNS(trgNS)
                        .build()

        ).post();
        RestUtils.printResponse(response, "restoreNamespace");
        return response;
    }

    public static BackgroundJob restoreNamespace(long fromTime, long initialTimeStamp, String srcClusterName,
                                                 String trgClusterName, String srcNS, String trgNS) {
        Response response = restore("/v1/restore/namespace").body(
                RestoreSetRequest.builder()
                        .fromTime(fromTime)
                        .toTime(initialTimeStamp)
                        .srcClusterName(srcClusterName)
                        .trgClusterName(trgClusterName)
                        .srcNS(srcNS)
                        .trgNS(trgNS)
                        .build()

        ).post();
        String restoreId = response.body().asString();

        Awaitility.await("Until restore is done")
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .failFast(() -> JobAPI.getJob(restoreId).getStatus().equals(FAILED))
                .until(() -> JobAPI.getJob(restoreId).getStatus().equals(DONE));

        return JobAPI.getJob(restoreId);
    }

    public static BackgroundJob restoreNamespace(long initialTimeStamp, String clusterName, String namespace) {
        return restoreNamespace(0L, initialTimeStamp, clusterName, clusterName, namespace, namespace);
    }

    public static RequestSpecification restore(String urlSuffix) {
        return RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec(urlSuffix)).log().all().when());
    }

    private static Map<String, Object> getBodyAsMap(Response response) {
        try {
            return response.body().as(new TypeRef<>() {
            });
        } catch (Exception e) {
            AerospikeLogger.info(e.getMessage());
            return Collections.emptyMap();
        }
    }
}
