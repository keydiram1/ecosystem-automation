package api.abs;

import api.abs.generated.ApiResponse;
import api.abs.generated.model.DtoBackupPolicy;
import lombok.experimental.UtilityClass;

import java.util.Map;

import static api.abs.API.configurationApi;

@UtilityClass
public class AbsPolicyApi {

    public static Map<String, DtoBackupPolicy> getAllPolicies() {
        return configurationApi.readPolicies();
    }

    public static DtoBackupPolicy getPolicy(String name) {
        return configurationApi.readPolicy(name);
    }

    public static ApiResponse<Void> createPolicy(String name, DtoBackupPolicy policy) {
        return configurationApi.addPolicyWithHttpInfo(name, policy);
    }

    public static ApiResponse<Void> updatePolicy(String name, DtoBackupPolicy policy) {
        return configurationApi.updatePolicyWithHttpInfo(name, policy);
    }

    public static ApiResponse<Void> deletePolicy(String name) {
        return configurationApi.deletePolicyWithHttpInfo(name);
    }
}
