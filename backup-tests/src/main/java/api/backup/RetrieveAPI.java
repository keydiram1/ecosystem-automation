package api.backup;

import api.RestUtils;
import api.backup.dto.RetrieveEntityRecord;
import com.google.common.base.Preconditions;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.experimental.UtilityClass;
import org.apache.http.HttpStatus;

import java.util.List;

@UtilityClass
public class RetrieveAPI {

    public static List<RetrieveEntityRecord> retrieve(WhatToRetrieve whatToRetrieve,
                                                      long toTime, String digest,
                                                      String srcClusterName,
                                                      String srcNS, String set) {
        return retrieve(whatToRetrieve, 0, toTime, digest, srcClusterName, srcNS, set, true);
    }

    public static List<RetrieveEntityRecord> retrieve(WhatToRetrieve whatToRetrieve,
                                                      long toTime, String digest,
                                                      String srcClusterName,
                                                      String srcNS, String set,
                                                      boolean debugPrint) {
        return retrieve(whatToRetrieve, 0, toTime, digest, srcClusterName, srcNS, set, debugPrint);
    }

    public static List<RetrieveEntityRecord> retrieve(WhatToRetrieve whatToRetrieve,
                                                      long fromTime, long toTime,
                                                      String digest, String srcClusterName,
                                                      String srcNS, String set) {
        return retrieve(whatToRetrieve, fromTime, toTime, digest, srcClusterName, srcNS, set, true);
    }

    public static List<RetrieveEntityRecord> retrieve(WhatToRetrieve whatToRetrieve,
                                                      long fromTime, long toTime,
                                                      String digest, String srcClusterName,
                                                      String srcNS, String set, boolean debugPrint) {
        Response response = retrieveResponse(whatToRetrieve, fromTime,  toTime, digest, srcClusterName, srcNS, set, debugPrint);
        Preconditions.checkState(response.getStatusCode() == HttpStatus.SC_OK, response.asPrettyString());
        return response.body().as(new TypeRef<>() {});
    }

    public static Response retrieveResponse(WhatToRetrieve whatToRetrieve, long toTime, String digest, String srcClusterName, String srcNS, String set) {
        return retrieveResponse(whatToRetrieve, 0, toTime, digest, srcClusterName, srcNS, set, true);
    }

    public static Response retrieveResponse(WhatToRetrieve whatToRetrieve,
                                            long fromTime, long toTime,
                                            String digest, String srcClusterName,
                                            String srcNS,
                                            String set, boolean debugPrint) {
        String urlSuffix = "/v1/retrieve/" + whatToRetrieve;

        RequestSpecification baseRequestSpec = BackupBaseRequests.getBaseRequestSpec(urlSuffix);

        RequestSpecification requestSpec = RestAssured.given(baseRequestSpec).when().queryParam("srcClusterName", srcClusterName)
                .queryParam("srcDigest", digest).queryParam("fromTime", fromTime).queryParam("set", set)
                .queryParam("srcNS", srcNS).queryParam("toTime", toTime);

        Response response = requestSpec.get();
        if (debugPrint) {
            RestUtils.printRequest(requestSpec);
            RestUtils.printResponse(response, "retrieve");
        }
        return response;
    }

    public enum WhatToRetrieve {

        LATEST("latest"),
        ALL("all");

        private final String data;

        WhatToRetrieve(final String data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return data;
        }
    }
}
