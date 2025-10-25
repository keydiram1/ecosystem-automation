package api.abs;

import api.abs.generated.ApiResponse;
import api.abs.generated.model.DtoStorage;
import lombok.experimental.UtilityClass;

import java.util.Map;

import static api.abs.API.configurationApi;

@UtilityClass
public class AbsStorageApi {
    public static Map<String, DtoStorage> getAllStorage() {
        return configurationApi.readAllStorage();
    }

    public static DtoStorage getStorage(String storageName) {
        return configurationApi.readStorage(storageName);
    }

    public static ApiResponse<Void> updateStorage(String name, DtoStorage storage) {
        return configurationApi.updateStorageWithHttpInfo(name, storage);
    }

    public static ApiResponse<Void> createStorage(String name, DtoStorage storage) {
        return configurationApi.addStorageWithHttpInfo(name, storage);
    }

    public static ApiResponse<Void> deleteStorage(String name) {
        return configurationApi.deleteStorageWithHttpInfo(name);
    }

}