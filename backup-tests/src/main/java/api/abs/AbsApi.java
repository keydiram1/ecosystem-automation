package api.abs;

import api.abs.generated.ApiResponse;
import lombok.experimental.UtilityClass;

import static api.abs.API.systemApi;

@UtilityClass
public class AbsApi {

    public static String getAbsVersion() {
        return systemApi.version();
    }

    public static ApiResponse<String> getApiDocs() {
        return systemApi.apiDocsWithHttpInfo();
    }
}
