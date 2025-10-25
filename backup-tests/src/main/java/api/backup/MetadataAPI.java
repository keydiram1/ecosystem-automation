package api.backup;

import api.RestUtils;
import com.aerospike.client.Key;
import com.google.common.base.Preconditions;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.experimental.UtilityClass;
import org.apache.http.HttpStatus;

import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@UtilityClass
public class MetadataAPI {

    public static List<Long> readBackupTimestampsForKey(String backupName, long from, long to, Key key) {
        String digest = HexFormat.of().formatHex(key.digest);
        Map<String, List<Long>> allTimestamps = readBackupTimestampsForKeys(backupName, from, to, key);
        return allTimestamps.get(digest);
    }

    public static Map<String, List<Long>> readBackupTimestampsForKeys(String backupName, long from, long to, Key... keys) {
        String urlSuffix = "/v1/metadata/backup";
        final Object[] digests = Stream.of(keys)
                .map(key -> HexFormat.of().formatHex(key.digest))
                .toArray(Object[]::new);
        RequestSpecification request = RestAssured.given(BackupBaseRequests.getBaseRequestSpec(urlSuffix))
                .queryParam("continuousBackup", backupName)
                .queryParam("from", from)
                .queryParam("to", to)
                .queryParam("srcDigest", digests);
        Response response = RestUtils.printRequest(request).get();
        RestUtils.printResponse(response, "readBackupTimestamps");
        Preconditions.checkState(response.getStatusCode() == HttpStatus.SC_OK, response.prettyPrint());
        return response.body().as(new TypeRef<>() {
        });
    }

    public static Response deleteBackupData(String continuousBackupName, String set, long fromTime, long toTime) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("continuousBackupName", continuousBackupName);
        body.put("set", set);
        body.put("fromTime", fromTime);
        body.put("toTime", toTime);
        String urlSuffix = "/v1/metadata/delete";
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec(urlSuffix)).when().body(body)).post();
        RestUtils.printResponse(response, "deleteBackupData");
        // We need sleep because this endpoint is asynchronous. It will be better you use wait till the job ends
        // like in the method restoreNamespace. Due to bug in RestAssured, the deleteBackupData doesn't return the
        // job id in the response body. If we want to use this method in performance test, the sleep will not
        // be enough. We will need to implement it with OkHttp. With OkHttp we get the job id in the response.
        return response;
    }
}
